# Контент

**Статус:** контент-фаза; 4 демо-задачи в Android · **Дата:** 28.10.2026
**Связано:** [CONTENT PLAN](../docs/design/CONTENT_PLAN.md) ·
[CONTENT AUTHORING](../docs/development/CONTENT_AUTHORING.md) ·
[схема](schema/puzzle-pack.schema.json)

Здесь лежат задачи Chess Problem: JSON-паки, схема и проверенные примеры.
Контент поставляется в APK/AAB, работает офлайн и не требует сервера.

---

## 1. Структура

```text
content/
├── schema/
│   └── puzzle-pack.schema.json   ← JSON Schema 2020-12
├── samples/
│   ├── novice.sample.json        ← 2 задачи
│   ├── professional.sample.json  ← 1 задача
│   └── grandmaster.sample.json   ← 1 задача
├── puzzles/
│   ├── novice.json               ← production, появится в v0.2.1
│   ├── professional.json
│   └── grandmaster.json
└── README.md
```

Один файл — один уровень. Production-паки собираются из 12 глав по 10 задач
(120 на уровень, 360 всего). Примеры нужны для проверки схемы, валидатора и
CI, а не для релиза.

---

## 2. Схема пака

```json
{
  "schemaVersion": 1,
  "contentVersion": "2026.10.1",
  "level": "NOVICE",
  "puzzles": []
}
```

| Поле | Тип | Правило |
| --- | --- | --- |
| `schemaVersion` | int | `1`; растёт при ломающих изменениях |
| `contentVersion` | string | `YYYY.MM.N` |
| `level` | enum | `NOVICE`, `PROFESSIONAL`, `GRANDMASTER` |
| `puzzles` | array | 1–120 задач |

Поля задачи:

| Поле | Тип | Правило |
| --- | --- | --- |
| `id` | string | `nov-001`, `pro-001`, `grm-001`; уникален навсегда |
| `chapter` | string | kebab-case ID главы |
| `fen` | string | позиция **до** `setupMove`; легальная |
| `setupMove` | UCI \| null | ход соперника-подводка |
| `solution` | UCI[] | нечётная длина; начинается с хода пользователя |
| `task.type` | enum | `MATE`, `WIN`, `DRAW` |
| `task.plies` | int | число ходов пользователя |
| `rating` | int | 600–2600, соответствует уровню |
| `themes` | string[] | 1–5 тегов |
| `timeLimitSec` | int | 60–420; формула из DESIGN §6.1 |
| `title`, `prompt` | `{ru,en}` | короткий заголовок и задание |
| `hints` | array | ровно 3: идея, фигура, ход |
| `explanation` | `{ru,en}` | 2–4 предложения |
| `source` | object | Lichess CC0 или original/proprietary |

Полная схема — [puzzle-pack.schema.json](schema/puzzle-pack.schema.json).
Человеческое описание — [CONTENT_AUTHORING.md](../docs/development/CONTENT_AUTHORING.md).

---

## 3. Примеры

| ID | Уровень | Тема | Задание | Рейтинг | Время |
| --- | --- | --- | --- | --- | --- |
| `nov-001` | Новичок | Мат по последней горизонтали | Мат в 1 | 800 | 115 с |
| `nov-002` | Новичок | Превращение | Провести пешку в ферзи | 850 | 120 с |
| `pro-001` | Профессионал | Вилка конём | Выиграть ферзя | 1450 | 185 с |
| `grm-001` | Гроссмейстер | Мат в 2 | Мат в 2 | 2100 | 245 с |

Все четыре задачи — авторские (`source.provider = "original"`), проверены
вручную и проходят валидатор. Это не финальный контент, а эталон формата.

---

## 4. Проверка

```bash
npm ci --prefix tools

node tools/validate-content.mjs content/samples/novice.sample.json
node tools/validate-content.mjs content/samples/professional.sample.json
node tools/validate-content.mjs content/samples/grandmaster.sample.json
```

Ожидаемый вывод:

```text
OK  content/samples/novice.sample.json: 2 puzzles
OK  content/samples/professional.sample.json: 1 puzzle
OK  content/samples/grandmaster.sample.json: 1 puzzle
```

Валидатор проверяет:

- JSON и обязательные поля;
- FEN, `setupMove`, сторону хода;
- легальность и длину линии;
- мат в конце для `MATE`;
- `task.plies`;
- дубли ID/FEN;
- RU/EN;
- время и формулу;
- источник/лицензию.

Он **не** заменяет ручное ревью: альтернативные выигрыши, качество подсказок
и объяснений проверяет второй человек.

---

## 5. Как добавить задачу

1. Прочитайте [CONTENT_AUTHORING.md](../docs/development/CONTENT_AUTHORING.md).
2. Выберите главу и ID из [CONTENT PLAN](../docs/design/CONTENT_PLAN.md).
3. Создайте FEN и линию; проверьте в Lichess/chess.js.
4. Заполните все поля, включая 3 подсказки и объяснение RU/EN.
5. Запустите валидатор.
6. Попросите второго человека решить задачу без подсказок.
7. Откройте PR с `content(<level>): ...`.

---

## 6. Лицензии и атрибуция

- **Lichess puzzle DB** — CC0 1.0. Атрибуция в приложении и
  [THIRD_PARTY_LICENSES.md](../docs/legal/THIRD_PARTY_LICENSES.md).
- **Авторские задачи** — proprietary, © RuChating (iilmchat).
- Имена игроков, партии и copyrighted-аннотации не сохраняются.
- Lichess не связан с приложением и не поддерживает его.

---

## 7. Версионирование

- `schemaVersion` — ломающие изменения схемы.
- `contentVersion` — `YYYY.MM.N`; показывается в «О приложении».
- ID задач неизменяемы; удалённые ID не переиспользуются.
- При исправлении линии ID сохраняется, `contentVersion` растёт.
- Прогресс сопоставляется по ID; новые задачи не сбрасывают старый прогресс.

---

## 8. CI

GitHub Actions (`docs-content`) запускает:

```bash
npm ci --prefix tools
node tools/check-docs.mjs
node tools/validate-content.mjs content/samples/*.json
```

Production-паки добавляются в проверку после v0.2.0. Красный валидатор —
блокер мержа.
