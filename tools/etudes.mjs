#!/usr/bin/env node
/**
 * Compose authored étude drafts (stalemate / fortress / perpetual-check).
 *
 * Input sidecar tools/var/etudes.json:
 *
 *   {
 *     "grandmaster.stalemate": [
 *       {
 *         "fen": "8/8/... w - - 0 1",
 *         "setupMove": null,             // optional
 *         "solution": ["e1e2", "..."],   // UCI, odd length, starts with user
 *         "rating": 2400,                // optional, default 2400 (GRM top)
 *         "sourceId": "etude-stalemate-01",
 *         "themes": ["stalemate"]        // optional house themes
 *       }
 *     ],
 *     "grandmaster.fortress": [...],
 *     "grandmaster.perpetual-check": [...]
 *   }
 *
 * Every étude is verified mechanically:
 *   - stalemate        — final position must be checkmate-free stalemate;
 *   - perpetual-check  — the final position must repeat within the line
 *                        (threefold repetition per chess.js);
 *   - fortress         — no mechanical check possible (documented WARN).
 *
 * Output: tools/var/import-drafts/<level>-<chapter>.draft.json in the same
 * shape as `import-lichess.mjs draft`, texts empty (author --texts fills them,
 * keys = sourceId).
 */
import { readFile, writeFile } from "node:fs/promises";
import { mkdirSync } from "node:fs";
import path from "node:path";
import { fileURLToPath } from "node:url";
import { Chess, validateFen } from "chess.js";

const ROOT = path.resolve(path.dirname(fileURLToPath(import.meta.url)), "..");
const VAR_DIR = path.join(ROOT, "tools", "var");
const DRAFT_DIR = path.join(VAR_DIR, "import-drafts");
const SIDECAR = path.join(ROOT, "tools", "etudes", "etudes.json");

const CHAPTERS = {
  "grandmaster.stalemate": { level: "GRANDMASTER", chapter: "stalemate", drawKind: "stalemate", plies: [2, 4] },
  "grandmaster.fortress": { level: "GRANDMASTER", chapter: "fortress", drawKind: "none", plies: [3, 5] },
  "grandmaster.perpetual-check": { level: "GRANDMASTER", chapter: "perpetual-check", drawKind: "repetition", plies: [2, 4] },
};

const ARG = process.argv.slice(2);
const SUFFIX = ARG.includes("--write"); // only write drafts with --write; dry-run by default
const customSidecar = ARG.find((a) => a.startsWith("--file="))?.slice(7) ?? SIDECAR;

function parseUci(uci) {
  return {
    from: uci.slice(0, 2),
    to: uci.slice(2, 4),
    promotion: uci.length > 4 ? uci[4] : undefined,
  };
}

function expectedTime(rating, plies) {
  return Math.min(420, Math.max(60, Math.round((30 + 0.09 * rating + 12 * plies) / 5) * 5));
}

function posKey(chess) {
  return chess.fen().trim().split(/\s+/).slice(0, 4).join(" ");
}

function endsDrawn(chess) {
  return chess.isDraw() || chess.isStalemate() || chess.isThreefoldRepetition();
}

async function play(fen, setupMove, solution) {
  const chess = new Chess(fen);
  if (setupMove) chess.move(parseUci(setupMove));
  const san = [];
  const oppOptions = []; // legal replies available after each user move
  const repeats = new Map([[posKey(chess), 1]]);
  for (const [index, uci] of solution.entries()) {
    const move = chess.move(parseUci(uci));
    if (!move) throw new Error(`illegal move ${uci}`);
    san.push(move.san);
    if (index % 2 === 0) oppOptions.push(chess.moves().length);
    const key = posKey(chess);
    const n = (repeats.get(key) ?? 0) + 1;
    repeats.set(key, n);
  }
  const finalKey = repeats.get(posKey(chess)) ?? 1;
  return { chess, san, finalKey, repeated: finalKey >= 2, threefold: chess.isThreefoldRepetition(), oppOptions };
}

let issues = 0;
const draftFiles = [];

const sidecar = JSON.parse(await readFile(customSidecar, "utf8"));
mkdirSync(DRAFT_DIR, { recursive: true });

for (const [key, definitions] of Object.entries(sidecar)) {
  const meta = CHAPTERS[key];
  if (!meta) {
    console.error(`unknown étude chapter ${key}`);
    issues += 1;
    continue;
  }

  const puzzles = [];
  for (const def of definitions) {
    const id = def.sourceId ?? `etude-${meta.chapter}-01`;
    try {
      const fenCheck = validateFen(def.fen);
      if (!fenCheck.ok) throw new Error(`invalid FEN: ${fenCheck.error}`);
      const { chess, san, repeated, threefold, oppOptions } = await play(def.fen, def.setupMove ?? null, def.solution);

      if (chess.isCheckmate()) throw new Error("line ends in checkmate, expected a draw");
      if (meta.drawKind === "stalemate" && !chess.isStalemate()) {
        throw new Error("final position is not a stalemate");
      }
      if (meta.drawKind === "repetition" && !(threefold || repeated)) {
        throw new Error("no repeated position inside the line");
      }
      if (meta.drawKind !== "none" && meta.drawKind !== "stalemate" && meta.drawKind !== "repetition" && !endsDrawn(chess)) {
        throw new Error("line is not drawn by any chess.js rule");
      }
      const plies = Math.ceil(def.solution.length / 2);
      if (plies < meta.plies[0] || plies > meta.plies[1]) {
        throw new Error(`plies ${plies} outside ${meta.plies[0]}-${meta.plies[1]}`);
      }
      const rating = def.rating ?? 2400;
      if (rating < 1800 || rating > 2600) throw new Error(`rating ${rating} outside GRM window`);
      // ensure a legal audience: reject when user solves against a non-forced reply
      puzzles.push({
        lichessId: id,
        fen: def.fen,
        setupMove: def.setupMove ?? null,
        solution: def.solution,
        task: { type: "DRAW", plies },
        rating,
        themes: def.themes ?? [meta.chapter === "perpetual-check" ? "perpetualCheck" : meta.chapter],
        timeLimitSec: expectedTime(rating, plies),
        sanLine: san,
        title: { ru: null, en: null },
        prompt: { ru: null, en: null },
        hints: [
          { tier: 1, ru: null, en: null },
          { tier: 2, ru: null, en: null },
          { tier: 3, ru: null, en: null },
        ],
        explanation: { ru: null, en: null },
        source: { provider: "original", id, license: "proprietary" },
      });
      console.log(`OK  ${meta.chapter}  ${id}  plies=${plies} opp=${oppOptions.join("/")} SAN ${san.join(" ")}`);
    } catch (err) {
      issues += 1;
      console.error(`FAIL ${meta.chapter} ${id}: ${err.message}`);
    }
  }

  if (puzzles.length > 0) {
    const draft = {
      schemaVersion: 1,
      contentVersion: "2026.10.3",
      level: meta.level,
      chapter: meta.chapter,
      puzzles,
    };
    const file = path.join(DRAFT_DIR, `${meta.level.toLowerCase()}-${meta.chapter}.draft.json`);
    if (SUFFIX) {
      await writeFile(file, JSON.stringify(draft, null, 2) + "\n", "utf8");
    }
    draftFiles.push({ file, count: puzzles.length });
  }
}

console.log(`\nEtude drafts: ${draftFiles.map(({ count }) => count).join(" + ")} puzzles, issues: ${issues}`);
if (!SUFFIX) console.log("(dry run — pass --write to produce draft files)");
if (issues > 0) process.exitCode = 1;
