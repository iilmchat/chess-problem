<#
.SYNOPSIS
  Устанавливает debug APK и снимает живые скриншоты RuStore через adb.

.DESCRIPTION
  Переключает профили экрана (phone/tablet), запускает приложение и сохраняет
  по 5 PNG в store/screenshots/<profile>/. После съёмки (в том числе при ошибке)
  возвращает wm size/density в исходное состояние.

  vivo: скриншоты делаются из реального приложения на устройстве/эмуляторе;
  оператор вручную открывает нужный экран и нажимает Enter. Скрипт отвечает за
  установку APK, профиль, съёмку, проверку размеров и сброс настроек экрана.

.EXAMPLE
  powershell -NoProfile -File tools/capture-store-screenshots.ps1 -DryRun

.EXAMPLE
  powershell -NoProfile -File tools/capture-store-screenshots.ps1 -Serial emulator-5554

.EXAMPLE
  powershell -NoProfile -File tools/capture-store-screenshots.ps1 -SkipInstall -NoPrompt -DelaySec 3
#>
[CmdletBinding()]
param(
    [string]$Adb = '',
    [string]$Serial = '',
    [string]$Package = 'com.iilmchat.chessproblem',
    [string]$Apk = 'app/build/outputs/apk/debug/app-debug.apk',
    [string]$Output = 'store/screenshots',
    [switch]$SkipInstall,
    [switch]$NoPrompt,
    [int]$DelaySec = 2,
    [switch]$DryRun
)

$ErrorActionPreference = 'Stop'
Add-Type -AssemblyName System.Drawing

if ([string]::IsNullOrWhiteSpace($Adb)) {
    $Adb = if ($env:ANDROID_HOME) { Join-Path $env:ANDROID_HOME 'platform-tools\adb.exe' } else { 'adb' }
}
if (-not [System.IO.Path]::IsPathRooted($Apk)) {
    $Apk = Join-Path (Join-Path $PSScriptRoot '..') $Apk
}
if (-not [System.IO.Path]::IsPathRooted($Output)) {
    $Output = Join-Path (Join-Path $PSScriptRoot '..') $Output
}

$profiles = @(
    @{ Name = 'phone'; Size = '1080x2400'; Density = 480 },
    @{ Name = 'tablet'; Size = '1600x2560'; Density = 320 }
)

$screens = @(
    @{ File = '01-home.png'; Label = 'Главная: задача дня и три уровня' },
    @{ File = '02-solver.png'; Label = 'Решатель: доска, задание, таймер, подсказки' },
    @{ File = '03-hints.png'; Label = 'Подсказки: три уровня и экономика' },
    @{ File = '04-result.png'; Label = 'Результат: звёзды, XP и разбор' },
    @{ File = '05-progress.png'; Label = 'Прогресс: статистика и серия' }
)

$adbArgs = if ([string]::IsNullOrWhiteSpace($Serial)) { @() } else { @('-s', $Serial) }

function Invoke-Adb([string[]]$Arguments) {
    if ($DryRun) {
        Write-Host "  [dry-run] adb $($Arguments -join ' ')"
        return ''
    }
    $psi = [System.Diagnostics.ProcessStartInfo]::new()
    $psi.FileName = $Adb
    foreach ($arg in $Arguments) { $psi.ArgumentList.Add($arg) }
    $psi.UseShellExecute = $false
    $psi.RedirectStandardOutput = $true
    $psi.RedirectStandardError = $true
    $proc = [System.Diagnostics.Process]::Start($psi)
    $stdout = $proc.StandardOutput.ReadToEnd()
    $stderr = $proc.StandardError.ReadToEnd()
    $proc.WaitForExit()
    if ($proc.ExitCode -ne 0) {
        throw "adb $($Arguments -join ' ') failed: $stderr"
    }
    return $stdout
}

# exec-out отдаёт бинарный PNG: копируем поток байтов, без текстовой конвертации.
function Save-AdbScreenshot([string]$TargetPath) {
    if ($DryRun) {
        Write-Host "  [dry-run] screencap -p -> $TargetPath"
        return
    }
    $psi = [System.Diagnostics.ProcessStartInfo]::new()
    $psi.FileName = $Adb
    foreach ($arg in $adbArgs) { $psi.ArgumentList.Add($arg) }
    $psi.ArgumentList.Add('exec-out')
    $psi.ArgumentList.Add('screencap')
    $psi.ArgumentList.Add('-p')
    $psi.UseShellExecute = $false
    $psi.RedirectStandardOutput = $true
    $psi.RedirectStandardError = $true
    $proc = [System.Diagnostics.Process]::Start($psi)
    $stream = [System.IO.File]::Create($TargetPath)
    try {
        $proc.StandardOutput.BaseStream.CopyTo($stream)
    }
    finally {
        $stream.Dispose()
    }
    $stderr = $proc.StandardError.ReadToEnd()
    $proc.WaitForExit()
    if ($proc.ExitCode -ne 0) { throw "screencap failed: $stderr" }

    $image = [System.Drawing.Image]::FromFile($TargetPath)
    try {
        Write-Host "  $TargetPath ($($image.Width)x$($image.Height), $((Get-Item $TargetPath).Length) bytes)"
    }
    finally {
        $image.Dispose()
    }
}

function Reset-Screen([string]$Name) {
    Write-Host "Сброс wm size/density ($Name)..."
    Invoke-Adb ($adbArgs + @('shell', 'wm', 'size', 'reset')) | Out-Null
    Invoke-Adb ($adbArgs + @('shell', 'wm', 'density', 'reset')) | Out-Null
}

if (-not $DryRun) {
    if (-not (Test-Path $Adb -PathType Leaf)) { throw "adb not found: $Adb" }
    $devices = Invoke-Adb @('devices')
    $lines = @($devices -split "`n" | Where-Object { $_ -match '^\S+\s+device\s*$' })
    if ($lines.Count -eq 0) { throw 'No connected device/emulator. Check `adb devices`.' }
    if ([string]::IsNullOrWhiteSpace($Serial) -and $lines.Count -gt 1) {
        throw "Multiple devices: $($lines.Count). Pass -Serial <serial>."
    }
    if ([string]::IsNullOrWhiteSpace($Serial)) {
        $Serial = ($lines[0] -split '\s+')[0]
        $adbArgs = @('-s', $Serial)
    }
    Write-Host "Device: $Serial"

    if (-not $SkipInstall) {
        Write-Host 'Building and installing app-debug.apk...'
        Push-Location (Join-Path $PSScriptRoot '..')
        try {
            & ./gradlew :app:installDebug
            if ($LASTEXITCODE -ne 0) { throw ':app:installDebug failed' }
        }
        finally { Pop-Location }
    }
    elseif (-not (Test-Path $Apk -PathType Leaf)) {
        throw "APK not found: $Apk"
    }
}

foreach ($profile in $profiles) {
    $name = $profile.Name
    $dir = Join-Path $Output $name
    Write-Host "`nProfile $name ($($profile.Size) @ $($profile.Density)dpi) -> $dir"
    if (-not $DryRun) { New-Item -ItemType Directory -Force -Path $dir | Out-Null }

    try {
        Invoke-Adb ($adbArgs + @('shell', 'wm', 'size', $profile.Size)) | Out-Null
        Invoke-Adb ($adbArgs + @('shell', 'wm', 'density', "$($profile.Density)")) | Out-Null
        Invoke-Adb ($adbArgs + @('shell', 'am', 'start', '-n', "$Package/.MainActivity")) | Out-Null
        Start-Sleep -Seconds $DelaySec

        foreach ($screen in $screens) {
            if ($NoPrompt -or $DryRun) {
                Write-Host "  $($screen.Label) — ждём $DelaySec с"
                if (-not $DryRun) { Start-Sleep -Seconds $DelaySec }
            }
            else {
                Read-Host "Откройте экран: $($screen.Label). Нажмите Enter для съёмки" | Out-Null
            }
            Save-AdbScreenshot (Join-Path $dir $screen.File)
        }
    }
    finally {
        Reset-Screen $name
    }
}

Write-Host "`nГотово. Проверьте PNG в $Output перед загрузкой в RuStore."
