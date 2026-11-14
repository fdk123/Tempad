# Tempad 3.0.x backport for Forge 1.20.1

This branch starts from upstream `1.20.1` (`fd59f8a`) and tracks the 3.0.x
rewrite from upstream `1.21.x` (`e3816ad`). The local fork is prepared with:

- `upstream`: `https://github.com/terrarium-earth/Tempad.git`
- `origin`: `https://github.com/fdk123/Tempad.git`
- branch: `backport/3.0.x-1.20.1`

## Scope

The 3.0.x line is a large rewrite, not a small compatibility patch. It moves
from the 1.20.1 Architectury Java multi-loader project to a 1.21.1 NeoForge
Kotlin project. The upstream comparison is approximately 711 changed files,
with about 19,939 added and 5,184 deleted lines.

The backport target for the modpack is Forge 1.20.1. The first implementation
should therefore keep the existing Forge/Fabric project layout and port the
feature behavior in layers. Copying the 1.21 tree wholesale would silently
change the loader, mappings, Java version, dependency APIs, resource paths and
license metadata.

## Port order

1. Keep the 1.20.1 build green and add a 3.0.x version suffix.
2. Port the common data model and server-side location/teleport APIs.
3. Port the new TVA device model: chronon handlers, upgrades, workstation,
   chronometer, batteries and card wallet.
4. Port networking and persistence, then timedoor placement/teleport behavior.
5. Port Forge 1.20.1 screens, renderers, keybinds and Curios integration.
6. Port generated resources, recipes, tags, guide data and optional mod
   integrations one group at a time.
7. Build and run a dedicated Forge 1.20.1 client/server before publishing.

## Compatibility decisions

### Shared Metronome chronons (Forge 1.20.1)

Metronomes owned by one player now share a persistent, cross-dimensional
chronon pool. Each booted block adds capacity; generation scales with the
number of active blocks by default. A new block needs 500 chronons by default
and 100 ticks to boot, as in 3.0. Existing 1.20.1 blocks without boot data start
booted and transfer their stored charge into the network on first tick.
Breaking a block removes its capacity and returns a proportional share of the
pool in the dropped Metronome item. The other blocks retain the remaining
charge. The item's charge can be transferred into other devices while carried.

The Metronome now stores four input and four output chronon containers in a
vanilla nine-slot screen with the middle slot disabled. These contents persist
and drop when the block is removed. The network feeds adjacent Workstations
and Projectors and preserves the existing nearby-owner charging behavior.
The Creative Chronometer is an inexhaustible source for carried devices and
placed chronon blocks. The 1.20.1 redstone pause is checked for loaded blocks;
unloaded network positions keep generating, matching the 3.0 global pool.
All new item models use vanilla textures.

### Upgrade download block (Forge 1.20.1)

The Workstation now accepts data driven `tempad:upgrade` recipes. A right click
with a matching upgrade item consumes one item and starts an 80 tick download.
Progress, the pending upgrade, the installed Tempad and the station owner are
saved in block NBT and on the dropped Workstation item. The Tempad cannot be
removed through the menu during a download. Completed upgrades are stored on
the Tempad item itself and appear in its tooltip.

The New Location upgrade can gate location saving when
`requireNewLocationUpgrade` is enabled. It defaults to disabled to preserve
existing 1.20.1 worlds and match the upstream unflagged behavior. Locations in
this branch are still player scoped, so any carried upgraded Tempad grants
access to saving locations. The Knowledge Repository upgrade opens the guide
from the Tempad screen. The Player Teleport Upgrade uses the upstream survival
crafting pattern and workstation download recipe, adapted to 1.20.1.

### Knowledge Projector and guide (Forge 1.20.1)

The Knowledge Projector is a standalone guide item. Its glass, book and copper
ingot recipe follows the 3.0 layout. The Knowledge Repository Upgrade recipe
now uses the Projector in its center, restoring the upstream ingredient layout.
Existing upgrades retain their item ID and installed state.

The guide can be opened directly from the Projector or through an installed
Knowledge Repository upgrade in a Tempad. It has ten overview pages plus Iron
and Steel device chapters with sixteen item pages. Chapters, topics and page
text are scrollable. All guide text is newly written for the working Forge
1.20.1 features, with English and Russian translations. The screen and item
model use vanilla Minecraft rendering and textures; the 1.21 artwork is not
included.

### Timedoor Projector block (Forge 1.20.1)

The 3.0 Timedoor Projector is now available as both an item and a placeable
block. A filled Location Card installs its target; an empty hand removes the
card from the placed block, while sneaking with an empty hand picks up the
projector. Portable card attachment and removal use secondary inventory clicks.
The portable item opens a door on use and places the block on sneak-use. The
placed block opens a door on a rising redstone signal and has a comparator
output based on charge. A battery or adjacent same-owner Metronome can charge
it. Charge, target and owner persist across block drops and placement.

The capacity (4000), portal cost (1000) and duration (200 ticks) match the
3.0 defaults and are configurable. The 1.20.1 timedoor entity uses the
projector-specific duration for both ends of the portal. Opening from a placed
projector requires its owner to be online because the existing 1.20.1 portal
API needs a player. The models are new, simple geometry with vanilla textures;
no 3.0 artwork has been copied.

### Timedoor Marker and Chronomark (Forge 1.20.1)

Both 3.0 marker types are placeable, configurable destinations in the Tempad.
Their shared cross-dimensional index is saved in the overworld and keeps
locations visible even while their chunks are unloaded. Each marker keeps its
identity, owner, name, color and access settings when broken and placed again.
The regular marker uses a position adjacent to the block; Chronomark adds a
configurable height offset and arrival angle. Timedoor travel now preserves
these precise coordinates and the portal color in the same dimension and
across dimensions. Old Location Cards and saved locations retain their old
arrival behavior.

Use a Location Card on a marker to bind it to the marker ID. Cards exported
from the Tempad retain the link, and Projectors carrying those cards resolve
the current marker position every time they open a portal. Removing the marker
invalidates its link. The marker settings screen and network handler validate
distance and edit access on the server. The regular marker has owner-only and
public access; Chronomark also supports the owner's FTB Team when FTB Teams is
installed. Existing public/private settings migrate to the new access mode.
An open Timedoor follows its marker and closes if access is revoked or the
marker is removed. Linked cards are not imported as fixed coordinates into
the Tempad: accessible markers already appear through the marker provider.
The index remembers removed marker IDs to suppress stale snapshots imported
by earlier builds when the marker ID is known to the index. The block and item
models use vanilla textures and new geometry.

### Time Twister and travel history (Forge 1.20.1)

Travel history is recorded every 1200 ticks of player movement and at death,
respawn and dimension transitions. It is stored per player in overworld SavedData
with the 3.0 default limit of 512 entries, so death and rejoining do not erase
it. The Time Twister opens the eight latest marked events and can return to one
for the configurable default cost of 500 chronons. The server validates the
history entry, charge source, cooldown, dimension and world border before
backtracking, then discards history from the selected point onward.

The Twister can be installed into a Tempad through secondary inventory clicks
and later removed without losing its charge or item metadata. It has a separate
chronon reserve; batteries and Metronomes fill the Tempad's main store first,
then the Twister reserve. An installed Twister adds a Timeline button to the
Tempad screen. The timeline uses a compact text interface in this backport;
the 3.0 radial artwork is not included.

### Tempad settings and quick actions (Forge 1.20.1)

Each Tempad now keeps its own start app, quick action and location ordering in
item NBT. The settings screen offers Locations, the installed Time Twister's
Timeline, or the installed Knowledge Repository as the start app. The existing
quick action key can open the favorite Timedoor or be disabled. Locations can be
sorted by name, dimension or type. Unavailable apps fall back to Locations, and
the server restricts received settings to known values. Each settings packet
identifies the exact Tempad that opened the screen, so carrying more than one
does not edit or drain the wrong device. The 3.0 app and macro
registries are represented by these currently available Forge 1.20.1 actions;
future apps can extend the setting list as they are ported.

### Workstation portal setup (Forge 1.20.1)

The P button on the Tempad screen configures the portal that its Workstation
will open. The device stores a selected location or follows its owner's current
favorite, three offsets relative to the Workstation facing, an angle, and a
vertical or floor door shape. Offsets are clamped to -5..5 blocks on the
server; the selected location is checked against the player's available
destinations. These settings stay with the Tempad when it is installed in or
removed from the Workstation, and the target is resolved again on activation.
Devices without saved portal settings keep the existing 1.20.1 placement.
Floor doors have a flat renderer and a matching server interaction area; their
exit portal appears overhead as in 3.0. The settings are edited while the
Tempad is carried, before installation; the 1.20.1 Workstation still uses its
existing inventory screen.
Portable Tempads and Projectors also open a floor door when used while looking
down more than 45 degrees, matching the 3.0 item behavior.

### Live player destinations and access devices (Forge 1.20.1)

The Location Broadcaster and Screening Device expose the carrier as an online,
moving destination. The Broadcaster starts disabled and toggles public access
on use. The Screening Device cycles Off, Public, and FTB Teams when that mod is
loaded. Its FTB Teams check uses the installed 1.20.1 API through an optional
Forge bridge. Both devices work from inventory or a Curios charm slot. Their
models use vanilla textures with item state overrides.

Player destinations appear only to a viewer carrying a Tempad with the Player
Teleport Upgrade. They cannot be deleted or exported to cards. Favorite and
Workstation portal targets can refer to a player; Workstations accept the
upgrade on their stored Tempad. Timedoors recheck the target's online status,
visibility, dimension and position while open. If access is revoked or the
player disconnects, the door closes. Moving targets update the destination
and receiving portal. The upgrade item, crafting recipe, and installation recipe
are ported. Optional Odyssey Allies access modes are not present in this Forge port.

| 3.0.x dependency/API | Forge 1.20.1 decision |
| --- | --- |
| NeoForge 21.1 / Minecraft 1.21.1 | Forge 47 / Minecraft 1.20.1 |
| Kotlin/JVM 21 | Java 17-compatible implementation unless Kotlin is needed for a specific port |
| ResourcefulLib/Config 3.x | 1.20.1 versions already used by the base branch |
| Botarium 3.x | Botarium 2.2 API adapters in the base branch |
| Curios 9.x | Curios 5.4.5 Forge API |
| `data/*/recipe` and singular 1.21 paths | 1.20.1 plural recipe/tag paths |
| NeoForge events/registries/networking | Forge 47 equivalents, isolated in platform classes |

## License and publication

The upstream 1.21 branch uses the **Terrarium License v1**. It is a split
license, not a blanket All Rights Reserved license:

- source, script, build, configuration, documentation, shader and data files
  covered by the license definitions are MIT-licensed;
- files outside those definitions remain All Rights Reserved, which includes
  some artwork and other non-code assets.

Keep the upstream copyright/license notice in the fork. Port the MIT-licensed
code and data with attribution, but do not redistribute protected upstream
textures, models, audio or other non-code assets without permission. The
backport should use original or separately licensed 1.20.1 assets where needed.

The CurseForge project labels this as a Custom License; the exact definitions
are in the upstream `LICENSE` file and must be treated as authoritative.

The initial branch is intentionally separate from the My Infinite Creation
workspace until the fork has a public remote and a first compiling Forge
1.20.1 milestone.
