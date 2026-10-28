# Chess Problem — документация

Навигационный хаб по проекту. Документы лежат в `docs/` и обновляются вместе с
кодом и контентом: правила — в [RULES.md](development/RULES.md).

---

## По ролям

### Продукт и дизайн

| Документ | Что внутри |
| --- | --- |
| [DESIGN v0.1](design/DESIGN_V01_MVP.md) | Концепция, аудитория, цикл, уровни, таймер, подсказки, звёзды, монетизация, риски |
| [UI/UX SPEC](design/UI_UX_SPEC.md) | Дизайн-токены, компоненты, экраны, состояния, доступность, handoff |
| [CONTENT PLAN](design/CONTENT_PLAN.md) | 360 задач, 36 глав, темы, схема пака, производство и QA |
| [USER_GUIDE.md](user_guide/USER_GUIDE.md) | Руководство игрока: как решать, подсказки, звёзды, настройки, FAQ |
| [Прототип](../design/prototype/README.md) | Как открыть, сценарии проверки, скриншоты и ограничения; сам прототип — [index.html](../design/prototype/index.html) |
| [Android screenshots](screenshots/android/README.md) | 12 снимков vertical slice и чек-лист проверки на эмуляторе |
| [RUSTORE.md](development/RUSTORE.md) | Требования RuStore, карточка, монетизация, риски модерации |

### Разработка

| Документ | Что внутри |
| --- | --- |
| [ARCHITECTURE.md](development/ARCHITECTURE.md) | Модули, состояние сессии, доска, данные, сборка, тесты |
| [GETTING_STARTED.md](development/GETTING_STARTED.md) | Развёртывание, сборка APK/AAB, troubleshooting |
| [CONTENT_AUTHORING.md](development/CONTENT_AUTHORING.md) | Как добавить задачу, проверить линию, написать подсказки и разбор |
| [RULES.md](development/RULES.md) | Правила кода, контента, документации, git, PR, лицензий |
| [ADR](development/adr/README.md) | Записи решений: Compose, chesslib, Lichess, офлайн, монетизация, модули |

### Релиз, QA и право

| Документ | Что внутри |
| --- | --- |
| [RELEASES.md](development/RELEASES.md) | Пошаговый чек-лист выпуска версии |
| [MANUAL_TESTING_GUIDE.md](qa/MANUAL_TESTING_GUIDE.md) | Ручные кейсы, матрица устройств, шаблон баг-репорта |
| [KNOWN_ISSUES.md](KNOWN_ISSUES.md) | Реестр известных проблем и ограничений (KI-XXX) |
| [CHANGELOG.md](CHANGELOG.md) | История версий в формате Keep a Changelog |
| [PRIVACY_POLICY.md](legal/PRIVACY_POLICY.md) | Политика конфиденциальности RU/EN |
| [THIRD_PARTY_LICENSES.md](legal/THIRD_PARTY_LICENSES.md) | Лицензии библиотек, шрифтов, задач и инструментов |
| [store/README.md](../store/README.md) | Черновик карточки RuStore, иконка, скриншоты, тексты |

---

## Быстрая навигация по проекту

| Что ищете | Где смотреть |
| --- | --- |
| Правила уровней, времени, звёзд и подсказок | [DESIGN v0.1](design/DESIGN_V01_MVP.md) |
| Цвета, шрифты, отступы, компоненты | [UI/UX SPEC](design/UI_UX_SPEC.md), [tokens.json](../design/tokens.json) |
| Состав 360 задач и темы глав | [CONTENT PLAN](design/CONTENT_PLAN.md) |
| Схему пака задач | [puzzle-pack.schema.json](../content/schema/puzzle-pack.schema.json) |
| Примеры задач | [content/samples](../content/samples) |
| Валидатор контента | [tools/validate-content.mjs](../tools/validate-content.mjs) |
| Модули и состояние сессии | [ARCHITECTURE.md](development/ARCHITECTURE.md) |
| Требования RuStore | [RUSTORE.md](development/RUSTORE.md) |
| Политику конфиденциальности | [PRIVACY_POLICY.md](legal/PRIVACY_POLICY.md) |
| Лицензии | [THIRD_PARTY_LICENSES.md](legal/THIRD_PARTY_LICENSES.md) |

---

## Статус документации

| Документ | Статус |
| --- | --- |
| README | актуален на v0.2.0-vertical-slice |
| DESIGN v0.1 | первая редакция, правила реализуются в Android |
| UI/UX SPEC | первая редакция, прототип и Android-срезы сверены |
| CONTENT PLAN | первая редакция, 4 демо-задачи проверены |
| USER_GUIDE | черновик до первого публичного билда |
| MANUAL_TESTING_GUIDE | ручной smoke на Pixel 6 API 36 выполнен; полная матрица — перед релизом |
| ARCHITECTURE | реализованная архитектура v0.2.0 |
| GETTING_STARTED | Android vertical slice, сборка и troubleshooting |
| CONTENT_AUTHORING | первая редакция, валидатор работает |
| RUSTORE | требования выверены по документации RuStore (10.2026) |
| RELEASES | первая редакция |
| KNOWN_ISSUES | актуален, KI-001/KI-005 закрыты |
| ADR | 8 решений зафиксировано |
| PRIVACY_POLICY | черновик для офлайн-запуска |
| THIRD_PARTY_LICENSES | актуален для Android-сборки |

---

## Принцип документации

1. **Один источник правды.** Числа времени, звёзд, подсказок и цен живут в
   дизайн-документе и затем в коде — не дублируются в трёх местах.
2. **Документ меняется вместе с поведением.** Изменили правило — обновили
   DESIGN/UI-спецификацию и CHANGELOG в том же PR.
3. **Ссылки относительные.** Никаких жёстких дисковых путей.
4. **Контент проверяем машинно.** Схема, валидатор и CI — обязательная часть
   контент-пайплайна, а не «на потом».
5. **Язык.** Документация и тексты интерфейса — на русском; идентификаторы,
   имена файлов и коммиты — на английском. EN-версия контента обязательна для
   релиза.
