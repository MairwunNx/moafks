# Mo’Afks: notes for AI agents

A Paper 26.3 plugin that gives players a safe AFK mode, with `/afk` or after standing idle: nothing hurts or moves them, mobs leave them alone, and they don't keep the night from passing. Messages reach each player in their own language: the one they chose with `/moafks language`, else picked by IP. README.md is the shop window (what it gives, badges, how to start, related projects); README.RU.md is its Russian copy. The manual lives in the GitHub wiki (`https://github.com/MairwunNx/moafks.wiki.git`, English only): installation, how AFK works, commands, configuration, languages, integrations, FAQ, and the developer pages. Moonlands, the owner's server (MairwunNx/minecraft), is where it's meant to run.

## Talking to the owner

- Reply in Russian, address the owner as «вы». Quotes «ёлочки», nested „лапки“, an em dash with spaces « — ».
- Code, comments, commit messages, README.md, and the English language file are in English: curly quotes, an em dash without spaces or an en dash with spaces, never a hyphen as a dash.
- README.RU.md and `lang/ru.yml` follow Russian typography; `lang/de.yml` uses „…“, an en dash with spaces, and „du“. Change both READMEs and MODRINTH.md together. A change to commands, permissions, config keys, placeholders, or behavior goes into the wiki page that covers it; the READMEs and MODRINTH.md don't repeat the manual, and they carry no License, Build, Stack, or AI sections.

## How it fits together

| File | What it does |
|---|---|
| `MoAfksPlugin.kt` | Startup, shutdown, and `reload()`: reads `config.yml` and `lang/`, builds the resolver and the effects, schedules the ticker, particles, and GeoIP refreshes |
| `Settings.kt` | `config.yml` through kaml, not strict: every field has a default; `migrate()` turns 1.x's `prevents` and `exit_triggers` lists into switches |
| `Rules.kt` | The decisions, apart from Bukkit: which input is activity, which damage is cancelled, when anti-abuse refuses AFK, which limit warning is due |
| `Afk.kt` | Who is AFK (by UUID), entering and leaving, the idle timer, the limit, and putting back exactly what AFK changed |
| `ProtectionListener.kt` | Every event that could hurt, move, or reward an AFK player; see the table below |
| `ActivityListener.kt` | What the player does with their own hands, mapped to a `Trigger` |
| `SessionListener.kt` | Country lookup at login, the idle timer at join, quiet exits on quit, death, and world change |
| `AfkEffects.kt` | Sounds, the particle ring, the name mark; game ids resolve against registries on load |
| `SafeSpot.kt` | On return, whether the player stands somewhere that hurts, and the nearest spot that doesn't |
| `Languages.kt`, `GeoIp.kt`, `Messenger.kt` | `Message` keys and texts, `LocaleResolver`, the DB-IP country database, and rendering per reader. A player's choice (`Messenger.chosen`) beats every rule in `LocaleResolver.resolve`; every language lookup goes through `Messenger.languageOf` |
| `AfkCommand.kt`, `MoAfksCommand.kt` | `/afk [reason]`; `/moafks language [code\|auto]` for every player; `/moafks reload`, `/moafks list`, `/moafks locale [player]` for admins |
| `Placeholders.kt` | The `<papi:name>` tag and the `%moafks_…%` expansion; PlaceholderAPI is optional, so its classes load only when it's there |
| `AdventurePlaceholders.kt` | MiniPlaceholders, optional: other plugins' tags in messages and the `<moafks_…>` expansion. Its classes load only when it's there |
| `Collision.kt` | Collision off on the client: TAB's collision rule when TAB is on, or the `moafks_afk` team. TAB is optional, so its classes load only when it's there |
| `Vanish.kt` | Who is vanished: the `vanished` metadata vanish plugins set, read on the server thread and kept in a set for TAB's and Plan's threads |
| `Plan.kt` | The Plan extension: AFK now, why, time AFK, and times AFK per player; who is AFK on the server. Plan is optional, so its classes load only when it's there; `Afk` nudges it on every enter and exit, and it registers again after `/plan reload` |

## Rules that keep it working

- State lives in maps keyed by UUID. `states`, `totals`, and `spells` are concurrent because TAB asks PlaceholderAPI from its own thread and Plan calls its extension from its own; `totals` and `spells` outlive a quit, since Plan reads them after it; everything else, Bukkit calls included, stays on the server thread. The chat event is async and hops over with `runTask`.
- AFK never changes anything the game saves: no `setInvulnerable`, no statistics. Collision, sleeping ignored, and the list name live in memory and come back on exit, quit, death, world change, reload, and disable. Only the AFK total, the count of AFK spells, and the language a player chose go into the player's persistent data. The one exception is the `moafks_afk` team (`Collision.kt`): it exists only while someone is AFK, and the start removes one a crash left behind.
- `restore()` undoes only what `apply()` changed, and only if nothing else changed it since.
- Nobody else may learn a vanished player is AFK: no broadcast (the console still logs it), no list mark, the particle ring only to them, and `visibleState()`/`visible()` for anything others read (placeholders, Plan). Their own protection and private lines stay. Read metadata only through `Vanish.check` on the server thread; off-thread code asks `Vanish.hidden`.
- `/moafks reload` only re-reads files and reapplies effects to AFK players. Never call `onDisable()`/`onEnable()` from it: `registerCommand` fails after startup.
- Activity comes from input, not position: `PlayerInputEvent` for keys, `PlayerMoveEvent` for the camera only. Water, pistons, and vehicles move a player without them doing anything.
- Game ids stay strings in `Settings`; `AfkEffects` resolves them once per load and logs a warning for a bad one. A bad id never fails the whole config.
- A new message gets a `Message` entry and a line in every bundled language (`src/main/resources/lang/`); `ResourcesTest` fails otherwise. Moonlands keeps its own copies in `minecraft/config/plugins/MoAfks/lang/`, so add the line there too.
- A new config key gets a default in `Settings` and the same default in `src/main/resources/config.yml`; `ResourcesTest` checks they match. A 1.x config must still load (`src/test/resources/config-1.x.yml`).
- Render with `Placeholder.unparsed` for player-provided text, so a name or an AFK reason can't inject tags. PlaceholderAPI goes through `<papi:…>` only; never run `%…%` replacement over whole texts.
- A message about a player renders through `Messenger.render(code, message, subject, …)`: it adds `about(subject)` and passes the subject as MiniMessage's target, which MiniPlaceholders' per-player tags read. `<moafks_…>` and `%moafks_…%` give the same values.
- A new protection is a handler in `ProtectionListener` behind a switch, a row in the table below, and, when it's a decision, a case in `Rules` with a test.

## Events

Every Paper 26.3 event that could hurt or move an AFK player, let them gain something, or mean they're back. “AFK” below means the player the event is about is in AFK mode.

### Protection

| Event | What Mo’Afks does | Switch |
|---|---|---|
| `EntityDamageEvent` (also `…ByEntity`, `…ByBlock`) | Cancels every cause on an AFK player but `KILL` and `SUICIDE`: attacks, PvP, projectiles, explosions, fire, lava, hot floors, fall (also one that began before AFK), suffocation, drowning, freezing, lightning, magic, poison, wither, thorns, sonic boom, cramming, falling blocks, dragon breath, world border, plugin damage | `protection.damage` |
| `EntityDamageEvent`, cause `VOID` | Cancels and teleports the player to where they went AFK (or their spawn point), and they stay AFK. Cancelling alone would leave them falling forever | `protection.void` |
| `EntityCombustEvent` (also `…ByBlock`, `…ByEntity`) | Cancelled: an AFK player doesn't catch fire | `protection.damage` |
| `EntityAirChangeEvent` | A drop in air is cancelled, so coming back doesn't start with drowning | `protection.damage` |
| Freezing in powder snow (no event) | Its damage is cancelled; on return freeze ticks reset to 0 and an unsafe spot moves the player | `protection.damage` |
| Fall distance (no event) | Reset to 0 on return, so a fall during AFK doesn't land after it | `protection.damage` |
| `EntityKnockbackEvent` (also `…ByEntity`) | Cancelled for every cause: hits, sweeps, explosions, wind charges, shields | `protection.knockback` |
| `EntityPushedByEntityAttackEvent` | Cancelled | `protection.knockback` |
| Entity pushing (no cancellable event) | `Player#setCollidable(false)` while AFK: mobs and the server don't push them. That alone isn't enough: the AFK player's own client pushes them away from other players, and the server takes the move. Clients learn the rule only from the team, so the player's team gets collision off too: through TAB's API when TAB runs teams (it overrides every other team), otherwise in the `moafks_afk` team. A player already in another team keeps it and can still be pushed | `protection.collision` |
| `EntityCollideWithEntityEvent` | Not used: collidable covers it | — |
| `ProjectileHitEvent` | Cancelled when it hits an AFK player: arrows, tridents, snowballs, eggs, ender pearls (no teleport onto them), wind charges, fishing hooks fly through | `protection.projectiles` |
| `PlayerFishEvent`, state `CAUGHT_ENTITY` | Cancelled when the hooked entity is AFK: a rod that hooked them earlier can't reel them in | `protection.projectiles` |
| `EntityPotionEffectEvent` | Harmful effects being added or changed are cancelled, unless by command; beneficial ones stay. Effects already running keep ticking, their damage cancelled | `protection.effects` |
| `PotionSplashEvent`, `LingeringPotionSplashEvent`, `AreaEffectCloudApplyEvent` | Not handled: their effects pass through `EntityPotionEffectEvent`, instant damage through `EntityDamageEvent` | `protection.effects`, `protection.damage` |
| `EntityEffectTickEvent` | Not used: damage covers poison and wither | — |
| `FoodLevelChangeEvent` | Drops cancelled. Eating is an interaction, which ends AFK first | `protection.hunger` |
| `EntityExhaustionEvent` | Cancelled | `protection.hunger` |
| `EntityRegainHealthEvent`, reason `SATIATED` | Cancelled: with hunger frozen, healing from a full bar would be free. Potions, beacons, and Regeneration still heal | `protection.hunger` |
| `EntityTargetEvent` (with `EntityTargetLivingEntityEvent`) | Cancelled when the target is AFK. Covers every mob, the Warden, piglins and their gold rule, creepers, phantoms. On entering AFK, mobs within 32 blocks drop the player as a target | `protection.mobs` |
| `WardenAngerChangeEvent` | A rise in anger at an AFK player is cancelled | `protection.mobs` |
| `EndermanAttackPlayerEvent` | Cancelled: an AFK player's stare doesn't anger endermen | `protection.mobs` |
| `PigZombieAngerEvent` | Cancelled when the target is AFK | `protection.mobs` |
| `PhantomPreSpawnEvent` | Cancelled when the AFK player is the one phantoms would spawn for. Insomnia (`TIME_SINCE_REST`) keeps counting: it's a vanilla statistic | `protection.mobs` |
| `CreeperIgniteEvent`, `ExplosionPrimeEvent` | Not handled: creepers can't target AFK players, and blast damage and knockback are cancelled | — |
| `GenericGameEvent` | Cancelled when the AFK player is the source: sculk sensors, shriekers, and wardens don't hear their steps or sounds | `protection.vibrations` |
| `RaidTriggerEvent` | Cancelled for an AFK player with Bad Omen in a village | `protection.raids` |
| `VehicleEnterEvent` | Cancelled when the one entering is AFK: a boat or minecart can't scoop them up | `protection.vehicles` |
| `VehicleEntityCollisionEvent` | Cancelled for AFK players: no pickup, no push | `protection.vehicles` |
| `PlayerLeashEntityEvent` | Not applicable: players can't be leashed | — |
| `BlockPistonExtendEvent`, `BlockPistonRetractEvent` | Not cancelled: an AFK player mustn't jam machines. A piston may move them; nothing hurts them, and on return an unsafe spot moves them | `protection.damage` |
| Water currents, bubble columns (no event) | The client moves the player; nothing to cancel. Not activity | — |
| `PlayerVelocityEvent` | Not handled: knockback has its own events; a plugin that sets velocity means to | — |
| `PlayerTeleportEvent` | Inside the world: not activity; the void rescue now leads here | — |
| `PlayerChangedWorldEvent`, `EntityPortalEnterEvent` | Changing worlds ends AFK quietly | — |

### Gains

| Event | What Mo’Afks does | Switch |
|---|---|---|
| `PlayerAttemptPickupItemEvent` | Cancelled, and the item doesn't fly at the player | `gains.items` |
| `PlayerPickupArrowEvent` | Cancelled: arrows and tridents stay | `gains.items` |
| `PlayerPickupExperienceEvent` | Cancelled: orbs wait | `gains.experience` |
| `RaidTriggerEvent` | Cancelled (see above): no raid rewards for standing in a village | `protection.raids` |
| `CreatureSpawnEvent`, `PlayerNaturallySpawnCreaturesEvent` | Not touched: AFK farms keep spawning. A farm that needs clicks (fishing, mob grinders) needs an active player, who isn't protected | — |
| `PlayerStatisticIncrementEvent` | Not touched: vanilla statistics, play time included, stay as they are. `%moafks_afk_total%` gives AFK time so others can subtract it | — |
| Sleeping (no event) | `Player#setSleepingIgnored(true)` while AFK, so AFK players don't block the night | `sleep_ignored` |

### Coming back

| Event | Trigger | Switch |
|---|---|---|
| `PlayerInputEvent` | A pressed movement key, jump, sneak, or sprint. Releasing a key doesn't count | `exit_triggers.move`, `.jump`, `.sneak`, `.sprint` |
| `PlayerMoveEvent` | Only the camera, once it turned past `look_degrees` since the last activity. Position is ignored | `exit_triggers.look` |
| `PlayerToggleSneakEvent`, `PlayerToggleSprintEvent`, `PlayerJumpEvent` | Not used: `PlayerInputEvent` covers the keys | — |
| `AsyncChatEvent` | Chat; hops to the server thread | `exit_triggers.chat` |
| `PlayerCommandPreprocessEvent` | Any command but `/afk`, `/moafks`, and `/moafks:…` | `exit_triggers.command` |
| `PrePlayerAttackEntityEvent` | An attack | `exit_triggers.attack` |
| `PlayerInteractEvent`, `PlayerInteractEntityEvent` | A click, except `PHYSICAL`: pressure plates fire when water carries a player onto them. Casting a rod, eating, and using items start here | `exit_triggers.interact` |
| `BlockBreakEvent`, `BlockPlaceEvent` | Breaking or placing | `exit_triggers.blocks` |
| `InventoryOpenEvent`, `InventoryClickEvent` | A container or an inventory click. The client opens the player's own inventory without telling the server | `exit_triggers.inventory` |
| `PlayerDropItemEvent` | Dropping | `exit_triggers.drop` |
| `PlayerSwapHandItemsEvent` | Swapping hands | `exit_triggers.swap_hands` |
| `PlayerItemHeldEvent` | Changing the held slot | `exit_triggers.held_slot` |

Events other plugins cancelled still count: the player still acted.

### Entering, and the session

| Event or check | What Mo’Afks does | Switch |
|---|---|---|
| `EntityDamageEvent` (not cancelled), `EntityDamageByEntityEvent` | Damage taken, or a hit dealt by the player or their projectile, marks a fight | `anti_abuse.combat_seconds` |
| `/afk` and the idle timer | Refused while in a fight, falling or gliding, on fire or freezing, in water or lava, or targeted by a mob within 32 blocks. `/afk` says why; an idle player waits until it's over | `anti_abuse.*` |
| `AsyncPlayerPreLoginEvent` | Country lookup for the language, off the server thread | `locales.sources`, `geoip.enabled` |
| `PlayerJoinEvent` | Starts the idle timer, reads the AFK total | — |
| `PlayerQuitEvent` (kicks end here too) | Quiet exit: everything restored, nothing about them kept but the total in their data | — |
| `PlayerDeathEvent`, `PlayerRespawnEvent` | Quiet exit | — |
| Plugin disable | Everyone leaves AFK quietly | — |
| `/moafks reload` | AFK players stay AFK; what the old settings applied is lifted and the new settings apply | — |

## Building and testing

- `./gradlew build` runs the tests and builds `build/libs/moafks-<version>.jar`, shaded and relocated under `ru.mairwunnx.moafks.libs`. JDK 25; Gradle provisions it through the foojay resolver.
- There is no MockBukkit for Paper 26.3; tests cover `Rules`, `Settings`, `LocaleResolver`, and the bundled files. The rest runs on a real Paper; see “Checking a change”.

## Checking a change

1. `./gradlew build`: the tests and the jar.
2. A real Paper with the jar, started and stopped by the script, which fails on any error about the plugin:
   ```sh
   python3 -I .claude/scripts/paper_smoke.py --game 26.3 --plugin build/libs/moafks-dev.jar --modrinth placeholderapi --modrinth plan --jar https://cdn.modrinth.com/data/HQyibRsN/versions/N2WfJ0ll/MiniPlaceholders-Paper-3.2.0.jar --command "papi list" --command "miniplaceholders expansions" --expect "registered internal expansion: moafks" --expect "Registered extension: Mo’Afks" --expect "^moafks$" --ignore "Plan\] Failed to enable geolocation"
   ```
   Pass `--game` the version the project targets, and `--build` to pin a Paper build. The script downloads Paper and Modrinth plugins into `~/.cache/paper-smoke` and keeps the server folder when something fails.
3. The console can't go AFK, so anything about a player needs a game client: after a release reaches Moonlands, the owner tries `/afk`, idling, and the night with someone AFK. Every event in “Events” is a case to try when its handling changed.

## Autopilot

`.github/workflows/autopilot.yml` runs Claude every Monday at 03:00 UTC: `/update-deps`, then `/update-minecraft`, a fix for whatever they break, a push to `master`, and the “Release” workflow with every store when what the project ships changed. The release's `deploy` job then starts Moonlands' `ship-plugin.yml` with the `FAMILY_ACTIONS_TOKEN` secret, and Claude puts the jar on the server there. The run itself needs the `CLAUDE_CODE_OAUTH_TOKEN` secret. “Autopilot” in `~/c/personal/minecraft/CLAUDE.md` explains the family's runs. A session with the owner still releases only when the owner asks.

## Versions

Where the versions live, for `/update-deps` and `/update-minecraft` (both in `.claude/skills/`, the same files in every project of the family):

- The game: `io.papermc.paper:paper-api` (twice: `compileOnly` and `testImplementation`), `gameVersions` (Modrinth) and `platformVersions` (Hangar) in `build.gradle.kts`, and `api-version` in `src/main/resources/paper-plugin.yml`. A new game version can add events that hurt, move, or reward a player: check them against “Events”.
- PlaceholderAPI: `compileOnly("me.clip:placeholderapi:…")`, from `repo.extendedclip.com`.
- MiniPlaceholders: `compileOnly("io.github.miniplaceholders:miniplaceholders-api:…")` from Maven Central. Its Modrinth release 3.2.0 lists games up to 26.2, so the check passes its jar with `--jar <Modrinth file URL>` instead of `--modrinth`; switch to `--modrinth miniplaceholders` once a release lists the game version.
- Plan: `compileOnly("com.djrapitops:plan-api:…")`, from `repo.playeranalytics.net`. Moonlands runs Plan 5.8, whose API lacks graphs and statistics (`DATA_EXTENSION_GRAPH_API`, `DATA_EXTENSION_STATISTICS_API`): use them only once Plan 5.9 is out and installed. `PlanAnalytics` checks the capabilities it uses.
- Java: `jvmToolchain` in `build.gradle.kts`, the foojay resolver in `settings.gradle.kts`, and CI's `setup-java`.
- Gradle in `gradle/wrapper/gradle-wrapper.properties`; Kotlin and the Gradle plugins in the `plugins` block; libraries in `dependencies`.

## Releasing and Moonlands

1. Add a `## X.Y.Z` section to CHANGELOG.md, written for server owners, and push it. After a change that reaches what the project ships, add it to CHANGELOG.md in the same push: under the topmost “## X.Y.Z” section while that version isn't released (`gh release view vX.Y.Z` fails), or under a new section with the next version: patch for fixes, minor for features, major for breaking changes. A release then needs only the workflow, and the workflow runs only when the owner asks.
2. Run the “Release” workflow with the new version: `gh workflow run release.yml -R MairwunNx/moafks -f version=X.Y.Z -f modrinth=true -f hangar=true`. Its `changelog` field overrides the CHANGELOG.md section; one of the two must be there. It builds with `-PreleaseVersion`, tags `vX.Y.Z`, and attaches `moafks-X.Y.Z.jar` to a GitHub release. Nothing in the repo holds the version; local and CI builds are `dev`.
3. To put it on Moonlands, point the Mo’Afks line in `minecraft/extras/jars.txt` (MairwunNx/minecraft) at the release jar, push, and deploy as its CLAUDE.md says, which also says when a restart may happen without asking.

## Modrinth

The project is `moafks` (id `6d2YvMjy`), MIT. The “Release” workflow has a `modrinth` switch, off by default: on, it runs `./gradlew modrinth`, which uploads the jar as a version and syncs MODRINTH.md as the description. MODRINTH.md is for server owners: the pitch, “What you get”, “Get started”, links into the wiki, and Related projects; no build or stack, and no Moonlands banner. The store page changes only with the next release that runs `modrinth`. A change to what the plugin does goes in README.md, README.RU.md, MODRINTH.md, and the wiki. It uses the `MODRINTH_TOKEN` secret; locally the same token is in `~/.secrets/cloud.env`. The icon is `media/icon.png`. Never put the token in the repo.

## Commits

Short one-line imperative English messages, one logical change per commit: “Let AFK players skip the night”.

## Hangar

The project is `MairwunNx/MoAfks` on hangar.papermc.io, MIT, created 2026-10-13. The “Release” workflow has a `hangar` switch, off by default: on, it runs `./gradlew publishPluginPublicationToHangar`, which uploads the jar to the Release channel for Paper 26.3. The project page came from MODRINTH.md when it was created; Hangar doesn't sync it, so edit the page on the site after a big change. It uses the `HANGAR_TOKEN` secret; locally the same key is in `~/.secrets/cloud.env`. The key (made 2026-10-13 with `create_version`) uploads versions; project settings, the page, and the icon are edited on the site. The first version of a new Hangar project can't go through the API (it answers 404 until the project has one), so it went through the website. Never put the key in the repo.
