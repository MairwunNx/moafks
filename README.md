# Mo’Afks

🛡️ **Mo’Afks** lets players on Paper servers step away without worry. Type `/afk`, or stand idle for a few minutes, and your character goes into a safe AFK mode: nothing hurts or pushes you, mobs leave you alone, and you don’t keep the night from passing. Move, and you’re back.

[Русская версия](https://github.com/MairwunNx/moafks/blob/master/README.RU.md)

## What you get

- 🛡️ **Nothing hurts an AFK player:** no damage of any kind, no burning, no drowning, no hunger. A fall into the void brings them back to where they went AFK.
- 🧲 **Nothing moves them:** no knockback from hits or explosions, mobs and players walk through them, projectiles and fishing hooks fly through, and boats and minecarts can’t scoop them up.
- 👻 **Mobs leave them alone:** no mob targets them, wardens don’t hear them, endermen don’t mind their stare, phantoms don’t come for them, and Bad Omen doesn’t start a raid.
- 🌙 **AFK players don’t block the night.** On a server where everyone has to sleep, one friend away from the keyboard no longer keeps the night going.
- ⚖️ **The protection doesn’t pay off.** Nobody goes AFK mid-fight, mid-fall, on fire, in water or lava, or with a mob on their heels. AFK players pick up no items or experience, and their health and hunger stay where they were.
- 🔙 **Coming back is up to the player:** walking, jumping, turning the camera, chatting, clicking, and more, each a switch. Water, pistons, and minecarts moving them don’t count. If they come back somewhere dangerous, they land on a safe spot nearby.
- 🌍 **Every player reads their own language.** The plugin finds the player’s country by IP and picks a language for it: English, Russian, Spanish, Portuguese, and German come bundled.
- ⏱️ **A time limit**, if you want one: a warning in the action bar, then a kick.
- ✨ **A ring of particles** around AFK players and an **[AFK] mark** in the player list.
- 🧩 **PlaceholderAPI**, if you have it: `%moafks_afk%` and friends for TAB and scoreboards, `<papi:…>` in messages.
- ⚙️ **`/moafks reload`** applies changes without a restart.

## Try it live

Moonlands is my hard survival server with friends, where my plugins run. Come in and play.

[![Play on Moonlands: pure vanilla, hard mobs, AI, claims. minecraft.mairwunnx.com](https://raw.githubusercontent.com/MairwunNx/moafks/master/media/og-adv-image.png)](https://minecraft.mairwunnx.com)

Join at `minecraft.mairwunnx.com`. The site has the live map, player stats, and the hall of fame.

## Requirements

- Paper 26.3 or a fork of it, like Purpur
- Java 25

## Install

1. Download the jar from [Releases](https://github.com/MairwunNx/moafks/releases).
2. Put it in your server’s `plugins/` folder and restart the server.
3. Edit `plugins/MoAfks/config.yml` and the files in `plugins/MoAfks/lang/`.
4. Run `/moafks reload`.

Coming from 1.x? Your `config.yml` keeps working: its `prevents` and `exit_triggers` lists turn into the new switches. Its messages don’t carry over; move your texts to `lang/`.

## How AFK works

A player goes AFK with `/afk [reason]`, or after `auto.seconds` (five minutes) without activity. What counts as activity is in `exit_triggers`: the movement keys, jumping, sneaking, sprinting, turning the camera by more than 30 degrees, chatting, commands, attacks, clicks, breaking and placing blocks, inventories, dropping items, swapping hands, and changing the held slot. Each one also ends AFK. The plugin watches the keys a player presses, not where they are, so an AFK pool or a piston doesn’t keep anyone awake.

While AFK, the switches in `protection` decide what the player is shielded from, and `gains` whether they may collect items and experience. `anti_abuse` decides when a player may not go AFK; an idle player in one of those spots waits until it’s over. When a player comes back, their fall distance and freezing reset, and if they stand in lava, fire, powder snow, or a wall, they’re moved to a safe spot nearby.

AFK changes nothing the game saves. A player who quits while AFK comes back not AFK, with nothing left over.

## How a player gets their language

The plugin tries the sources in `locales.sources`, in order:

1. **`ip`:** the country of the player’s IP address. `locales.countries` maps languages to country codes: Russia, Belarus, and Kazakhstan get Russian; Spain and Latin America get Spanish; Brazil and Portugal get Portuguese; Germany, Austria, and Switzerland get German.
2. **`client`:** the language set in the player’s game, when a file in `lang/` matches it.

When nothing matches, the player gets `locales.default`. Check what a player got with `/moafks locale <player>`.

The country lookup needs no account and sends no address anywhere. On the first start, the plugin downloads DB-IP’s free country database (4 MB, 8 MB unpacked) into `plugins/MoAfks/geoip/` and refreshes it every 30 days. Players who connect through a proxy or a relay show up with its address, so they get the relay’s country.

### Add a language

1. Copy `lang/en.yml` to, say, `lang/it.yml` and translate it.
2. Add a line to `locales.countries`: `it: [ IT, SM, VA ]`.
3. Run `/moafks reload`.

A key missing from a language file comes from the default language. An empty string turns a message off for that language.

## Commands and permissions

| Command | What it does |
|---|---|
| `/afk [reason]` | Goes AFK, or comes back |
| `/moafks reload` | Reloads `config.yml` and the language files; AFK players stay AFK |
| `/moafks list` | Shows who is AFK and for how long |
| `/moafks locale [player]` | Shows a player’s language, country, and game language |

`/afk` needs `moafks.afk`, which everyone has. The rest need `moafks.admin`, which operators have; 1.x’s `moafks.reload` still works.

## PlaceholderAPI

With [PlaceholderAPI](https://modrinth.com/plugin/placeholderapi) installed, Mo’Afks adds:

| Placeholder | Shows |
|---|---|
| `%moafks_afk%` | Whether the player is AFK, as PlaceholderAPI’s true or false |
| `%moafks_afk_time%` | Seconds in the current AFK spell, 0 when not AFK |
| `%moafks_afk_total%` | Seconds AFK across all sessions, so you can leave it out of play time |
| `%moafks_reason%` | The reason given to `/afk`, or nothing |
| `%moafks_tag%` | The `effects.nickname.mark` while AFK, otherwise nothing |

If TAB or another plugin formats the player list, it overwrites Mo’Afks’ mark. Turn `effects.nickname.enabled` off and put `%moafks_tag%` into that plugin’s prefix instead.

The other way round, write `<papi:name>` in any message to show `%name%` for the player it’s about: `<papi:luckperms_prefix><player>`. Mo’Afks doesn’t look for `%…%` in your text, so a percent sign stays a percent sign. Without PlaceholderAPI the tags show nothing.

## Build

You need JDK 25; Gradle downloads it when it’s missing.

```shell
git clone https://github.com/MairwunNx/moafks
cd moafks
./gradlew build
```

The jar lands in `build/libs/`. Releases come from the “Release” workflow on GitHub, which takes the version as input.

## Stack

- Kotlin and Gradle Kotlin DSL
- Paper API 26.3 and Adventure MiniMessage
- kotlinx-serialization with kaml for the config
- MaxMind DB reader for the country database

## AI involvement

AI wrote parts of the documentation and some of the code: the serializers and the safe-spot search at first, then the 2.0 rewrite with languages by IP and the review of every Paper event. AI also writes the commit messages from the changes.

## Related projects

[MCBuddy Server](https://github.com/mcbuddy-ai/mcbuddy-server)—🛠️⚡ the backend for MCBuddy, an AI assistant for Minecraft, built on OpenRouter.

[MCBuddy Spigot](https://github.com/mcbuddy-ai/mcbuddy-spigot)—💬 brings MCBuddy into the game: ask the AI anything with `/ask`, right in the chat. 🎮

## From the same author

[Mo’Joins](https://github.com/MairwunNx/mojoins)—🎉 greets players in their own language: join and quit lines, a welcome for newcomers, sounds, and particles.

[Mo’Bosses](https://github.com/mairwunnx/mobosses)—🏆 an RPG plugin that turns ordinary mobs into epic bosses, with a deep player progression system. Every fight matters, and every level opens something new. ⚔

## License

MIT, see [LICENSE](https://github.com/MairwunNx/moafks/blob/master/LICENSE).

## Credits

[IP Geolocation by DB-IP](https://db-ip.com), licensed under CC BY 4.0.

> 🫡 Made by Pavel Erokhin (Павел Ерохин), aka mairwunnx.
