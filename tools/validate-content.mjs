#!/usr/bin/env node
/**
 * Chess Problem — content validator.
 *
 * Usage:
 *   node tools/validate-content.mjs
 *   node tools/validate-content.mjs content/samples/*.json
 *   node tools/validate-content.mjs --strict content/samples/novice.sample.json
 *
 * Checks JSON shape, FEN/setupMove/solution legality, mate, plies, duplicates,
 * localization, time formula, hints and source/license.
 */

import { readFile, readdir, stat } from "node:fs/promises";
import { existsSync } from "node:fs";
import path from "node:path";
import { fileURLToPath } from "node:url";
import { Chess, validateFen } from "chess.js";

const ROOT = path.resolve(path.dirname(fileURLToPath(import.meta.url)), "..");
const LEVELS = {
  NOVICE: { prefix: "nov", min: 600, max: 1199 },
  PROFESSIONAL: { prefix: "pro", min: 1200, max: 1799 },
  GRANDMASTER: { prefix: "grm", min: 1800, max: 2600 },
};
const KNOWN_CHAPTERS = {
  NOVICE: new Set([
    "mate-in-1", "capture-queen", "fork", "pin-skewer", "back-rank",
    "double-attack", "remove-defender", "promotion", "discovered-attack",
    "defense", "mate-in-2", "mixed-review",
  ]),
  PROFESSIONAL: new Set([
    "mate-in-2", "mate-in-3", "deflection", "decoy", "interference",
    "x-ray", "zwischenzug", "pawn-break", "bishop-vs-knight", "endgame",
    "sacrifice", "mixed-review",
  ]),
  GRANDMASTER: new Set([
    "quiet-move", "deep-sacrifice", "study-draw", "underpromotion",
    "stalemate", "fortress", "zugzwang", "perpetual-check", "rare-mate",
    "calculation", "punish", "final-exam",
  ]),
};
const REQUIRED_PUZZLE_FIELDS = [
  "id", "chapter", "fen", "setupMove", "solution", "task", "rating",
  "themes", "timeLimitSec", "title", "prompt", "hints", "explanation",
  "source",
];
const UCI_RE = /^[a-h][1-8][a-h][1-8][qrbn]?$/;
const ID_RE = /^(nov|pro|grm)-[0-9]{3}$/;
const CHAPTER_RE = /^[a-z0-9]+(?:-[a-z0-9]+)*$/;
const CONTENT_VERSION_RE = /^[0-9]{4}\.[0-9]{2}\.[0-9]+$/;

const args = process.argv.slice(2);
const strict = args.includes("--strict");
const help = args.includes("--help") || args.includes("-h");
const inputArgs = args.filter((a) => a !== "--strict" && a !== "--help" && a !== "-h");

const errors = [];
const warnings = [];
const seenIds = new Map();
const seenFens = new Map();

function rel(file) {
  return path.relative(ROOT, file).replaceAll("\\", "/");
}

function fail(file, puzzleId, message) {
  errors.push(`${rel(file)}${puzzleId ? ` [${puzzleId}]` : ""}: ${message}`);
}

function warn(file, puzzleId, message) {
  warnings.push(`${rel(file)}${puzzleId ? ` [${puzzleId}]` : ""}: ${message}`);
}

function isObject(value) {
  return value !== null && typeof value === "object" && !Array.isArray(value);
}

function isNonEmptyString(value) {
  return typeof value === "string" && value.trim().length > 0;
}

function localized(value) {
  return isObject(value) && isNonEmptyString(value.ru) && isNonEmptyString(value.en);
}

function parseUci(uci) {
  return {
    from: uci.slice(0, 2),
    to: uci.slice(2, 4),
    promotion: uci.length > 4 ? uci[4] : undefined,
  };
}

function normalizeFen(fen) {
  return fen.trim().split(/\s+/).slice(0, 4).join(" ");
}

function roundTo5(value) {
  return Math.round(value / 5) * 5;
}

function expectedTime(rating, plies) {
  return Math.min(420, Math.max(60, roundTo5(30 + 0.09 * rating + 12 * plies)));
}

function opposite(side) {
  return side === "w" ? "b" : "w";
}

async function collectFiles() {
  if (inputArgs.length > 0) {
    const files = [];
    for (const arg of inputArgs) {
      const resolved = path.resolve(process.cwd(), arg);
      if (!existsSync(resolved)) {
        fail(resolved, null, "file not found");
        continue;
      }
      const info = await stat(resolved);
      if (info.isDirectory()) {
        const entries = await readdir(resolved, { withFileTypes: true });
        for (const entry of entries) {
          if (entry.isFile() && entry.name.endsWith(".json")) {
            files.push(path.join(resolved, entry.name));
          }
        }
      } else {
        files.push(resolved);
      }
    }
    return files;
  }

  const files = [];
  for (const dir of ["content/samples", "content/puzzles"]) {
    const resolved = path.join(ROOT, dir);
    if (!existsSync(resolved)) continue;
    const entries = await readdir(resolved, { withFileTypes: true });
    for (const entry of entries) {
      if (entry.isFile() && entry.name.endsWith(".json")) {
        files.push(path.join(resolved, entry.name));
      }
    }
  }
  return files;
}

function checkLocalized(file, puzzleId, field, value) {
  if (!localized(value)) {
    fail(file, puzzleId, `${field} must contain non-empty ru and en strings`);
    return;
  }
  for (const lang of ["ru", "en"]) {
    const text = value[lang].trim();
    if (/\b(TODO|FIXME|XXX|Lorem ipsum)\b/i.test(text)) {
      fail(file, puzzleId, `${field}.${lang} contains a placeholder`);
    }
    if (lang === "ru" && !/[А-Яа-яЁё]/.test(text)) {
      warn(file, puzzleId, `${field}.ru does not contain Cyrillic text`);
    }
    if (lang === "en" && /[А-Яа-яЁё]/.test(text)) {
      warn(file, puzzleId, `${field}.en contains Cyrillic text`);
    }
  }
}

function checkHints(file, puzzleId, hints) {
  if (!Array.isArray(hints) || hints.length !== 3) {
    fail(file, puzzleId, "hints must be an array of exactly 3 items");
    return;
  }
  hints.forEach((hint, index) => {
    if (!isObject(hint)) {
      fail(file, puzzleId, `hints[${index}] must be an object`);
      return;
    }
    if (hint.tier !== index + 1) {
      fail(file, puzzleId, `hints[${index}].tier must be ${index + 1}`);
    }
    for (const lang of ["ru", "en"]) {
      if (!isNonEmptyString(hint[lang])) {
        fail(file, puzzleId, `hints[${index}].${lang} is required`);
      } else if (/\b(TODO|FIXME|XXX|Lorem ipsum)\b/i.test(hint[lang])) {
        fail(file, puzzleId, `hints[${index}].${lang} contains a placeholder`);
      }
    }
    if (index === 0 && isNonEmptyString(hint.ru)) {
      if (/\b[a-h][1-8][-–—]?[a-h][1-8][qrbn]?\b/i.test(hint.ru)) {
        warn(file, puzzleId, "hints[0] (idea) looks like it contains a move");
      }
    }
  });
}

function checkSource(file, puzzleId, source) {
  if (!isObject(source)) {
    fail(file, puzzleId, "source must be an object");
    return;
  }
  if (source.provider === "lichess") {
    if (!isNonEmptyString(source.id)) fail(file, puzzleId, "source.id is required for Lichess");
    if (!isNonEmptyString(source.url)) fail(file, puzzleId, "source.url is required for Lichess");
    if (source.license !== "CC0-1.0") fail(file, puzzleId, 'source.license must be "CC0-1.0" for Lichess');
    if (isNonEmptyString(source.url) && isNonEmptyString(source.id)) {
      if (!source.url.endsWith(`/${source.id}`)) {
        warn(file, puzzleId, "source.url does not end with source.id");
      }
    }
  } else if (source.provider === "original") {
    if (!isNonEmptyString(source.id)) fail(file, puzzleId, "source.id is required for original");
    if (source.license !== "proprietary") {
      fail(file, puzzleId, 'source.license must be "proprietary" for original');
    }
  } else {
    fail(file, puzzleId, 'source.provider must be "lichess" or "original"');
  }
}

function checkPuzzle(file, level, puzzle, index) {
  if (!isObject(puzzle)) {
    fail(file, null, `puzzles[${index}] must be an object`);
    return;
  }

  for (const field of REQUIRED_PUZZLE_FIELDS) {
    if (!(field in puzzle)) {
      fail(file, puzzle.id ?? `#${index}`, `missing field "${field}"`);
    }
  }

  const id = puzzle.id;
  if (!isNonEmptyString(id) || !ID_RE.test(id)) {
    fail(file, id ?? `#${index}`, 'id must match "nov-001", "pro-001" or "grm-001"');
  } else {
    const expectedPrefix = LEVELS[level].prefix;
    if (!id.startsWith(`${expectedPrefix}-`)) {
      fail(file, id, `id prefix must be "${expectedPrefix}" for level ${level}`);
    }
    if (seenIds.has(id)) {
      fail(file, id, `duplicate id (also in ${seenIds.get(id)})`);
    } else {
      seenIds.set(id, rel(file));
    }
  }

  if (!isNonEmptyString(puzzle.chapter) || !CHAPTER_RE.test(puzzle.chapter)) {
    fail(file, id, "chapter must be a kebab-case id");
  } else if (!KNOWN_CHAPTERS[level].has(puzzle.chapter)) {
    warn(file, id, `unknown chapter "${puzzle.chapter}" for ${level}`);
  }

  const fen = puzzle.fen;
  if (!isNonEmptyString(fen)) {
    fail(file, id, "fen is required");
    return;
  }
  const fenCheck = validateFen(fen);
  if (!fenCheck.ok) {
    fail(file, id, `invalid FEN: ${fenCheck.error}`);
    return;
  }

  let chess;
  try {
    chess = new Chess(fen);
  } catch (error) {
    fail(file, id, `cannot load FEN: ${error.message}`);
    return;
  }

  const normalizedFen = normalizeFen(fen);
  if (seenFens.has(normalizedFen)) {
    fail(file, id, `duplicate FEN (also in ${seenFens.get(normalizedFen)})`);
  } else {
    seenFens.set(normalizedFen, rel(file));
  }

  if (puzzle.setupMove !== null) {
    if (!isNonEmptyString(puzzle.setupMove) || !UCI_RE.test(puzzle.setupMove)) {
      fail(file, id, "setupMove must be a UCI move or null");
      return;
    }
    try {
      chess.move(parseUci(puzzle.setupMove));
    } catch (error) {
      fail(file, id, `illegal setupMove "${puzzle.setupMove}": ${error.message}`);
      return;
    }
    if (chess.isGameOver()) {
      fail(file, id, "position is already over after setupMove");
      return;
    }
  }

  const solverSide = chess.turn();
  const solution = puzzle.solution;
  if (!Array.isArray(solution) || solution.length === 0) {
    fail(file, id, "solution must be a non-empty array");
    return;
  }
  if (solution.length % 2 === 0) {
    fail(file, id, "solution length must be odd (user, opponent, user, ...)");
  }
  if (solution.length > 11) {
    fail(file, id, "solution is too long (max 11 plies)");
  }
  for (let i = 0; i < solution.length; i += 1) {
    if (!isNonEmptyString(solution[i]) || !UCI_RE.test(solution[i])) {
      fail(file, id, `solution[${i}] must be a UCI move`);
    }
  }

  const sanMoves = [];
  for (let i = 0; i < solution.length; i += 1) {
    const uci = solution[i];
    if (!isNonEmptyString(uci) || !UCI_RE.test(uci)) break;
    const expectedSide = i % 2 === 0 ? solverSide : opposite(solverSide);
    if (chess.turn() !== expectedSide) {
      fail(file, id, `solution[${i}] expects ${expectedSide} to move, but ${chess.turn()} is to move`);
      break;
    }
    let move;
    try {
      move = chess.move(parseUci(uci));
    } catch (error) {
      fail(file, id, `illegal solution[${i}] "${uci}": ${error.message}`);
      break;
    }
    if (!move) {
      fail(file, id, `illegal solution[${i}] "${uci}"`);
      break;
    }
    sanMoves.push(move.san);
    if (i < solution.length - 1 && chess.isGameOver()) {
      fail(file, id, `line ends early at solution[${i}] (${move.san})`);
      break;
    }
  }

  const task = puzzle.task;
  if (!isObject(task)) {
    fail(file, id, "task must be an object");
  } else {
    if (!["MATE", "WIN", "DRAW"].includes(task.type)) {
      fail(file, id, 'task.type must be "MATE", "WIN" or "DRAW"');
    }
    if (!Number.isInteger(task.plies) || task.plies < 1 || task.plies > 6) {
      fail(file, id, "task.plies must be an integer 1..6");
    } else {
      const userMoves = Math.ceil(solution.length / 2);
      if (task.plies !== userMoves) {
        fail(file, id, `task.plies is ${task.plies}, but solution has ${userMoves} user moves`);
      }
    }
    if (task.type === "MATE" && sanMoves.length === solution.length && !chess.isCheckmate()) {
      fail(file, id, "MATE line does not end in checkmate");
    }
    if (task.type === "MATE" && task.plies > 3) {
      warn(file, id, `MATE with ${task.plies} user moves — allowed only for rare mates`);
    }
    if (task.type !== "MATE" && sanMoves.length === solution.length && chess.isCheckmate()) {
      warn(file, id, "line ends in checkmate; consider task.type MATE");
    }
    if (task.type === "DRAW" && sanMoves.length === solution.length) {
      if (!chess.isDraw() && !chess.isStalemate()) {
        warn(file, id, "DRAW line does not end in a draw detected by chess.js");
      }
    }
  }

  if (!Number.isInteger(puzzle.rating)) {
    fail(file, id, "rating must be an integer");
  } else {
    const range = LEVELS[level];
    if (puzzle.rating < range.min || puzzle.rating > range.max) {
      fail(file, id, `rating ${puzzle.rating} is outside ${range.min}–${range.max} for ${level}`);
    }
  }

  if (!Array.isArray(puzzle.themes) || puzzle.themes.length < 1 || puzzle.themes.length > 5) {
    fail(file, id, "themes must contain 1..5 items");
  } else {
    const unique = new Set(puzzle.themes);
    if (unique.size !== puzzle.themes.length) {
      fail(file, id, "themes must be unique");
    }
    for (const theme of puzzle.themes) {
      if (!isNonEmptyString(theme) || !/^[A-Za-z][A-Za-z0-9]{1,31}$/.test(theme)) {
        fail(file, id, `invalid theme "${theme}"`);
      }
    }
  }

  if (!Number.isInteger(puzzle.timeLimitSec) || puzzle.timeLimitSec < 60 || puzzle.timeLimitSec > 420) {
    fail(file, id, "timeLimitSec must be an integer 60..420");
  } else if (Number.isInteger(puzzle.rating) && Number.isInteger(task?.plies)) {
    const expected = expectedTime(puzzle.rating, task.plies);
    const diff = puzzle.timeLimitSec - expected;
    if (Math.abs(diff) > 20) {
      warn(file, id, `timeLimitSec ${puzzle.timeLimitSec}s differs from formula ${expected}s by ${diff}s`);
    }
  }

  checkLocalized(file, id, "title", puzzle.title);
  checkLocalized(file, id, "prompt", puzzle.prompt);
  checkLocalized(file, id, "explanation", puzzle.explanation);
  if (localized(puzzle.explanation)) {
    for (const lang of ["ru", "en"]) {
      if (puzzle.explanation[lang].trim().length < 40) {
        fail(file, id, `explanation.${lang} is shorter than 40 characters`);
      }
    }
  }

  checkHints(file, id, puzzle.hints);
  checkSource(file, id, puzzle.source);
}

function checkPack(file, pack) {
  if (!isObject(pack)) {
    fail(file, null, "pack must be a JSON object");
    return 0;
  }
  if (pack.schemaVersion !== 1) {
    fail(file, null, "schemaVersion must be 1");
  }
  if (!isNonEmptyString(pack.contentVersion) || !CONTENT_VERSION_RE.test(pack.contentVersion)) {
    fail(file, null, "contentVersion must match YYYY.MM.N");
  }
  if (!Object.hasOwn(LEVELS, pack.level)) {
    fail(file, null, "level must be NOVICE, PROFESSIONAL or GRANDMASTER");
    return 0;
  }
  if (!Array.isArray(pack.puzzles) || pack.puzzles.length < 1) {
    fail(file, null, "puzzles must be a non-empty array");
    return 0;
  }
  if (pack.puzzles.length > 120) {
    fail(file, null, "puzzles must contain at most 120 items");
  }
  pack.puzzles.forEach((puzzle, index) => checkPuzzle(file, pack.level, puzzle, index));
  return pack.puzzles.length;
}

async function main() {
  if (help) {
    console.log(`Chess Problem content validator

Usage:
  node tools/validate-content.mjs [--strict] [files...]

Without files, validates content/samples/*.json and content/puzzles/*.json.
--strict treats warnings as errors.`);
    return;
  }

  const files = await collectFiles();
  if (files.length === 0) {
    console.error("No JSON files to validate.");
    process.exitCode = 1;
    return;
  }

  let puzzleCount = 0;
  for (const file of files.sort()) {
    const errorsBefore = errors.length;
    let raw;
    try {
      raw = await readFile(file, "utf8");
    } catch (error) {
      fail(file, null, `cannot read file: ${error.message}`);
      continue;
    }
    let pack;
    try {
      pack = JSON.parse(raw);
    } catch (error) {
      fail(file, null, `invalid JSON: ${error.message}`);
      continue;
    }
    const count = checkPack(file, pack);
    puzzleCount += count;
    if (errors.length === errorsBefore) {
      console.log(`OK  ${rel(file)}: ${count} ${count === 1 ? "puzzle" : "puzzles"}`);
    }
  }

  for (const message of warnings) {
    console.warn(`WARN ${message}`);
  }
  for (const message of errors) {
    console.error(`ERROR ${message}`);
  }

  console.log(
    `\nChecked ${files.length} file(s), ${puzzleCount} puzzle(s): ` +
      `${errors.length} error(s), ${warnings.length} warning(s).`,
  );

  if (errors.length > 0 || (strict && warnings.length > 0)) {
    process.exitCode = 1;
  }
}

main().catch((error) => {
  console.error(error);
  process.exitCode = 1;
});
