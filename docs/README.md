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
| [ADR-0009](development/adr/0009-content-versioning.md) | `contentVersion` и изоляция прогресса в Room (KI-018) |
| [ADR-0010](development/adr/0010-cold-start-theme-cache.md) | Синхронный кэш `theme`/`language` при холодном старте (KI-015) |

### Релиз, QA и право

| Документ | Что внутри |
| --- | --- |
| [RELEASES.md](development/RELEASES.md) | Пошаговый чек-лист выпуска версии |
| [Release checklist v0.4.0](reviews/v0.4.0-release-checklist.md) | Сборка, подпись, store-ассеты, device QA и блокеры v0.4.0 |
| [MANUAL_TESTING_GUIDE.md](qa/MANUAL_TESTING_GUIDE.md) | Ручные кейсы, матрица устройств, шаблон баг-репорта |
| [Content review](../tools/review/README.md) | Офлайн-ревьюер 360 задач: две доски, RU/EN тексты, статусы, импорт/экспорт |
| [AI review v0.4.0](reviews/v0.4.0-ai-review.md) | Пакет ревью 180 новых задач: проверки, исправления, человеческое подтверждение 180/180 |
| [KNOWN_ISSUES.md](KNOWN_ISSUES.md) | Реестр известных проблем и ограничений (KI-XXX) |
| [CHANGELOG.md](CHANGELOG.md) | История версий в формате Keep a Changelog |
| [PRIVACY_POLICY.md](legal/PRIVACY_POLICY.md) | Политика конфиденциальности RU/EN |
| [privacy.html](legal/privacy.html) | Статическая политика RU/EN одним файлом для публикации (KI-006) |
| [RuStore listing](../store/RUSTORE_LISTING.md) | Готовые поля карточки RuStore: названия, описания, теги, декларация |
| [Release checklist v0.5.0](releases/v0.5.0-checklist.md) | Чек-лист RuStore Closed Beta и внешние блокеры |
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
| Воспроизводимый отбор 60 задач (v0.2.1) | [tools/lichess-selection.json](../tools/lichess-selection.json) |
| Воспроизводимый отбор 180 задач (v0.3.0) | [tools/lichess-selection-v030.json](../tools/lichess-selection-v030.json) |
| Воспроизводимый отбор 180 задач (v0.4.0) | [tools/lichess-selection-v040.json](../tools/lichess-selection-v040.json) |
| Модули и состояние сессии | [ARCHITECTURE.md](development/ARCHITECTURE.md) |
| Требования RuStore | [RUSTORE.md](development/RUSTORE.md) |
| Политику конфиденциальности | [PRIVACY_POLICY.md](legal/PRIVACY_POLICY.md), [privacy.html](legal/privacy.html) |
| Готовые поля карточки RuStore | [RUSTORE_LISTING.md](../store/RUSTORE_LISTING.md) |
| Чек-лист релиза v0.5.0 | [v0.5.0-checklist.md](releases/v0.5.0-checklist.md) |
| Лицензии | [THIRD_PARTY_LICENSES.md](legal/THIRD_PARTY_LICENSES.md) |

---

## Статус документации

| Документ | Статус |
| --- | --- |
| README | актуален на v0.5.0 (beta-кандидат) |
| DESIGN v0.1 | первая редакция; v0.4.0 реализован, DRAW/этюды/планшет сверены |
| UI/UX SPEC | первая редакция, прототип и Android-срезы сверены в v0.4.0 |
| CONTENT PLAN | первая редакция, 360 production-задач собраны; человеческое ревью — 360/360 OK |
| USER_GUIDE | черновик до первого публичного билда |
| MANUAL_TESTING_GUIDE | smoke v0.3.0; 360 задач, drag/TalkBack, планшет — device QA (KI-010/KI-012) |
| ARCHITECTURE | реализованная архитектура v0.4.1 (Room v2, синхронный кэш) |
| GETTING_STARTED | Android-проект, 360 задач, сборка и troubleshooting |
| CONTENT_AUTHORING | 360 задач, пайплайн v0.4.0, `contentVersion` из паков, валидатор работает |
| RUSTORE | требования выверены по документации RuStore (10.2026) |
| RELEASES | первая редакция |
| KNOWN_ISSUES | актуален, KI-001/KI-003/KI-005/KI-008/KI-009/KI-017 закрыты |
| ADR | 10 решений зафиксировано, включая ADR-0009/0010 |
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
