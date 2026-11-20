# Changelog

Все заметные изменения проекта. Формат — [Keep a Changelog](https://keepachangelog.com/ru/1.1.0/),
версии — [SemVer](https://semver.org/lang/ru/).

## [0.4.0-360-puzzles] — 2026-10-29

### Added

- 360 production-задач (`contentVersion 2026.10.4`): по 120 на уровень,
  12 глав × 10 задач.
- Шесть новых глав на уровень:
  - Новичок: `double-attack`, `remove-defender`, `discovered-attack`,
    `defense`, `mate-in-2`, `mixed-review`;
  - Профессионал: `mate-in-3`, `x-ray`, `zwischenzug`, `pawn-break`,
    `minor-endgame`, `mixed-review`;
  - Гроссмейстер: `study-draw`, `stalemate`, `fortress`, `perpetual-check`,
    `punish`, `final-exam`.
- Тип задачи `DRAW` в ядре (`TaskType`) и ярлык «Ничья» в UI.
- 30 авторских этюдов (`tools/etudes.mjs`, `tools/etudes/etudes.json`):
  10 патов (жертва фигуры с шахом → вынужденное взятие → пат), 10 крепостей
  (ладейная пешка в углу, оппозиция), 10 вечных шахов (угловой маятник ферзя).
  Для патов и вечных шахов единственный ответ соперника подтверждён машинно
  (`opp=1`).
- `study-draw` — Lichess `equality` (позиционная ничья); `minor-endgame`
  вместо `bishop-vs-knight` (`bishopEndgame` + `knightEndgame`).
- Названия новых глав RU/EN (`Localization.kt`, `strings.xml`).
- Инструменты: `multiframe-zst.mjs` (34 склеенных zstd-фрейма),
  `gen-texts.mjs` (авторинг 150 текстов), `probe-equality.mjs`
  (параллельная разведка equality), `study-draw-select.mjs`.
- `tools/precheck.mjs` — механический предчек текстов перед человеческим
  ревью: воспроизводит линию через chess.js и сверяет с ней тексты (число
  мата в промпте против `task.plies`, сторона хода, обещанная в WIN цель,
  спойлеры/SAN/поля, tier2/tier3, длина и RU/EN-синхрон разборов). На
  nov-001..029 не даёт ложных срабатываний высокого уровня.
- Drag-and-drop фигур, подсветка целей, drag-to-promotion и тактильная
  отдача; TalkBack-семантика 64 клеток с итоговым узлом доски.
- Release-подпись через `keystore.properties` (шаблон
  `keystore.properties.example`), release AAB в CI, monochrome-иконка и
  store-иконка 512×512.

### Changed

- Второе человеческое ревью v0.3.0: **180/180 задач «ОК»**, правок нет
  (`review-notes.json`).
- AI-assisted подготовка ревью 180 новых задач v0.4.0: 140 WIN/MATE
  проверены механически, 40 DRAW разобраны. Пакет —
  `docs/reviews/v0.4.0-ai-review.md`, черновик —
  `tools/review/drafts/v0.4.0-review-draft.json`.
- Второе человеческое ревью v0.4.0: **180/180 «ОК»**, ошибок и замечаний
  нет; итоговые заметки — `docs/reviews/v0.4.0-review-notes.json`.
- WARN-политика: DRAW-линии без механической ничьей (study-draw, fortress,
  вечный шах с 2-кратным повторением) документированы валидатором.

### Fixed

- `PLAN_ORDER`: `bishop-vs-knight` → `minor-endgame` (глава пропускалась при
  merge).
- Валидатор допускает чётную длину решения для `DRAW`: классический пат-этюд
  заканчивается вынужденным ходом соперника.
- Конь на доске ревьюера и Android-доске заменён на стандартный шахматный
  силуэт из шапки приложения (референс icon666): убрана искажённая форма из
  прототипа, обновлены `tools/review/template.html`,
  `design/prototype/index.html` и `PieceVectors.kt`.
- Предчек (`tools/precheck.mjs`) нашёл и исправил: в шести редких матах
  (`grm-042`, `grm-045`, `grm-047`–`grm-050`) русский промпт обещал «мат в
  четыре хода», тогда как линия занимает пять (EN уже говорил "mate in 5");
  в разборе `nov-028` в EN восстановлено упоминание ладьи (RU/EN-синхрон).
- По итогам первого прохода ревью (`nov-001…029`) у 77 WIN-задач убран общий
  промпт «Выиграйте материал»: цель названа конкретно — ферзь, ладья, слон,
  конь, превращение пешки или цугцванг. Валидатор теперь не пропускает
  формулировку «Выиграйте материал» / «Win material».
- Разборы `nov-020` (подводка и последовательность) и `nov-028` (один из
  возможных ответов соперника) уточнены; в ревьюере показывается SAN
  setup-хода, чтобы `O-O-O` и позиция после рокировки не вызывали вопросов.
- 23 авторских этюда v0.4.0 исправлены/пересобраны: 5 патов, 8 крепостей,
  10 вечных шахов. Проверка Stockfish/Lichess tablebase: у решающей стороны
  нет матовой или выигрывающей альтернативы; обновлены FEN, линии, тексты и
  подсказки (RU/EN).

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
- Тексты 180 задач прошли второе человеческое ревью (KI-017, закрыт в v0.4.0).

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

## [0.4.1] — 2026-11-20

Технический релиз: закрыт архитектурный долг `:core`/`:data`, адаптирован UI и
синхронизирована документация перед RuStore closed beta (v0.5.0).

### Added

- `puzzle_progress.contentVersion` и `chapter_progress` в Room: версия схемы 2,
  `Migration_1_2` и пересчёт денормализованного кэша глав (KI-018).
- Синхронный кэш `theme`/`language` на SharedPreferences с зеркалированием
  записей `SettingsDataStore` и чтением до первой композиции (KI-015).
- Unit-тесты `:data`: парсинг всех 360 задач (3 уровня, 36 глав, `MATE`/`WIN`/
  `DRAW`, RU/EN) и тесты Room DAO/миграции на in-memory базе.
- detekt 1.23.8 + `detekt-formatting` (ktlint) через convention-плагин
  `chessproblem.detekt`; лимит строки 120, единый конфиг и пустой baseline (KI-016).
- `ResponsiveScaffoldContainer` в `:ui`: колонка 560 dp, центрирование и поля
  32 dp на экранах ≥ 600 dp; применён на всех основных экранах (KI-010).
- Четыре smoke-теста Compose UI на JVM (Robolectric): главная, ход на доске,
  лист подсказок, результат; `:ui:testDebugUnitTest` в CI (KI-014).
- Просмотр лицензий из `app/src/main/assets/licenses/` на экране «О приложении»;
  версия из `BuildConfig.VERSION_NAME`, контент-версия из паков (KI-019).
- Полные тексты лицензий в `app/src/main/assets/licenses/`: `Apache-2.0.txt`,
  `OFL-1.1.txt`, `CC0-1.0.txt`, `BSD-2-Clause.txt` и `NOTICE` с атрибуцией
  Lichess, Kotlin, AndroidX, chesslib и шрифтов.
- `ADR-0009` (версионирование контента и защита прогресса в Room) и `ADR-0010`
  (синхронный кэш `theme`/`language`) — по шаблону ADR-0004.
- Документация синхронизирована с v0.4.0: README, ARCHITECTURE,
  GETTING_STARTED, CONTENT_AUTHORING, DESIGN/UI-UX SPEC, QA, USER GUIDE и
  `content/README.md` — 360 задач, 12 глав на уровень, `DRAW`, 30 авторских
  этюдов, планшетная колонка 560 dp.

### Changed

- Актуальная версия контента берётся из заголовков JSON-паков через
  `ContentVersionProvider`, а не захардкода; теперь она показывается в «О приложении».
- Версия приложения на экранах «О приложении» и «Настройки» берётся из
  `BuildConfig.VERSION_NAME` через параметры `:ui` (KI-019).
- `versionName` `0.4.1`, `versionCode` `401`.
- `PRIVACY POLICY` §2.2 и `ARCHITECTURE` §5.5/§6.3 согласованы: карточка
  «Продолжить» строится по Room `puzzle_progress.lastPlayedAt`, текущий таймер
  не сохраняется и не восстанавливается.
- KNOWN_ISSUES: KI-015/KI-016/KI-018/KI-019 закрыты; KI-010 — адаптация
  реализована, визуальная проверка остаётся; KI-014 — 4 JVM Compose UI-теста,
  остаются screenshot-тесты и process death/restore.

### Fixed

- KI-018: демо-прогресс v0.2.0 с пустой/устаревшей версией контента больше не
  проецируется на production-задачи; несовместимые строки удаляются при первой
  инициализации репозитория прогресса.
- KI-015: светлая тема и неверная локаль больше не мелькают на холодном старте.
- KI-016: все замечания detekt/ktlint устранены в коде, а не спрятаны в baseline.
- KI-010: контент и доска не растягиваются на планшете, оставаясь читаемыми.
- KI-019: тексты лицензий доступны из UI, захардкоженная версия `v0.3.0` удалена.

## [0.5.0-closed-beta] — 2026-11-20

### Added

- Магазинная иконка `store/icon-512.png`: 512×512 PNG без альфа-канала, фон
  залит фирменным `#08110E`, сгенерирована из `design/icon.svg` (KI-019/RuStore).
- Roborazzi 1.79.0 поверх Robolectric: 10 эталонных скриншотов — 5 экранов
  (главная, решатель, подсказки, результат, прогресс) в профилях 360×800 и
  800×1280; эталоны лежат в `ui/src/test/screenshots`, верификация —
  `:ui:verifyRoborazziDebug` в CI (KI-014).
- Снимок активной сессии (`PuzzleSessionSnapshot`) и восстановление доски,
  ходов, попыток, подсказок и остатка таймера через `SavedStateHandle` после
  process death; восстановленная сессия сразу ставится на паузу с overlay.
- Unit-тесты восстановления сессии в `:core` (пересчёт доски и счётчиков) и `:ui`
  (ViewModel: пауза, отсутствие тика до Resume, игнорирование завершённых
  снимков).
- R8-правило стабильности полей `PuzzleSessionSnapshot` и `PlayedMove`.

### Changed

- `AppViewModelFactory` передаёт `SavedStateHandle` из `CreationExtras` в
  ViewModel вместо пустой фабрики.
- Release-аудит: в merged manifest нет `INTERNET` и `android:debuggable`,
  `allowBackup="false"`, R8/ProGuard для chesslib, kotlinx.serialization, Room и
  снимка сессии проходят минификацию.
- Документация таймера и политики конфиденциальности согласована с сохранением
  остатка времени в state process death (KI-014).
- `store/README.md`: иконка для загрузки — корневой `store/icon-512.png`,
  добавлена команда генерации и проверки отсутствия прозрачности.

### Fixed

- Фикстура `PuzzleViewModelRestoreTest.activeSnapshot()` создавала завершённую
  сессию (демо-задача решалась ходом e2e4 и ответом соперника), из-за чего
  JVM-тесты восстановления падали; теперь снимок остаётся активным.

### Planned

- Аппаратная проверка process death/restore, drag-and-drop и TalkBack на
  эмуляторе/устройстве (KI-012).
- Аппаратная проверка планшета 7–10" (KI-010).
- RuStore closed beta (v0.5.0).

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
