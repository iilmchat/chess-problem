# ADR-0007 — Ручной DI через `AppContainer`

- **Статус:** Accepted
- **Дата:** 28.10.2026
- **Связано:** [ARCHITECTURE §8](../ARCHITECTURE.md#8-di-и-сборка)

## Контекст

Проект состоит из четырёх модулей. Нужно собирать реализации репозиториев,
Room, DataStore и ViewModel-фабрики. Варианты:

- DI-фреймворк (Hilt/Koin) — привычно, но добавляет kapt/KSP, генерацию,
  время сборки и кривую обучения;
- ручной контейнер — меньше магии, но нужно следить за областями жизни и
  тестами.

Для MVP с одним `Application`, без динамических фич и без сложного графа
ручной DI достаточен.

## Решение

Используем **ручной DI**:

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

`AppContainer` живёт в `:app`, создаётся в `Application`. ViewModel-фабрики
получают зависимости из контейнера. В тестах `core`/`data` используются
обычные конструкторы и фейки, без DI-фреймворка.

## Последствия

### Положительные

- Нет kapt/KSP и генерации: быстрее сборка, меньше версий.
- Граф зависимостей явный; видно, кто что создаёт.
- Легко подменить реализацию в тестах.
- Меньше порог входа для нового разработчика.

### Отрицательные / ограничения

- При росте графа `AppContainer` может стать «божественным объектом».
- Нет автоматической проверки отсутствующих зависимостей.
- ViewModel-фабрики пишутся вручную; больше boilerplate.
- Области жизни (singleton vs screen) нужно соблюдать осознанно.

### Что сделать

- [ ] Создать `AppContainer` в v0.2.0.
- [ ] Добавить фабрики для `HomeViewModel`, `PuzzleViewModel`,
      `ProgressViewModel`.
- [ ] Проверить, что `:core` и `:data` не знают о контейнере.
- [ ] Пересмотреть решение, если модулей станет больше 8 или появятся
      динамические фичи/несколько процессов.

## Альтернативы

| Вариант | Почему нет |
| --- | --- |
| Hilt | kapt/KSP, генерация, версии; для 4 модулей избыточно |
| Koin | Runtime-резолвинг, ошибки позже; лишняя зависимость |
| Service Locator | Скрытые зависимости, сложнее тесты |
| Ручной DI без контейнера | Дублирование создания в Activity/ViewModel |

## Ссылки

- [ARCHITECTURE §8](../ARCHITECTURE.md#8-di-и-сборка)
- [RULES §1.2](../RULES.md#12-слои)
- [GETTING_STARTED](../../development/GETTING_STARTED.md)
