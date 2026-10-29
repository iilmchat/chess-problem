#!/usr/bin/env node
/**
 * Build study-draw candidate records from the equality probe dump.
 *
 * The equality theme is rare and its puzzles are rarely played, so the global
 * popularity/plays thresholds do not apply; this selector uses a relaxed
 * quality gate (popularity >= 20, plays >= 100, deviation <= 250) and the
 * GRANDMASTER window (rating 1800-2600, 3-5 user plies).
 */
import { readFileSync, writeFileSync } from "node:fs";
import path from "node:path";
import { fileURLToPath } from "node:url";
import { Chess } from "chess.js";

const ROOT = path.resolve(path.dirname(fileURLToPath(import.meta.url)), "..");
const SRC = path.join(ROOT, "tools", "var", "lichess-pool", "equality-rows.ndjson");
const OUT = path.join(ROOT, "tools", "var", "lichess-pool", "study-draw-candidates.ndjson");

const IDS = [
  "MXlXE", "ARvbh", "2IdQn", "9CwUD", "Wu5sa",
  "w8fkq", "WmBQI", "4f5Na", "2kWMT", "Xofyp",
];

const GENERIC = new Set(["short", "long", "veryLong", "oneMove", "master", "opening", "middlegame", "endgame"]);

const rows = readFileSync(SRC, "utf8").split("\n").filter(Boolean);
const byId = new Map(rows.map((line) => {
  const f = line.split(",");
  return [f[0], f];
}));

const records = [];
for (const id of IDS) {
  const f = byId.get(id);
  if (!f) throw new Error(`id ${id} not found`);
  const rating = Number(f[3]);
  const deviation = Number(f[4]);
  const popularity = Number(f[5]);
  const plays = Number(f[6]);
  if (rating < 1800 || rating > 2600) throw new Error(`${id}: rating ${rating} outside GRM`);
  if (popularity < 20 || plays < 100 || deviation > 250) throw new Error(`${id}: quality gate failed`);

  const moves = f[2].split(" ").filter(Boolean);
  const plies = Math.ceil((moves.length - 1) / 2);
  if (plies < 3 || plies > 5) throw new Error(`${id}: user plies ${plies} outside 3-5`);

  const chess = new Chess(f[1]);
  const san = [];
  for (const uci of moves) {
    const move = chess.move({ from: uci.slice(0, 2), to: uci.slice(2, 4), promotion: uci[4] });
    if (!move) throw new Error(`${id}: illegal move ${uci}`);
    san.push(move.san);
  }

  const tags = f[7].split(" ").filter(Boolean);
  const themes = ["studyDraw", "equality", ...tags.filter((t) => !GENERIC.has(t) && t !== "equality" && t !== "studyDraw").slice(0, 3)];
  records.push({
    level: "GRANDMASTER",
    chapter: "study-draw",
    lichessId: id,
    fen: f[1],
    setupMove: moves[0],
    solution: moves.slice(1),
    sanLine: san,
    plies,
    mate: chess.isCheckmate(),
    drawEnd: chess.isDraw() || chess.isStalemate(),
    taskType: "DRAW",
    rating,
    ratingDeviation: deviation,
    popularity,
    plays,
    themes,
    lichessTags: f[7],
    gameUrl: f[8],
  });
  console.log(`${id} r${rating} plies=${plies} SAN ${san.join(" ")}`);
}

writeFileSync(OUT, records.map((r) => JSON.stringify(r)).join("\n") + "\n", "utf8");
console.log(`Wrote ${path.relative(ROOT, OUT)} (${records.length} candidates)`);
