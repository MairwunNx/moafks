# Mo’Afks

<p align="center">
  <a href="https://github.com/MairwunNx/moafks/releases/latest"><img alt="Release" src="https://img.shields.io/github/v/release/MairwunNx/moafks?label=release"></a>
  <a href="https://modrinth.com/plugin/moafks"><img alt="Modrinth" src="https://img.shields.io/modrinth/dt/moafks?logo=modrinth&label=Modrinth&color=00AF5C"></a>
  <a href="https://hangar.papermc.io/MairwunNx/MoAfks"><img alt="Hangar" src="https://img.shields.io/hangar/dt/MoAfks?label=Hangar&color=1E88E5"></a>
  <a href="https://www.curseforge.com/projects/1766155"><img alt="CurseForge" src="https://img.shields.io/curseforge/dt/1766155?logo=curseforge&label=CurseForge&color=F16436"></a>
  <img alt="Paper 26.3" src="https://img.shields.io/badge/Paper-26.3-F7CF0D">
  <img alt="Java 25" src="https://img.shields.io/badge/Java-25-E76F00">
  <a href="https://github.com/MairwunNx/moafks/actions/workflows/build.yml"><img alt="Build" src="https://img.shields.io/github/actions/workflow/status/MairwunNx/moafks/build.yml?branch=master&label=build"></a>
  <a href="LICENSE"><img alt="License" src="https://img.shields.io/github/license/MairwunNx/moafks"></a>
</p>

🛡️ **Mo’Afks** позволяет игрокам на серверах Paper спокойно отойти от компьютера. Введите `/afk` или постойте без дела несколько минут, и ваш персонаж перейдёт в безопасный режим AFK: его ничто не ранит и не толкает, мобы его не трогают, а ночь проходит и без вас. Пошевелитесь — и вы снова в игре.

[English version](https://github.com/MairwunNx/moafks/blob/master/README.md)

[![Играйте на Moonlands: чистая ваниль, сложные мобы, ИИ, приваты. ru.minecraft.mairwunnx.com](https://raw.githubusercontent.com/MairwunNx/moafks/master/media/og-adv-image.ru.png)](https://minecraft.mairwunnx.com)

Mo’Afks работает на Moonlands — моём сервере со сложным выживанием для друзей. Заходите на `ru.minecraft.mairwunnx.com` из России или на `minecraft.mairwunnx.com` откуда угодно, введите `/afk` и посмотрите, от чего он защищает.

## Возможности

- 🛡️ **Игрока в AFK ничто не ранит и не сдвинет:** ни урона, ни огня, ни утопления, ни голода, ни отбрасывания, а снаряды пролетают насквозь. Из пустоты плагин вернёт его назад.
- 👻 **Мобы его не трогают:** никто не выбирает его целью, варден его не слышит, фантомы не прилетают, а «Дурное знамение» не начинает рейд.
- 🌙 **Игроки в AFK не мешают ночи пройти.** Один отошедший друг больше не держит всех в темноте.
- ⚖️ **Защитой нельзя злоупотребить.** В AFK не уйти посреди боя, в падении или когда за вами гонится моб, а игрок в AFK не подбирает предметы и опыт.
- 🔙 **Возвращается игрок сам.** Его вернёт нажатая клавиша, клик или сообщение в чат, но не вода и не поршень. Из опасного места плагин перенесёт его в безопасное.
- 🌍 **Каждый читает на своём языке:** плагин выбирает его по IP, а игрок может выбрать сам. Английский, русский, испанский, португальский и немецкий идут в комплекте.
- ✨ **Все видят, кто отошёл:** кольцо частиц и метка [AFK]. По желанию — ограничение времени с киком. Игроки в ванише остаются невидимыми.
- 🧩 **Плагин встраивается в сервер:** PlaceholderAPI, TAB, MiniPlaceholders и Plan, если они стоят, и `/moafks reload` без перезапуска.

## Как начать

1. Скачайте jar с [Modrinth](https://modrinth.com/plugin/moafks), [Hangar](https://hangar.papermc.io/MairwunNx/MoAfks) или [GitHub](https://github.com/MairwunNx/moafks/releases/latest/download/moafks.jar) и положите в `plugins/` сервера Paper 26.3 на Java 25. В [itzg/minecraft-server](https://docker-minecraft-server.readthedocs.io) вместо этого добавьте `moafks` в `MODRINTH_PROJECTS`.
2. Перезапустите сервер, зайдите и введите `/afk обедаю`.
3. Страница [First steps](https://github.com/MairwunNx/moafks/wiki/First-steps) объясняет, как настроить таймер бездействия и кик.

## Документация

Руководство — в [вики](https://github.com/MairwunNx/moafks/wiki), на английском.

**Игрокам и владельцам серверов:** [Installation](https://github.com/MairwunNx/moafks/wiki/Installation) · [First steps](https://github.com/MairwunNx/moafks/wiki/First-steps) · [How AFK works](https://github.com/MairwunNx/moafks/wiki/How-AFK-works) · [Commands and permissions](https://github.com/MairwunNx/moafks/wiki/Commands-and-permissions) · [Configuration](https://github.com/MairwunNx/moafks/wiki/Configuration) · [Languages](https://github.com/MairwunNx/moafks/wiki/Languages) · [Integrations](https://github.com/MairwunNx/moafks/wiki/Integrations) · [FAQ](https://github.com/MairwunNx/moafks/wiki/FAQ)

**Разработчикам:** [Building from source](https://github.com/MairwunNx/moafks/wiki/Building-from-source) · [Development](https://github.com/MairwunNx/moafks/wiki/Development) · [Releasing](https://github.com/MairwunNx/moafks/wiki/Releasing)

## Связанные проекты

- **[Moonlands](https://minecraft.mairwunnx.com)** — сервер со сложным выживанием, где работают все эти проекты.
- **[WorldAPI](https://github.com/MairwunNx/minecraft-worldapi)** — REST и WebSocket API к миру, игрокам и статистике сервера на Paper, Fabric или NeoForge.
- **[Minecraft MCP](https://github.com/MairwunNx/minecraft-mcp)** — даёт ИИ-клиенту читать сервер и управлять им через WorldAPI.
- **[Allay](https://github.com/allay-ai/minecraft)** — ИИ-помощник в игре: спросите через `/ask` прямо в чате.
- **[Allay Server](https://github.com/allay-ai/server)** — бэкенд Allay.
- **[Mo’Joins](https://github.com/MairwunNx/mojoins)** — сообщения о входе и выходе на языке каждого игрока.
- **[Mo’Bosses](https://github.com/MairwunNx/mobosses)** — монстры, которые появляются боссами с классами, способностями и добычей.
- **[minecraft-exporter](https://github.com/MairwunNx/minecraft-exporter)** — метрики Prometheus из WorldAPI.
- **[minecraft-grafana](https://github.com/MairwunNx/minecraft-grafana)** — дашборд Grafana для WorldAPI.

[IP Geolocation by DB-IP](https://db-ip.com), лицензия CC BY 4.0.

> 🫡 Сделал Павел Ерохин (Pavel Erokhin), он же mairwunnx.
