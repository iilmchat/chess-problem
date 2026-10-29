#!/usr/bin/env node
/**
 * One-off probe: dump every Lichess puzzle tagged `equality` from the zstd
 * export. Frames are decompressed in parallel workers (~15 min instead of
 * ~100 min single-threaded). Output: tools/var/lichess-pool/equality-rows.ndjson
 */
import { createReadStream, writeFileSync } from "node:fs";
import { Worker } from "node:worker_threads";
import path from "node:path";
import { fileURLToPath } from "node:url";

const ROOT = path.resolve(path.dirname(fileURLToPath(import.meta.url)), "..");
const FILE = path.join(ROOT, "tools", "var", "lichess-pool", "lichess_db_puzzle.csv.zst");
const OUT = path.join(ROOT, "tools", "var", "lichess-pool", "equality-rows.ndjson");
const SKIP_MAGIC = Buffer.from([0x50, 0x2a, 0x4d, 0x18]);

function scanSkips(file) {
  return new Promise((resolve, reject) => {
    const rs = createReadStream(file, { highWaterMark: 1 << 22 });
    let carry = Buffer.alloc(0);
    let readTotal = 0;
    const skips = [];
    rs.on("data", (chunk) => {
      const buf = Buffer.concat([carry, chunk]);
      const base = readTotal - carry.length;
      let idx = buf.indexOf(SKIP_MAGIC);
      while (idx !== -1) {
        skips.push(base + idx);
        idx = buf.indexOf(SKIP_MAGIC, idx + 1);
      }
      readTotal += chunk.length;
      carry = buf.subarray(buf.length - 3);
    });
    rs.on("error", reject);
    rs.on("end", () => resolve(skips));
  });
}

const skips = await scanSkips(FILE);
const starts = skips.map((offset) => offset + 12);
const segments = starts.map((start, i) => [
  start,
  i + 1 < starts.length ? starts[i + 1] - 13 : undefined,
]);
console.log(`frames: ${segments.length}`);

const results = [];
const workers = Math.min(8, segments.length);
let next = 0;
let active = 0;

await new Promise((resolve) => {
  const spawn = () => {
    if (next >= segments.length) {
      if (active === 0) resolve();
      return;
    }
    const [start, end] = segments[next++];
    active += 1;
    const worker = new Worker(new URL("./equality-probe-worker.mjs", import.meta.url), {
      workerData: { file: FILE, start, end },
    });
    worker.on("message", (lines) => {
      results.push(...lines);
      active -= 1;
      console.log(`frames done ${next}/${segments.length}, equality rows ${results.length}`);
      worker.terminate();
      spawn();
    });
    worker.on("error", (error) => {
      console.error(`worker failed: ${error.message}`);
      active -= 1;
      worker.terminate();
      spawn();
    });
  };
  for (let i = 0; i < workers; i += 1) spawn();
});

writeFileSync(OUT, results.join("\n") + "\n", "utf8");
console.log(`Wrote ${path.relative(ROOT, OUT)} (${results.length} rows)`);
