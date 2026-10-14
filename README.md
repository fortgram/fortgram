# FortGram

**A Telegram client for children, with parental controls built into the app.**
*Telegram-клиент для детей со встроенным родительским контролем — [по-русски ниже](#по-русски).*

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

The build is the upstream Telegram X build (Gradle; native dependencies are compiled by Gradle tasks). What you need:

1. JDK 21, Git with Git LFS, and the Android SDK with the NDK version pinned in `version.properties`
   (`scripts/setup-sdk.sh` installs them).
2. `local.properties` — copy `local.properties.sample` and fill in:
   - `app.id` — **your own package name**; `org.fgram.app` belongs to the official builds;
   - `telegram.api_id` / `telegram.api_hash` — your own, from [my.telegram.org](https://my.telegram.org/apps);
   - `keystore.file` — path to a `keystore.properties` file **outside** the repository with `keystore.file`,
     `keystore.password`, `key.alias`, `key.password`. Never commit signing material (`.gitignore` blocks the usual names).
3. Push notifications need your own Firebase project: replace `app/google-services.json` (the committed one is for
   `org.fgram.app`) and register the FCM service account at my.telegram.org for your `api_id`.

```
git submodule update --init --recursive --depth=1
./gradlew assembleLatestArm64Release
```

The APK lands in `app/build/outputs/apk/latestArm64/release/`. `scripts/setup-host.sh` bootstraps a fresh Ubuntu
host end to end. The upstream [Telegram X README](https://github.com/TGX-Android/Telegram-X#readme) covers the
build system, flavours and ABIs in more depth.

## License

FortGram is free software under the [GNU General Public License v3.0](LICENSE), as a derivative of Telegram X
(© Telegram X authors). FortGram's changes are © the FortGram contributors. It is provided "as is", without
warranty of any kind — see the [Disclaimer](https://github.com/fortgram/fortgram-site/blob/main/docs/DISCLAIMER.md).

FortGram is an independent project and is not affiliated with Telegram or Google.

---

## По-русски

**FortGram** — Android-клиент Telegram для детей, форк [Telegram X](https://github.com/TGX-Android/Telegram-X).
Он ставится на телефон ребёнка вместо обычного Telegram: родитель решает, с кем ребёнок может общаться
(контакты, разрешённые пользователи и группы, при желании — стикерпаки), а всё остальное приложение скрывает —
в списке чатов, в поиске и в уведомлениях. Настройки защищены родительским PIN; правила можно сохранить в файл
или QR-код и перенести на другой телефон.

Сайт и документация: **[fgram.org](https://fgram.org)** —
[Быстрый старт](https://github.com/fortgram/fortgram-site/blob/main/docs/quickstart.md#быстрый-старт),
[Как это работает](https://github.com/fortgram/fortgram-site/blob/main/docs/fortgram.md#fortgram--как-это-работает),
[Вопросы и решение проблем](https://github.com/fortgram/fortgram-site/blob/main/docs/faq.md#вопросы-и-решение-проблем),
[Политика конфиденциальности](https://github.com/fortgram/fortgram-site/blob/main/docs/privacy.md#политика-конфиденциальности).
Скачать: [Releases](https://github.com/fortgram/fortgram/releases) (Android 7.0+, arm64).

Серверов FortGram не существует: приложение общается с Telegram через TDLib, как и Telegram X, а Firebase
используется только для доставки push-уведомлений. Никакой аналитики и телеметрии.

Лицензия — GPL-3.0. FortGram — независимый проект, не связанный с Telegram или Google.
