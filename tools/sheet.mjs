// Authoring helper: print authoring sheets (FEN, SAN line, tags) for pool records.
// Usage: node tools/sheet.mjs "id1,id2,..." [chapter] [poolFile]  (chapter "" = all)
import { readFile } from "node:fs/promises";
import { Chess } from "chess.js";

const ids = (process.argv[2] ?? "").split(",").filter(Boolean);
const chapter = process.argv[3];

const poolFile = process.argv[4] ?? "tools/var/lichess-pool/candidates.ndjson";
const pool = (await readFile(poolFile, "utf8"))
  .split("\n").filter(Boolean).map((l) => JSON.parse(l));

for (const rec of pool) {
  if (chapter && rec.chapter !== chapter) continue;
  if (ids.length > 0 && !ids.includes(rec.lichessId)) continue;
  const chess = new Chess(rec.fen);
  if (rec.setupMove) {
    chess.move({ from: rec.setupMove.slice(0, 2), to: rec.setupMove.slice(2, 4) });
  }
  const side = chess.turn() === "w" ? "white" : "black";
  console.log(`${rec.lichessId} | rating ${rec.rating} | ${side} to move | plies ${rec.plies} | mate ${rec.mate}`);
  console.log(`  tags: ${rec.lichessTags}`);
  console.log(`  FEN: ${rec.fen}`);
  console.log(`  setup ${rec.setupMove} | SAN: ${rec.sanLine.join(" ")}`);
  console.log(`  themes: ${rec.themes.join(", ")} | game: ${rec.gameUrl}`);
  console.log("");
}
