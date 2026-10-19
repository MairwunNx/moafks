# Mo’Afks

<p align="center">
  <a href="https://github.com/MairwunNx/moafks/releases/latest"><img alt="Release" src="https://img.shields.io/github/v/release/MairwunNx/moafks?label=release"></a>
  <a href="https://modrinth.com/plugin/moafks"><img alt="Modrinth" src="https://img.shields.io/modrinth/dt/moafks?logo=modrinth&label=Modrinth&color=00AF5C"></a>
  <a href="https://hangar.papermc.io/MairwunNx/MoAfks"><img alt="Hangar" src="https://img.shields.io/hangar/dt/MoAfks?label=Hangar&color=1E88E5"></a>
  <img alt="Paper 26.3" src="https://img.shields.io/badge/Paper-26.3-F7CF0D">
  <img alt="Java 25" src="https://img.shields.io/badge/Java-25-E76F00">
  <a href="https://github.com/MairwunNx/moafks/actions/workflows/build.yml"><img alt="Build" src="https://img.shields.io/github/actions/workflow/status/MairwunNx/moafks/build.yml?branch=master&label=build"></a>
  <a href="LICENSE"><img alt="License" src="https://img.shields.io/github/license/MairwunNx/moafks"></a>
</p>

🛡️ **Mo’Afks** lets players on Paper servers step away without worry. Type `/afk`, or stand idle for a few minutes, and your character goes into a safe AFK mode: nothing hurts or pushes you, mobs leave you alone, and you don’t keep the night from passing. Move, and you’re back.

[Русская версия](https://github.com/MairwunNx/moafks/blob/master/README.RU.md)

[![Play on Moonlands: pure vanilla, hard mobs, AI, claims. minecraft.mairwunnx.com](https://raw.githubusercontent.com/MairwunNx/moafks/master/media/og-adv-image.png)](https://minecraft.mairwunnx.com)

Mo’Afks runs on Moonlands, my hard survival server with friends. Join at `minecraft.mairwunnx.com`, type `/afk`, and see what it keeps away.

## What you get

- 🛡️ **Nothing hurts or moves an AFK player:** no damage, fire, drowning, or hunger, no knockback, and projectiles fly through. A fall into the void brings them back.
- 👻 **Mobs leave them alone:** nothing targets them, wardens don’t hear them, phantoms don’t come, and Bad Omen doesn’t start a raid.
- 🌙 **AFK players don’t block the night.** One friend away from the keyboard no longer keeps everyone in the dark.
- ⚖️ **The protection doesn’t pay off.** Nobody goes AFK mid-fight, mid-fall, or with a mob on their heels, and AFK players pick up no items or experience.
- 🔙 **Coming back is up to the player.** A key press, a click, or a chat line brings them back; water and pistons don’t. A dangerous spot moves them somewhere safe.
- 🌍 **Every player reads their own language,** picked by IP or by their own choice. English, Russian, Spanish, Portuguese, and German come bundled.
- ✨ **Everyone sees who’s away:** a ring of particles and an [AFK] mark. Add a time limit with a kick if you want one; vanished players stay hidden.
- 🧩 **It fits your server:** PlaceholderAPI, TAB, MiniPlaceholders, and Plan if you have them, and `/moafks reload` without a restart.

## Get started

1. Download the jar from [Modrinth](https://modrinth.com/plugin/moafks), [Hangar](https://hangar.papermc.io/MairwunNx/MoAfks), or [GitHub](https://github.com/MairwunNx/moafks/releases/latest/download/moafks.jar) and put it in `plugins/` of a Paper 26.3 server on Java 25. With [itzg/minecraft-server](https://docker-minecraft-server.readthedocs.io), add `moafks` to `MODRINTH_PROJECTS` instead.
2. Restart the server, join, and type `/afk making dinner`.
3. Read [First steps](https://github.com/MairwunNx/moafks/wiki/First-steps) to set the idle timer and the kick.

## Documentation

The [wiki](https://github.com/MairwunNx/moafks/wiki) has the manual.

**For players and server owners:** [Installation](https://github.com/MairwunNx/moafks/wiki/Installation) · [First steps](https://github.com/MairwunNx/moafks/wiki/First-steps) · [How AFK works](https://github.com/MairwunNx/moafks/wiki/How-AFK-works) · [Commands and permissions](https://github.com/MairwunNx/moafks/wiki/Commands-and-permissions) · [Configuration](https://github.com/MairwunNx/moafks/wiki/Configuration) · [Languages](https://github.com/MairwunNx/moafks/wiki/Languages) · [Integrations](https://github.com/MairwunNx/moafks/wiki/Integrations) · [FAQ](https://github.com/MairwunNx/moafks/wiki/FAQ)

**For developers:** [Building from source](https://github.com/MairwunNx/moafks/wiki/Building-from-source) · [Development](https://github.com/MairwunNx/moafks/wiki/Development) · [Releasing](https://github.com/MairwunNx/moafks/wiki/Releasing)

## Related projects

- **[Moonlands](https://minecraft.mairwunnx.com)**—the hard-survival server where all of these run.
- **[WorldAPI](https://github.com/MairwunNx/minecraft-worldapi)**—REST and WebSocket API to a Paper server's world, players, and stats.
- **[Minecraft MCP](https://github.com/MairwunNx/minecraft-mcp)**—lets an AI client read and run a server through WorldAPI.
- **[Allay](https://github.com/allay-ai/minecraft)**—an AI helper in the game: ask with `/ask`, right in the chat.
- **[Allay Server](https://github.com/allay-ai/server)**—the backend behind Allay.
- **[Mo’Joins](https://github.com/MairwunNx/mojoins)**—join and quit messages in each player's language.
- **[Mo’Bosses](https://github.com/MairwunNx/mobosses)**—monsters that spawn as bosses with classes, abilities, and loot.
- **[minecraft-exporter](https://github.com/MairwunNx/minecraft-exporter)**—Prometheus metrics from WorldAPI.
- **[minecraft-grafana](https://github.com/MairwunNx/minecraft-grafana)**—the Grafana dashboard for WorldAPI.

[IP Geolocation by DB-IP](https://db-ip.com), licensed under CC BY 4.0.

> 🫡 Made by Pavel Erokhin (Павел Ерохин), aka mairwunnx.
