# Getting Started

**Статус:** Android-проект + 60 production-задач · **Дата:** 28.10.2026
**Связано:** [ARCHITECTURE](ARCHITECTURE.md) · [RULES](RULES.md) · [RUSTORE](RUSTORE.md)

Android-проект собран в v0.2.0 и наполнен 60 задачами в v0.2.1: `app`, `core`,
`data`, `ui`, Gradle wrapper, convention-плагины, Room/DataStore. Этот документ
описывает требования, сборку, запуск и проверку vertical slice. Оставшиеся 300
задач, instrumented/screenshot-тесты и release-подпись — следующие этапы.

---

## 1. Что нужно

| Инструмент | Версия | Зачем |
| --- | --- | --- |
| Git | любая актуальная | Клонирование |
| JDK | **21** | Gradle/AGP, target 17 |
| Android SDK | platform **37.0**, build-tools **36.0.0** или **37.0.0** | Compose 1.12 / AndroidX 2026.09 |
| Android Studio | последняя стабильная | IDE и эмулятор |
| Node.js | 24 LTS | Валидатор контента и проверка ссылок |
| Python | не обязателен | Только для локальных экспериментов |

```bash
git clone https://github.com/iilmchat/chess-problem.git
cd chess-problem

# Прототип
start design/prototype/index.html      # Windows
open design/prototype/index.html       # macOS
xdg-open design/prototype/index.html   # Linux

# Android-проект
./gradlew :app:assembleDebug
```

---

## 2. Проверка документации и контента

```bash
npm ci --prefix tools

# Все относительные ссылки в Markdown существуют
node tools/check-docs.mjs

# Production-паки: 60 задач (по умолчанию content/puzzles)
node tools/validate-content.mjs

# Примеры формата
node tools/validate-content.mjs content/samples
```

Ожидаемый результат:

```text
OK  content/puzzles/grandmaster.json: 20 puzzles
OK  content/puzzles/novice.json: 20 puzzles
OK  content/puzzles/professional.json: 20 puzzles
OK  content/samples/novice.sample.json: 2 puzzles
OK  content/samples/professional.sample.json: 1 puzzle
OK  content/samples/grandmaster.sample.json: 1 puzzle
```

Если валидатор падает — читайте сообщение: оно указывает `id`, поле и причину.

---

## 3. Android-проект (v0.2.1)

### 3.1. Требования

- **JDK 21** (Gradle/AGP; Kotlin target 17).
- **Android Studio** — последняя стабильная.
- **Android SDK:** platform **37.0**, build-tools **36.0.0** или **37.0.0**.
- **Gradle** не нужен: wrapper 9.8.2 в репозитории.
- **Эмулятор** или устройство Android 8.0+ (minSdk 26).

Проверка:

```bash
java -version
# openjdk version "21.x" ...
```

Если `java` не найден, задайте `JAVA_HOME`:

```powershell
$env:JAVA_HOME = "C:\Program Files\Microsoft\jdk-21.0.13.21-hotspot"
$env:Path = "$env:JAVA_HOME\bin;$env:Path"
```

### 3.2. Android SDK

Создайте `local.properties` в корне (файл не коммитится):

```properties
sdk.dir=D\:/Dev/Android/Sdk
```

На Windows lint требует экранировать двоеточие; в Git файл не попадает.
Установить нужные пакеты:

```bash
sdkmanager "platform-tools" "platforms;android-37.0" "build-tools;36.0.0"
```

### 3.3. Сборка и проверки

```bash
# Debug APK
./gradlew :app:assembleDebug
# → app/build/outputs/apk/debug/app-debug.apk

# Unit-тесты ядра (21 тест)
./gradlew :core:test

# Lint (включая Android-ресурсы и Compose)
./gradlew :app:lintDebug

# Всё сразу
./gradlew :core:test :app:assembleDebug :app:lintDebug

# Windows PowerShell: .\gradlew.bat :app:assembleDebug

# Release AAB (нужна подпись)
./gradlew :app:bundleRelease
# → app/build/outputs/bundle/release/app-release.aab
```

### 3.4. Запуск на устройстве

```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb shell am start -n com.iilmchat.chessproblem/.MainActivity
```

Или через Android Studio: Run → `app`. Debug-приложение использует
`applicationId com.iilmchat.chessproblem`.

### 3.5. Контент

Единственный источник контента — `content/puzzles/*.json` (60 задач: по 20 на
уровень). Перед сборкой Gradle-задача `syncPuzzlePacks` копирует паки в
генерируемый assets-каталог `data` (статические
`data/src/main/assets/content/` удалены в v0.2.1). Паки читаются
`AssetPuzzleSource` при старте.

После изменения:

```bash
node tools/import-lichess.mjs merge --drafts "*.draft.json"
node tools/validate-content.mjs
node tools/validate-content.mjs content/samples
./gradlew :app:assembleDebug
```

Проверить, что паки попали в APK:

```bash
jar tf app/build/outputs/apk/debug/app-debug.apk | findstr assets/content
```

Подробно — [CONTENT_AUTHORING.md](CONTENT_AUTHORING.md).

---

## 4. Структура проекта

```text
app/                     ← entry point, DI, navigation
core/                    ← чистая логика: chesslib, session, scoring, timer
data/                    ← Room, DataStore, JSON loader
ui/                      ← Compose theme, components, screens
build-logic/             ← convention plugins
gradle/libs.versions.toml
```

Правила зависимостей — в [ARCHITECTURE.md](ARCHITECTURE.md) и [RULES.md](RULES.md).

---

## 5. Troubleshooting

### `SDK location not found`

Нет `local.properties` или неверный `sdk.dir`. Проверьте путь к Android SDK.

### `Unsupported class file major version`

Запущен не JDK 21. Проверьте `java -version` и `JAVA_HOME`.

### `Failed to find package 'platforms;android-37'`

Пакет называется `platforms;android-37.0` (minor API). Установите его через
`sdkmanager`; `compileSdk = 37` в convention-плагине сам подхватит платформу.

### `android.disallowKotlinSourceSets=false` warning

KSP 2.2.10 всё ещё регистрирует generated sources через `kotlin.sourceSets`.
AGP 9 built-in Kotlin по умолчанию это запрещает; property оставлен до
миграции KSP. Warning ожидаем.

### `Could not resolve com.github.bhlangonijr:chesslib`

JitPack временно недоступен или не добавлен репозиторий. Проверьте
`settings.gradle.kts`; fallback — вендоринг исходников (ADR-0003).

### `Promotion piece required`

В UCI-ходе превращения не указана фигура: `a7a8q`, `a7a8n`, `a7a8r`, `a7a8b`.
Валидатор контента это ловит.

### Валидатор контента падает на `mate`

Проверьте `task.plies` (число ходов пользователя, не полуходов) и последний
ход линии. Для мата в 2: `solution` из 3 UCI-ходов, `plies = 2`.

### Ссылки в документации «сломаны»

`node tools/check-docs.mjs` проверяет только относительные файлы, не якоря.
Если файл существует, но ссылка с `#anchor` не работает — проверьте заголовок
вручную.

### Прототип выглядит иначе на Windows

Прототип использует инлайновые SVG, но шрифт зависит от ОС. Это ограничение
прототипа (KI-013), не продакшена.

---

## 6. Полезные команды

```bash
# Контент и документация
npm ci --prefix tools
node tools/check-docs.mjs
for f in content/samples/*.json; do node tools/validate-content.mjs "$f"; done

# Android
./gradlew :core:test
./gradlew :app:assembleDebug
./gradlew :app:lintDebug
./gradlew :core:test :app:assembleDebug :app:lintDebug

# Устройство
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb shell am start -n com.iilmchat.chessproblem/.MainActivity
```

---

## 7. Куда дальше

1. Прочитать [DESIGN](../design/DESIGN_V01_MVP.md) и [UI/UX SPEC](../design/UI_UX_SPEC.md).
2. Посмотреть [прототип](../../design/prototype/index.html).
3. Прочитать [ARCHITECTURE](ARCHITECTURE.md).
4. Для контента — [CONTENT_AUTHORING](CONTENT_AUTHORING.md).
5. Для релиза — [RUSTORE](RUSTORE.md) и [RELEASES](RELEASES.md).
