# Сторонние лицензии / Third-Party Licenses

**Версия:** 1.0 · **Дата:** 28.10.2026
**Связано:** [LICENSE](../../LICENSE) · [RULES §7](../development/RULES.md#7-лицензии-и-ассеты) ·
[CONTENT PLAN](../design/CONTENT_PLAN.md)

Реестр сторонних компонентов Chess Problem. Полные тексты лицензий
скопированы в `app/src/main/assets/licenses/` и читаются экраном
«О приложении → Лицензии» с v0.4.1 (KI-019 закрыт).
Для компонентов, которые не поставляются в APK (инструменты сборки и
валидации), достаточно указать их в этом реестре.

---

## 1. Что поставляется в приложении

| Компонент | Версия | Лицензия | Использование | Уведомление |
| --- | --- | --- | --- | --- |
| Kotlin stdlib | 2.2.10 | Apache 2.0 | Язык и runtime | Copyright JetBrains s.r.o. |
| AndroidX / Jetpack Compose | BOM 2026.09.00 | Apache 2.0 | UI, lifecycle, navigation | Copyright The Android Open Source Project |
| Material 3 (Compose) | по BOM | Apache 2.0 | Компоненты и тема | Copyright The Android Open Source Project |
| Room | 2.8.5 | Apache 2.0 | Локальная БД прогресса | Copyright The Android Open Source Project |
| DataStore | 1.3.0 | Apache 2.0 | Настройки и кошелёк подсказок | Copyright The Android Open Source Project |
| kotlinx.serialization | 1.9.0 | Apache 2.0 | JSON-контент | Copyright JetBrains s.r.o. |
| kotlinx.coroutines | 1.10.2 | Apache 2.0 | Асинхронность | Copyright JetBrains s.r.o. |
| chesslib | 1.3.7 | Apache 2.0 | FEN, ходы, мат, SAN | Copyright bhlangonijr |
| Montserrat | variable | SIL OFL 1.1 | Заголовки | Copyright The Montserrat Project Authors |
| Manrope | variable | SIL OFL 1.1 | Интерфейс | Copyright The Manrope Project Authors |
| Roboto Mono | variable | SIL OFL 1.1 | SAN и табличные числа | Copyright Google LLC |
| Material Icons Extended | по BOM | Apache 2.0 | Иконки интерфейса | Copyright Google LLC |

> Версии зафиксированы в `gradle/libs.versions.toml`; таблица обновляется
> вместе с зависимостями.

### 1.1. Шрифты

Variable-шрифты добавлены в `ui/src/main/res/font/`; полные тексты OFL лежат в
`licenses/fonts/`:

- `OFL-Montserrat.txt`
- `OFL-Manrope.txt`
- `OFL-RobotoMono.txt`

Шрифты покрывают RU/EN, цифры и SAN. Все три текста объединены в
`app/src/main/assets/licenses/OFL-1.1.txt` и доступны с экрана
«О приложении → Лицензии».

### 1.2. Иконки и фигуры

- Иконка приложения — собственная (`design/icon.svg`).
- Шахматные фигуры — собственный векторный набор; сторонние наборы
  (Cburnett, Chess.com и др.) не копируются.
- Иконки интерфейса — Material Symbols (Apache 2.0) или собственные.

---

## 2. Контент

### 2.1. Lichess puzzle database

- **Источник:** <https://database.lichess.org/#puzzles>
- **Лицензия:** **CC0 1.0 Universal** (public domain dedication).
- **Что берём:** FEN, линию, рейтинг, темы, популярность; имена игроков не
  сохраняем.
- **Атрибуция в приложении:**

> Шахматные задачи основаны на открытой базе Lichess (CC0 1.0).
> Lichess не связан с Chess Problem и не поддерживает приложение.

CC0 не требует атрибуции, но мы указываем источник этически и для доверия.
Lichess — товарный знак Lichess.org; мы не используем его логотип и не
заявляем о партнёрстве.

### 2.2. Авторские задачи

- **Лицензия:** proprietary (см. [LICENSE](../../LICENSE)).
- **Автор:** RuChating (iilmchat) или контрактор с передачей прав.
- **Проверка:** каждая авторская задача проходит второе ревью и валидатор.

### 2.3. Будущие паки

Новые паки могут включать задачи из CC0-источников или авторские. Для каждого
пака фиксируется источник и лицензия в JSON (`source.provider`,
`source.license`) и в этом реестре.

---

## 3. Инструменты разработки (не поставляются)

| Инструмент | Версия | Лицензия | Использование |
| --- | --- | --- | --- |
| Gradle | 9.8.2 | Apache 2.0 | Сборка |
| Android Gradle Plugin | 9.4.1 | Apache 2.0 | Сборка Android |
| Kotlin Gradle Plugin | 2.2.10 | Apache 2.0 | Сборка Kotlin |
| detekt + detekt-formatting | актуальная | Apache 2.0 | Статический анализ |
| JUnit | 4.13.2 / 5.x | EPL 1.0 / EPL 2.0 | Unit-тесты (не в APK) |
| Node.js | 24 LTS | MIT | Валидаторы контента/ссылок |
| chess.js | 1.4.0 | BSD-2-Clause | Валидация линий в CI (не в APK) |
| npm | 11.x | Artistic 2.0 | Управление зависимостями tools |

`python-chess` (GPL) **не используется** в репозитории и пайплайне. Если
понадобится GPL-инструмент, он не должен попадать в продакшн-сборку без
юридической проверки.

---

## 4. Полные тексты лицензий

В `app/src/main/assets/licenses/` добавлены:

- `Apache-2.0.txt` — для Kotlin, AndroidX, chesslib, Material Symbols;
- `OFL-1.1.txt` — полные тексты для Montserrat, Manrope и Roboto Mono;
- `BSD-2-Clause.txt` — для chess.js (сейчас — только CI);
- `CC0-1.0.txt` — для базы Lichess;
- `NOTICE` — атрибуция Lichess, Kotlin, AndroidX, chesslib и шрифтов.

С v0.4.1 экран «О приложении → Лицензии» открывает диалог выбора файла и
показывает полный текст с прокруткой (KI-019 закрыт).

Официальные тексты:

- Apache 2.0: <https://www.apache.org/licenses/LICENSE-2.0>
- SIL OFL 1.1: <https://openfontlicense.org/open-font-license-official-text/>
- BSD-2-Clause: <https://opensource.org/license/bsd-2-clause>
- CC0 1.0: <https://creativecommons.org/publicdomain/zero/1.0/>

### 4.1. BSD-2-Clause (chess.js)

```text
Copyright (c) 2025, Jeff Hlywa (jhlywa@gmail.com)
All rights reserved.

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions are met:

1. Redistributions of source code must retain the above copyright notice, this
   list of conditions and the following disclaimer.

2. Redistributions in binary form must reproduce the above copyright notice,
   this list of conditions and the following disclaimer in the documentation
   and/or other materials provided with the distribution.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
```

> Проверьте год и имя правообладателя в `tools/node_modules/chess.js/LICENSE`
> при обновлении зависимости.

---

## 5. Процесс добавления компонента

1. Проверить лицензию: Apache/MIT/BSD/OFL/CC0 или совместимая.
2. Добавить строку в таблицу с версией, использованием и уведомлением.
3. Скопировать полный текст в `assets/licenses/` (если поставляется).
4. Обновить экран «О приложении → Лицензии».
5. Проверить, что лицензия не требует раскрытия проприетарного кода.
6. Для GPL/AGPL/SSPL — не добавлять без юридического решения и ADR.

---

## 6. Контакты

По вопросам лицензий: [info@iiruchating.ru](mailto:info@iiruchating.ru).
