# Mo’Afks

🛡️ **Mo’Afks** lets players step away without worry. Type `/afk`, or stand idle for a few minutes, and your character goes into a safe AFK mode: nothing hurts or pushes you, mobs leave you alone, and you don’t keep the night from passing. Move, and you’re back.

## What you get

- 🛡️ **Nothing hurts an AFK player:** no damage of any kind, no burning, no drowning, no hunger. A fall into the void brings them back to where they went AFK.
- 🧲 **Nothing moves them:** no knockback from hits or explosions, mobs and players walk through them, projectiles and fishing hooks fly through, and boats and minecarts can’t scoop them up.
- 👻 **Mobs leave them alone:** no mob targets them, wardens don’t hear them, endermen don’t mind their stare, phantoms don’t come for them, and Bad Omen doesn’t start a raid.
- 🌙 **AFK players don’t block the night.** On a server where everyone has to sleep, one friend away from the keyboard no longer keeps the night going.
- ⚖️ **The protection doesn’t pay off.** Nobody goes AFK mid-fight, mid-fall, on fire, in water or lava, or with a mob on their heels. AFK players pick up no items or experience, and their health and hunger stay where they were.
- 🔙 **Coming back is up to the player:** walking, jumping, turning the camera, chatting, clicking, and more, each a switch. Water, pistons, and minecarts moving them don’t count, so AFK pools don’t fool it. If they come back somewhere dangerous, they land on a safe spot nearby.
- 🌍 **Every player reads their own language.** The plugin finds the player’s country by IP and picks a language for it: English, Russian, Spanish, Portuguese, and German come bundled. Write messages in [MiniMessage](https://docs.advntr.dev/minimessage/format): colors, gradients, clickable links.
- ⏱️ **A time limit**, if you want one: a warning in the action bar, then a kick.
- ✨ **A ring of particles** around AFK players and an **[AFK] mark** in the player list. Players hidden by a vanish plugin stay hidden: nobody learns they went AFK.
- ⚙️ **`/moafks reload`** applies changes without a restart, and AFK players stay AFK.

AFK changes nothing the game saves. A player who quits while AFK comes back not AFK, with nothing left over.

## Install

Mo’Afks needs Paper 26.3 or a fork of it, like Purpur, on Java 25. PlaceholderAPI, MiniPlaceholders, TAB, and Plan are optional.

**With Docker (recommended).** In [itzg/minecraft-server](https://docker-minecraft-server.readthedocs.io), name it and the server fetches it:

```yaml
environment:
  TYPE: PAPER
  VERSION: "26.3"
  MODRINTH_PROJECTS: |
    moafks
```

**By hand.** Put the jar in `plugins/` and restart the server.

On its first start Mo’Afks writes `plugins/MoAfks/config.yml` and the messages in `plugins/MoAfks/lang/`. Type `/afk making dinner`, and everyone else reads `» Steve stepped away: making dinner`. A 1.x `config.yml` keeps working; move its messages to `lang/`.

## How a player gets their language

1. **The player’s choice.** A player who picked a language with `/moafks language <code>` always gets it.
2. **By IP.** The plugin looks up the player’s country and checks `locales.countries`: Russia gets Russian, Brazil gets Portuguese, Germany gets German, and so on. You can change the lists.
3. **By game language.** When the country isn’t listed, the plugin takes the language set in the player’s game, if a file in `lang/` matches it.
4. **Default.** Everyone else gets `locales.default`, English out of the box.

The country lookup needs no account and sends no address anywhere. On the first start, the plugin downloads a free country database (4 MB) and refreshes it every month. Players who connect through a proxy show up with the proxy’s address, so they get the proxy’s country.

**Add a language:** copy `lang/en.yml` to, say, `lang/it.yml`, translate it, add `it: [ IT, SM, VA ]` to `locales.countries`, and run `/moafks reload`.

## Commands and permissions

| Command | What it does |
|---|---|
| `/afk [reason]` | Goes AFK, or comes back |
| `/moafks language [code\|auto]` | Sets the language Mo’Afks speaks to you; `auto` lets the plugin pick it again. Without an argument, lists the languages |
| `/moafks reload` | Reloads the config and the messages |
| `/moafks list` | Shows who is AFK and for how long |
| `/moafks locale [player]` | Shows a player’s language, country, and game language |

`/afk` needs `moafks.afk`, which everyone has. Every player can use `/moafks language`. The rest need `moafks.admin`, which operators have.

## PlaceholderAPI

With [PlaceholderAPI](https://modrinth.com/plugin/placeholderapi) installed, Mo’Afks adds `%moafks_afk%`, `%moafks_afk_time%` (seconds in this AFK spell), `%moafks_afk_total%` (seconds AFK across all sessions, to leave out of play time), `%moafks_reason%`, and `%moafks_tag%` (the [AFK] mark while AFK). If TAB formats your player list, turn off Mo’Afks’ own mark and put `%moafks_tag%` into TAB’s prefix.

In messages, `<papi:name>` shows `%name%` for the player it’s about. Mo’Afks doesn’t look for `%…%` in your text, so a percent sign stays a percent sign.

## MiniPlaceholders

With [MiniPlaceholders](https://modrinth.com/plugin/miniplaceholders) installed, Mo’Afks adds `<moafks_afk>`, `<moafks_afk_time>`, `<moafks_afk_total>`, `<moafks_reason>`, and `<moafks_tag>`, and its messages fill in any MiniPlaceholders tag for the player they’re about.

## Plan

With [Plan](https://modrinth.com/plugin/plan) installed, its Plugins Overview shows who is AFK now, for how long, and why. Each player’s Plugins tab and the players table show their time AFK across all sessions, so you can tell AFK time from time really played.

## Links

- Source code, issues, the full config, and the list of every game event Mo’Afks handles: [GitHub](https://github.com/MairwunNx/moafks)
- From the same author: [Mo’Joins](https://github.com/MairwunNx/mojoins), join and quit lines in each player’s language, and [Mo’Bosses](https://github.com/mairwunnx/mobosses), an RPG plugin that turns ordinary mobs into epic bosses

[IP Geolocation by DB-IP](https://db-ip.com), licensed under CC BY 4.0.
