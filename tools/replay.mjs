// Authoring helper: replay pool records move by move with SAN, captures and checks.
// Usage: node tools/replay.mjs "id1,id2,..." [poolFile]
import { readFile } from "node:fs/promises";
import { Chess } from "chess.js";

const ids = process.argv[2]?.split(",") ?? [];
const poolFile = process.argv[3] ?? "tools/var/lichess-pool/candidates.ndjson";

const pool = (await readFile(poolFile, "utf8"))
  .split("\n").filter(Boolean).map((l) => JSON.parse(l));

for (const rec of pool) {
  if (ids.length > 0 && !ids.includes(rec.lichessId)) continue;
  const chess = new Chess(rec.fen);
  console.log(`=== ${rec.lichessId} (${rec.chapter}, rating ${rec.rating}) ===`);
  console.log(`start FEN: ${rec.fen}`);
  const all = [rec.setupMove, ...rec.solution].filter(Boolean);
  for (let i = 0; i < all.length; i += 1) {
    const m = chess.move({ from: all[i].slice(0, 2), to: all[i].slice(2, 4), promotion: all[i][4] });
    const who = i === 0 ? "setup" : "solution";
    console.log(`  ${who}[${i}] ${all[i]} -> ${m.san} | piece ${m.piece}, captured ${m.captured ?? "-"} | check ${chess.isCheck()} mate ${chess.isCheckmate()}`);
  }
  console.log(`final FEN: ${chess.fen()}`);
  console.log("");
}
