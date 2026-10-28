# Production-паки

Здесь лежат рабочие паки задач, которые попадают в APK через
`syncPuzzlePacks` → `build/generated/puzzlePacks/content/`.

| Файл | Уровень | v0.2.1 | Цель v1.0 |
| --- | --- | --- | --- |
| `novice.json` | Новичок | 20 задач, 2 главы | 120 задач, 12 глав |
| `professional.json` | Профессионал | 20 задач, 2 главы | 120 задач, 12 глав |
| `grandmaster.json` | Гроссмейстер | 20 задач, 2 главы | 120 задач, 12 глав |

**Статус:** 60 задач v0.2.1 (`contentVersion 2026.10.2`) собраны из Lichess
(CC0 1.0), проверены `validate-content.mjs` и прошли ручной smoke. Отбор и
воспроизводимость — [`tools/lichess-selection.json`](../../tools/lichess-selection.json);
пайплайн — [`tools/import-lichess.mjs`](../../tools/import-lichess.mjs).

Формат и правила — в [CONTENT_AUTHORING.md](../../docs/development/CONTENT_AUTHORING.md),
состав — в [CONTENT PLAN](../../docs/design/CONTENT_PLAN.md).

Не добавляйте сюда черновики без прохождения валидатора и второго ревью.
Примеры формата лежат в [`../samples/`](../samples/).
