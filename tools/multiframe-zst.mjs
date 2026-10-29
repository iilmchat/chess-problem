/**
 * Lichess DB exports ship zstd as many concatenated single frames, each
 * preceded by a 12-byte skippable frame. Node's streaming zstd decoder stops
 * after the first frame, so the source below decompresses one frame at a time
 * and yields the pieces in order. Plain files and legacy single-frame exports
 * (no skippable headers) fall back to a stream decompressor.
 */
import { createReadStream } from "node:fs";
import { Readable } from "node:stream";
import zlib from "node:zlib";

const SKIP_MAGIC = Buffer.from([0x50, 0x2a, 0x4d, 0x18]);

async function scanSkips(file) {
  return new Promise((resolve, reject) => {
    const rs = createReadStream(file, { highWaterMark: 1 << 22 });
    let carry = Buffer.alloc(0);
    let readTotal = 0; // file bytes consumed after carry/chunk concat below
    const skips = [];
    rs.on("data", (chunk) => {
      const buf = Buffer.concat([carry, chunk]);
      // buf[0] sits at readTotal - carry.length in the file.
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

function readSegment(file, start, end) {
  return new Promise((resolve, reject) => {
    const chunks = [];
    const rs = createReadStream(file, { start, end }); // end is inclusive
    rs.on("data", (c) => chunks.push(c));
    rs.on("error", reject);
    rs.on("end", () => resolve(Buffer.concat(chunks)));
  });
}

async function* frames(file, starts) {
  for (let i = 0; i < starts.length; i += 1) {
    // The next segment includes its own skippable header: stop the buffer one
    // byte before it (end is inclusive).
    const end = i + 1 < starts.length ? starts[i + 1] - 13 : undefined;
    const buf = await readSegment(file, starts[i], end);
    yield zlib.zstdDecompressSync(buf);
  }
}

export async function createZstSource(file) {
  const skips = await scanSkips(file);
  if (skips.length === 0) {
    // Legacy single-frame export: let the stream decoder run to the end.
    return createReadStream(file).pipe(zlib.createZstdDecompress());
  }
  return Readable.from(frames(file, skips.map((o) => o + 12)));
}
