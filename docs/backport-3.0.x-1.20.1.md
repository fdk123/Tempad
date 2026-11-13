# Tempad 3.0.x backport for Forge 1.20.1

This branch starts from upstream `1.20.1` (`fd59f8a`) and tracks the 3.0.x
rewrite from upstream `1.21.x` (`e3816ad`). The local fork is prepared with:

- `upstream`: `https://github.com/terrarium-earth/Tempad.git`
- `origin`: `https://github.com/fdk123/Tempad.git` (to be created/published)
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
access to saving locations. The Knowledge Repository upgrade opens a compact
text guide from the Tempad screen. Its guide text and item models use only new
text and vanilla Minecraft textures; the 1.21 guide artwork is not included.

The Knowledge Repository crafting recipe uses a Chronometer in place of the
1.21 Knowledge Projector, which has not been ported. The upstream
`player_teleport_upgrade` is deliberately absent because the 1.21 branch does
not register a matching install recipe or application for it.

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
