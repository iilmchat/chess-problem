#!/usr/bin/env node
/**
 * Chess Problem - content review builder.
 *
 * Reads production packs, applies setupMove and the full solution with
 * chess.js, then embeds the review data into tools/review/index.html.
 *
 * The generated reviewer is a standalone HTML file: no fetch(), no server,
 * no external assets. Open it by double-clicking / file://.
 *
 * Usage:
 *   node tools/review/build-review.mjs
 */

import { readFile, readdir, writeFile } from "node:fs/promises";
import path from "node:path";
import { fileURLToPath } from "node:url";
import { Chess } from "chess.js";

const ROOT = path.resolve(path.dirname(fileURLToPath(import.meta.url)), "../..");
const PUZZLES_DIR = path.join(ROOT, "content", "puzzles");
const TEMPLATE = path.join(ROOT, "tools", "review", "template.html");
const OUTPUT = path.join(ROOT, "tools", "review", "index.html");

const LEVEL_ORDER = ["NOVICE", "PROFESSIONAL", "GRANDMASTER"];
const LEVEL_LABELS = {
  NOVICE: { ru: "Новичок", en: "Novice" },
  PROFESSIONAL: { ru: "Профессионал", en: "Professional" },
  GRANDMASTER: { ru: "Гроссмейстер", en: "Grandmaster" },
};
const CHAPTER_LABELS = {
  "mate-in-1": { ru: "Мат в один ход", en: "Mate in one" },
  "capture-queen": { ru: "Выигрыш ферзя", en: "Win the queen" },
  fork: { ru: "Вилка", en: "Fork" },
  pin: { ru: "Связка", en: "Pin" },
  "back-rank": { ru: "Последняя горизонталь", en: "Back rank" },
  promotion: { ru: "Превращение", en: "Promotion" },
  "mate-in-2": { ru: "Мат в два", en: "Mate in two" },
  deflection: { ru: "Отвлечение", en: "Deflection" },
  decoy: { ru: "Завлечение", en: "Decoy" },
  interference: { ru: "Перекрытие", en: "Interference" },
  endgame: { ru: "Эндшпиль", en: "Endgame" },
  sacrifice: { ru: "Жертва", en: "Sacrifice" },
  "quiet-move": { ru: "Тихий ход", en: "Quiet move" },
  "deep-sacrifice": { ru: "Глубокая жертва", en: "Deep sacrifice" },
  zugzwang: { ru: "Цугцванг", en: "Zugzwang" },
  underpromotion: { ru: "Слабое превращение", en: "Underpromotion" },
  "rare-mate": { ru: "Редкий мат", en: "Rare mate" },
  calculation: { ru: "Расчёт", en: "Calculation" },
  "mixed-review": { ru: "Смешанное повторение", en: "Mixed review" },
  "final-exam": { ru: "Финальный экзамен", en: "Final exam" },
};

function applyUci(chess, uci) {
  return chess.move({
    from: uci.slice(0, 2),
    to: uci.slice(2, 4),
    promotion: uci.length > 4 ? uci[4] : undefined,
  });
}

function buildPuzzle(puzzle, level, contentVersion) {
  const before = new Chess(puzzle.fen);
  const board = new Chess(puzzle.fen);
  let setup = null;

  if (puzzle.setupMove) {
    const move = applyUci(board, puzzle.setupMove);
    setup = {
      uci: puzzle.setupMove,
      san: move.san,
      from: move.from,
      to: move.to,
    };
  }

  const boardFen = board.fen();
  const sideToMove = board.turn();
  const solutionSan = [];
  let finalMove = null;
  for (const uci of puzzle.solution) {
    const move = applyUci(board, uci);
    solutionSan.push(move.san);
    finalMove = { from: move.from, to: move.to };
  }
  const finalFen = board.fen();

  return {
    id: puzzle.id,
    level,
    chapter: puzzle.chapter,
    rating: puzzle.rating,
    timeLimitSec: puzzle.timeLimitSec,
    task: puzzle.task,
    themes: puzzle.themes,
    title: puzzle.title,
    prompt: puzzle.prompt,
    hints: puzzle.hints,
    explanation: puzzle.explanation,
    source: puzzle.source,
    fenBeforeSetup: puzzle.fen,
    fen: boardFen,
    sideToMove,
    setup,
    startLastMove: setup ? { from: setup.from, to: setup.to } : null,
    solution: {
      uci: puzzle.solution,
      san: solutionSan,
      finalFen,
      lastMove: finalMove,
    },
    contentVersion,
  };
}

async function main() {
  const files = (await readdir(PUZZLES_DIR))
    .filter((name) => name.endsWith(".json"))
    .sort();
  if (files.length === 0) throw new Error("No production packs in content/puzzles");

  const packs = [];
  for (const file of files) {
    const pack = JSON.parse(await readFile(path.join(PUZZLES_DIR, file), "utf8"));
    packs.push({ file, pack });
  }

  const contentVersion = packs[0].pack.contentVersion;
  if (!packs.every(({ pack }) => pack.contentVersion === contentVersion)) {
    throw new Error("Production packs have different contentVersion values");
  }

  const puzzles = [];
  for (const { file, pack } of packs) {
    if (!LEVEL_ORDER.includes(pack.level)) {
      throw new Error(`${file}: unknown level ${pack.level}`);
    }
    for (const puzzle of pack.puzzles) {
      puzzles.push(buildPuzzle(puzzle, pack.level, contentVersion));
    }
  }
  puzzles.sort((a, b) => {
    const level = LEVEL_ORDER.indexOf(a.level) - LEVEL_ORDER.indexOf(b.level);
    return level || a.id.localeCompare(b.id);
  });

  const data = {
    reviewerVersion: "1.0",
    generatedAt: new Date().toISOString(),
    contentVersion,
    levels: LEVEL_LABELS,
    chapters: CHAPTER_LABELS,
    puzzles,
  };

  const template = await readFile(TEMPLATE, "utf8");
  const json = JSON.stringify(data).replace(/</g, "\\u003c");
  const html = template.replace("__REVIEW_DATA_JSON__", json);
  await writeFile(OUTPUT, html, "utf8");

  const byLevel = Object.fromEntries(
    LEVEL_ORDER.map((level) => [level, puzzles.filter((p) => p.level === level).length]),
  );
  console.log(`Wrote ${path.relative(ROOT, OUTPUT)}`);
  console.log(`  contentVersion ${contentVersion}`);
  console.log(`  puzzles ${puzzles.length} (${Object.entries(byLevel).map(([k, v]) => `${k}=${v}`).join(", ")})`);
  console.log("  open tools/review/index.html in a browser; no server required");
}

main().catch((error) => {
  console.error(error);
  process.exitCode = 1;
});
