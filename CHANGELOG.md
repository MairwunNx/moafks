# Changelog

The “Release” workflow takes the notes for a version from its section here, unless you type them into its `changelog` field. The same notes go to GitHub and Modrinth.

## 2.0.0

A rewrite for Paper 26.3 and Java 25.

- 🌍 **Each player reads their own language.** The plugin finds the player’s country by IP, with DB-IP’s free database, and picks a language for it: English, Russian, Spanish, Portuguese, or German. When the country isn’t listed, it takes the language set in the game. Broadcasts reach everyone in their own language.
- 📨 **Messages live in `lang/<language>.yml`**, apart from the settings. New: why `/afk` was refused (a fight, a fall, fire, water, a mob), how long a player was away, the void rescue, and `/moafks list`.
- 🌙 **AFK players no longer block the night** on servers where everyone has to sleep. `sleep_ignored`, on by default.
- 🛡️ **Protection from everything.** Every kind of damage is cancelled now, not only hits, projectiles, explosions, fire, and falls: drowning (air stays), suffocation, freezing, lightning, magic, poison, wither, thorns, the warden’s sonic boom, cramming, and falling blocks too. New: harmful potion effects don’t stick, wardens and sculk don’t hear AFK players, endermen and zombified piglins don’t get angry at them, phantoms don’t spawn for them, fishing rods can’t reel them in, ender pearls fly through them, boats and minecarts can’t pick them up, and Bad Omen doesn’t start a raid. Each group has a switch in `protection`.
- 🕳️ **The void no longer traps a player.** 1.x cancelled void damage and left them falling forever; now they’re taken back to where they went AFK.
- ⚖️ **The protection doesn’t pay off.** No AFK while on fire, freezing, in water or lava, or with a mob on your heels, on top of fights and falls; idle players wait until it’s over. AFK players don’t pick up items or experience (`gains`), and with hunger frozen, health doesn’t regenerate for free either.
- 🔙 **Coming back means doing something.** Activity comes from the keys a player presses, so being pushed by water, a piston, or a minecart no longer ends AFK, and AFK pools no longer keep anyone from going AFK. New triggers in `exit_triggers`, each a switch: turning the camera past `look_degrees`, jumping, sneaking, sprinting, attacks, clicks, blocks, inventories, dropping, swapping hands, and the held slot.
- 🧩 **PlaceholderAPI.** `%moafks_afk%`, `%moafks_afk_time%`, `%moafks_afk_total%` (to leave AFK time out of play time), `%moafks_reason%`, and `%moafks_tag%` for TAB and scoreboards; `<papi:name>` in messages. PlaceholderAPI is optional.
- 🏷️ **The [AFK] mark goes on the player list only**, set in `effects.nickname.mark`. The display name no longer changes, so other plugins’ messages don’t carry the mark. If TAB formats the list, turn the mark off and use `%moafks_tag%`.
- ⚙️ **`/moafks reload` reloads the files** instead of restarting the plugin, and AFK players stay AFK under the new settings. `/moafks list` shows who is AFK; `/moafks locale [player]` shows a player’s language and why. The admin commands need `moafks.admin`; `moafks.reload` still works.
- **A 1.x config still loads.** Every key has a default, an unknown sound or particle logs a warning, and the `prevents` and `exit_triggers` lists turn into the new switches. Its messages and sound keys are ignored: move your texts to `lang/` and your sounds to `sounds`. The idle time keeps your value; the new default is 300 seconds instead of 60.

Fixes:

- `/moafks reload` dropped every AFK player without lifting their protection, so they stayed unpushable and ignored for sleep until they rejoined. With `enabled: false`, it switched every listener off until a restart. A player who stayed idle through a reload never went AFK, because the idle timers were lost.
- Exiting AFK somewhere the old safe-spot check didn’t like, such as a carpet, a snow layer, or water, could teleport the player to the world spawn. Now only lava, fire, powder snow, magma, and walls count as unsafe, and when nothing safe is near, the player stays put.
- A fall that happened during AFK hurt the player right after they came back. Fall distance and freezing now reset on return.
- `/moafks:afk` while AFK ended AFK and then started it again.
- Kicked players kept their warnings and timers in memory, and players who left stayed in the activity and combat maps.
- Messages were built once, in one language, for everyone; now each reader gets their own. An AFK reason can’t inject MiniMessage tags.
- The `moafks_noclip` scoreboard team, which could outlive a crash in the world save, is gone and removed on start.

## 1.1.1

- More warnings in the last seconds before the limit.
- Return messages no longer repeat the AFK reason.
- Placeholders render through MiniMessage.

## 1.1.0

- Separate messages for `/afk` with and without a reason.
- Auto-AFK picks up idle players more reliably.

## 1.0.1

- Loads after the worlds.

## 1.0.0

- First release: `/afk [reason]`, auto-AFK on idling, protection from damage, hunger, mobs, knockback, and collision, particles and a name mark, a time limit with warnings, and no AFK in a fight or a fall.
