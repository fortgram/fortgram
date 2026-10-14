# Contributing to FortGram

Thank you for looking at the code. This page is short on purpose — the project is small, and the rules below exist
mainly so that merging new Telegram X releases stays painless.

## Where things are

- **FortGram's own code** — `app/src/main/java/org/thunderdog/challegram/fortgram/`. `ParentalPolicy` is the single
  source of truth for the rules; everything else asks it.
- **Chokepoint patches** in upstream files (`Tdlib`, `TdlibUi`, `ChatsController`, `SearchManager`,
  `TdlibNotificationHelper`, `AppUpdater`) — a few lines each, always calling into `ParentalPolicy`. Keep them that way:
  the smaller the diff against Telegram X, the easier the next upstream merge.
- **Strings** — `app/src/main/res/values*/local_strings.xml` (English, Russian, Ukrainian). Never edit the generated
  `strings.xml`.
- Everything else is upstream [Telegram X](https://github.com/TGX-Android/Telegram-X); build instructions are in the
  [README](README.md).

## How changes land

1. Branch from the latest `main`. The branch is protected: no direct pushes, every change is a pull request.
2. Open a PR; the template is a five-line checklist. Say what the change does and on which device it was tested.
3. PRs are merged with a **merge commit**. Squash and rebase are disabled on purpose — Telegram X is merged into
   `main` with merge commits, and rewriting that history would break every future upstream merge.
4. Keep commits focused and messages descriptive. Commit with your GitHub noreply address if you do not want your
   e-mail in the public history.

## What is welcome

- Bug fixes, especially ones reproduced on a real device (phones with MIUI / ColorOS / One UI behave differently
  in the background — reports from those are valuable).
- Translations: a new `values-xx/local_strings.xml` with the same keys as `values/local_strings.xml`.
- Improvements to the parental-control screens that keep the current model (parent decides on the child's device,
  the app hides everything else).

Larger changes — a new way to manage the rules, remote control from the parent's phone, anything touching the PIN
or the whitelist logic — are worth an issue first, so the approach is agreed before the code is written. The
[roadmap](https://github.com/fortgram/fortgram-site/blob/main/docs/roadmap.md) lists what is planned.

## Security issues

Not in issues or PRs — see [SECURITY.md](.github/SECURITY.md).

## Licence

By contributing you agree that your changes are released under the [GNU GPL v3.0](LICENSE), like the rest of the
project.

---

## По-русски

Код FortGram живёт в `app/src/main/java/org/thunderdog/challegram/fortgram/`; `ParentalPolicy` — единственный
источник правды о правилах. Правки апстримовых файлов — несколько строк в узловых точках, всегда через
`ParentalPolicy`: чем меньше дифф с Telegram X, тем проще следующий мерж. Строки — в `local_strings.xml`
(`values`, `values-ru`, `values-uk`), сгенерированный `strings.xml` не трогать.

Изменения только через pull request от свежего `main`; вливаются **merge-коммитом** (squash и rebase выключены
намеренно — история Telegram X должна остаться нетронутой). В PR — что меняется и на каком устройстве проверено.

Приветствуются исправления ошибок (особенно воспроизведённые на реальных телефонах с MIUI / ColorOS / One UI),
переводы (`values-xx/local_strings.xml` с теми же ключами) и улучшения экранов родительского контроля в рамках
текущей модели. Крупные изменения — PIN, логика белого списка, удалённое управление — сначала обсуждаются в issue.

Уязвимости — не в issues и не в PR, а через [SECURITY.md](.github/SECURITY.md). Вклад публикуется под
[GNU GPL v3.0](LICENSE), как и весь проект.
