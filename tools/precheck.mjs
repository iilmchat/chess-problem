#!/usr/bin/env node
/**
 * Chess Problem — mechanical precheck of puzzle texts before human review (KI-017).
 *
 * Usage:
 *   node tools/precheck.mjs [--all] [--json <file>]
 *
 * Replays every solution with chess.js and cross-checks the ru/en texts:
 *   - «мат в N» / "mate in N" in prompt vs task.plies (ru and en);
 *   - «Ход белых/чёрных» / "White|Black to move" vs the actual solver side;
 *   - WIN prompt promising a piece that is never captured in the line;
 *   - tactic spoilers, SAN and bare squares in prompt (for titles only SAN and
 *     squares — tactic names in titles are the accepted house style);
 *   - tier2 must name the first-move piece (or a captured one), tier3 must name
 *     the first move (SAN or its squares) and not reveal later moves;
 *   - explanation: 2–4 sentences, at most 600 chars, ru/en pieces in sync;
 *   - duplicate titles across different chapters.
 *
 * Structural checks (FEN, legality, plies, time formula, generic prompts) live
 * in validate-content.mjs; this tool complements it, is advisory and never
 * fails the build. nov-001..029 passed the first human review pass and are
 * skipped unless --all.
 */

import { readFile } from "node:fs/promises";
import { mkdirSync, writeFileSync } from "node:fs";
import path from "node:path";
import { fileURLToPath } from "node:url";
import { Chess } from "chess.js";

const ROOT = path.resolve(path.dirname(fileURLToPath(import.meta.url)), "..");
const PACK_NAMES = ["novice", "professional", "grandmaster"];
const DEFAULT_JSON = "tools/var/precheck-report.json";

const args = process.argv.slice(2);
const includeReviewed = args.includes("--all");
const wantJson = args.includes("--json");
const jsonPath = wantJson ? (args[args.indexOf("--json") + 1] ?? DEFAULT_JSON) : null;

// nov-001..029 already passed the first human review pass (KI-017).
const REVIEWED = new Set(
  Array.from({ length: 29 }, (_, i) => `nov-${String(i + 1).padStart(3, "0")}`),
);

const PIECE_VALUE = { p: 1, n: 3, b: 3, r: 5, q: 9, k: 0 };
// chess.js piece codes are lowercase; texts use my uppercase keys.
const KEY_OF = { q: "Q", r: "R", b: "B", n: "N", p: "P", k: "K" };
const RU_NAME = { Q: "ферзь", R: "ладья", B: "слон", N: "конь", P: "пешка", K: "король" };
const EN_NAME = { Q: "queen", R: "rook", B: "bishop", N: "knight", P: "pawn", K: "king" };

// JS \b does not see Cyrillic letters as word chars, hence explicit lookarounds.
const RU_PIECE_RES = [
  ["Q", /ферз(?:ь|я|ю|е|и|ё)(?:м|й|ми)?(?![а-яё])/i],
  ["R", /ладь(?:я|и|ю|е|ё)(?:м|й|ми)?(?![а-яё])/i],
  ["B", /слон(?:а|у|ом|е|и|ами|ов|ы)?(?![а-яё])/i],
  ["N", /кон(?:ь|я|ю|е|ё|и)(?:м|й|ми)?(?![а-яё])/i],
  ["P", /пеш(?:к|ек|еч)/i],
  ["K", /корол(?:ь|я|ю|е|ё)(?:м|й|ми)?(?![а-яё])/i],
];
const EN_PIECE_RES = [
  ["Q", /\bqueens?\b/i],
  ["R", /\brooks?\b/i],
  ["B", /\bbishops?\b/i],
  ["N", /\bknights?\b/i],
  ["P", /\bpawns?\b/i],
  ["K", /\bkings?\b/i],
];

const RU_SPOILER_RES = [
  ["жертва", /жертв/i],
  ["вилка", /вилк/i],
  ["связка", /связ(?:к|ан)/i],
  ["отвлечение", /отвлеч/i],
  ["завлечение", /завлеч/i],
  ["перекрытие", /перекрыт/i],
  ["сквозной удар", /сквозн/i],
  ["двойной удар", /двойн/i],
  ["рентген", /рентген/i],
  ["размен", /размен/i],
  ["цугцванг", /цугцванг/i],
];
const EN_SPOILER_RES = [
  ["sacrifice", /sacrific/i],
  ["fork", /fork/i],
  ["pin", /\bpin\b/i],
  ["pinned", /pinn/i],
  ["skewer", /skewer/i],
  ["deflection", /deflect/i],
  ["decoy", /decoy/i],
  ["interference", /interference/i],
  ["discovered attack", /discover/i],
  ["zugzwang", /zugzwang/i],
];

const MATE_NUM_RU = { один: 1, два: 2, три: 3, четыре: 4, пять: 5, шесть: 6 };
const MATE_NUM_EN = { one: 1, two: 2, three: 3, four: 4, five: 5, six: 6 };

function parseUci(uci) {
  return {
    from: uci.slice(0, 2),
    to: uci.slice(2, 4),
    promotion: uci.length > 4 ? uci[4] : undefined,
  };
}

function parseNum(word) {
  const lower = word.toLowerCase();
  return MATE_NUM_RU[lower] ?? MATE_NUM_EN[lower] ?? Number(word);
}

function pieceKeys(text, lang) {
  const found = new Set();
  for (const [key, re] of lang === "ru" ? RU_PIECE_RES : EN_PIECE_RES) {
    if (re.test(text)) found.add(key);
  }
  return found;
}

function squaresIn(text) {
  return [...text.matchAll(/\b[a-h][1-8]\b/g)].map((m) => m[0].toLowerCase());
}

function unique(values) {
  return [...new Set(values)];
}

function stripSan(san) {
  return san.replace(/[+#!?]+$/, "");
}

function sanHits(text, sanList) {
  const lower = text.toLowerCase();
  const byIndex = new Map();
  for (const { index, san } of sanList) {
    if (!lower.includes(stripSan(san).toLowerCase())) continue;
    const kept = byIndex.get(index);
    if (!kept || kept.san.length < san.length) byIndex.set(index, { index, san });
  }
  return [...byIndex.values()];
}

function sentenceCount(text) {
  const matches = text.match(/[^.!?]+[.!?]+(?=\s|$)/g);
  return matches ? matches.length : 0;
}

function materialAdvantage(board, color) {
  let mine = 0;
  let theirs = 0;
  for (const row of board) {
    for (const cell of row) {
      if (!cell) continue;
      if (cell.color === color) mine += PIECE_VALUE[cell.type];
      else theirs += PIECE_VALUE[cell.type];
    }
  }
  return mine - theirs;
}

function fmtKeys(keys, names) {
  return keys.length === 0 ? "—" : keys.map((k) => names[k]).join(", ");
}

function pieceDesync(ruKeys, enKeys) {
  const ruOnly = [...ruKeys].filter((k) => !enKeys.has(k));
  const enOnly = [...enKeys].filter((k) => !ruKeys.has(k));
  if (ruOnly.length === 0 && enOnly.length === 0) return null;
  return `RU: ${fmtKeys(ruOnly, RU_NAME)} / EN: ${fmtKeys(enOnly, EN_NAME)}`;
}

function analyze(puzzle, finds) {
  const add = (sev, code, msg) => {
    if (!finds.some((f) => f.code === code && f.msg === msg)) finds.push({ sev, code, msg });
  };

  const chess = new Chess(puzzle.fen);
  let setupSan = null;
  if (puzzle.setupMove) {
    const setup = chess.move(parseUci(puzzle.setupMove));
    setupSan = setup.san;
  }
  const side = chess.turn();
  const moves = [];
  for (const uci of puzzle.solution) {
    moves.push(chess.move(parseUci(uci)));
  }

  const sanList = [
    ...(setupSan ? [{ index: -1, san: setupSan }] : []),
    ...moves.map((m, i) => ({ index: i, san: m.san })),
  ];
  const mover0 = KEY_OF[moves[0].piece];
  const from0 = moves[0].from;
  const to0 = moves[0].to;
  const san0 = stripSan(moves[0].san);
  const isCastle0 = moves[0].san.startsWith("O-O");
  const solverCaptures = new Set(
    moves.filter((_, i) => i % 2 === 0).map((m) => KEY_OF[m.captured]).filter(Boolean),
  );
  const allCaptures = new Set(moves.map((m) => KEY_OF[m.captured]).filter(Boolean));

  const before = new Chess(puzzle.fen);
  if (puzzle.setupMove) before.move(parseUci(puzzle.setupMove));
  const netMaterial =
    materialAdvantage(chess.board(), side) - materialAdvantage(before.board(), side);

  const promptRu = puzzle.prompt.ru;
  const promptEn = puzzle.prompt.en;
  const titleRu = puzzle.title.ru;
  const titleEn = puzzle.title.en;
  const type = puzzle.task.type;
  const plies = puzzle.task.plies;

  const promotionGoal =
    /превращ|проведите\s+пешку/i.test(promptRu) || /promot/i.test(promptEn + " " + titleRu);
  const zugzwangGoal = /цугцванг/i.test(promptRu) || /zugzwang/i.test(promptEn);

  // (a) WIN goal vs actual captures.
  if (type === "WIN" && !promotionGoal) {
    if (zugzwangGoal && puzzle.chapter !== "zugzwang") {
      add("med", "spoiler", "цугцванг в промпте вне главы zugzwang");
    }
    if (!zugzwangGoal) {
      const ruKeys = pieceKeys(promptRu, "ru");
      const enKeys = pieceKeys(promptEn, "en");
      const sync = pieceDesync(ruKeys, enKeys);
      if (sync && (ruKeys.size > 0 || enKeys.size > 0)) {
        add("med", "sync", `промпт называет разные фигуры: ${sync}`);
      }
      const named = unique([...ruKeys, ...enKeys]).filter((k) => k !== "K");
      if (named.length === 0) {
        add("info", "goal", "WIN-промпт не называет конкретную цель");
      } else {
        const missing = named.filter((k) => !solverCaptures.has(k));
        if (missing.length === named.length) {
          add("high", "goal", `промпт обещает «${fmtKeys(named, RU_NAME)}», но в линии она не берётся`);
        } else if (missing.length > 0) {
          add("info", "goal", `часть обещанного не берётся в линии: ${fmtKeys(missing, RU_NAME)}`);
        }
        if (netMaterial <= 0) {
          add("info", "material", `материал по линии не растёт (${netMaterial >= 0 ? "+" : ""}${netMaterial})`);
        }
      }
    }
  }

  // (b) mate count vs plies.
  const ruMate = promptRu.match(/мат(?:а)?\s+в\s+(один|два|три|четыре|пять|шесть|[1-6])\s+хо/i);
  const enMate = promptEn.match(/mate\s+in\s+(one|two|three|four|five|six|[1-6])\b/i);
  if (type === "MATE") {
    if (!ruMate) {
      add("high", "mate", "MATE-промпт не называет число ходов до мата");
    } else if (parseNum(ruMate[1]) !== plies) {
      add("high", "mate", `промпт «мат в ${ruMate[1].toLowerCase()}», а линия занимает ${plies} ход(ов)`);
    }
    if (!enMate) {
      add("med", "mate", 'EN-промпт без "mate in N"');
    } else if (parseNum(enMate[1]) !== plies) {
      add("high", "mate", `EN "mate in ${enMate[1]}", а линия занимает ${plies} ход(ов)`);
    }
    if (ruMate && enMate && parseNum(ruMate[1]) !== parseNum(enMate[1])) {
      add("med", "sync", "RU и EN указывают разное число ходов до мата");
    }
  } else if (ruMate || /мат(?![а-яё])/i.test(promptRu)) {
    add("med", "mate", "промпт упоминает мат, но task.type не MATE");
  }

  // (c) solver side in prompt.
  const ruSide = /ход\s+белых/i.test(promptRu)
    ? "w"
    : /ход\s+чёрных/i.test(promptRu)
      ? "b"
      : null;
  const enSide = /white\s+to\s+move/i.test(promptEn)
    ? "w"
    : /black\s+to\s+move/i.test(promptEn)
      ? "b"
      : null;
  if (ruSide && ruSide !== side) {
    add("high", "side", `промпт «ход ${ruSide === "w" ? "белых" : "чёрных"}», а решают ${side === "w" ? "белые" : "чёрные"}`);
  }
  if (enSide && enSide !== side) {
    add("high", "side", `EN говорит "${enSide === "w" ? "White" : "Black"} to move", а решают ${side === "w" ? "белые" : "чёрные"}`);
  }
  if (ruSide && enSide && ruSide !== enSide) {
    add("med", "sync", "RU и EN называют разные стороны хода");
  }
  if (!ruSide && !enSide) {
    add("info", "side", "сторона хода не указана в промпте");
  }

  // (d) spoilers, SAN and squares in the prompt.
  for (const [name, re] of RU_SPOILER_RES) {
    if (!re.test(promptRu)) continue;
    if (name === "цугцванг" && puzzle.chapter === "zugzwang") continue;
    add("high", "spoiler", `промпт называет приём «${name}»`);
  }
  for (const [name, re] of EN_SPOILER_RES) {
    if (!re.test(promptEn)) continue;
    if (name === "zugzwang" && puzzle.chapter === "zugzwang") continue;
    add("high", "spoiler", `EN-промпт называет приём "${name}"`);
  }
  if (/(?<![а-яё])[А-ЯЁ][a-h][1-8]/.test(promptRu)) {
    add("high", "spoiler", "промпт похож на запись хода в русской нотации");
  }
  for (const hit of sanHits(promptRu + " " + promptEn, sanList)) {
    add("high", "spoiler", `промпт содержит SAN хода линии: ${hit.san}`);
  }
  const promptSquares = unique(squaresIn(promptRu + " " + promptEn));
  if (promptSquares.length > 0) {
    add("high", "spoiler", `промпт называет поле: ${promptSquares.join(", ")}`);
  }
  // Titles: only SAN and squares (tactic names in titles are house style).
  for (const hit of sanHits(titleRu + " " + titleEn, sanList)) {
    add("high", "spoiler", `title содержит SAN хода линии: ${hit.san}`);
  }
  const titleSquares = unique(squaresIn(titleRu + " " + titleEn));
  if (titleSquares.length > 0) {
    add("high", "spoiler", `title называет поле: ${titleSquares.join(", ")}`);
  }

  // Title pieces (WIN only): the named piece should appear in the line.
  if (type === "WIN" && !promotionGoal && !["promotion", "underpromotion"].includes(puzzle.chapter)) {
    const titleKeys = unique([...pieceKeys(titleRu, "ru"), ...pieceKeys(titleEn, "en")]);
    const involved = new Set([mover0, ...allCaptures]);
    const unknown = titleKeys.filter((k) => k !== "K" && !involved.has(k));
    if (unknown.length > 0) {
      add("info", "title", `title называет «${fmtKeys(unknown, RU_NAME)}», но её нет в линии`);
    }
  }

  // (e) hints.
  const [tier1, tier2, tier3] = puzzle.hints;
  if (!tier1 || !tier2 || !tier3) {
    add("high", "hint", "hints должны быть ровно 3 (см. validate-content)");
    return;
  }

  const tier2Keys = unique([...pieceKeys(tier2.ru, "ru"), ...pieceKeys(tier2.en, "en")]);
  const tier2Allowed = new Set([mover0, ...allCaptures, "K"]);
  const wrongTier2 = tier2Keys.filter((k) => !tier2Allowed.has(k));
  if (wrongTier2.length > 0) {
    add("med", "hint", `tier2 называет ${fmtKeys(wrongTier2, RU_NAME)} — не фигура первого хода и не захваченная`);
  }
  if (tier2Keys.length === 0) {
    add("info", "hint", "tier2 не называет фигуру");
  }
  // Squares in tier2 are the house style here («Решает ферзь с поля f7»), not flagged.
  const tier2Sync = pieceDesync(pieceKeys(tier2.ru, "ru"), pieceKeys(tier2.en, "en"));
  if (tier2Sync) {
    add("info", "sync", `tier2: ${tier2Sync}`);
  }

  const tier3Text = tier3.ru + " " + tier3.en;
  const tier3Lower = tier3Text.toLowerCase();
  const tier3NamesMove =
    tier3Lower.includes(san0.toLowerCase()) ||
    unique(squaresIn(tier3Text)).some((s) => s === from0 || s === to0) ||
    (isCastle0 && /рокир|castl/i.test(tier3Text));
  if (!tier3NamesMove) {
    add("high", "hint", `tier3 не называет первый ход (${san0})`);
  }
  for (let i = 2; i < moves.length; i += 2) {
    const later = stripSan(moves[i].san);
    if (later.toLowerCase() !== san0.toLowerCase() && tier3Lower.includes(later.toLowerCase())) {
      add("info", "hint", `tier3 упоминает поздний ход линии: ${later}`);
    }
  }

  // (f) explanation.
  const explRu = puzzle.explanation.ru;
  const explEn = puzzle.explanation.en;
  const sentRu = sentenceCount(explRu);
  const sentEn = sentenceCount(explEn);
  if (sentRu < 2 || sentRu > 4) {
    add("info", "explanation", `explanation.ru: ${sentRu} предложений (норма 2–4)`);
  }
  if (sentEn < 2 || sentEn > 4) {
    add("info", "explanation", `explanation.en: ${sentEn} предложений (норма 2–4)`);
  }
  if (explRu.length > 600) {
    add("med", "explanation", `explanation.ru ${explRu.length} символов (>600)`);
  }
  if (explEn.length > 600) {
    add("med", "explanation", `explanation.en ${explEn.length} символов (>600)`);
  }
  const explSync = pieceDesync(pieceKeys(explRu, "ru"), pieceKeys(explEn, "en"));
  if (explSync && pieceKeys(explRu, "ru").size > 0 && pieceKeys(explEn, "en").size > 0) {
    add("med", "sync", `explanation: ${explSync}`);
  }

  // (g) prompt should stay one short instruction.
  const promptSentences = sentenceCount(promptRu);
  if (promptSentences > 3) {
    add("info", "prompt", `промпт из ${promptSentences} предложений`);
  }
  if (promptRu.length > 120) {
    add("info", "prompt", `промпт ${promptRu.length} символов`);
  }
}

async function loadPuzzles() {
  const puzzles = [];
  for (const name of PACK_NAMES) {
    const file = path.join(ROOT, "content/puzzles", `${name}.json`);
    const pack = JSON.parse(await readFile(file, "utf8"));
    for (const puzzle of pack.puzzles) puzzles.push({ file: name, puzzle });
  }
  return puzzles;
}

async function main() {
  if (args.includes("--help") || args.includes("-h")) {
    console.log("Usage: node tools/precheck.mjs [--all] [--json <file>]");
    console.log("Default scope: all puzzles except the reviewed nov-001..029.");
    return;
  }

  const all = await loadPuzzles();
  const inScope = all.filter(({ puzzle }) => includeReviewed || !REVIEWED.has(puzzle.id));

  const report = [];
  for (const { puzzle } of inScope) {
    const finds = [];
    try {
      analyze(puzzle, finds);
    } catch (error) {
      finds.push({ sev: "high", code: "replay", msg: `не удалось воспроизвести линию: ${error.message}` });
    }
    if (finds.length > 0) report.push({ id: puzzle.id, finds });
  }

  // (g) duplicate titles: only cross-chapter ones get per-puzzle flags.
  const byTitle = new Map();
  for (const { puzzle } of all) {
    const key = puzzle.title.ru.trim().toLowerCase();
    if (!byTitle.has(key)) byTitle.set(key, []);
    byTitle.get(key).push({ id: puzzle.id, chapter: puzzle.chapter, scoped: inScope.some((e) => e.puzzle === puzzle) });
  }
  const sameChapterGroups = [];
  for (const [key, members] of byTitle) {
    if (members.length < 2) continue;
    const scoped = members.filter((m) => m.scoped);
    if (scoped.length === 0) continue;
    const chapters = unique(members.map((m) => m.chapter));
    if (chapters.length > 1) {
      for (const m of scoped) {
        report.find((r) => r.id === m.id)?.finds.push({
          sev: "info",
          code: "title",
          msg: `дубль заголовка «${key}» в другой главе (${members.map((x) => x.id).join(", ")})`,
        });
      }
    } else {
      sameChapterGroups.push(`${scoped.map((m) => m.id).join(", ")} — «${key}» ×${members.length}`);
    }
  }

  const order = { high: 0, med: 1, info: 2 };
  const flat = [];
  for (const { id, finds } of report) {
    for (const find of finds) flat.push({ id, ...find });
  }
  flat.sort((a, b) => order[a.sev] - order[b.sev] || a.id.localeCompare(b.id));

  const count = (sev) => flat.filter((f) => f.sev === sev).length;
  console.log(`Предчек: ${inScope.length} задач (${includeReviewed ? "все" : "без nov-001..029"}), сигналов: ${count("high")} high / ${count("med")} med / ${count("info")} info`);
  console.log("");
  for (const sev of ["high", "med", "info"]) {
    const rows = flat.filter((f) => f.sev === sev);
    if (rows.length === 0) continue;
    console.log(`=== ${sev.toUpperCase()} (${rows.length}) ===`);
    for (const row of rows) console.log(`[${row.id}] (${row.code}) ${row.msg}`);
    console.log("");
  }
  if (sameChapterGroups.length > 0) {
    console.log(`=== Повторы заголовков внутри глав (${sameChapterGroups.length} групп) ===`);
    for (const line of sameChapterGroups) console.log(line);
    console.log("");
  }

  if (wantJson) {
    const file = path.resolve(ROOT, jsonPath);
    mkdirSync(path.dirname(file), { recursive: true });
    const payload = {
      generatedAt: new Date().toISOString(),
      scope: includeReviewed ? "all" : "unreviewed (nov-001..029 excluded)",
      puzzles: Object.fromEntries(report.map(({ id, finds }) => [id, finds])),
      sameChapterTitles: sameChapterGroups,
    };
    writeFileSync(file, JSON.stringify(payload, null, 2) + "\n", "utf8");
    console.log(`Отчёт: ${path.relative(ROOT, file)}`);
  }
}

main().catch((error) => {
  console.error(error);
  process.exitCode = 1;
});
