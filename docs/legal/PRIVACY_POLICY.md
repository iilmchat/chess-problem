# Политика конфиденциальности / Privacy Policy

**Версия:** 1.0 · **Дата вступления:** 28.10.2026
**Приложение:** Chess Problem / «Шахматные задачи» (`com.iilmchat.chessproblem`)
**Оператор:** RuChating (iilmchat) · [info@iiruchating.ru](mailto:info@iiruchating.ru)
**Область:** Android-приложение, распространяемое в RuStore

> Это черновик политики для офлайн-версии v1.0. Перед публикацией проверьте
> текст у юриста и опубликуйте по постоянному URL. Политика описывает только
> текущую версию; при добавлении рекламы или покупок она будет обновлена до
> релиза этих функций.

---

## 1. Кратко

- **v1.0 не собирает персональные данные.**
- Нет аккаунта, облака, аналитики, рекламы и сетевых запросов.
- Прогресс и настройки хранятся только на устройстве.
- Приложение работает офлайн; для игры интернет не нужен.
- Удаление приложения удаляет локальные данные (если ОС не сохранила их в
  резервной копии; в v1.0 резервное копирование отключено).

---

## 2. Русский

### 2.1. Общие сведения

Настоящая Политика конфиденциальности описывает, какие данные обрабатывает
приложение Chess Problem («Приложение»), как они хранятся и какие права есть у
пользователя. Оператор — RuChating (iilmchat).

Приложение предназначено для решения шахматных задач. Оно не требует
регистрации, не создаёт учётную запись и не передаёт данные на сервер.

### 2.2. Какие данные обрабатываются

В версии v1.0 Приложение **не собирает и не передаёт**:

- имя, e-mail, телефон, адрес;
- геолокацию;
- идентификаторы устройства или рекламный идентификатор;
- контакты, фото, файлы, микрофон, камеру;
- историю браузера или других приложений;
- аналитику использования и crash-отчёты.

На устройстве локально хранятся:

| Данные | Зачем | Где |
| --- | --- | --- |
| Прогресс: решённые задачи, звёзды, XP, серия, достижения | Показывать прогресс и статистику | Room, приватное хранилище приложения |
| Настройки: язык, тема, доска, звук, haptics, доступность | Сохранять выбор пользователя | DataStore и синхронный кэш SharedPreferences (язык, тема), приватное хранилище приложения |
| Последняя задача и отметка времени (`lastPlayedAt`) | Показать карточку «Продолжить» после сворачивания; текущее время таймера не сохраняется | Room, таблица `puzzle_progress` |

Эти данные не покидают устройство. Доступ к ним имеет только Приложение в
пределах своей песочницы Android.

### 2.3. Разрешения

В v1.0 Приложение не запрашивает опасных разрешений. Для тактильной отдачи
может использоваться системный haptic feedback, который не требует доступа к
персональным данным. Если в будущем понадобится разрешение, оно будет
запрошено с понятным объяснением, а Политика обновлена.

### 2.4. Сеть и сторонние SDK

В v1.0 Приложение не содержит `INTERNET`, рекламных, аналитических или
платёжных SDK. Контент и прогресс локальные.

В Приложении указана атрибуция Lichess: часть задач основана на открытой базе
Lichess (CC0 1.0). Lichess не получает данные пользователя через Приложение и
не связан с ним.

Магазин RuStore может самостоятельно собирать статистику установок, крашей и
отзывов в соответствии со своей политикой. Это происходит на стороне магазина
и не контролируется Приложением; см. политику RuStore.

### 2.5. Дети

Приложение имеет возрастную маркировку 6+. Мы сознательно не собираем данные
детей и не показываем рекламу. Если в будущем появится реклама или покупки,
Политика и возрастная маркировка будут пересмотрены.

### 2.6. Хранение и удаление

Данные хранятся на устройстве, пока установлено Приложение. Пользователь может:

- сбросить прогресс в «Настройки → Данные → Сбросить прогресс»;
- удалить Приложение — вместе с ним удаляются локальные данные приложения.

В v1.0 `allowBackup=false`, поэтому прогресс не копируется в облачный backup
Android. При удалении Приложения восстановить прогресс невозможно.

### 2.7. Права пользователя

Поскольку Оператор не получает персональные данные, запросы на доступ,
исправление или удаление данных на сервере неприменимы. По вопросам
приватности можно написать на [info@iiruchating.ru](mailto:info@iiruchating.ru).

### 2.8. Изменения

Мы можем обновлять Политику при изменении функций Приложения. Новая версия
публикуется по тому же URL с датой вступления. Существенные изменения
(например, появление рекламы или покупок) будут отражены в Приложении и
карточке RuStore до включения функции.

### 2.9. Контакты

- E-mail: [info@iiruchating.ru](mailto:info@iiruchating.ru)
- GitHub: [github.com/iilmchat](https://github.com/iilmchat)
- Сайт: [iiruchating.ru](https://iiruchating.ru)

---

## 3. English

### 3.1. Overview

This Privacy Policy explains what data Chess Problem (“the App”) processes, how
it is stored, and what rights users have. The operator is RuChating (iilmchat).

The App is a chess puzzle trainer. It does not require an account and does not
send data to a server.

### 3.2. Data processed

In v1.0 the App **does not collect or transmit**:

- name, email, phone number, or address;
- location;
- device identifiers or advertising ID;
- contacts, photos, files, microphone, or camera;
- browsing history or usage of other apps;
- usage analytics or crash reports.

The App stores the following locally on the device:

| Data | Purpose | Where |
| --- | --- | --- |
| Progress: solved puzzles, stars, XP, streak, achievements | Show progress and statistics | Room, app-private storage |
| Settings: language, theme, board, sound, haptics, accessibility | Remember user preferences | DataStore and synchronous SharedPreferences cache (language, theme), app-private storage |
| Last puzzle and its `lastPlayedAt` timestamp | Show the "Continue" card after backgrounding; the running timer is not persisted | Room, `puzzle_progress` table |

This data never leaves the device. Only the App can access it within its Android
sandbox.

### 3.3. Permissions

v1.0 requests no dangerous permissions. Haptic feedback may use the system
haptic API, which does not provide access to personal data. Any future
permission will be requested with a clear explanation and an updated Policy.

### 3.4. Network and third-party SDKs

v1.0 has no `INTERNET` permission and contains no advertising, analytics, or
payment SDKs. Content and progress are local.

The App includes Lichess attribution: some puzzles are based on the open Lichess
puzzle database (CC0 1.0). Lichess does not receive user data through the App
and is not affiliated with it.

RuStore may independently collect install, crash, and review statistics under
its own policy. This happens on the store side and is not controlled by the App;
see RuStore’s policy.

### 3.5. Children

The App is rated 6+. We intentionally do not collect children’s data and do not
show ads. If advertising or purchases are added, the Policy and age rating will
be reviewed.

### 3.6. Retention and deletion

Data stays on the device while the App is installed. The user can:

- reset progress in “Settings → Data → Reset progress”;
- uninstall the App, which removes its local data.

v1.0 uses `allowBackup=false`, so progress is not copied to Android cloud
backup. Uninstalling the App permanently removes progress.

### 3.7. User rights

Because the operator does not receive personal data, server-side access,
correction, or deletion requests do not apply. For privacy questions, contact
[info@iiruchating.ru](mailto:info@iiruchating.ru).

### 3.8. Changes

We may update this Policy when App features change. The new version will be
published at the same URL with an effective date. Material changes (for example,
ads or purchases) will be reflected in the App and the RuStore listing before
the feature is enabled.

### 3.9. Contact

- Email: [info@iiruchating.ru](mailto:info@iiruchating.ru)
- GitHub: [github.com/iilmchat](https://github.com/iilmchat)
- Website: [iiruchating.ru](https://iiruchating.ru)

---

## 4. Публикация и версии

| Версия | Дата | Что изменилось |
| --- | --- | --- |
| 1.0 | 28.10.2026 | Первая редакция для офлайн-версии v1.0: данные не собираются |

**Плановый URL:** `https://iiruchating.ru/chess-problem/privacy`
(до публикации — GitHub Pages или раздел сайта).

Политика должна быть доступна:

- в карточке RuStore;
- в Приложении: «Настройки → О приложении → Политика конфиденциальности»;
- по постоянному URL без авторизации.

---

## 5. Будущие версии (не действует сейчас)

Если в v1.1 появятся **rewarded-реклама** и/или **встроенные покупки**, до их
включения Политика будет дополнена:

- перечнем рекламных/платёжных SDK и их данными;
- правовым основанием и согласием;
- данными о покупках (без номера карты — его обрабатывает RuStore);
- возрастом и настройками персонализации;
- способом отключить рекламу (Premium).

До обновления Политики эти функции не включаются.
