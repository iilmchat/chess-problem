#!/usr/bin/env node
/**
 * Chess Problem — Lichess puzzle import pipeline (KI-008).
 *
 * The Lichess puzzle database (CC0 1.0, https://database.lichess.org) ships as
 * one big CSV (`lichess_db_puzzle.csv.zst`). This tool turns it into production
 * packs for `content/puzzles/*.json` in three steps:
 *
 *   1. filter  — stream the CSV, keep verified candidates per planned chapter,
 *                write a sorted NDJSON pool to tools/var/lichess-pool/.
 *   2. draft   — select candidates from the pool into a draft pack file under
 *                tools/var/import-drafts/ (machine fields filled, texts empty,
 *                ready for authoring).
 *   3. merge   — validate drafts, assign stable IDs, write the master pack
 *                files in content/puzzles/ and re-run the content validator.
 *   3b. split  — regenerate drafts from a master pack (before authoring new
 *                chapters), so that merges always cover the whole level.
 *   3c. author — inject authored texts (a sidecar keyed by lichess ID) into
 *                drafts, so machine fields stay untouched.
 *
 * Usage:
 *   node tools/import-lichess.mjs filter --input lichess_db_puzzle.csv
 *   node tools/import-lichess.mjs draft  --pool <ndjson> --level NOVICE --chapter mate-in-1
 *   node tools/import-lichess.mjs merge  --drafts "tools/var/import-drafts/*.draft.json"
 *   node tools/import-lichess.mjs split  --level NOVICE
 *   node tools/import-lichess.mjs author --texts tools/var/import-texts.json
 *
 * `filter` accepts plain CSV as well as `.csv.zst`. The published Lichess
 * archive contains concatenated/skippable frames that Node's zstd decoder
 * rejects; decompress with the `zstd` CLI first for reliability.
 *
 * Lichess CSV row layout (comma-separated, unquoted):
 *   PuzzleId,FEN,Moves,Rating,RatingDeviation,Popularity,NbPlays,Themes,
 *   GameUrl,OpeningTags,DailyDate
 *
 * FEN is the position *before* the opponent's setup move; Moves[0] is that
 * setup move (UCI); Moves[1..] is the full winning line from the player's
 * side. That maps 1:1 onto our pack format: `fen`, `setupMove`, `solution`.
 */

import { createReadStream, createWriteStream, mkdirSync } from "node:fs";
import { readFile, readdir, writeFile } from "node:fs/promises";
import { createInterface } from "node:readline";
import path from "node:path";
import { fileURLToPath } from "node:url";
import zlib from "node:zlib";
import { spawn } from "node:child_process";
import { Chess, validateFen } from "chess.js";

const ROOT = path.resolve(path.dirname(fileURLToPath(import.meta.url)), "..");
const VAR_DIR = path.join(ROOT, "tools", "var");
const POOL_DIR = path.join(VAR_DIR, "lichess-pool");
const DRAFT_DIR = path.join(VAR_DIR, "import-drafts");
const PUZZLES_DIR = path.join(ROOT, "content", "puzzles");
const SAMPLES_DIR = path.join(ROOT, "content", "samples");

const POOL_KEEP_PER_CHAPTER = 24; // candidates kept per chapter (spares included)
const TAKE_PER_CHAPTER = 10; // puzzles per chapter in this phase

// --- Chapter plan (v0.2.1: two chapters per level, see CONTENT_PLAN §3) ----
// Later phases append chapters here; PLAN_ORDER below controls pack order.

const CHAPTERS = {
  "novice.mate-in-1": {
    level: "NOVICE",
    chapter: "mate-in-1",
    rating: [600, 950],
    // Mate in one: Moves = [setup, winning move].
    userPlies: [1, 1],
    requireAny: ["mateIn1"],
    lastIsMate: true,
    captureQueen: false,
    primaryTag: "mateIn1",
  },
  "novice.capture-queen": {
    level: "NOVICE",
    chapter: "capture-queen",
    rating: [750, 1199],
    // Win the queen: usually [setup, capture], sometimes capture + convert.
    userPlies: [1, 2],
    requireAny: ["hangingPiece", "fork", "skewer", "capturingDefender", "deflection", "attraction", "trappedPiece"],
    lastIsMate: false,
    captureQueen: true,
    primaryTag: "captureQueen",
  },
  "professional.mate-in-2": {
    level: "PROFESSIONAL",
    chapter: "mate-in-2",
    rating: [1200, 1599],
    // Moves = [setup, us1, opp, us2 = mate].
    userPlies: [2, 2],
    requireAny: ["mateIn2"],
    lastIsMate: true,
    captureQueen: false,
    primaryTag: "mateIn2",
  },
  "professional.deflection": {
    level: "PROFESSIONAL",
    chapter: "deflection",
    rating: [1200, 1799],
    userPlies: [2, 2],
    requireAny: ["deflection"],
    lastIsMate: false,
    captureQueen: false,
    primaryTag: "deflection",
  },
  "grandmaster.quiet-move": {
    level: "GRANDMASTER",
    chapter: "quiet-move",
    rating: [1800, 2600],
    userPlies: [2, 4],
    requireAny: ["quietMove"],
    lastIsMate: false,
    captureQueen: false,
    primaryTag: "quietMove",
  },
  "grandmaster.deep-sacrifice": {
    level: "GRANDMASTER",
    chapter: "deep-sacrifice",
    rating: [1800, 2600],
    userPlies: [3, 5],
    requireAny: ["sacrifice"],
    lastIsMate: false,
    captureQueen: false,
    primaryTag: "deepSacrifice",
  },
};

const PLAN_ORDER = [
  ["NOVICE", ["mate-in-1", "capture-queen"]],
  ["PROFESSIONAL", ["mate-in-2", "deflection"]],
  ["GRANDMASTER", ["quiet-move", "deep-sacrifice"]],
];

const LEVEL_PREFIX = { NOVICE: "nov", PROFESSIONAL: "pro", GRANDMASTER: "grm" };

/** Zstd skippable frame (magic 0x184D2A50) at the head of Lichess exports. */
const SKIPPED_FRAME_BYTES = 12;

// Quality thresholds from CONTENT_PLAN §6.1.
const MIN_POPULARITY = 80;
const MIN_PLAYS = 1000;
const MAX_RATING_DEVIATION = 100;

// --- helpers ---------------------------------------------------------------

function fail(message) {
  console.error(`ERROR ${message}`);
  process.exitCode = 1;
}

function parseArgs(argv, named = []) {
  const args = {};
  for (let i = 0; i < argv.length; i += 1) {
    const arg = argv[i];
    if (!arg.startsWith("--")) continue;
    const name = arg.slice(2);
    if (named.includes(name) && argv[i + 1] !== undefined) {
      args[name] = argv[i + 1];
      i += 1;
    } else if (name.endsWith("=") || arg.includes("=")) {
      const [key, value] = arg.split("=", 2);
      args[key.slice(2)] = value;
    } else {
      args[name] = true;
    }
  }
  return args;
}

/** RFC-ish CSV split: handles quotes although Lichess rows are unquoted. */
function splitCsvLine(line) {
  const fields = [];
  let current = "";
  let inQuotes = false;
  for (let i = 0; i < line.length; i += 1) {
    const c = line[i];
    if (inQuotes) {
      if (c === '"') {
        if (line[i + 1] === '"') {
          current += '"';
          i += 1;
        } else {
          inQuotes = false;
        }
      } else {
        current += c;
      }
    } else if (c === '"' && current.length === 0) {
      inQuotes = true;
    } else if (c === ",") {
      fields.push(current);
      current = "";
    } else {
      current += c;
    }
  }
  fields.push(current);
  return fields.map((f) => f.trim());
}

function uciToMove(uci) {
  return {
    from: uci.slice(0, 2),
    to: uci.slice(2, 4),
    promotion: uci.length > 4 ? uci[4] : undefined,
  };
}

/** Play the full line (setup + solution) and return SAN for authoring. */
function playLine(fen, uciMoves) {
  const chess = new Chess(fen);
  const san = [];
  for (const uci of uciMoves) {
    const move = chess.move(uciToMove(uci));
    if (!move) return null;
    san.push(move.san);
  }
  return san;
}

/** After the setup move (index 0), player moves sit at odd indexes. */
function playerCapturesQueen(fen, moves) {
  const chess = new Chess(fen);
  for (let i = 0; i < moves.length; i += 1) {
    const move = chess.move(uciToMove(moves[i]));
    if (!move) return false;
    if (i % 2 === 1 && move.captured === "q") return true;
  }
  return false;
}

/** User moves = ceil(solution / 2), solution = Moves minus the setup move. */
function userPlies(moves) {
  return Math.ceil((moves.length - 1) / 2);
}

function expectedTime(rating, plies) {
  return Math.min(420, Math.max(60, Math.round((30 + 0.09 * rating + 12 * plies) / 5) * 5));
}

function ourThemes(chapter, lichessTags) {
  const our = new Set([chapter.primaryTag]);
  for (const map of CHAPTER_THEME_ALIASES) {
    const [lichTag, ourTag] = map;
    if (lichessTags.includes(lichTag)) our.add(ourTag);
  }
  for (const tag of lichessTags) {
    if (PASSTHROUGH_TAGS.includes(tag)) our.add(tag);
  }
  return [...our].slice(0, 5);
}

/** Lichess → our vocabulary (CONTENT_PLAN §5; validator theme regex). */
const CHAPTER_THEME_ALIASES = [
  ["capturingDefender", "removeDefender"],
  ["intermezzo", "zwischenzug"],
  ["xRayAttack", "xRay"],
  ["attraction", "decoy"],
];

/** Lichess tags we keep with the same name. */
const PASSTHROUGH_TAGS = [
  "mateIn1", "mateIn2", "mateIn3", "backRankMate", "smotheredMate",
  "doubleCheck", "discoveredAttack", "clearance", "hangingPiece", "fork",
  "pin", "skewer", "sacrifice", "deflection", "interference", "quietMove",
  "zugzwang", "trappedPiece", "promotion", "underPromotion", "enPassant",
  "endgame", "kingsideAttack", "queensideAttack", "exposedKing",
  "defensiveMove", "advancedPawn", "castling", "attackingF2F7",
];

function candidateScore(rec) {
  // Deterministic order: community approval first, then engagement reach.
  return rec.popularity + Math.min(50, rec.plays) / 20;
}

function verify(chapter, fen, moves) {
  const plies = userPlies(moves);
  if (plies < chapter.userPlies[0] || plies > chapter.userPlies[1]) {
    return { plies, reason: `plies ${plies} outside ${chapter.userPlies.join("-")}` };
  }
  const san = playLine(fen, moves);
  if (san === null) return { plies, reason: "illegal line" };
  const chess = new Chess(fen);
  for (const uci of moves) chess.move(uciToMove(uci));
  const mate = chess.isCheckmate();
  if (chapter.lastIsMate && !mate) {
    return { plies, san, mate, reason: "line does not end in mate" };
  }
  if (chapter.captureQueen && !playerCapturesQueen(fen, moves)) {
    return { plies, san, mate, reason: "queen is never captured by the player" };
  }
  return { plies, san, mate, reason: null };
}

// --- 1. filter -------------------------------------------------------------

async function cmdFilter(args) {
  const input = args.input;
  const out = args.out ?? path.join(POOL_DIR, "candidates.ndjson");
  if (!input) {
    fail("filter requires --input <lichess_db_puzzle.csv[.zst]>");
    return;
  }
  const started = process.hrtime.bigint();

  const pools = Object.values(CHAPTERS).map((chapter) => ({
    chapter,
    required: new Set(chapter.requireAny),
    list: [],
  }));
  const seenFens = new Set();
  const reasonCount = new Map();
  let read = 0;

  mkdirSync(path.dirname(out), { recursive: true });
  const dst = createWriteStream(out, "utf8");

  const rl = createInterface({
    input: input.endsWith(".zst")
      // The Lichess export starts with a 12-byte skippable frame that Node's
      // Zstd decoder rejects; skip it before piping into the decoder.
      ? createReadStream(input, { start: SKIPPED_FRAME_BYTES }).pipe(zlib.createZstdDecompress())
      : createReadStream(input),
    crlfDelay: Infinity,
  });

  rl.on("line", (line) => {
    read += 1;
    if (read === 1 || line.length === 0) return;
    const f = splitCsvLine(line);
    if (f.length < 10) return;
    const rating = Number(f[3]);
    if (!Number.isFinite(rating) || rating < 600 || rating > 2600) return;
    const popularity = Number(f[5]);
    const plays = Number(f[6]);
    if (!(popularity >= MIN_POPULARITY) || !(plays >= MIN_PLAYS)) return;
    const deviation = Number(f[4]);
    if (!(deviation <= MAX_RATING_DEVIATION)) return;
    const themes = f[7].split(" ").filter(Boolean);
    // Chapters whose cheap filters pass; costly verification runs once per row.
    const candidates = [];
    for (const pool of pools) {
      if (rating < pool.chapter.rating[0] || rating > pool.chapter.rating[1]) continue;
      if (!pool.required.size || !themes.some((tag) => pool.required.has(tag))) continue;
      candidates.push(pool);
    }
    if (candidates.length === 0) return;
    const fen = f[1];
    if (seenFens.has(fen) || validateFen(fen).ok === false) return;
    const moves = f[2].split(" ").filter(Boolean);
    if (moves.length < 2 || moves.length > 11) return;

    const verified = candidates.map((pool) => ({
      pool,
      result: verify(pool.chapter, fen, moves),
    }));
    const viable = verified.filter((v) => v.result.reason === null);
    if (viable.length === 0) {
      for (const { pool, result } of verified) {
        if (result.reason) {
          const key = `${pool.chapter.chapter}: ${result.reason}`;
          reasonCount.set(key, (reasonCount.get(key) ?? 0) + 1);
        }
      }
      return;
    }
    // Balancing rule: a puzzle joins the matching chapter with the least stock.
    viable.sort((a, b) => a.pool.list.length - b.pool.list.length);
    const { pool, result } = viable[0];
    pool.list.push(recordOf(pool.chapter, f, moves, result));
    seenFens.add(fen);
  });

  await new Promise((resolve, reject) => {
    rl.on("error", (err) => {
      dst.destroy();
      reject(err);
    });
    rl.on("close", () => {
      try {
        for (const pool of pools) {
          pool.list.sort((a, b) => candidateScore(b) - candidateScore(a));
          pool.list = pool.list.slice(0, POOL_KEEP_PER_CHAPTER);
          for (const rec of pool.list) dst.write(JSON.stringify(rec) + "\n");
        }
        dst.end(resolve);
      } catch (err) {
        dst.destroy();
        reject(err);
      }
    });
    dst.on("error", reject);
  });

  const seconds = Number(process.hrtime.bigint() - started) / 1e9;
  console.log(`Read ${input}`);
  console.log(`Rows processed: ${read}, kept: ${[...pools].reduce((sum, p) => sum + p.list.length, 0)}, time: ${seconds.toFixed(1)}s`);
  for (const pool of pools) {
    const status = pool.list.length >= TAKE_PER_CHAPTER ? "OK" : "SHORT";
    console.log(`  ${pool.chapter.chapter.padEnd(16)} ${String(pool.list.length).padStart(3)}  ${status}`);
  }
  const topReasons = [...reasonCount.entries()].sort((a, b) => b[1] - a[1]).slice(0, 10);
  if (topReasons.length > 0) {
    console.log("Top rejections:");
    for (const [reason, count] of topReasons) console.log(`  ${count}  ${reason}`);
  }
}

function recordOf(chapter, csvFields, moves, result) {
  return {
    level: chapter.level,
    chapter: chapter.chapter,
    lichessId: csvFields[0],
    fen: csvFields[1],
    setupMove: moves[0],
    solution: moves.slice(1),
    sanLine: result.san,
    plies: result.plies,
    mate: result.mate,
    rating: Number(csvFields[3]),
    ratingDeviation: Number(csvFields[4]),
    popularity: Number(csvFields[5]),
    plays: Number(csvFields[6]),
    themes: ourThemes(chapter, csvFields[7].split(" ")),
    lichessTags: csvFields[7],
    gameUrl: csvFields[8],
  };
}

// --- 2. draft --------------------------------------------------------------

async function cmdDraft(args) {
  const poolFile = args.pool ?? path.join(POOL_DIR, "candidates.ndjson");
  const level = args.level;
  const chapter = args.chapter;
  const take = Number(args.take ?? TAKE_PER_CHAPTER);
  const ids = typeof args.ids === "string" ? args.ids.split(",").filter(Boolean) : null;
  if (!level || !chapter) {
    fail("draft requires --level and --chapter");
    return;
  }

  const rows = [];
  for (const line of (await readFile(poolFile, "utf8")).split("\n")) {
    if (!line.trim()) continue;
    const rec = JSON.parse(line);
    if (rec.level === level && rec.chapter === chapter) rows.push(rec);
  }

  const picked = [];
  if (ids) {
    for (const id of ids) {
      const rec = rows.find((r) => r.lichessId === id);
      if (!rec) {
        fail(`id ${id} is not in the ${level}/${chapter} pool`);
        return;
      }
      picked.push(rec);
    }
  } else {
    picked.push(...rows.slice(0, take));
  }
  if (picked.length !== take) {
    fail(`only ${picked.length} candidate(s) for ${level}/${chapter}, need ${take}`);
    return;
  }

  mkdirSync(DRAFT_DIR, { recursive: true });
  const file = path.join(DRAFT_DIR, `${level.toLowerCase()}-${chapter}.draft.json`);
  await writeFile(file, JSON.stringify(draftPack(level, chapter, picked), null, 2) + "\n", "utf8");

  console.log(`Wrote ${path.relative(ROOT, file)}`);
  console.log("");
  console.log(`# ${level} / ${chapter} — authoring sheet`);
  for (const rec of picked) {
    console.log(`- lichessId=${rec.lichessId} rating=${rec.rating} plies=${rec.plies} tags=[${rec.lichessTags}]`);
    console.log(`    SAN ${rec.sanLine.join(" ")}`);
    console.log(`    FEN ${rec.fen} | setup ${rec.setupMove}`);
    console.log(`    https://lichess.org/training/${rec.lichessId}`);
  }
}

function draftPack(level, chapter, records) {
  return {
    schemaVersion: 1,
    contentVersion: "2026.10.2",
    level,
    chapter,
    puzzles: records.map((rec) => ({
      lichessId: rec.lichessId,
      fen: rec.fen,
      setupMove: rec.setupMove,
      solution: rec.solution,
      task: {
        type: rec.mate ? "MATE" : "WIN",
        plies: rec.plies,
      },
      rating: rec.rating,
      themes: rec.themes,
      timeLimitSec: expectedTime(rec.rating, rec.plies),
      gameUrl: rec.gameUrl,
      sanLine: rec.sanLine,
      // Texts to be authored before merge — the validator fails while empty.
      title: { ru: null, en: null },
      prompt: { ru: null, en: null },
      hints: [
        { tier: 1, ru: null, en: null },
        { tier: 2, ru: null, en: null },
        { tier: 3, ru: null, en: null },
      ],
      explanation: { ru: null, en: null },
      source: {
        provider: "lichess",
        id: rec.lichessId,
        url: `https://lichess.org/training/${rec.lichessId}`,
        license: "CC0-1.0",
      },
    })),
  };
}

// --- 2b. split (master back into drafts, for staged authoring) -------------

async function cmdSplit(args) {
  const level = args.level;
  if (!level) {
    fail("split requires --level");
    return;
  }
  const masterFile = path.join(PUZZLES_DIR, `${level.toLowerCase()}.json`);
  const master = JSON.parse(await readFile(masterFile, "utf8"));
  const byChapter = new Map();
  for (const puzzle of master.puzzles) {
    const chapter = puzzle.chapter;
    if (!byChapter.has(chapter)) byChapter.set(chapter, []);
    byChapter.get(chapter).push(puzzle);
  }
  for (const puzzle of master.puzzles) {
    puzzle.lichessId = puzzle.source.id;
  }
  for (const [chapter, puzzles] of byChapter) {
    const draft = draftPack(level, chapter, []);
    draft.puzzles = puzzles;
    mkdirSync(DRAFT_DIR, { recursive: true });
    const file = path.join(DRAFT_DIR, `${level.toLowerCase()}-${chapter}.draft.json`);
    await writeFile(file, JSON.stringify(draft, null, 2) + "\n", "utf8");
    console.log(`Wrote ${path.relative(ROOT, file)} (${puzzles.length} puzzles)`);
  }
}

// --- 2c. author ------------------------------------------------------------

/** Inject authored texts from a sidecar into drafts, keyed by lichess ID. */
async function cmdAuthor(args) {
  const textsFile = args.texts ?? path.join(VAR_DIR, "import-texts.json");
  const dir = path.resolve(ROOT, args.draftsDir ?? DRAFT_DIR);

  let sidecar;
  try {
    sidecar = JSON.parse(await readFile(textsFile, "utf8"));
  } catch (err) {
    fail(`cannot read texts sidecar ${relative(ROOT, textsFile)}: ${err.message}`);
    return;
  }

  const files = (await readdir(dir)).filter((name) => name.endsWith(".draft.json"));
  if (files.length === 0) {
    fail(`no drafts matched in ${relative(ROOT, dir)}`);
    return;
  }

  const issues = [];
  const applied = new Map(); // lichessId -> draft file
  const stillEmpty = new Map(); // draft -> missing ids
  const updated = [];
  for (const name of files) {
    const file = path.join(dir, name);
    const draft = JSON.parse(await readFile(file, "utf8"));
    const missing = [];
    let changed = false;
    for (const puzzle of draft.puzzles) {
      const lid = puzzle.lichessId ?? "?";
      const text = sidecar[lid];
      if (text) {
        const problem = validateAuthored(lid, text);
        if (problem) {
          issues.push(`${name}: ${lid} - ${problem}`);
          continue;
        }
        puzzle.title = text.title;
        puzzle.prompt = text.prompt;
        puzzle.hints = text.hints;
        puzzle.explanation = text.explanation;
        applied.set(lid, name);
        changed = true;
      } else {
        missing.push(lid);
      }
    }
    if (missing.length > 0) stillEmpty.set(name, missing);
    if (changed) updated.push({ file, draft });
  }

  const unknown = Object.keys(sidecar).filter((lid) => !applied.has(lid));
  for (const lid of unknown) issues.push(`texts for ${lid} do not match any draft puzzle`);

  await Promise.all(updated.map(({ file, draft }) =>
    writeFile(file, JSON.stringify(draft, null, 2) + "\n", "utf8")));

  console.log(`Applied texts to ${applied.size} puzzle(s) from ${relative(ROOT, textsFile)}`);
  for (const [name, missing] of stillEmpty) {
    console.log(`  ${name}: awaiting authoring — ${missing.join(", ")}`);
  }
  if (issues.length > 0) {
    for (const issue of issues) fail(issue);
    process.exitCode = 1;
  }
}

function validateAuthored(lid, text) {
  for (const field of ["title", "prompt", "explanation"]) {
    const value = text[field];
    if (!isObject(value) || !isNonEmptyString(value.ru) || !isNonEmptyString(value.en)) {
      return `${field} must contain non-empty ru and en strings`;
    }
    if (/\b(TODO|FIXME|XXX)\b/i.test(value.ru + value.en)) {
      return `${field} contains a placeholder`;
    }
  }
  if (!Array.isArray(text.hints) || text.hints.length !== 3) {
    return "hints must be an array of exactly 3 items";
  }
  for (const [index, hint] of text.hints.entries()) {
    if (!isObject(hint) || hint.tier !== index + 1) {
      return `hints[${index}].tier must be ${index + 1}`;
    }
    if (!isNonEmptyString(hint.ru) || !isNonEmptyString(hint.en)) {
      return `hints[${index}] must contain non-empty ru and en strings`;
    }
  }
  return null;
}

function isNonEmptyString(value) {
  return typeof value === "string" && value.trim().length > 0;
}

function isObject(value) {
  return value !== null && typeof value === "object" && !Array.isArray(value);
}

// --- 3. merge --------------------------------------------------------------

async function cmdMerge(args) {
  const pattern = (args.drafts ?? "*.draft.json").split(/[\\/]/).pop();
  const dir = path.resolve(ROOT, args.draftsDir ?? DRAFT_DIR);
  const files = (await readdir(dir))
    .filter((name) => name.endsWith(".draft.json"))
    .filter((name) => matchGlob(name, pattern));
  if (files.length === 0) {
    fail(`no drafts matched ${draftsArg} in ${relative(ROOT, dir)}`);
    return;
  }

  // Level drafts in plan order and complete (texts authored).
  const errors = [];
  const batching = { NOVICE: [], PROFESSIONAL: [], GRANDMASTER: [] };
  for (const name of files) {
    const raw = await readFile(path.join(dir, name), "utf8");
    const draft = JSON.parse(raw);
    const missing = [];
    draft.puzzles.forEach((puzzle) => {
      const lid = puzzle.lichessId ?? "?";
      if (puzzle.title?.ru == null || puzzle.title?.en == null) missing.push(`title ${lid}`);
      if (puzzle.prompt?.ru == null || puzzle.prompt?.en == null) missing.push(`prompt ${lid}`);
      if (puzzle.hints.some((h) => h.ru == null || h.en == null)) missing.push(`hints ${lid}`);
      if (puzzle.explanation?.ru == null || puzzle.explanation?.en == null) missing.push(`explanation ${lid}`);
    });
    if (missing.length > 0) {
      errors.push(`${name}: authoring incomplete — ${[...new Set(missing)].join(", ")}`);
      continue;
    }
    batching[draft.level].push(draft);
  }
  if (errors.length > 0) {
    for (const e of errors) fail(e);
    return;
  }

  const existingBylevel = {};
  for (const level of Object.keys(batching)) {
    const file = path.join(PUZZLES_DIR, `${level.toLowerCase()}.json`);
    try {
      existingBylevel[level] = JSON.parse(await readFile(file, "utf8"));
    } catch {
      existingBylevel[level] = null;
    }
  }

  // Global uniqueness across all masters and samples.
  const usedIds = new Set();
  const usedFens = new Set();
  for (const pack of Object.values(existingBylevel)) {
    if (!pack) continue;
    for (const p of pack.puzzles) {
      usedIds.add(p.id);
      usedFens.add(fenKey(p.fen));
    }
  }
  for (const raw of await sampleRawPacks()) {
    for (const p of raw.puzzles) usedFens.add(fenKey(p.fen));
  }

  const composed = [];
  for (const [level, chapters] of PLAN_ORDER) {
    const drafts = batching[level];
    const existing = existingBylevel[level];
    if (drafts.length > 0) {
      composed.push(composePack(level, chapters, drafts, existing, usedIds, usedFens));
    } else if (existing) {
      composed.push(existing);
    }
  }
  if (process.exitCode === 1) return;

  for (const pack of composed) {
    const file = path.join(PUZZLES_DIR, `${pack.level.toLowerCase()}.json`);
    await writeFile(file, JSON.stringify(pack, null, 2) + "\n", "utf8");
    console.log(`Wrote ${path.relative(ROOT, file)} (${pack.puzzles.length} puzzles)`);
  }

  // Final consistency check with the in-house validator.
  const child = spawn(process.execPath, ["tools/validate-content.mjs"], {
    cwd: ROOT,
    stdio: "inherit",
  });
  child.on("exit", (code) => {
    if (code !== 0) process.exitCode = 1;
  });
}

function matchGlob(name, pattern) {
  if (!pattern || pattern === "*" || pattern === "*.draft.json") {
    return name.endsWith(".draft.json");
  }
  if (!pattern.includes("*")) return name === pattern;
  const re = new RegExp(`^${pattern.replace(/[.+]/g, "\\$&").replace(/\*/g, ".*")}$`);
  return re.test(name);
}

function relative(root, target) {
  return path.relative(root, target).replaceAll("\\", "/");
}

function composePack(level, chapters, drafts, existing, usedIds, usedFens) {
  const oldById = new Map((existing?.puzzles ?? []).map((p) => [p.source.id, p]));
  let nextNumber = maxPuzzleNumber(existing) + 1;
  const draftsByChapter = new Map(drafts.filter((d) => d.level === level).map((d) => [d.chapter, d]));

  const puzzles = [];
  const seenSourceIds = new Set();
  for (const chapterId of chapters) {
    const draft = draftsByChapter.get(chapterId);
    if (!draft) {
      continue;
    }
    const sorted = [...draft.puzzles].sort(
      (a, b) => a.rating - b.rating || a.lichessId.localeCompare(b.lichessId),
    );
    for (const puzzle of sorted) {
      if (seenSourceIds.has(puzzle.source.id)) {
        fail(`duplicate lichess source ${puzzle.source.id} within level ${level}`);
        process.exitCode = 1;
        return null;
      }
      seenSourceIds.add(puzzle.source.id);
      puzzles.push(finalPuzzle(puzzle, chapterId));
    }
  }
  if (process.exitCode === 1) return null;

  // Assign IDs: existing chapter contributions keep their IDs, new puzzles get
  // the next free number (nov-021 …). Draft chapter membership grows over time.
  for (const p of puzzles) {
    const old = oldById.get(p.source.id);
    if (old) {
      p.id = old.id;
    }
    if (p.id) usedIds.add(p.id);
  }
  for (const p of puzzles) {
    if (p.id) continue;
    while (usedIds.has(`${LEVEL_PREFIX[level]}-${String(nextNumber).padStart(3, "0")}`)) {
      nextNumber += 1;
    }
    p.id = `${LEVEL_PREFIX[level]}-${String(nextNumber).padStart(3, "0")}`;
    usedIds.add(p.id);
    nextNumber += 1;
  }

  if (puzzles.length > 0) {
    for (const p of puzzles) {
      // Re-merging a master pack must not treat its own existing FENs as
      // duplicates; only newly introduced puzzles need the global check.
      if (oldById.has(p.source.id)) continue;
      if (usedFens.has(fenKey(p.fen))) {
        fail(`${p.id}: FEN already used in another pack`);
        process.exitCode = 1;
      } else {
        usedFens.add(fenKey(p.fen));
      }
    }
  }
  if (composedOverflow(puzzles, level)) process.exitCode = 1;
  if (process.exitCode === 1) return null;

  return packShell(level, puzzles);
}

function composedOverflow(puzzles, level) {
  if (puzzles.length > 120) {
    fail(`level ${level} would exceed 120 puzzles`);
    return true;
  }
  return false;
}

function maxPuzzleNumber(existing) {
  let max = 0;
  for (const p of existing?.puzzles ?? []) {
    const n = Number(p.id.slice(-3));
    if (n > max) max = n;
  }
  return max;
}

function finalPuzzle(puzzle, chapter) {
  const { sanLine, gameUrl, lichessId, ...rest } = puzzle;
  const source = { ...puzzle.source };
  const final = {
    ...rest,
    chapter,
    source,
  };
  return final;
}

function packShell(level, puzzles) {
  return {
    schemaVersion: 1,
    contentVersion: "2026.10.2",
    level,
    puzzles,
  };
}

async function sampleRawPacks() {
  try {
    const files = (await readdir(SAMPLES_DIR)).filter((n) => n.endsWith(".json"));
    return await Promise.all(files.map(async (n) =>
      JSON.parse(await readFile(path.join(SAMPLES_DIR, n), "utf8"))));
  } catch {
    return [];
  }
}

function fenKey(fen) {
  return fen.trim().split(/\s+/).slice(0, 4).join(" ");
}

// --- main ------------------------------------------------------------------

const [, , command, ...argv] = process.argv;
const help = () => {
  console.log(`
Chess Problem Lichess import pipeline

Commands:
  filter --input lichess_db_puzzle.csv[.zst] [--out candidates.ndjson]
      Stream the database; keep verified candidates per chapter.
  draft --pool candidates.ndjson --level NOVICE --chapter mate-in-1 [--take 10] [--ids id1,id2]
      Pick puzzles into a draft under tools/var/import-drafts/.
  split --level NOVICE
      Rebuild per-chapter drafts from the master pack before authoring more.
  author --texts tools/var/import-texts.json
      Inject authored texts (keyed by lichess ID) into drafts.
  merge [--drafts "novice-*.draft.json"]
      Merge drafts into content/puzzles/<level>.json and run the validator.
`);
};
if (!command || command === "--help" || command === "-h") {
  help();
} else if (command === "filter") {
  await cmdFilter(parseArgs(argv, ["input", "out"]))
    .catch((err) => fail(err?.message ?? String(err)));
} else if (command === "draft") {
  await cmdDraft(parseArgs(argv, ["pool", "level", "chapter", "take", "ids"]))
    .catch((err) => fail(err?.message ?? String(err)));
} else if (command === "merge") {
  await cmdMerge(parseArgs(argv, ["drafts", "draftsDir"]))
    .catch((err) => fail(err?.message ?? String(err)));
} else if (command === "split") {
  await cmdSplit(parseArgs(argv, ["level"]))
    .catch((err) => fail(err?.message ?? String(err)));
} else if (command === "author") {
  await cmdAuthor(parseArgs(argv, ["texts", "draftsDir"]))
    .catch((err) => fail(err?.message ?? String(err)));
} else {
  fail(`unknown command: ${command}`);
  help();
}
