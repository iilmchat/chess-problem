// Authoring helper: compact one-line-per-puzzle pool table (rating, side, SAN, themes).
// Usage: node tools/compact.mjs [chapter] [minRating] [maxRating] [poolFile]
import { readFile } from "node:fs/promises";
import { Chess } from "chess.js";

const chapter = process.argv[2];
const minRating = Number(process.argv[3] ?? 0);
const maxRating = Number(process.argv[4] ?? 99999);

const poolFile = process.argv[5] ?? "tools/var/lichess-pool/candidates-v2.ndjson";
const pool = (await readFile(poolFile, "utf8"))
  .split("\n").filter(Boolean).map((l) => JSON.parse(l));

for (const rec of pool) {
  if (chapter && rec.chapter !== chapter) continue;
  if (rec.rating < minRating || rec.rating > maxRating) continue;
  const chess = new Chess(rec.fen);
  if (rec.setupMove) {
    chess.move({ from: rec.setupMove.slice(0, 2), to: rec.setupMove.slice(2, 4) });
  }
  const side = chess.turn() === "w" ? "W" : "B";
  const topThemes = String(rec.lichessTags ?? "").split(" ").filter((t) => t && t !== "short").slice(0, 5).join(",");
  console.log(
    `${rec.lichessId} | ${rec.rating} | ${side} | ${rec.plies}p | mate=${rec.mate ? 1 : 0} | ${rec.sanLine.join(" ")} | ${topThemes}`,
  );
}
