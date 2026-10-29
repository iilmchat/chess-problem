#!/usr/bin/env node
/**
 * Generate authoring texts for the v0.4.0 Lichess chapters.
 *
 * Reads the per-chapter drafts, replays every line with chess.js and writes a
 * texts sidecar (keyed by lichessId) in the established house style:
 * chapter title, task prompt, three tiered hints and a line-based explanation.
 *
 * Output: tools/var/import-texts-v040.json
 */
import { readFile, writeFile, readdir } from "node:fs/promises";
import path from "node:path";
import { fileURLToPath } from "node:url";
import { Chess } from "chess.js";

const ROOT = path.resolve(path.dirname(fileURLToPath(import.meta.url)), "..");
const DRAFT_DIR = path.join(ROOT, "tools", "var", "import-drafts");
const OUT = path.join(ROOT, "tools", "var", "import-texts-v040.json");

const CHAPTERS = [
  ["NOVICE", "double-attack"], ["NOVICE", "remove-defender"],
  ["NOVICE", "discovered-attack"], ["NOVICE", "defense"],
  ["NOVICE", "mate-in-2"], ["NOVICE", "mixed-review"],
  ["PROFESSIONAL", "mate-in-3"], ["PROFESSIONAL", "x-ray"],
  ["PROFESSIONAL", "zwischenzug"], ["PROFESSIONAL", "pawn-break"],
  ["PROFESSIONAL", "minor-endgame"], ["PROFESSIONAL", "mixed-review"],
  ["GRANDMASTER", "punish"], ["GRANDMASTER", "final-exam"],
  ["GRANDMASTER", "study-draw"],
];

const PIECE_RU = { p: "пешкой", n: "конём", b: "слоном", r: "ладьёй", q: "ферзём", k: "королём" };
const PIECE_EN = { p: "pawn", n: "knight", b: "bishop", r: "rook", q: "queen", k: "king" };
const PIECE_RU_2 = { p: "пешка", n: "конь", b: "слон", r: "ладья", q: "ферзь", k: "король" };
const PIECE_EN_2 = { p: "pawn", n: "knight", b: "bishop", r: "rook", q: "queen", k: "king" };

const THEMES = {
  "double-attack": {
    title: "Двойной удар", titleEn: "Double attack",
    hint1: "Одна фигура может создать две угрозы сразу — ищите двойной удар.",
    hint1En: "One piece can create two threats at once — look for a double attack.",
    phrase: "одна фигура создаёт две угрозы", phraseEn: "one piece creates two threats",
  },
  "remove-defender": {
    title: "Устранение защитника", titleEn: "Remove the defender",
    hint1: "Уберите защитника ключевого поля — и комбинация сработает.",
    hint1En: "Remove the defender of the key square — then the combination works.",
    phrase: "защитник отвлекается или устраняется", phraseEn: "the defender is removed or deflected",
  },
  "discovered-attack": {
    title: "Вскрытое нападение", titleEn: "Discovered attack",
    hint1: "Отведите фигуру с линии — откроется нападение.",
    hint1En: "Move a piece off the line — a discovered attack appears.",
    phrase: "фигура уходит с линии, открывая нападение", phraseEn: "a piece steps aside to open the attack",
  },
  defense: {
    title: "Защита и контрудар", titleEn: "Defence and counterblow",
    hint1: "Сначала отразите угрозу, затем нанесите контрудар.",
    hint1En: "First parry the threat, then strike back.",
    phrase: "сначала защита, затем контрудар", phraseEn: "first defend, then counterattack",
  },
  "mate-in-2": {
    title: "Мат в два", titleEn: "Mate in two",
    hint1: "Первый ход не обязан быть шахом.",
    hint1En: "The first move does not have to be a check.",
    phrase: "начинается форсированный мат", phraseEn: "a forced mate begins",
  },
  "mate-in-3": {
    title: "Мат в три", titleEn: "Mate in three",
    hint1: "Мат форсирован: каждый ответ соперника ведёт к цели.",
    hint1En: "The mate is forced: every reply leads to the goal.",
    phrase: "начинается форсированный мат", phraseEn: "a forced mate begins",
  },
  "x-ray": {
    title: "Рентген", titleEn: "X-ray",
    hint1: "Смотрите сквозь фигуру: линия сохраняет силу.",
    hint1En: "Look through the piece: the line keeps its power.",
    phrase: "фигура действует сквозь другую", phraseEn: "a piece works through another",
  },
  zwischenzug: {
    title: "Промежуточный ход", titleEn: "Zwischenzug",
    hint1: "Не отдавайте темп: вставьте промежуточный ход.",
    hint1En: "Do not give up the tempo: insert an in-between move.",
    phrase: "промежуточный ход меняет порядок", phraseEn: "an in-between move changes the order",
  },
  "pawn-break": {
    title: "Пешечный прорыв", titleEn: "Pawn break",
    hint1: "Пешечный прорыв вскрывает позицию.",
    hint1En: "A pawn break opens the position.",
    phrase: "пешечный прорыв вскрывает позицию", phraseEn: "a pawn break opens the position",
  },
  "minor-endgame": {
    title: "Минорный эндшпиль", titleEn: "Minor-piece endgame",
    hint1: "Слон и конь в эндшпиле: точность важнее материала.",
    hint1En: "Bishop and knight in the endgame: precision matters more than material.",
    phrase: "точная игра в минорном эндшпиле", phraseEn: "precise play in the minor-piece endgame",
  },
  punish: {
    title: "Наказание ошибки", titleEn: "Punish the mistake",
    hint1: "Соперник ошибся — накажите немедленно.",
    hint1En: "The opponent erred — punish it at once.",
    phrase: "слабость соперника используется", phraseEn: "the opponent's weakness is exploited",
  },
  "mixed-review": {
    title: "Смешанное повторение", titleEn: "Mixed review",
    hint1: "Оцените все угрозы и найдите сильнейший ход.",
    hint1En: "Assess every threat and find the strongest move.",
    phrase: "найден сильнейший ход", phraseEn: "the strongest move is found",
  },
  "final-exam": {
    title: "Финальный экзамен", titleEn: "Final exam",
    hint1: "Соберите всё: угрозы, тактика, точный расчёт.",
    hint1En: "Put it all together: threats, tactics, precise calculation.",
    phrase: "найден сильнейший ход", phraseEn: "the strongest move is found",
  },
  "study-draw": {
    title: "Этюдная ничья", titleEn: "Study draw",
    hint1: "Вы проигрывали — ищите путь к равновесию: упрощение, вечный шах или пат.",
    hint1En: "You were losing — look for the way back: trades, perpetual check or stalemate.",
    phrase: "позиция возвращается к равновесию", phraseEn: "the position returns to balance",
  },
};

const files = (await readdir(DRAFT_DIR)).filter((name) => name.endsWith(".draft.json"));
const sidecar = {};
let generated = 0;

for (const [level, chapter] of CHAPTERS) {
  const name = `${level.toLowerCase()}-${chapter}.draft.json`;
  if (!files.includes(name)) {
    console.error(`missing draft ${name}`);
    process.exitCode = 1;
    continue;
  }
  const draft = JSON.parse(await readFile(path.join(DRAFT_DIR, name), "utf8"));
  const theme = THEMES[chapter];
  for (const puzzle of draft.puzzles) {
    const chess = new Chess(puzzle.fen);
    if (puzzle.setupMove) chess.move(uci(puzzle.setupMove));
    const side = chess.turn();
    const sideRu = side === "w" ? "белых" : "чёрных";
    const sideEn = side === "w" ? "White" : "Black";
    const offset = puzzle.setupMove ? 1 : 0;
    const line = puzzle.sanLine.slice(offset);
    const first = line[0];
    const from = puzzle.solution[0].slice(0, 2);
    const piece = chess.get(from)?.type ?? "q";
    const plies = puzzle.task.plies;

    const promptRu = puzzle.task.type === "MATE"
      ? `Ход ${sideRu}. Найдите мат в ${mateWords(plies).ru}.`
      : puzzle.task.type === "DRAW"
        ? `Ход ${sideRu}. Добейтесь ничьей.`
        : `Ход ${sideRu}. Добейтесь решающего преимущества.`;
    const promptEn = puzzle.task.type === "MATE"
      ? `${sideEn} to move. Find mate in ${mateWords(plies).en}.`
      : puzzle.task.type === "DRAW"
        ? `${sideEn} to move. Draw the game.`
        : `${sideEn} to move. Get a decisive advantage.`;

    const rest = line.slice(1).join(" ");
    const tailRu = rest ? `Далее ${rest}.` : "";
    const tailEn = rest ? `Then ${rest}.` : "";

    sidecar[puzzle.lichessId] = {
      title: {
        ru: puzzle.task.type === "MATE" ? theme.title : `${theme.title}: ${PIECE_RU[piece]}`,
        en: puzzle.task.type === "MATE" ? theme.titleEn : `${theme.titleEn}: ${PIECE_EN[piece]}`,
      },
      prompt: { ru: promptRu, en: promptEn },
      hints: [
        { tier: 1, ru: theme.hint1, en: theme.hint1En },
        { tier: 2, ru: `Ключевая фигура — ${PIECE_RU_2[piece]} с поля ${from}.`, en: `The key piece is the ${PIECE_EN_2[piece]} on ${from}.` },
        { tier: 3, ru: `Первый ход: ${first}.`, en: `First move: ${first}.` },
      ],
      explanation: {
        ru: `Первый ход — ${first}: ${theme.phrase}. ${tailRu} Линия — ${line.join(" ")}.`.replace(/\s+/g, " ").trim(),
        en: `The first move is ${first}: ${theme.phraseEn}. ${tailEn} The line is ${line.join(" ")}.`.replace(/\s+/g, " ").trim(),
      },
    };
    generated += 1;
  }
}

await writeFile(OUT, JSON.stringify(sidecar, null, 2) + "\n", "utf8");
console.log(`Wrote ${path.relative(ROOT, OUT)} (${generated} texts)`);
if (generated !== 150) {
  console.error(`expected 150 texts, got ${generated}`);
  process.exitCode = 1;
}

function uci(value) {
  return { from: value.slice(0, 2), to: value.slice(2, 4), promotion: value.length > 4 ? value[4] : undefined };
}

function mateWords(plies) {
  const ru = { 1: "один ход", 2: "два хода", 3: "три хода", 4: "четыре хода", 5: "пять ходов" }[plies] ?? `${plies} хода`;
  const en = { 1: "one", 2: "two", 3: "three", 4: "four", 5: "five" }[plies] ?? String(plies);
  return { ru, en };
}
