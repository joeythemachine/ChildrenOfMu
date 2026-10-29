# Children of Mu: notes for Claude

Read this first in every session. It's the project's memory: the owner's standing rules, how work gets to the laptop,
the tools, and lessons already learned the hard way. Add to it when something new is learned.

## The project

- Minecraft **1.21.1, NeoForge** mod. Fantasy race folk and their towns: Ithk (eyeball folk, villages), RCB City
  (Rats, Cats, Bats), Frog towns, traveller huts. Later: Dog/Frog/Log, Lilbuns.
- Owner plays it in their own pack, **Colonies & Skies** (MineColonies + Create and friends).
- Source of truth: the owner's laptop, `C:\MCModding\ChildrenOfMu`. Dev runs: `run\` (saves, logs, mods).
- Mods in the dev run (`run\mods`): Domum Ornamentum, Quark (+Zeta), Supplementaries (+Moonlight),
  Farmer's Delight, Macaw's Roofs, JEI, Jade. Their block ids and blockstate properties are listed in
  `tools/reference/mod_blocks.json`, item ids in `tools/reference/mod_items.txt`. Check modded names there before
  using them (trades, templates). Unknown ids in trade pools are skipped silently, so a typo just vanishes.

## Owner's standing rules

- Always end a delivery with **"relaunch needed"** (and say whether a *new world/city* is needed to see it:
  worldgen changes only show in newly generated chunks; entity/AI changes work in the current world).
- **Verify files reached the laptop**: after committing, list the folder and compare sizes with the local copy.
- **No chests** in Ithk buildings.
- Towns/cities should be **rare**.
- Ithk stay in the overworld for now.
- Ask before big feature work when the owner asks for that; otherwise just do it.
- Owner tests in game and sends screenshots, ideally with **F3 coordinates**. Their play-feel and looks are the
  judge; scripts check the mechanical stuff (paths, doorways, gaps).
- Keep Claude usage reasonable: scripts should print only problems; don't page through huge outputs; images are
  expensive (screen control only for rare visual checks).

## How work gets to the laptop

The cloud session has a mirror of the project (it may be wiped between sessions: re-stage from the laptop if
missing, or clone from GitHub once that's set up).
1. Edit in the mirror. Compile check: `tools/compilecheck/check.sh` (javac against the NeoForge/Minecraft jars
   staged from the laptop's `build` folder and the CurseForge install; FML bits stubbed). Prints `OK` or errors.
2. Templates: `cd tools && python3 make_rcb_city.py` (and the other `make_*.py`) rebuild `.nbt` structures.
3. Stage changed files under `/mnt/user-data/outputs/cofm_fixN/...` and commit them to
   `C:\MCModding\ChildrenOfMu\...` with the device commit tool (force, at most 50 files per call).
4. List the laptop folders and check the sizes match. (PNGs come out re-compressed through the outputs folder, so
   compare a PNG's size with its *staged* copy, or its pixels, not the mirror's file.)

## Templates and the owner's edits

- RCB buildings come from `tools/make_rcb_city.py` + `tools/rcb_build.py`. `apply_overrides(B, name)` applies
  `tools/overrides/<name>.json` (`[[x,y,z], block, props]`, template coordinates) before saving. The owner's
  in-world decorating (lanterns, fences, trapdoors, vines, the Justice Hall interior) lives there.
- Folding new world edits into templates: snapshot the city's blocks before and after the owner edits
  (`tools/worldcheck`), diff them, map each change to its building's template with `place.py`
  (`to_local`, rotation handling), and append to the overrides. **Don't filter vines as noise**: the owner places
  them on purpose (that bug happened once).
- Each modded block has a vanilla alternate (`MOD_ALTS`), and each template has a `_vanilla` twin for packs
  without those mods.
- Modded blocks may not turn with a rotated structure (Quark posts kept their axis). `RotateFixProcessor` fixes
  axis and side connections for any block whose `rotate()` does nothing.

## World checking tools (`tools/worldcheck`)

Python, no dependencies. Stage the save's region files from the laptop first.
- `anvil.py`: region/NBT reader (`blocks(path, x0,x1,z0,z1,y0,y1)`, `chunks(path)`).
- `load.py <region dir> <out.pkl>`: finds the `rcb_city` structure start and snapshots every block in the city.
- `walk.py <pkl>`: walks the whole wall ring (catwalk, wall top, gallery) and flags jumps, gaps and dead ends.
- `place.py`: building pieces and template/world coordinate mapping. `vines.py`, `sig.py`, `diff.py`,
  `mkoverride.py`: the edit-folding pipeline.

## RCB City: how it fits together (and lessons learned)

- `IthkVillageStructure` lays out the city: buildings, towers at the ring's bends (`keepOut` boxes), gatehouses,
  walls (`CityWallPiece`), streets, sewers, skywalks, terrain (`CityTerrainPiece`, first) and ground paving
  (`CityGroundPiece`, last). Pieces run in list order per chunk.
- **Wall walk** = the wall's inner top cell; **catwalk** = timber boards two cells in, level with it, rail on the
  inner edge; where a building backs onto the wall, the **gallery** carries boards along the wall top under a roof.
  Where the walk climbs a block (near towers), a stair goes on the higher cell (`rampStair`).
- The wall skips columns where something is *built* just above the ground (`built()`), so it doesn't cut houses.
  Lessons: only look **above** the ground (paving and packed mud aren't buildings), and treat **anything growing**
  (flowers, bushes, ferns, saplings, vines) as ground. Each of those once left full-height gaps in the wall.
- Doorways into towers: cut straight ahead of the walk; at a tower corner, two wide; **never through the wall a
  ladder hangs on**. Guard tower ladders hang on the back wall (faces out of town), off the corner. No merlons
  within 2 blocks of a tower. Decide whether the catwalk has room *before* cutting doorways (a doorway's sill
  once stole the last board).
- Where two wall stretches meet, one stretch's timber (boards, rail) may sit on the other's cells: the catwalk
  treats dark oak slabs/fences/stairs as relayable.
- `CityTerrainPiece` removes forest rocks (mossy cobblestone boulders, old growth taiga) first: they read as ground
  and the city built and paved round them.
- Ground paving treats natural blocks as paveable: it must **leave sand alone** (it once paved over the fight pit,
  so the bookie couldn't find his pit).
- Skywalks join opposite upper floors across streets (`TemplateInfo` anchors: plain flat wall both sides, a real
  floor beam at the threshold, no glass). Deck of top slabs; doorways cut through anything (timber frames carry
  block entities).
- Villages keep apart via structure-set exclusion zones (`childrenofmu:avoiding_spread` with an `avoid` list).
  **Avoidance must be one-way** (cities > towns > huts/outposts): two sets avoiding each other once recursed forever
  (StackOverflowError, world wouldn't load). `AvoidingSpreadPlacement.near()` now checks only the other set's grid
  candidates and never asks it about its own neighbours, but keep the lists one-way anyway.
- Our cities don't reserve their land, so vanilla surface structures (a ruined portal once landed in Dog City) are
  kept out by overriding their sets in `data/minecraft/worldgen/structure_set/` (ruined portals, trail ruins, igloos,
  temples, swamp huts, mansions, shipwrecks, ocean ruins, outposts): same spacing/salt, `avoiding_spread` away from
  our towns. Other mods' structures in the owner's pack aren't covered.

## Folk behaviour (entity/folk)

- `FolkWork`: work/off/sleep phases. **Post keepers** (shopkeepers, stallholders, barman, bookie, stage bands)
  return to and stay at their spot while working (`PostGoal`); the bookie always.
- Shops: each shop sign gets a rat shopkeeper with `Shop` NBT; trades come from `shop:<FIRST WORD>` pools in
  `FolkTrades`.
- Pit fights (`FolkPit`): bookie finds the pit by its sand, calls bouts when a player is within 32 blocks, bets by
  clickable chat stakes, pays double; anyone not fighting is lifted out of the pit onto the stands.
- `unpit()` lifts folk out of holes they're stuck in; `unjam()` frees folk stuck on ladders.

## Dog City (DC)

- Owner's design: an 8-point star fort (~170 blocks) of crenellated stone brick (`CityWallPiece` style 1: plain
  stone bricks, no vines) with a water moat (`DogMoatPiece`) and a bridge to the gate in the south tip. Inside: four
  military towers (bow, sword, pike, and the Biters' kennel), four quarters of four workshops each (cloth; smithy/
  tannery/livery/sawmill; meat; mess/music/school/teahouse), the Keep on a raised plinth (monks, courtyard), and the
  forbidden dark tower in the middle (spiral stair, bandaged dog kneeling on a mat at the top).
- `DogCityStructure` lays it out in plan coordinates (gate to +z) and turns the whole city by a random quarter turn
  (`turn()`); `DogGroundPiece.road()` is the road plan. Buildings are axis-aligned squares.
- Templates: `tools/make_dog_city.py` + `tools/dog_build.py` (`dc_*.nbt`). Look them over without the game:
  `tools/worldcheck/render_template.py <nbt> <png> [scale] [view]` (env `CUT_Y` to see inside).
- Wall corners (star tips/notches) and where the diagonals meet the gatehouse: `DogCornerPiece` lays solid stone
  landings level with the walk after the wall runs (the runs' boards/rails crossed and left pits there).
- Quarter colour themes (`THEMES` in `dog_build.py`): cloth = dark prismarine roofs, blue/light blue/white;
  yards = brick, brown/orange/black; meat = red nether brick, red/gray/white; hall = oxidized copper,
  green/yellow/lime. They set roofs, bedroom rugs, beds, front banners, cloths. Military: gray + tower colour.
- **Every bedroom gets a carpet rug** (`B.rug`). **Never a ladder over a window** (`unblock_windows()` in
  `DogBuilder.finish` turns glass behind a ladder into wall). Fire under a pot is sunk into the floor.
- `tools/worldcheck/load.py <region dir> <out.pkl> dog_city` snapshots Dog City (third arg = structure name).
- **Dog sizes** vary 0.5x-1.5x a player (`FolkRace.dogHeight`, synced `SIZE` on FolkEntity, saved as "Size";
  hitbox never wider than 0.9). Workshop folk <= 1.1x (2-high doors); soldiers up to ~1.35x (towers and gatehouse
  rooms have 3-high archways); the **High Guard** (`high_guard`: samurai-Templar, dark tower emblem, sashimono) is 1.5x
  and stands at the foot of the dark tower's stair.
- **Trust** (`DogTrust`, world saved data, per city|player): the folk's confidence from trades (+2), gifts (sneak +
  right-click a dog with something it values, one a day per dog) and hostile mob kills (+1); killing a dog -40.
  30 opens the Keep's halls. The monks (sneak to trade with them) ask 5 questions answerable only from the 5 fables
  on the library lecterns (`FABLES` in make_dog_city.py, `QUESTIONS/ANSWERS/RIGHT` in DogTrust - keep them in step);
  a wrong answer restarts and makes them wait a minute. All 5 opens the courtyard.
- **Keep halls without confidence:** turned back with a warning; a second try within 5 minutes and up to 3 soldiers
  escort them (FolkEntity `EscortGoal`: stand between the player and the Keep, talk) until they leave the city.
  **Courtyard without the monks' trust:** an alarm (`DogTrust.onServerTick`): High Guards attack at once, the nearest
  monk runs to the Keep's bell and rings it, every soldier in the city joins after 3 s; stands down 20 s after the
  intruder leaves the Keep (or dies). Dog soldiers have melee (archers shoot), fight back, never panic.
- Dark tower: solid base, door 4 up in a blackstone portal, a stair up to it in the Keep template; the spiral turns
  on flat corner landings (corner *stairs* in a 1-wide well became outer-shaped lips and couldn't be climbed).
- **Outside the walls** (`DogFarmPiece`, after the moat): wheat terraces following the star outward, a step down
  every 7 blocks (max 3), risers faced in stone brick with andesite kerbs, a 9-grid of paths (stair steps where they
  climb a riser), a water hole per field, the road out from the bridge level, a stone rim round the cleared ground
  and the farmsteads; everything above the fields is cleared (no floating rocks). Four `dc_farmstead`s in the outer
  corners (plan +-82): tool hut + bed + stablemaster, paddock with cows, horses, chickens.
- **Lawns inside the walls** (`DogGroundPiece`, before the roads): every unbuilt cell outside the Keep gets pits and
  cave mouths filled with dirt, a grass surface, and anything loose above cleared (hanging leaves, bushes, rocks,
  exposed stone/ore). Then ~50 trees of the local kind (spruce in taiga/snow, oak and birch elsewhere), 8-17 high,
  placed from the plan (`tree()`, 7-block grid, clear of roads, buildings and the wall) so chunks agree; **no leaves
  in a tree's lowest 6 blocks** (owner: clearance).
- **Dog folk are strictly carnivorous**: no bread, vegetables, cake or fruit in their trades, gifts or stores
  (wheat is for the animals).
- **Soldiers (samurai, pikemen, bowmen) don't trade.** With the monks' trust a player can recruit up to 2
  (right-click; sneak + right-click sends them home): they follow (teleport when far), fight what attacks the player
  or what the player strikes, forgive the player's own slips; `companionOf` is saved.
- Every dog has a bed except the Biters. Dog variants for the trades, plus `pikeman`, `biter`, `bandaged`
  (not random, don't trade).
- Round 2 (to do): guards (High Guards) warn then attack near/in the dark tower; Biters stay in the kennel; killing the bandaged
  dog: scream, Biters released, every dog turns rabid, permanently (world saved data). **After one night of the
  curse, every dog folk turns into a vanilla wolf** (owner, 2026-10-28).
- Later: swords, pikes and bows from other weapon mods for the soldiers (owner wants this once the basics work).
