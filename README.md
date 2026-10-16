<a id="lang-en"></a>
🌐 **Languages / Языки:** [English](#lang-en) · [Русский](#lang-ru)

# FortGram

**A Telegram client for children, with parental controls built into the app.**

FortGram is a fork of [Telegram X](https://github.com/TGX-Android/Telegram-X) for Android. It replaces the
standard Telegram app on a child's phone: the parent decides who the child can talk to, and the app hides
everyone else — in the chat list, in search and in notifications. Settings are protected by a parental PIN.

- Website and documentation: **[fgram.org](https://fgram.org)** — [Quick Start](https://github.com/fortgram/fortgram-site/blob/main/docs/quickstart.md), [How it works](https://github.com/fortgram/fortgram-site/blob/main/docs/fortgram.md), [FAQ](https://github.com/fortgram/fortgram-site/blob/main/docs/faq.md)
- Download: [GitHub Releases](https://github.com/fortgram/fortgram/releases) (Android 7.0+, arm64)
- [Privacy Policy](https://github.com/fortgram/fortgram-site/blob/main/docs/privacy.md) · [Disclaimer](https://github.com/fortgram/fortgram-site/blob/main/docs/DISCLAIMER.md)

## What it does

- **Whitelist mode** — the child sees only phone-book contacts, users and groups the parent allowed, and Telegram's
  service chat. Everything else is hidden from the chat list, search results and notifications.
- **Allowed groups** — the parent switches individual groups on; in an allowed group the child can read and write.
- **Sticker packs** — optionally only parent-approved packs, with search by name.
- **Parental PIN** — settings are locked behind a PIN; the child can use the app but cannot change the rules.
- **Backup and transfer** — export the rules to a file, the clipboard or a QR code and import them on another phone,
  with a preview where individual entries can be unticked. The export never contains the PIN or any messages.
- **Background work and calls** — a settings screen with shortcuts to the vendor-specific switches (battery,
  autostart, pop-ups in background) that decide whether calls ring when the app is closed.

What it does *not* do: read, filter or block message content, monitor the child, or stop the child from using the
account on another device. The documentation describes the device- and account-level measures that go with it.

## Privacy

There are no FortGram servers. The app talks to Telegram through [TDLib](https://github.com/tdlib/td) exactly like
Telegram X does, and uses Firebase Cloud Messaging only to receive push notifications. No analytics, no crash
reporting, no telemetry. Details: [Privacy Policy](https://github.com/fortgram/fortgram-site/blob/main/docs/privacy.md).

## Verifying a release

Official builds are signed with one key. Before installing an APK downloaded from anywhere other than this
repository's Releases page:

```
apksigner verify --print-certs FortGram-*.apk
```

must show certificate SHA-256
`54:DA:5E:95:C6:83:2D:DE:0A:89:35:48:2D:5D:EE:C7:A3:FA:C0:A1:9B:10:AF:58:E1:E1:70:F7:1C:17:5F:2B`.
Android itself refuses to install an update signed with a different key over an existing FortGram.

## What is changed compared to Telegram X

Upstream is kept as close to 1:1 as possible so that new Telegram X versions can be merged. The fork adds

- `app/src/main/java/org/thunderdog/challegram/fortgram/` — the parental-control layer: `ParentalPolicy` (the
  single source of truth for the rules), the settings screens, export / import (`ParentalConfig`);
- small patches at a few chokepoints (`Tdlib`, `TdlibUi`, `ChatsController`, `SearchManager`,
  `TdlibNotificationHelper`, `AppUpdater`) that consult `ParentalPolicy`;
- strings in `app/src/main/res/values*/local_strings.xml` (English, Russian, Ukrainian);
- the Telegram X in-app updater is disabled — updates come from Releases.

`git log --author=TheRu27` lists every change; everything else is upstream Telegram X.

## Building

The build is the upstream Telegram X build: Gradle compiles the native dependencies itself, and the SDK, NDK and
build-tools versions are pinned in `version.properties`. Official FortGram builds are made on macOS (Apple
Silicon); Ubuntu is the second supported path. Either way the result is `./gradlew assembleFortgramArm64Release`.

### Option A — macOS

1. Xcode Command Line Tools with the licence accepted: `xcode-select --install`, then
   `sudo xcodebuild -license accept` (without it the libvpx `make` step fails).
2. Homebrew packages: `brew install git-lfs wget gnu-sed && git lfs install`.
3. JDK 25 — any OpenJDK build: `brew install openjdk@25`, or a Temurin archive if you want it outside Homebrew; point `JAVA_HOME` at it.
4. Clone with submodules: `git clone --recursive --depth=1 --shallow-submodules https://github.com/fortgram/fortgram.git`
5. Android SDK + NDK: choose a directory, `export ANDROID_SDK_ROOT=<that directory>`, then run `scripts/setup-sdk.sh`
   from the repository root — it downloads the command-line tools, platform, build-tools and the pinned NDK into it.
6. Do the **common steps** below, then `./gradlew assembleFortgramArm64Release`.

### Option B — Ubuntu

`scripts/setup-host.sh` does the whole thing on a fresh host: apt packages (OpenJDK 25, git-lfs, yasm, nasm, …),
the Android SDK + NDK, a `local.properties` template, submodules, and the build.

1. `git clone --depth=1 https://github.com/fortgram/fortgram.git && cd fortgram`
2. `scripts/setup-host.sh deps` — system packages and the SDK (`ANDROID_SDK_ROOT` defaults to `~/android-sdk`).
3. `scripts/setup-host.sh sync` — writes a `local.properties` template and fetches submodules.
4. Do the **common steps** below.
5. `scripts/setup-host.sh build` — Gradle build; the script prints the APK path and its certificate.

### Common steps

- `local.properties` (macOS: copy `local.properties.sample`; Ubuntu: the template from step 3). Fill in:
  - `sdk.dir` — your Android SDK directory;
  - `app.id` — **your own package name**; `org.fgram.app` belongs to the official builds;
  - `telegram.api_id` / `telegram.api_hash` — your own, from [my.telegram.org](https://my.telegram.org/apps);
  - `keystore.file` — path to a `keystore.properties` file **outside** the repository with `keystore.file`,
    `keystore.password`, `key.alias`, `key.password` for your signing key (`keytool -genkeypair` creates one).
    Never commit signing material — `.gitignore` blocks the usual names, keep it that way.
- Push notifications need your own Firebase project: replace `app/google-services.json` (the committed one is for
  `org.fgram.app`) and register the FCM service account at my.telegram.org for your `api_id`.

The APK lands in `app/build/outputs/apk/fortgramArm64/release/`. The first build compiles the native dependencies
(about 7 minutes on an M1 Pro, over an hour on a 4-core cloud VM); later builds take a minute or two. The upstream
[Telegram X README](https://github.com/TGX-Android/Telegram-X#readme) covers the build system, flavours and ABIs
in more depth.

## License

FortGram is free software under the [GNU General Public License v3.0](LICENSE), as a derivative of Telegram X
(© Telegram X authors). FortGram's changes are © the FortGram contributors. It is provided "as is", without
warranty of any kind — see the [Disclaimer](https://github.com/fortgram/fortgram-site/blob/main/docs/DISCLAIMER.md).

FortGram is an independent project and is not affiliated with Telegram or Google.

---

<a id="lang-ru"></a>
🌐 **Languages / Языки:** [English](#lang-en) · [Русский](#lang-ru)

# FortGram

**Telegram-клиент для детей со встроенным родительским контролем.**

FortGram — форк [Telegram X](https://github.com/TGX-Android/Telegram-X) для Android. Он ставится на телефон
ребёнка вместо обычного Telegram: родитель решает, с кем ребёнок может общаться, а всех остальных приложение
скрывает — в списке чатов, в поиске и в уведомлениях. Настройки защищены родительским PIN.

- Сайт и документация: **[fgram.org](https://fgram.org)** — [Быстрый старт](https://github.com/fortgram/fortgram-site/blob/main/docs/quickstart.md#быстрый-старт), [Как это работает](https://github.com/fortgram/fortgram-site/blob/main/docs/fortgram.md#fortgram--как-это-работает), [Вопросы и решение проблем](https://github.com/fortgram/fortgram-site/blob/main/docs/faq.md#вопросы-и-решение-проблем)
- Скачать: [GitHub Releases](https://github.com/fortgram/fortgram/releases) (Android 7.0+, arm64)
- [Политика конфиденциальности](https://github.com/fortgram/fortgram-site/blob/main/docs/privacy.md#политика-конфиденциальности) · [Дисклеймер](https://github.com/fortgram/fortgram-site/blob/main/docs/DISCLAIMER.md#дисклеймер)

## Что умеет

- **Режим белого списка** — ребёнок видит только контакты из телефонной книги, разрешённых родителем
  пользователей и группы и служебный чат Telegram. Всё остальное скрыто из списка чатов, результатов поиска
  и уведомлений.
- **Разрешённые группы** — родитель включает группы по одной; в разрешённой группе ребёнок может читать и писать.
- **Стикерпаки** — по желанию только одобренные родителем наборы, с поиском по названию.
- **Родительский PIN** — настройки закрыты PIN-кодом; ребёнок пользуется приложением, но не может менять правила.
- **Резервная копия и перенос** — правила экспортируются в файл, буфер обмена или QR-код и импортируются на
  другом телефоне; перед применением показывается предпросмотр, где лишние записи можно снять. В экспорте никогда
  нет ни PIN, ни сообщений.
- **Фоновая работа и звонки** — экран настроек с переходами к переключателям конкретных производителей
  (батарея, автозапуск, всплывающие окна в фоне), от которых зависит, прозвонит ли вызов при закрытом приложении.

Чего приложение *не* делает: не читает, не фильтрует и не блокирует содержимое сообщений, не следит за ребёнком
и не мешает использовать аккаунт на другом устройстве. Меры на уровне устройства и аккаунта, которые идут
в комплекте с ним, описаны в документации.

## Приватность

Серверов FortGram не существует. Приложение общается с Telegram через [TDLib](https://github.com/tdlib/td) ровно
так же, как Telegram X, а Firebase Cloud Messaging использует только для получения push-уведомлений. Никакой
аналитики, отчётов о сбоях и телеметрии. Подробнее: [Политика конфиденциальности](https://github.com/fortgram/fortgram-site/blob/main/docs/privacy.md#политика-конфиденциальности).

## Проверка подлинности выпуска

Официальные сборки подписаны одним ключом. Перед установкой APK, скачанного откуда-либо кроме страницы Releases
этого репозитория:

```
apksigner verify --print-certs FortGram-*.apk
```

должен показать сертификат SHA-256
`54:DA:5E:95:C6:83:2D:DE:0A:89:35:48:2D:5D:EE:C7:A3:FA:C0:A1:9B:10:AF:58:E1:E1:70:F7:1C:17:5F:2B`.
Android и сам откажется ставить поверх установленного FortGram обновление с другой подписью.

## Что изменено по сравнению с Telegram X

Апстрим сохраняется максимально близко к 1:1, чтобы новые версии Telegram X можно было вливать. Форк добавляет

- `app/src/main/java/org/thunderdog/challegram/fortgram/` — слой родительского контроля: `ParentalPolicy`
  (единственный источник правды о правилах), экраны настроек, экспорт / импорт (`ParentalConfig`);
- небольшие правки в нескольких узловых точках (`Tdlib`, `TdlibUi`, `ChatsController`, `SearchManager`,
  `TdlibNotificationHelper`, `AppUpdater`), которые обращаются к `ParentalPolicy`;
- строки в `app/src/main/res/values*/local_strings.xml` (английский, русский, украинский);
- встроенное обновление Telegram X отключено — обновления берутся из Releases.

`git log --author=TheRu27` показывает каждое изменение; всё остальное — исходный Telegram X.

## Сборка

Сборка — это сборка Telegram X: Gradle сам компилирует нативные зависимости, а версии SDK, NDK и build-tools
зафиксированы в `version.properties`. Официальные сборки FortGram делаются на macOS (Apple Silicon); второй
поддерживаемый путь — Ubuntu. В обоих случаях итог один: `./gradlew assembleFortgramArm64Release`.

### Вариант A — macOS

1. Xcode Command Line Tools с принятой лицензией: `xcode-select --install`, затем
   `sudo xcodebuild -license accept` (без этого падает шаг `make` у libvpx).
2. Пакеты Homebrew: `brew install git-lfs wget gnu-sed && git lfs install`.
3. JDK 25 — любая сборка OpenJDK: `brew install openjdk@25` или архив Temurin, если хотите держать его вне Homebrew; укажите на него `JAVA_HOME`.
4. Клонирование с сабмодулями: `git clone --recursive --depth=1 --shallow-submodules https://github.com/fortgram/fortgram.git`
5. Android SDK + NDK: выберите каталог, `export ANDROID_SDK_ROOT=<этот каталог>` и запустите `scripts/setup-sdk.sh`
   из корня репозитория — он скачает туда command-line tools, платформу, build-tools и зафиксированный NDK.
6. Выполните **общие шаги** ниже, затем `./gradlew assembleFortgramArm64Release`.

### Вариант B — Ubuntu

`scripts/setup-host.sh` делает всё на чистом хосте: apt-пакеты (OpenJDK 25, git-lfs, yasm, nasm, …),
Android SDK + NDK, шаблон `local.properties`, сабмодули и саму сборку.

1. `git clone --depth=1 https://github.com/fortgram/fortgram.git && cd fortgram`
2. `scripts/setup-host.sh deps` — системные пакеты и SDK (`ANDROID_SDK_ROOT` по умолчанию `~/android-sdk`).
3. `scripts/setup-host.sh sync` — создаёт шаблон `local.properties` и забирает сабмодули.
4. Выполните **общие шаги** ниже.
5. `scripts/setup-host.sh build` — сборка Gradle; скрипт печатает путь к APK и его сертификат.

### Общие шаги

- `local.properties` (macOS: скопируйте `local.properties.sample`; Ubuntu: шаблон из шага 3). Заполните:
  - `sdk.dir` — каталог вашего Android SDK;
  - `app.id` — **своё имя пакета**; `org.fgram.app` принадлежит официальным сборкам;
  - `telegram.api_id` / `telegram.api_hash` — свои, с [my.telegram.org](https://my.telegram.org/apps);
  - `keystore.file` — путь к файлу `keystore.properties` **вне** репозитория с полями `keystore.file`,
    `keystore.password`, `key.alias`, `key.password` для вашего ключа подписи (его создаёт `keytool -genkeypair`).
    Никогда не коммитьте материалы подписи — `.gitignore` закрывает типичные имена, не ослабляйте его.
- Для push-уведомлений нужен свой проект Firebase: замените `app/google-services.json` (закоммиченный — для
  `org.fgram.app`) и зарегистрируйте сервисный аккаунт FCM на my.telegram.org для своего `api_id`.

APK появится в `app/build/outputs/apk/fortgramArm64/release/`. Первая сборка компилирует нативные зависимости
(около 7 минут на M1 Pro, больше часа на 4-ядерной облачной VM), последующие занимают минуту-две. Система сборки,
флейворы и ABI подробнее описаны в [README Telegram X](https://github.com/TGX-Android/Telegram-X#readme).

## Лицензия

FortGram — свободное программное обеспечение под [GNU General Public License v3.0](LICENSE) как производная
работа от Telegram X (© авторы Telegram X). Изменения FortGram — © участники проекта FortGram. Предоставляется
«как есть», без каких-либо гарантий — см. [Дисклеймер](https://github.com/fortgram/fortgram-site/blob/main/docs/DISCLAIMER.md#дисклеймер).

FortGram — независимый проект, не связанный с Telegram или Google.
