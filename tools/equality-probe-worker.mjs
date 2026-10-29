import { parentPort, workerData } from "node:worker_threads";
import { createReadStream } from "node:fs";
import zlib from "node:zlib";

const { file, start, end } = workerData;
const chunks = [];
const rs = createReadStream(file, { start, end });
rs.on("data", (chunk) => chunks.push(chunk));
rs.on("error", (error) => {
  throw error;
});
rs.on("end", () => {
  const text = zlib.zstdDecompressSync(Buffer.concat(chunks)).toString("utf8");
  const out = [];
  for (const line of text.split("\n")) {
    if (!line) continue;
    const f = line.split(",");
    if (f.length >= 10 && f[7].split(" ").includes("equality")) out.push(line);
  }
  parentPort.postMessage(out);
});
