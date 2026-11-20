# Живые скриншоты для карточки RuStore

Папка для скриншотов, снятых **с реального приложения на устройстве/эмуляторе**
через `adb exec-out screencap -p`. Это не заменяет эталоны Roborazzi из
`ui/src/test/screenshots/`: те — регрессионная база в dp, здесь — живые PNG
в пикселях для загрузки в консоль RuStore.

```text
store/screenshots/
├── phone/
│   ├── 01-home.png        1080×2400
│   ├── 02-solver.png
│   ├── 03-hints.png
│   ├── 04-result.png
│   └── 05-progress.png
└── tablet/
    ├── 01-home.png        1600×2560
    ├── 02-solver.png
    ├── 03-hints.png
    ├── 04-result.png
    └── 05-progress.png
```

Минимум для карточки — 3 телефона и 3 планшета; рекомендуем все 5+5.
Формат PNG/JPG, до 3 МБ на телефонный и до 5 МБ на планшетный файл, без
заглушек, чужих логотипов и водяных знаков; портретная ориентация.

## Съёмка

Скрипт [`tools/capture-store-screenshots.ps1`](../../tools/capture-store-screenshots.ps1):

```powershell
# Проверить план без устройства
powershell -NoProfile -File tools/capture-store-screenshots.ps1 -DryRun

# Реальная съёмка (устройство подключено)
powershell -NoProfile -File tools/capture-store-screenshots.ps1 -Serial <serial>
```

Скрипт собирает и ставит `app-debug.apk`, переключает `wm size`/`wm density`
(1080×2400 @480 dpi и 1600×2560 @320 dpi), запускает приложение, запрашивает
открытие каждого из 5 экранов, сохраняет PNG и в `finally` обязательно делает
`wm size reset` / `wm density reset`. Пропустить установку можно через
`-SkipInstall`, автоматизировать ожидание — `-NoPrompt -DelaySec 3`.

**Статус v0.5.0:** живые скриншоты ещё не сняты — в среде UI/QA нет
подключённого устройства и установленного эмулятора (KI-012).
