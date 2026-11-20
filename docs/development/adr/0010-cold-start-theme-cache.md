# ADR-0010 — Синхронный кэш темы и локали при холодном старте

- **Статус:** Accepted, реализовано в v0.4.1 (`0eede8c`)
- **Дата:** 20.11.2026
- **Связано:** [ARCHITECTURE §6.3](../ARCHITECTURE.md#63-datastore) ·
  [ARCHITECTURE §10](../ARCHITECTURE.md#10-производительность) ·
  [UI/UX SPEC](../../design/UI_UX_SPEC.md) ·
  [KNOWN ISSUES KI-015](../../KNOWN_ISSUES.md#ki-015-долгий-холодный-старт-на-эмуляторе) ·
  [ADR-0005](0005-offline-first.md)

## Контекст

На Pixel 6 (API 36) первый запуск после установки занимает заметное время; до
чтения DataStore могут мелькнуть светлая тема и неверная локаль (KI-015).
`SettingsDataStore` отдаёт `Flow<UserSettings>` асинхронно, поэтому первые
кадры Compose получают значения по умолчанию, а затем перерисовываются.

Требования:

- тема и язык должны быть корректны с первого кадра;
- нельзя блокировать main thread (`runBlocking` на DataStore — риск ANR);
- приложение остаётся офлайн-first, без сети и аккаунта (ADR-0005);
- кэш не должен хранить прогресс или новые категории персональных данных.

## Решение

Введены два синхронных хранилища SharedPreferences — `ThemePreferences` и
`LocalePreferences` (интерфейсы `ThemeStore`/`LocaleStore` в `:core`), которые
являются рабочим источником для двух настроек первого кадра:

- `theme`: `system|dark|light`;
- `language`: `system|ru|en`.

Порядок работы:

1. `SettingsDataStore.update()` после `DataStore.edit` синхронно записывает
   изменившиеся `theme`/`language` в `ThemeStore`/`LocaleStore`. При первом
   чтении DataStore legacy-значение темы один раз переносится в кэш (сидинг для
   обновления с v0.4.0).
2. `SettingsRepository` открывает `startupTheme()`/`startupLanguage()`;
   `SettingsViewModel` инициализирует состояние из кэша, а `MainActivity`
   применяет системные полосы до первой композиции.
3. Для этих двух ключей синхронные store — источник правды; legacy-ключи
   DataStore нужны только для сидинга. При отсутствии значений используются
   системные тема и язык.
4. Остальные настройки (`boardTheme`, `coordinates`, `sound`, `haptics` и т.д.)
   по-прежнему читаются асинхронно из DataStore.
5. Предзагрузка контента и Baseline Profile — отдельные решения после замеров
   на реальном устройстве.

## Последствия

### Положительные

- Нет мелькания светлой темы и неверной локали на холодном старте.
- Чтение SharedPreferences дешево и не блокирует main thread.
- Смена локали применяется до создания первого composable.
- Политика конфиденциальности не меняется: сохраняются те же две настройки.

### Отрицательные / ограничения

- Две точки записи для двух ключей: при завершении процесса между
  `DataStore.edit` и записью в кэш возможно расхождение; сидинг при первом
  чтении сужает окно.
- Legacy-ключи DataStore и синхронные store хранят одни и те же значения;
  нужно следить за форматами и не писать theme/language напрямую в DataStore.
- Решение не ускоряет парсинг контента и не заменяет замеры на устройстве.

### Что сделать

- [x] Реализовать `ThemePreferences`/`LocalePreferences` в `:data` и подключить
  через `AppContainer`.
- [x] Открыть `startupTheme()`/`startupLanguage()` и инициализировать UI до
  первой композиции.
- [x] Сверить ARCHITECTURE §5.5/§6.3 и PRIVACY POLICY §2.2.
- [ ] Измерить холодный старт на реальном устройстве; Baseline Profile — после
  замеров.

## Альтернативы

| Вариант | Почему нет |
| --- | --- |
| `runBlocking` на `DataStore.data.first()` | Блокирует main thread, риск ANR |
| Splash до загрузки всех настроек | Увеличивает воспринимаемое время старта |
| Только системные тема/язык | Теряется выбор пользователя |
| Хранить настройки в Room | Избыточно для двух флагов, лишняя миграция |
| Оставить только DataStore | Мелькание темы/локали сохраняется (KI-015) |

## Ссылки

- [ARCHITECTURE §5.5](../ARCHITECTURE.md#55-таймер)
- [ARCHITECTURE §6.3](../ARCHITECTURE.md#63-datastore)
- [ARCHITECTURE §10](../ARCHITECTURE.md#10-производительность)
- [UI/UX SPEC §2.3](../../design/UI_UX_SPEC.md#23-отступы-и-сетка)
- [PRIVACY POLICY §2.2](../../legal/PRIVACY_POLICY.md#22-какие-данные-обрабатываются)
- [KNOWN ISSUES KI-015](../../KNOWN_ISSUES.md#ki-015-долгий-холодный-старт-на-эмуляторе)
- [ADR-0005](0005-offline-first.md)
- [ThemePreferences.kt](../../../data/src/main/kotlin/com/iilmchat/chessproblem/data/local/ThemePreferences.kt) ·
  [LocalePreferences.kt](../../../data/src/main/kotlin/com/iilmchat/chessproblem/data/local/LocalePreferences.kt) ·
  [SettingsDataStore.kt](../../../data/src/main/kotlin/com/iilmchat/chessproblem/data/local/SettingsDataStore.kt)
