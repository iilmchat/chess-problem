# Changelog

Все заметные изменения проекта. Формат — [Keep a Changelog](https://keepachangelog.com/ru/1.1.0/),
версии — [SemVer](https://semver.org/lang/ru/).

## [0.3.0-180-puzzles] — 2026-10-29

### Added

- 180 production-задач (`contentVersion 2026.10.3`): по 60 на уровень,
  6 глав × 10 задач.
- Новые главы:
  - Новичок: `fork`, `pin`, `back-rank`, `promotion`;
  - Профессионал: `decoy`, `interference`, `endgame`, `sacrifice`;
  - Гроссмейстер: `zugzwang`, `underpromotion`, `rare-mate`, `calculation`.
- Воспроизводимый отбор 180 ID:
  `tools/lichess-selection-v030.json`; пул `candidates-v3.ndjson`.
- RU/EN названия, подсказки и разборы для 120 новых задач; неспойлерные
  названия глав и задач.
- Android UI: названия и локализация новых глав; app versionName `0.3.0`,
  versionCode `300`.
- `tools/review`: автономный офлайн-ревьюер 180 задач (`build-review.mjs`,
  `template.html`, generated `index.html`), две доски, RU/EN тексты, статусы,
  комментарии, localStorage и экспорт JSON/Markdown.

### Changed

- Пайплайн расширен с 6 до 18 глав; filter проверяет 6.1M строк и держит
  24 кандидата на главу.
- Валидатор KNOWN_CHAPTERS дополнен `pin`; production-паки 3 × 60.
- Обновлены README, CHANGELOG, KNOWN_ISSUES, CONCEPT/UI/ARCHITECTURE,
  CONTENT_AUTHORING, GETTING_STARTED, QA, USER_GUIDE и screenshots.

### Notes

- 12 WARN `grm-*`: маты в 4–5 ходов в главе `rare-mate`; это ожидаемые
  редкие случаи, ошибок нет.
- 180 из 360 задач; осталось 6 глав на уровень.
- Тексты 180 задач требуют второго человеческого ревью (KI-017).

## [0.2.1-60-puzzles] — 2026-10-28

### Added

- Lichess import pipeline `tools/import-lichess.mjs` (filter → draft → author →
  merge/split) и воспроизводимый отбор `tools/lichess-selection.json`.
- 60 production-задач (`contentVersion 2026.10.2`): по 20 на уровень,
  2 главы × 10 задач:
  `mate-in-1`, `capture-queen`, `mate-in-2`, `deflection`, `quiet-move`,
  `deep-sacrifice`.
- `syncPuzzlePacks`: production-паки из `content/puzzles/` копируются в
  generated assets через AGP 9 Variant API.
- RU/EN тексты, три подсказки и разборы для всех 60 задач; Lichess source URL
  и CC0-1.0.
- Локализованные названия новых глав в Android UI.

### Changed

- `validate-content.mjs` по умолчанию проверяет production-паки, если они
  есть; примеры `content/samples` проверяются отдельной командой.
- `tools/import-lichess.mjs author` сохраняет изменённые drafts (исправлен
  повторный merge), `merge` идемпотентен и работает с Windows-путями.
- README, CHANGELOG, KNOWN_ISSUES, CONTENT_AUTHORING, GETTING_STARTED,
  USER_GUIDE, ARCHITECTURE и QA-гайд обновлены под 60 задач.

### Notes

- Валидатор предупреждает о `grm-020` (мат в 4 хода) — это редкий, но
  допустимый случай.
- До 360 задач остаётся 300; следующие главы добавляются тем же пайплайном.
- DEMO-контент `content/samples` сохранён для схемы и примеров, но не
  попадает в APK.

## [0.2.0-vertical-slice] — 2026-10-28

### Added

- Android-проект: `app`, `core`, `data`, `ui`, `build-logic`, Gradle wrapper
  9.8.2, AGP 9.4.1, Kotlin 2.2.10, version catalog, convention-плагины.
- `core`: модели доски/задач, chesslib-обёртка, FEN/SAN, `PuzzleSession`,
  звёзды, XP, формула таймера; 21 unit-тест.
- `data`: JSON-загрузчик assets, Room-прогресс, DataStore-настройки,
  дневной кошелёк подсказок, SharedPreferences-локаль.
- `ui`: тема «Тихая доска», кастомная Canvas-доска с SVG-путями фигур,
  экраны главной, уровня, главы, решателя, подсказок, результата, прогресса,
  настроек и «О приложении», RU/EN.
- `app`: ручной DI, MainActivity с переключением локали, Navigation Compose,
  нижняя навигация, adaptive icon, data extraction rules.
- 4 демо-задачи в assets: мат в 1, превращение, вилка, мат в 2.
- Скриншоты Android vertical slice и чек-лист в
  `docs/screenshots/android/README.md`.
- Android job в CI: unit-тесты, debug APK и lint.
- `design/prototype/README.md`: сценарии проверки, курируемые скриншоты,
  ограничения и чек-лист передачи в Android.
- GitHub Actions `ci.yml`: проверка ссылок, валидация контента, Android job.
- Шаблон pull request с чек-листом для документации, контента и UI.
- Галерея прототипа и Android-срезов в корневом README.
- Журнал ходов в решателе: SAN-пилюли, счётчик `N / M`, пустое состояние.

### Changed

- `compileSdk` поднят до **37** (требование Compose 1.12.1 / AndroidX 2026.09);
  `targetSdk` остаётся **36**.
- AndroidX Compose BOM 2026.09.00, Room 2.8.5, DataStore 1.3.0,
  Navigation 2.10.2, Lifecycle 2.11.0.
- `GETTING_STARTED`, `ARCHITECTURE`, README и QA-гайд переведены из
  дизайн-фазы в статус Android vertical slice.
- UI/UX §8 синхронизирован с прототипом: SVG-фигуры, переворот доски,
  продолжение один раз, динамические звёзды/XP.
- `KI-001` и `KI-005` закрыты; добавлены ограничения vertical slice
  (KI-014…KI-016).

### Fixed

- Ответ соперника больше не откладывается бесконечно из-за перезапуска
  корутины на каждом тике таймера.
- Статус «Верно» и подсказки корректно исчезают по таймеру.
- Выход из задачи сохраняет попытку и показывает карточку «Продолжить».
- Локализация короткого времени (`26 с` / `26s`) и заголовка главной на RU.
- Шахматная доска: координаты, подсветка последнего хода, шаха, выбранной
  клетки, легальных целей, подсказки-стрелки и кольца.
- Прототип: фигуры и иконки — инлайновые SVG; устранён горизонтальный скролл
  на 360 dp; исправлены гигантские иконки результата.
- Прототип: звёзды и XP считаются по фактическому времени, ошибкам и
  подсказкам; продолжение только один раз; переворот пересчитывает координаты.

### Notes

- В репозитории **4 демо-задачи**, а не 360. Полный контент-пак — v0.2.1+.
- Нет drag-and-drop и TalkBack-навигации по клеткам; см. `KNOWN_ISSUES.md`.
- Монетизация по-прежнему не подключена: RuStore требует ИП/юрлицо.

## [Unreleased]

### Planned

- 60 задач в v0.2.1, затем 180 и 360.
- Drag-and-drop, TalkBack-семантика клеток, Compose UI/screenshot tests.
- detekt/ktlint, release-подпись, RuStore closed beta.

## [0.1.0-design] — 2026-10-28

### Added

- Гейм-дизайн MVP: концепция, аудитория, три уровня, 36 глав, таймер, звёзды,
  подсказки, продолжение, экономика, монетизация, риски и дорожная карта.
- UI/UX-спецификация: дизайн-токены, компоненты, экраны, состояния,
  доступность, локализация и handoff.
- Интерактивный HTML-прототип четырёх экранов: главная, решатель, подсказки,
  результат.
- Концепт иконки `design/icon.svg` и дизайн-токены `design/tokens.json`.
- Контент-план на 360 задач и JSON Schema пака задач.
- Четыре проверенные примерные задачи: мат в 1, превращение, вилка, мат в 2.
- Валидатор контента на Node.js + chess.js и проверка относительных ссылок.
- CI GitHub Actions: проверка документации и примеров контента.
- Документация: архитектура, getting started, content authoring, RuStore,
  releases, rules, ADR, privacy policy, third-party licenses, QA, user guide,
  store draft, known issues.

### Notes

- Android-код ещё не создан: это осознанная дизайн-фаза, а не забытый scaffold.
- Монетизация спроектирована, но не подключена: RuStore требует ИП/юрлицо для
  платежей. До этого приложение остаётся бесплатным и без рекламы.
