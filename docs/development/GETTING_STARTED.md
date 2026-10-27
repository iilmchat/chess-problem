# Getting Started

**Статус:** дизайн-фаза · **Дата:** 28.10.2026
**Связано:** [ARCHITECTURE](ARCHITECTURE.md) · [RULES](RULES.md) · [RUSTORE](RUSTORE.md)

В репозитории пока нет Android-проекта: сначала зафиксированы продукт, UX и
контент-пайплайн. Этот документ описывает, что можно сделать сейчас, и что
понадобится после scaffold в v0.2.0.

---

## 1. Что нужно сейчас

| Инструмент | Версия | Зачем |
| --- | --- | --- |
| Git | любая актуальная | Клонирование |
| Браузер | Chrome/Edge/Firefox | Прототип |
| Node.js | 24 LTS | Валидатор контента и проверка ссылок |
| Python | не обязателен | Только для локальных экспериментов |

```bash
git clone https://github.com/iilmchat/chess-problem.git
cd chess-problem

# Прототип
start design/prototype/index.html      # Windows
open design/prototype/index.html       # macOS
xdg-open design/prototype/index.html   # Linux

# Документация
start docs/design/DESIGN_V01_MVP.md
```

---

## 2. Проверка документации и контента

```bash
npm ci --prefix tools

# Все относительные ссылки в Markdown существуют
node tools/check-docs.mjs

# Примеры задач: FEN, линия, мат, локализация, время
node tools/validate-content.mjs content/samples/novice.sample.json
node tools/validate-content.mjs content/samples/professional.sample.json
node tools/validate-content.mjs content/samples/grandmaster.sample.json
```

Ожидаемый результат:

```text
OK  novice.sample.json: 2 puzzles
OK  professional.sample.json: 1 puzzle
OK  grandmaster.sample.json: 1 puzzle
```

Если валидатор падает — читайте сообщение: оно указывает `id`, поле и причину.

---

## 3. После scaffold Android (v0.2.0)

### 3.1. Требования

- **JDK 21** (toolchain; target 17).
- **Android Studio** — последняя стабильная.
- **Android SDK:** platform **36**, build-tools **36.0.0**.
- **Gradle** не нужен: wrapper будет в репозитории.
- **Эмулятор** или устройство Android 8.0+.

Проверка:

```bash
java -version
# openjdk version "21.x" ...
```

Если `java` не найден, задайте `JAVA_HOME`:

```powershell
$env:JAVA_HOME = "C:\Program Files\Java\jdk-21"
$env:Path = "$env:JAVA_HOME\bin;$env:Path"
```

### 3.2. Android SDK

Создайте `local.properties` в корне (файл не коммитится):

```properties
sdk.dir=D:/Dev/Android/Sdk
```

Windows-путь — с прямыми слэшами или экранированными обратными.

### 3.3. Сборка

```bash
# Debug APK
./gradlew :app:assembleDebug
# → app/build/outputs/apk/debug/app-debug.apk

# Unit-тесты
./gradlew :app:testDebugUnitTest

# Lint + detekt
./gradlew :app:lintDebug detekt

# Release AAB (нужна подпись)
./gradlew :app:bundleRelease
# → app/build/outputs/bundle/release/app-release.aab
```

### 3.4. Запуск на устройстве

```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb shell am start -n com.iilmchat.chessproblem/.MainActivity
```

Или через Android Studio: Run → `app`.

### 3.5. Контент

Production-паки лежат в `content/puzzles/` и копируются в
`app/src/main/assets/content/` Gradle-задачей. После изменения контента:

```bash
node tools/validate-content.mjs content/puzzles/novice.json
./gradlew :app:assembleDebug
```

Подробно — [CONTENT_AUTHORING.md](CONTENT_AUTHORING.md).

---

## 4. Структура, которую увидите после scaffold

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

Прототип использует Unicode-фигуры; шрифт зависит от ОС. Это ограничение
прототипа (KI-013), не продакшена.

---

## 6. Полезные команды

```bash
# Все проверки, которые есть сейчас
npm ci --prefix tools
node tools/check-docs.mjs
for f in content/samples/*.json; do node tools/validate-content.mjs "$f"; done

# После scaffold
./gradlew build
./gradlew :app:lintDebug detekt
./gradlew :app:testDebugUnitTest
```

---

## 7. Куда дальше

1. Прочитать [DESIGN](../design/DESIGN_V01_MVP.md) и [UI/UX SPEC](../design/UI_UX_SPEC.md).
2. Посмотреть [прототип](../../design/prototype/index.html).
3. Прочитать [ARCHITECTURE](ARCHITECTURE.md).
4. Для контента — [CONTENT_AUTHORING](CONTENT_AUTHORING.md).
5. Для релиза — [RUSTORE](RUSTORE.md) и [RELEASES](RELEASES.md).
