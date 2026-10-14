# Security Policy

FortGram is a parental-control client: a security bug here usually means a way for a child to see hidden chats,
change the rules or remove the PIN. Please report such issues privately so they can be fixed before they are
described in public.

## Reporting a vulnerability

Use **[Report a vulnerability](https://github.com/fortgram/fortgram/security/advisories/new)** (the *Security* tab of
this repository). The report is visible only to the maintainers. Please include:

- the FortGram version (*Settings → About*, long-press the version → *Copy Report Details*);
- the phone model and Android version;
- steps to reproduce, and what a child would gain by doing them.

You will get a reply within a few days. Once a fix is released, the report is published as a security advisory with
credit to you (unless you prefer to stay anonymous).

Please do **not** open a public issue for bypasses of the parental controls — the issue would be a how-to for every
child whose phone has not been updated yet.

## What is in scope

- The parental-control layer (`app/src/main/java/org/thunderdog/challegram/fortgram/`) and the places where
  FortGram changes Telegram X behaviour: whitelist, allowed groups, sticker restriction, PIN, export / import,
  notification and search filtering.
- The official APKs published on the Releases page.

Bugs in Telegram X itself or in TDLib are better reported upstream: [Telegram X security
policy](https://github.com/TGX-Android/Telegram-X/blob/main/docs/SECURITY.md), [Telegram bug bounty
program](https://core.telegram.org/bug-bounty). If you are not sure where a bug belongs, report it here and we will
forward it.

## Out of scope

FortGram does not protect against a child who has the account credentials and another device, and it does not
filter message content. These are documented limitations, not vulnerabilities — see
[Known limitations](https://github.com/fortgram/fortgram-site/blob/main/docs/fortgram.md#known-limitations).

---

## По-русски

FortGram — клиент с родительским контролем, поэтому уязвимость здесь, как правило, означает способ для ребёнка
увидеть скрытые чаты, изменить правила или снять PIN. Сообщайте о таких проблемах приватно, чтобы их можно было
исправить до публичного описания.

**Как сообщить:** кнопка **[Report a vulnerability](https://github.com/fortgram/fortgram/security/advisories/new)**
на вкладке *Security* этого репозитория. Отчёт видят только мейнтейнеры. Укажите версию FortGram (*Настройки →
О программе*, долгое нажатие на версию → *Copy Report Details*), модель телефона и версию Android, шаги
воспроизведения и что получает ребёнок в результате. Ответ — в течение нескольких дней; после выпуска исправления
отчёт публикуется как security advisory с указанием автора (если вы не против).

Пожалуйста, **не открывайте публичный issue** про обход родительского контроля — он станет инструкцией для каждого
ребёнка, чей телефон ещё не обновился.

Ошибки в самом Telegram X или TDLib лучше сообщать апстриму (ссылки выше); если не уверены, куда, — пишите сюда,
перенаправим. Использование аккаунта на другом устройстве и отсутствие фильтрации содержимого сообщений —
документированные ограничения, а не уязвимости.
