# Архитектура Chess Problem

**Статус:** целевая архитектура до scaffold · **Версия:** 0.1 · **Дата:** 28.10.2026
**Связано:** [DESIGN](../design/DESIGN_V01_MVP.md) · [RULES](RULES.md) · [ADR](adr/README.md)

Документ описывает целевую архитектуру Android-приложения. Код появится в
v0.2.0; до этого документ — контракт для реализации и ревью.

---

## 1. Принципы

1. **Правила — в `core`, UI — в `ui`.** Шахматная логика, сессия, таймер и
   звёзды не зависят от Android и Compose.
2. **Офлайн-first.** Контент в assets, прогресс в Room/DataStore, никаких
   сетевых вызовов в v1.0.0.
3. **Однонаправленный поток.** UI отправляет события, `PuzzleSession`/ViewModel
   возвращает неизменяемое состояние.
4. **Доска — кастомный Canvas.** Никаких сторонних board-библиотек: контроль
   над доступностью, анимациями и производительностью.
5. **Никакого движка.** Подсказки и решения — из проверенного контента; для
   матов принимаются альтернативные маты. Это дешевле, быстрее и честнее.
6. **Монетизация — адаптер.** Реклама и покупки не проникают в `core` и `ui`
   напрямую; на старте их нет.

---

## 2. Карта модулей

```mermaid
graph TD
    app[:app<br/>entry, DI, navigation, platform] --> ui[:ui<br/>Compose, screens, viewmodels]
    app --> data[:data<br/>Room, DataStore, content loader]
    ui --> core[:core<br/>chess, session, scoring, timer, models]
    data --> core
    core --> chesslib[chesslib 1.3.7<br/>Apache 2.0]
    data --> assets[content/*.json<br/>bundled assets]
    app --> ads[Ads/Billing adapters<br/>phase 2, feature flag]
```

| Модуль | Содержит | Зависимости |
| --- | --- | --- |
| `:app` | `MainActivity`, `Application`, `AppContainer`, навигация, locale, ads/billing adapters | `:ui`, `:data`, `:core` |
| `:ui` | Compose theme, components, board, screens, ViewModels | `:core` |
| `:data` | Room, DataStore, JSON loader, repository implementations | `:core` |
| `:core` | модели, chesslib wrapper, `PuzzleSession`, scoring, timer, repository interfaces | Kotlin, chesslib |
| `build-logic` | convention plugins: Android app, Kotlin JVM, Compose, detekt | Gradle, AGP, KGP |

**Правила зависимостей:**

- `:core` не импортирует `android.*`, `androidx.*`, `com.iilmchat.chessproblem.ui.*`.
- `:ui` не импортирует `:data`; получает репозитории через интерфейсы `:core`.
- `:data` не импортирует `:ui`.
- `:app` — единственное место, где собираются реализации и интерфейсы.
- `:core` может зависеть от chesslib (JVM), но не от Android.

---

## 3. Структура пакетов

```text
com.iilmchat.chessproblem
├── app/
│   ├── ChessProblemApp.kt
│   ├── MainActivity.kt
│   ├── AppContainer.kt
│   ├── navigation/
│   │   ├── AppNavHost.kt
│   │   └── Routes.kt
│   └── platform/
│       ├── locale/LocaleController.kt
│       ├── ads/AdGateway.kt          ← phase 2
│       └── billing/BillingGateway.kt ← phase 2
├── core/
│   ├── chess/
│   │   ├── ChessRules.kt
│   │   ├── ChesslibRules.kt
│   │   ├── UciMove.kt
│   │   └── BoardSnapshot.kt
│   ├── model/
│   │   ├── Puzzle.kt
│   │   ├── PuzzlePack.kt
│   │   ├── Level.kt
│   │   ├── Chapter.kt
│   │   ├── Task.kt
│   │   ├── Hint.kt
│   │   └── Progress.kt
│   ├── session/
│   │   ├── PuzzleSession.kt
│   │   ├── PuzzleState.kt
│   │   ├── PuzzleEvent.kt
│   │   └── SessionResult.kt
│   ├── scoring/
│   │   ├── StarPolicy.kt
│   │   ├── XpPolicy.kt
│   │   └── StreakPolicy.kt
│   ├── timer/
│   │   ├── PuzzleTimer.kt
│   │   └── TimeSource.kt
│   └── repository/
│       ├── PuzzleRepository.kt
│       ├── ProgressRepository.kt
│       └── SettingsRepository.kt
├── data/
│   ├── content/
│   │   ├── AssetPuzzleSource.kt
│   │   ├── PuzzlePackJson.kt
│   │   └── ContentValidator.kt
│   ├── local/
│   │   ├── ChessDatabase.kt
│   │   ├── entity/
│   │   ├── dao/
│   │   └── settings/SettingsDataStore.kt
│   └── repository/
│       ├── DefaultPuzzleRepository.kt
│       ├── DefaultProgressRepository.kt
│       └── DefaultSettingsRepository.kt
└── ui/
    ├── theme/
    │   ├── Color.kt
    │   ├── Type.kt
    │   ├── Shape.kt
    │   └── ChessProblemTheme.kt
    ├── components/
    │   ├── board/ChessBoard.kt
    │   ├── board/PieceLayer.kt
    │   ├── TimerPill.kt
    │   ├── TaskCard.kt
    │   ├── StarRow.kt
    │   ├── LevelCard.kt
    │   └── ...
    ├── screens/
    │   ├── home/
    │   ├── level/
    │   ├── puzzle/
    │   ├── result/
    │   ├── hints/
    │   ├── progress/
    │   ├── settings/
    │   └── about/
    └── viewmodel/
        ├── HomeViewModel.kt
        ├── PuzzleViewModel.kt
        └── ProgressViewModel.kt
```

---

## 4. Шахматное ядро

### 4.1. Библиотека

Используем **chesslib 1.3.7** (Apache 2.0, JitPack): FEN, SAN, легальные ходы,
мат, пат, рокировка, превращение, взятие на проходе. Своё ядро не пишем:
ошибка в генераторе ходов — критический баг, а библиотека проверена perft-тестами.

Обёртка `ChessRules`:

```kotlin
interface ChessRules {
    fun load(fen: String): BoardSnapshot
    fun legalMoves(board: BoardSnapshot, from: Square): List<Move>
    fun move(board: BoardSnapshot, move: Move): MoveResult
    fun toSan(board: BoardSnapshot, move: Move): String
    fun isCheckmate(board: BoardSnapshot): Boolean
    fun isStalemate(board: BoardSnapshot): Boolean
    fun isDraw(board: BoardSnapshot): Boolean
}
```

`BoardSnapshot` — неизменяемая модель: 64 клетки, сторона хода, права
рокировки, поле en passant, счётчики. UI не работает с `chesslib.Board`
напрямую: только через снимки, чтобы Compose видел изменения.

### 4.2. Perft

Обязательные тесты:

- стартовая позиция, depth 1–5;
- Kiwipete, depth 1–4;
- позиции 3–6 из стандартного набора;
- рокировка, en passant, превращение, мат/пат.

Если perft не совпадает — релиз блокируется.

---

## 5. Сессия задачи

### 5.1. Состояние

```kotlin
data class PuzzleState(
    val puzzleId: String,
    val board: BoardSnapshot,
    val status: Status,
    val moves: List<PlayedMove>,
    val attemptsLeft: Int,
    val hintsUsed: Set<HintTier>,
    val elapsedMs: Long,
    val timeLimitMs: Long,
    val stars: Int?,
    val starsCap: Int,
    val solutionIndex: Int,
    val feedback: Feedback?,
) {
    enum class Status { LOADING, READY, SOLVING, OPPONENT, SOLVED, FAILED, REVIEW }
}
```

`PlayedMove` хранит `uci`, `san`, `byUser`, `isCorrect`, `timestamp`.

### 5.2. События

```kotlin
sealed interface PuzzleEvent {
    data class SelectSquare(val square: Square)
    data class MakeMove(val from: Square, val to: Square, val promotion: PieceType?)
    data object Tick
    data class UseHint(val tier: HintTier)
    data object Pause
    data object Resume
    data object ContinueWithTime
    data object Retry
    data object Reveal
    data object FlipBoard
}
```

### 5.3. Машина состояний

```text
LOADING → READY → SOLVING ⇄ OPPONENT
                    │           │
                    │           └──(ответ применён)──> SOLVING
                    ├──(ошибка, attempts > 0)──> SOLVING (revert)
                    ├──(ошибка, attempts = 0)──> FAILED
                    ├──(время 0)──> FAILED
                    └──(мат/цель)──> SOLVED → REVIEW
FAILED → CONTINUE (SOLVING, +60s, 1 attempt) | RETRY (READY) | REVEAL (REVIEW)
```

### 5.4. Проверка хода

1. `ChessRules.legalMoves` — ход легален?
2. Если `task.type == MATE` и ход ставит мат — принять, даже если он не в линии.
3. Иначе сравнить UCI с `solution[solutionIndex]`.
4. Если совпал:
   - применить ход, добавить в историю;
   - если это последний ход — `SOLVED`;
   - иначе `OPPONENT`, применить ответ из `solution[solutionIndex + 1]`,
     вернуться в `SOLVING`.
5. Если не совпал:
   - вернуть снимок до хода;
   - `attemptsLeft--`;
   - `feedback = WrongMove`;
   - если 0 — `FAILED`.

### 5.5. Таймер

`PuzzleTimer` — чистый Kotlin:

```kotlin
class PuzzleTimer(
    private val timeSource: TimeSource,
    private val limitMs: Long,
) {
    fun start()
    fun pause()
    fun resume()
    fun elapsedMs(): Long
    fun remainingMs(): Long
    fun isExpired(): Boolean
}
```

- UI вызывает `tick()` раз в 250 мс; таймер не создаёт собственный поток.
- Пауза на lifecycle, hint sheet, ad, диалогах.
- `elapsedMs` сохраняется в Room/DataStore после каждого хода и при паузе.
- После process death — восстановление с `status = READY` и `elapsedMs`,
  таймер на паузе; игрок нажимает «Продолжить».

---

## 6. Данные

### 6.1. Контент

- JSON-паки в `app/src/main/assets/content/` (копируются из `content/puzzles/`).
- Загрузка через `kotlinx.serialization`; валидация в debug и CI.
- `PuzzleRepository` отдаёт `Flow<List<Puzzle>>` по уровню/главе; кэш в памяти.
- `contentVersion` показывается в «О приложении».
- При обновлении приложения контент заменяется; прогресс по ID сохраняется.

### 6.2. Room

```text
puzzle_progress(
  puzzleId TEXT PRIMARY KEY,
  solved INTEGER NOT NULL,
  reviewed INTEGER NOT NULL,
  stars INTEGER NOT NULL,
  bestTimeMs INTEGER,
  attempts INTEGER NOT NULL,
  hintsUsed INTEGER NOT NULL,
  lastPlayedAt INTEGER NOT NULL
)

chapter_progress(
  chapterId TEXT PRIMARY KEY,
  solvedCount INTEGER NOT NULL,
  stars INTEGER NOT NULL,
  completedAt INTEGER
)

daily_streak(
  date TEXT PRIMARY KEY,      -- ISO local date
  solvedCount INTEGER NOT NULL
)

achievement(
  id TEXT PRIMARY KEY,
  unlockedAt INTEGER
)
```

- `puzzle_progress` — единственный источник звёзд.
- `chapter_progress` — денормализованный кэш для быстрых списков; пересчитывается
  из `puzzle_progress` при старте.
- `daily_streak` — для серии и задачи дня.
- Индексы: `puzzleId`, `chapterId`, `date`.

### 6.3. DataStore

Ключи:

- `language`: `system|ru|en`
- `theme`: `system|dark|light`
- `boardTheme`: `sage|classic|slate|high_contrast`
- `coordinates`: Boolean
- `autoFlip`: Boolean
- `colorblind`: Boolean
- `reduceMotion`: Boolean
- `sound`: Boolean
- `haptics`: Boolean
- `timerSound`: Boolean
- `onboardingSeen`: Boolean
- `lastPuzzleId`: String?
- `lastPuzzleElapsedMs`: Long
- `dailyReminder`: Boolean (v0.4.0)

### 6.4. Backup

`android:allowBackup="false"` в v1.0.0: прогресс не уходит в облако. Это
согласуется с политикой «данные остаются на устройстве». Если появится
облачная синхронизация — отдельное решение и политика.

---

## 7. UI-слой

### 7.1. Compose

- Material 3, кастомная тема из `design/tokens.json`.
- Навигация — Navigation Compose с type-safe routes (kotlinx.serialization).
- ViewModel на экран; `StateFlow<PuzzleState>`; события — методы.
- Никакой бизнес-логики в composables: только отображение и вызов событий.
- `collectAsStateWithLifecycle()` для состояния.
- Иммутабельные UI-модели; стабильные ключи в списках.

### 7.2. Доска

- `ChessBoard` — `Canvas` + `Layout` для фигур.
- Клетки — 8×8; координаты — `drawText` с `TextMeasurer`.
- Фигуры — `ImageVector`/`Path` из собственного набора; кэш `ImageBitmap`.
- Анимация хода — `Animatable<Offset>` 180 мс; при `reduceMotion` — 0.
- Слои: поля → подсветки → фигуры → стрелки/точки → координаты.
- Семантика: `Modifier.semantics` на каждую клетку; действия «Выбрать»,
  «Ход», «Отменить выбор».
- Drag: `pointerInput` + `detectDragGestures`; tap: `detectTapGestures`.
- Никаких аллокаций в `draw`; пути и кисти — кэш.

### 7.3. Состояния экранов

| Экран | ViewModel | Состояние |
| --- | --- | --- |
| Home | `HomeViewModel` | daily, continue, levels, streak, loading/error |
| Level | `LevelViewModel` | chapters, progress, recommended |
| Puzzle | `PuzzleViewModel` | `PuzzleState` + hints + timer |
| Result | `ResultViewModel` | stars, explanation, next |
| Progress | `ProgressViewModel` | stats, achievements |
| Settings | `SettingsViewModel` | settings, language, reset |

### 7.4. Навигация

```text
home
level/{level}
chapter/{chapterId}
puzzle/{puzzleId}
result/{puzzleId}
progress
settings
about
```

- `puzzle` — full screen, bottom nav hidden.
- `result` — full screen или sheet; в v0.2.0 — full screen.
- Back stack: home → level → chapter → puzzle → result.
- Deep links не нужны.

---

## 8. DI и сборка

### 8.1. Ручной DI

`:app` создаёт `AppContainer`:

```kotlin
class AppContainer(context: Context) {
    val database by lazy { ChessDatabase.create(context) }
    val settings by lazy { SettingsDataStore(context) }
    val puzzleRepository: PuzzleRepository by lazy {
        DefaultPuzzleRepository(AssetPuzzleSource(context.assets))
    }
    val progressRepository: ProgressRepository by lazy {
        DefaultProgressRepository(database.puzzleProgressDao())
    }
}
```

- ViewModel factories получают зависимости из `AppContainer`.
- Никакого Hilt/Koin в MVP: меньше kapt/KSP, быстрее сборка, проще тесты.
- Если модулей станет больше 8 или DI-граф вырастет — пересмотреть (ADR-0007).

### 8.2. Gradle

- Gradle 9.8.2, AGP 9.4.1, Kotlin 2.2.10, JDK 21, target 17.
- `gradle/libs.versions.toml` — единственный источник версий.
- `build-logic` — convention plugins:
  - `chessproblem.android.application`
  - `chessproblem.android.library`
  - `chessproblem.kotlin.jvm`
  - `chessproblem.compose`
  - `chessproblem.detekt`
- `org.gradle.caching=true`, `org.gradle.configuration-cache=true`.
- R8 + shrinkResources в release.
- Подпись — из `keystore.properties` (не коммитится).

---

## 9. Тестирование

| Уровень | Что | Где |
| --- | --- | --- |
| Unit | perft, FEN, SAN, сессия, таймер, звёзды, XP, серия | `core/src/test` |
| Unit | JSON-парсинг, Room DAO, репозитории | `data/src/test` |
| UI | выбор фигуры, ход, подсказка, результат, смена языка | `ui/src/androidTest` |
| Screenshot | главная, решатель, результат, подсказки (Paparazzi/Roborazzi) | `ui/src/test` |
| Content | схема, легальность, мат, дубли, локализация | `tools/validate-content.mjs` |
| Manual | устройства, lifecycle, TalkBack, таймер | [QA guide](../qa/MANUAL_TESTING_GUIDE.md) |

**Обязательные тесты сессии:**

- неверный ход возвращает позицию и уменьшает попытки;
- третий неверный ход → FAILED;
- правильный ход применяет ответ соперника;
- последний ход → SOLVED, звёзды по политике;
- таймаут → FAILED, continue даёт +60 с и 1 попытку;
- process death → восстановление с паузой;
- альтернативный мат принимается.

---

## 10. Производительность

| Метрика | Цель | Как проверяем |
| --- | --- | --- |
| Холодный старт | < 2 с | Macrobenchmark |
| FPS | 60 | Macrobenchmark/JankStats |
| Память | < 150 МБ | Android Studio Profiler |
| APK | < 20 МБ | CI artifact size |
| Размер контента | < 2 МБ | JSON + сжатие |
| Батарея | без фоновой работы | ручной тест |

- Baseline Profile для главной и решателя.
- Никаких сетевых SDK в v1.0.0.
- Шрифты — subset, только нужные глифы.
- Фигуры — векторы, не PNG.
- Контент — JSON, сжатый в APK.

---

## 11. Безопасность и приватность

- Нет `INTERNET` в v1.0.0.
- Нет WebView, нет динамического кода.
- `android:exported=false` для всех компонентов, кроме launcher.
- `allowBackup=false`.
- Нет логирования PII; в debug — только ID задач.
- Keystore — вне репозитория, backup в менеджере паролей.
- R8 в release; mapping сохраняется для crash-репортов.
- При добавлении рекламы/покупок — отдельный аудит разрешений, политики и
  декларации RuStore.

---

## 12. Точки расширения

| Что добавляем | Где правится |
| --- | --- |
| Новый уровень | `Level` enum, контент-пак, главы, store-описание |
| Новый пак задач | `content/puzzles/*.json`, `contentVersion`, CI |
| Новая тема | `themes` в контенте, статистика, фильтры |
| Новая подсказка | `HintTier`, `HintPolicy`, UI-лист |
| Новый язык | `values-xx`, localized content, QA |
| Реклама | `AdGateway` в `:app`, feature flag, политика |
| Покупки | `BillingGateway` в `:app`, Premium UI, RuStore SDK |
| Облако | `ProgressRepository` remote, аккаунт, политика |
| iOS | общий `core`, KMP-исследование (не в v1.x) |

---

## 13. Открытые технические вопросы

1. **JitPack и chesslib.** Если JitPack недоступен — вендорим исходники
   Apache 2.0 в `third_party/chesslib` и собираем локально.
2. **Paparazzi vs Roborazzi.** Выбрать после scaffold; Paparazzi быстрее, но
   требует JVM-совместимости.
3. **Room и KSP.** Проверить версии под Kotlin 2.2.10/AGP 9.4.1.
4. **Compose BOM.** Зафиксировать после scaffold.
5. **Baseline Profile.** Нужен ли в v0.2.0 или после контента.
6. **Планшет.** Одна колонка max 560 dp или две панели в v0.4.0.
7. **Локальные напоминания.** `POST_NOTIFICATIONS` и политика — только если
   реально нужно.
