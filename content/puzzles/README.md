# Production-паки

Здесь лежат рабочие паки задач, которые попадают в APK через
`syncPuzzlePacks` → `build/generated/puzzlePacks/content/`.

| Файл | Уровень | v0.3.0 | Цель v1.0 |
| --- | --- | --- | --- |
| `novice.json` | Новичок | 60 задач, 6 глав | 120 задач, 12 глав |
| `professional.json` | Профессионал | 60 задач, 6 глав | 120 задач, 12 глав |
| `grandmaster.json` | Гроссмейстер | 60 задач, 6 глав | 120 задач, 12 глав |

**Статус:** 180 задач v0.3.0 (`contentVersion 2026.10.3`) собраны из Lichess
(CC0 1.0), проверены `validate-content.mjs` и прошли smoke. Отбор и
воспроизводимость — [`tools/lichess-selection-v030.json`](../../tools/lichess-selection-v030.json);
пайплайн — [`tools/import-lichess.mjs`](../../tools/import-lichess.mjs).

Формат и правила — в [CONTENT_AUTHORING.md](../../docs/development/CONTENT_AUTHORING.md),
состав — в [CONTENT PLAN](../../docs/design/CONTENT_PLAN.md).

Не добавляйте сюда черновики без прохождения валидатора и второго ревью.
Примеры формата лежат в [`../samples/`](../samples/).
