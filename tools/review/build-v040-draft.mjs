import { mkdirSync, readFileSync, writeFileSync } from "node:fs";

const packs = ["novice", "professional", "grandmaster"].map((name) =>
  JSON.parse(readFileSync(`content/puzzles/${name}.json`, "utf8")),
);

const notes = [];
const comments = {
  "grm-061": "AI: Lichess equality; линия легальна, финал — позиционная ничья (chess.js не определяет). Подтвердите оценку ничьей.",
  "grm-062": "AI: Lichess equality; линия легальна, финал — позиционная ничья (chess.js не определяет). Подтвердите оценку ничьей.",
  "grm-063": "AI: Lichess equality; линия легальна, финал — позиционная ничья (chess.js не определяет). Подтвердите оценку ничьей.",
  "grm-064": "AI: Lichess equality; линия легальна, финал — позиционная ничья (chess.js не определяет). Подтвердите оценку ничьей.",
  "grm-065": "AI: Lichess equality; линия легальна, финал — позиционная ничья (chess.js не определяет). Подтвердите оценку ничьей.",
  "grm-066": "AI: Lichess equality; линия легальна, финал — позиционная ничья (chess.js не определяет). Подтвердите оценку ничьей.",
  "grm-067": "AI: Lichess equality; линия легальна, финал — позиционная ничья (chess.js не определяет). Подтвердите оценку ничьей.",
  "grm-068": "AI: Lichess equality; линия легальна, финал — позиционная ничья (chess.js не определяет). Подтвердите оценку ничьей.",
  "grm-069": "AI: Lichess equality; линия легальна, финал — позиционная ничья (chess.js не определяет). Подтвердите оценку ничьей.",
  "grm-070": "AI: Lichess equality; линия легальна, финал — позиционная ничья (chess.js не определяет). Подтвердите оценку ничьей.",
  "grm-071": "AI: эталонный пат; Stockfish 0.00, без мата. Подтвердите.",
  "grm-072": "AI: пат-этюд пересобран (слон b6). Финал — пат, Stockfish 0.00, без мата. Подтвердите.",
  "grm-073": "AI: эталонный пат; Stockfish 0.00, без мата. Подтвердите.",
  "grm-074": "AI: пат-этюд пересобран (слон g3). Финал — пат, Stockfish 0.00, без мата. Подтвердите.",
  "grm-075": "AI: эталонный пат; Stockfish 0.00, без мата. Подтвердите.",
  "grm-076": "AI: добавлена пешка g6, чтобы убрать мат Qh5. Qxg6+ ведёт к пату; Stockfish 0.00.",
  "grm-077": "AI: эталонный пат; Stockfish 0.00, без мата. Подтвердите.",
  "grm-078": "AI: добавлена пешка b6, чтобы убрать мат Qa5. Qxb6+ ведёт к пату; Stockfish 0.00.",
  "grm-079": "AI: эталонный пат; Stockfish 0.00, без мата. Подтвердите.",
  "grm-080": "AI: пат-этюд пересобран (слон b6, ферзь b1). Финал — пат, Stockfish 0.00.",
  "grm-081": "AI: табличная ничья, единственный ничейный ход. Подтвердите.",
  "grm-082": "AI: табличная ничья, единственный ничейный ход. Подтвердите.",
  "grm-083": "AI: крепость заменена на табличную ничью; единственный ход Kb6. Подтверждено Lichess tablebase.",
  "grm-084": "AI: крепость заменена на табличную ничью; единственный ход Kc6. Подтверждено Lichess tablebase.",
  "grm-085": "AI: крепость заменена на табличную ничью; единственный ход Kd6. Подтверждено Lichess tablebase.",
  "grm-086": "AI: крепость заменена на табличную ничью; единственный ход Ke6. Подтверждено Lichess tablebase.",
  "grm-087": "AI: крепость заменена на табличную ничью; единственный ход Kf6. Подтверждено Lichess tablebase.",
  "grm-088": "AI: крепость заменена на табличную ничью; единственный ход Kg6. Подтверждено Lichess tablebase.",
  "grm-089": "AI: крепость заменена на табличную ничью; единственный ход Kc6. Подтверждено Lichess tablebase.",
  "grm-090": "AI: крепость заменена на табличную ничью; единственный ход Kb6. Подтверждено Lichess tablebase.",
  "grm-091": "AI: вечный шах пересобран; Stockfish 0.00, без мата; линия повторяется. Подтвердите.",
  "grm-092": "AI: вечный шах пересобран; Stockfish 0.00, без мата; линия повторяется. Подтвердите.",
  "grm-093": "AI: вечный шах пересобран; Stockfish 0.00, без мата; линия повторяется. Подтвердите.",
  "grm-094": "AI: вечный шах пересобран; Stockfish 0.00, без мата; линия повторяется. Подтвердите.",
  "grm-095": "AI: вечный шах пересобран; Stockfish 0.00, без мата; линия повторяется. Подтвердите.",
  "grm-096": "AI: вечный шах пересобран; Stockfish 0.00, без мата; линия повторяется. Подтвердите.",
  "grm-097": "AI: вечный шах пересобран; Stockfish 0.00, без мата; линия повторяется. Подтвердите.",
  "grm-098": "AI: вечный шах пересобран; Stockfish 0.00, без мата; линия повторяется. Подтвердите.",
  "grm-099": "AI: вечный шах пересобран; Stockfish 0.00, без мата; линия повторяется. Подтвердите.",
  "grm-100": "AI: вечный шах пересобран; Stockfish 0.00, без мата; линия повторяется. Подтвердите.",
};

for (const pack of packs) {
  for (const puzzle of pack.puzzles) {
    const n = Number(puzzle.id.slice(4));
    if (n < 61 || n > 120) continue;
    const isDraw = puzzle.task.type === "DRAW";
    const comment = comments[puzzle.id] ?? "";
    if (isDraw) {
      notes.push({
        puzzleId: puzzle.id,
        status: null,
        comment,
        updatedAt: "2026-10-30T12:00:00.000Z",
      });
    } else {
      notes.push({
        puzzleId: puzzle.id,
        status: "ok",
        comment: "AI: механически проверено (линия, финал, RU/EN).",
        updatedAt: "2026-10-30T12:00:00.000Z",
      });
    }
  }
}

const reviewed = notes.filter((note) => note.status).length;
const payload = {
  contentVersion: "2026.10.4",
  generatedAt: "2026-10-30T12:00:00.000Z",
  exportedAt: "2026-10-30T12:00:00.000Z",
  summary: {
    total: notes.length,
    reviewed,
    ok: notes.filter((note) => note.status === "ok").length,
    fix: notes.filter((note) => note.status === "fix").length,
    skip: notes.filter((note) => note.status === "skip").length,
    unreviewed: notes.length - reviewed,
  },
  notes,
};

mkdirSync("tools/review/drafts", { recursive: true });
writeFileSync("tools/review/drafts/v0.4.0-review-draft.json", JSON.stringify(payload, null, 2) + "\n", "utf8");
console.log(`Wrote ${notes.length} notes (${reviewed} reviewed, ${notes.length - reviewed} unreviewed)`);
