<!-- Thank you for contributing to FortGram. A short checklist, nothing more. / Спасибо за вклад. Короткий чеклист. -->

**What this changes / Что меняется**


**Why / Зачем**


**Checklist / Чеклист**
- [ ] Based on the latest `main`; merged with a merge commit (no squash / rebase — upstream Telegram X history must stay intact).
      Ветка от свежего `main`; вливается merge-коммитом (история апстрима должна остаться нетронутой).
- [ ] FortGram-specific code lives in `app/src/main/java/org/thunderdog/challegram/fortgram/`; changes to upstream files are the minimum needed and go through `ParentalPolicy`.
      Код FortGram — в пакете `fortgram/`; правки апстримовых файлов минимальны и идут через `ParentalPolicy`.
- [ ] New strings are in `local_strings.xml` for `values`, `values-ru` and `values-uk`.
      Новые строки — в `local_strings.xml` для `values`, `values-ru`, `values-uk`.
- [ ] Tested on a real device (model + Android version): …
      Проверено на реальном устройстве (модель + версия Android): …
- [ ] No secrets, keys, `local.properties` or personal data in the diff.
      В диффе нет секретов, ключей, `local.properties` и личных данных.
