# Children of Mu

Strange peoples of a lost continent, for Minecraft 1.21.1 (NeoForge).

## First race: the Ithk

Floating, telepathic eyeball folk who keep knowledge atop crystal towers.

Working so far (v0.2):
- Slow floating flight with a gentle sine-wave bob
- Each Ithk gets a random name (shown when you look at it) and eye color
- Telepathy: greets players within 8 blocks, in chat
- Struck once: becomes ethereal for 5 seconds (blink, then a glowing green wireframe, untouchable)
- Struck again by the same aggressor within 30 seconds: the aggressor is teleported to a random spot just outside the village
- Random idle blinking; the whole eye turns to look at you
- Four professions, each with a costume: Transmuter (spinning iridescent cones), Archivist (graduation cap),
  Gatekeeper (starry cape with a color-shifting lining), Curator (striped scarf)
- Trading: right-click to trade. Five levels per profession, two trades per level, deliberately expensive.
  Trades restock once per Minecraft day. New trades unlock after you finish trading at a level-up.

- Quiescent Ithk: at rest, beyond any vocation. No costume and no trades, but no lesser in station.
- Titles over their heads when you look at them, like "Ouk the Archivist"

- Ithk villages: the Amethyst Tower at the center, with the other buildings scattered around it at random: an
  Athenaeum, one or two Watchers, sometimes a Nexus, a Golem Cradle, a Hot Fount, Sleep Lids, the Eye Statue,
  Mud Gardens, Attuners, sometimes a Scrying Pool, Star Perch or Memory Cairn, and Lens Arches around the edge.
  Every village is different. Generates rarely in plains, savannas, meadows and swamps (never centered on water).
  Which buildings, how many and how far out: `data/childrenofmu/worldgen/structure/ithk_village.json`
  (`min_count`/`max_count`, `min_distance`/`max_distance` from the center, `footprint` = the room each one needs).
  How often villages appear: `structure_set/amethyst_tower.json` (it now places villages, not lone towers).
- Village Ithk wander: every few minutes, day and night, an Ithk drifts off to float somewhere else in its village
  for a while, then home again or on to another spot. Trading or attending to you always comes first.
  Egg-spawned Ithk (not in a village) stay home.
- Aggressors cast out by a village Ithk land just beyond the whole village, not just its own building.
- The Amethyst Tower: a floating amethyst-and-stone spire with hanging crystals, weeping vines below, an archive, a
  crystal garden, and an open-sided top room where four Ithk live. Ithk stay near the room they appeared in.

- The Levitation Shard: hold use to drift upward; while it's in your hand you sink gently and take no fall damage.
  400 durability (about 3 minutes of rising), repair with amethyst shards. Every Gatekeeper sells one at Novice level.

- The Amethyst Watcher: a single round room hanging in the air, with a great eye-shaped window, curling stone
  tentacles, a crown of crystal spikes and an inverted crystal base. One Ithk lives inside; the way in is a narrow
  gap at the back. Generates as part of Ithk villages.
- The Athenaeum: a great amethyst cube standing on one corner, cut through the middle by a glowing emerald band,
  holding a library with two Archivist Ithk. A diamond window in the front is the way in. Village-only.
- The Athanor: home of the Transmuters, a floating tower in an ancient-Egyptian manner: an upside-down pyramid
  carrying a four-armed platform, a columned purpur hall with ankhs and a flared cornice, a stair to its doorway,
  and a pyramid floating above, joined by a column of light. Inside, two Transmuters tend a lava-filled furnace in a
  transmutation circle. One in every village.
- The Nexus: a tall floating diamond of deepslate brick and amethyst with lines of emerald, gold, lapis and
  glowstone, rings at its waist, and one cube room lined with six glowing portal panels (Nether, Aether, Twilight
  Forest, the End, the Undergarden, the Bumblezone) around a crystal pedestal and a beam of light. Two Gatekeeper
  Ithk. The front portal is an open doorway. The portals are decorative for now. Village-only.
- Smaller village pieces (village-only, `/place` to test):
  - The Hot Fount: a small pyramid pouring a stream of lava into a wider upside-down pyramid that holds a lava pool.
  - The Eye Statue: a floating eyeball of quartz and grey stone with an amethyst iris, gazing skyward, hung with
    red vines.
  - The Sleep Lid: the Ithk have no eyelids and never stop watching, so they rest inside one: a small almond-shaped
    deepslate shell whose lids have almost closed, lined with obsidian and kept dark so the Ithk inside can "blink".
    No light by design; the floor is carpeted so monsters can't spawn in it.
  - Attuners: small floating pieces that, in Ithk lore, keep the area in psychic tune. A crystal in a tilted hoop
    (`attuner_shard`), a crystal tuning fork (`attuner_fork`) and an upright ring holding a crystal (`attuner_ring`).
  - The Lens Arch: a deepslate archway with a crystal lens hanging in it, marking a village's edge.
  - The Scrying Pool: still, dark water in a raised rim, candles on crystal posts, an Ithk gazing in.
  - The Mud Garden: a little floating island of mud and red plants, trailing roots and vines.
  - The Star Perch: a thin spire with a small platform where an Ithk watches the sky through a crystal.
  - The Memory Cairn: a floating stack of stone discs etched with lapis lines, glowing at their hearts.
  - The Golem Cradle: a floating platform with three amber-lit alcoves where amber golems rest, around a pool of
    lava they step into to melt down and come back out renewed. (The golems themselves are still to come.)
- Inlaid Emerald, Inlaid Gold and Inlaid Lapis: look like the real blocks but only drop themselves, so Ithk
  buildings can't be mined for a fortune.
- Buildings float over the land as it is, turned to a random side. Instead of a flat platform, the real ground
  beneath turns to mud with red plants, fading back into the grass at a ragged edge
  (`worldgen/IthkBuildingPiece.java`; the patch size is `ground_radius` in each `worldgen/structure/*.json`).
- All structures glow at night: crystal pendants with glowing hearts, lanterns, candles and end rods.
- All structures taper their edges with stairs and slabs. The mod adds Amethyst Stairs and Amethyst Slabs
  (vanilla has none): craft them like any stairs/slabs or use a stonecutter.

- Walking folk (first bodies, from the character sheets in `CoM Reference Images`): Rat Folk (Runner, Scavenger,
  Gong Rat with a shovel, and the Beautiful Ones, who never leave home and groom themselves all day), Cat Folk (Rogue, Adventurer, Hunter, Fellmonger), Bat Folk (Folk, Flapper, Barman,
  Jazz), Dog Folk (Monk, Husky Monk, and feudal-Japanese Samurai, Archer and Stablemaster), Frog Folk (Jumper, Guard,
  Potioneer, Coppersmith, Chitinier the chef and his Cooks, Fisher, Fishmonger, Schoolmarm, Treasurer, Plumber, Elder and little hopping Froglets), Goxa (Havu, Seminata) and Lilbuns (Farmer, Friend; 1/12 of a player's height). Spawn eggs pick a random
  variant. For now they wander, look at you and run when hurt. Rats keep the burrows clean and
  trade odd, cheap things (right-click): soil and fertilizer from Gong Rats, scraps of food from Runners, bits and
  pieces from Scavengers. Cats are thieves: each sells two random stolen goods, and new ones every week.
  The Samurai sells swords, the Archer bows and arrows, the Stablemaster horses (as spawn eggs), saddles and leather.
  The bat Barman sells drinks (Brewin' and Chewin' drinks when installed); the other bats play jazz all night, in
  time with each other: bass, drums and a wandering melody. Frog Potioneers, Coppersmiths and Chitiniers trade
  potions, copper and insect goods. Trade tables: `entity/folk/FolkTrades.java`; the music: `FolkJazz.java`. Models and textures:
  `tools/make_folk_assets.py`; sizes, speeds and what each variant carries: `entity/folk/FolkRace.java`.
  A specific variant: `/summon childrenofmu:rat_folk ~ ~ ~ {Variant:"gong"}` (also `cat_folk`, `bat_folk`, `dog_folk`, `frog_folk`, `goxa`, `lilbun`).

### Plans for the Ithk

For now the Ithk live in the overworld, so they're easy to test. In the end they'll live in a small dream dimension of
their own: the overworld keeps only a ruined stone facsimile of each village, dormant, and somewhere in it (probably
the portal room) an End-portal-style portal leads to the living village on the other side.

## Second race at home: Frog towns

Frog towns grow in swamps and mangrove swamps around a giant mangrove (`frog_great_mangrove`): the Town Hall is
carved out inside its trunk (the Treasurer and the Elder), a deck rings it with fish and bugs drying on lines, and
stepping pads spiral up its side to a lookout under the canopy, where a Guard keeps watch. Around it, 8 to 14 stilt
buildings over the water, joined to the tree by 3-wide boardwalks (lanterns every few posts, lily pads alongside). Buildings are in
the style of the "Frog Hut 1" sheet: oak walls framed in spruce logs, mossy hip roofs with leaves, porches with
railings, mossy stepped pilings that frogs hop up from their little docks, vines everywhere. Their stilts reach down to the swamp floor wherever they
generate (`extend_supports` in `worldgen/structure/frog_*.json`).

- Building shapes: square stilt huts, round huts under thatched cone roofs (`frog_hut_round`), small round huts under
  tall witch's-hat roofs (`frog_hut_tall`), the Potioneer's round shop under a giant red mushroom cap, the
  Coppersmith's round forge under a copper cone roof, a round guard lookout high on braced stilts, a courier landing
  pole beside the post office, and a belfry on the schoolhouse.
- The restaurant (`frog_chitinier`): the Chitinier is the town's fancy insect chef. Right-click him for tonight's
  menu in chat (Honey-Glazed Beetle, Cricket Crumble, Mosquito Consommé, Dragonfly Tartare, Firefly Flambé, Moth
  Soufflé, Bog Snail Escargot); click a dish and pay (10 to 22 emeralds, outrageously). Nothing is cooked ahead:
  his Cooks rush between the stoves, pots and cutting boards until it's done, then he hands it over (or leaves it on
  the pass if you've wandered off). Frogs in town order dinner too, and eat it noisily. All copper: copper door,
  trim, shutters, bulbs, chimney. Code: `entity/folk/FolkKitchen.java`.
- Day and night: at dusk every frog except the Guards finds a Reed Mat (rows of them in the long, damp
  Torpor House `frog_torpor_house`, a few in each home), tucks in its head and limbs and goes torpid until morning;
  hurting one wakes it. In the daytime the froglets go to school (`frog_schoolhouse`: desks, a blackboard, a globe,
  a school bell) and stay by the Schoolmarm (bonnet, spectacles, a willow pointer; she sells books, paper and quills).
  Code: `entity/folk/FolkRest.java`.
- Fighting: Guards carry bamboo blowpipes and spit poison darts at monsters near town (not creepers) and at anyone
  who hurts them. Grown frogs with ordinary jobs hunt small slimes; spiders hunt frogs, day or night.
  Code: `entity/folk/FolkCombat.java`.
- Slime: the frogs trade in it (slimeballs, slime blocks, sticky pistons). A Master Potioneer sometimes sells
  Pink Slime, which will matter a great deal later.
- Traders level up like villagers (Novice to Master): two trades to start, two more each level.
- The Treasurer's vault is under the Town Hall floor: an iron hatch behind the counter, a ladder down, iron bars and
  a safe, and piles of (decorative) gold and emeralds.
- The fish market (`frog_fish_market`): the Fishmonger (striped apron, flat cap) sells cooked fish, Farmer's
  Delight fish dishes when installed, buckets of tropical fish, and buys raw catch.
- Fish and bugs hang drying under every porch's eaves and on lines around the market and the great mangrove
  (the Strung Fish and Strung Bugs blocks).
- Town Hall (Treasurer, Elder, a bell; now inside the great mangrove, and also placeable on its own), Post Office (two Jumpers, a Post Box on the porch), Guard Tower (a Guard),
  Potion Shop, Coppersmith (lightning rod on the roof), Chitinier (beehives, honey), Pump House (Plumber),
  Fishing Dock (Fisher), and homes with a random frog and sometimes a Froglet.
- Every frog has a name, with a title for some jobs ("Sergeant Wilbur Mudgley", "Treasurer Hortense Bogwhistle",
  "Old Ambrose Reedmantle", "Lil Pip"); the trading window shows the name and job. Frogs stand a little taller
  than a block (froglets about half that), and they never despawn or drown.
- Towns only grow in a real swamp (the middle and the land around it must all be swamp), every building stays within
  5 blocks of the Town Hall's height, and they keep their distance from normal villages.
- Frog decor blocks (in the creative tab, also used all over the towns): Reed Thatch (block, stairs, slab), Reed
  Screen (a woven lattice that connects like glass panes), Lily Lantern (stands or hangs), Frog Idol, Fishing Net
  and Cattails. Generator: `tools/make_frog_blocks.py`.
- Borrowed decor: the buildings also use blocks from Quark, Farmer's Delight and Supplementaries (stoves and
  cooking pots, cutting boards, canvas rugs, jars, sacks, notice boards, stools, rope, straw thatch). Each frog
  building comes in two versions, `frog_x.nbt` with those blocks and `frog_x_vanilla.nbt` with stand-ins; the game
  uses the decorated one when Quark, Farmer's Delight and Supplementaries are all installed
  (`worldgen/ModdedTemplates.java`). The test game loads copies of those mods from `run/mods` (taken from the
  Colonies & Skies pack); delete them from there to test the plain versions.
- Frog jobs: the Fisher trades fish, kelp and lily pads, the Treasurer changes gold for emeralds and back (and has a diamond now and then),
  the Plumber sells buckets, pipes and water things (Create pipes when installed).
- The post: right-click a town's Post Box to open it (it learns the town's name the first time). Hold any item and
  right-click a Jumper: it lists every town whose post box you've opened as clickable names. Pick one, pay one
  emerald, and the parcel arrives in that box a few minutes later (further = longer, at most one Minecraft day).
- Gossip: right-click a Guard or the Elder. Guards share town gossip, news from other towns, and now and then a
  rumour pointing the way to the nearest Ithk village. The Elder tells old stories about the other races.
- Find one: `/locate structure childrenofmu:frog_town`. Build one here: `/place structure childrenofmu:frog_town`
  (stand in a swamp). Single buildings: `/place structure childrenofmu:frog_hut` (also `frog_town_hall`,
  `frog_great_mangrove`, `frog_fish_market`, `frog_torpor_house`, `frog_schoolhouse`, `frog_post`, `frog_guard_tower`, `frog_potion_shop`, `frog_coppersmith`, `frog_chitinier`, `frog_pump_house`,
  `frog_fishing_dock`). Generators: `tools/frog_build.py` and `tools/make_frog_town.py`.

Not yet: amber golems, Create/Quark crystal swaps, the folk's cultures and homes.

## RCB City (Rats, Cats and Bats)

A rare, crowded, crooked city on open plains and meadows, in the spirit of Ankh-Morpork and Antipolis. Tall, narrow,
leaning houses with jettied upper floors and red-tile (or verdigris copper, or slate) spires crowd together, joined
by winding cobbled alleys with lamp posts. Built by `tools/make_rcb_city.py`; laid out by `rcb_city.json`
(`streets: true` gives the labyrinth: each building's door joins the nearest door already placed).

- **A walled town**: an irregular ring of stone wall, different in every city (each side its own distance out, often
  stepped in or out partway, the corners cut off on the diagonal), with a walk along the top behind merlons. Guard
  towers stand at every bend: door into town, a ladder all the way up, guards' quarters (bunk, table, barrels of
  arrows and bows), a floor level with the wall walk (the wall cuts its own doorways into each tower and gatehouse),
  and archers on a fighting platform on top, with open arrow loops. Rows of houses and single houses stand with their
  backs against the inside of the wall. Two gatehouses (the lived-in bridge, facing into town) let the streets out;
  their little houses open onto a covered catwalk, reached from the towers. The land inside is levelled plot by plot
  (`CityTerrainPiece`), so nothing floats or sinks. Inside, each quarter has its own character: the **Bazz quarter**
  (clubs, decks, bat towers, the fighting pit), the **market** (stalls, shops, the watch house), the **houses** (rows,
  skyways) and the **Rat works** (the junk yard and the way down to the sewers). Set by `wall_radius`, `gates` and each
  building's `district`, `span` and `back` in `rcb_city.json`.
- **The Justice Hall** at the heart: three storeys of stone with round corner turrets (a little room at every floor;
  one has a ladder up to the roof), a bell tower with a dead tree on top, and open staircases along the walls.
  Ground floor: the Watch (desks under banker's lamps, the charge book, wanted posters, a rack of arms, a fireplace,
  and a cell under the stairs with a rat in it). Middle floor: barracks and armory (bunk beds, armor on stands,
  weapons on the walls, barrels of arrows, a mess table, a smithy). Top floor: the throne room of **the Great Gar**,
  vast and fat on a great yellow cushion on a gilded dais, with stained glass, chandeliers, banners, and his clerks
  (rats in green eyeshades) at their desks. Every city has its own name and its own Gar.
- **The Great Gar** never gets up, unless someone attacks him: then he waddles away, very slowly, never leaving his
  room, and every cop and guard in the city comes for the attacker (no paying them off). He can be killed: the bells
  toll, every Writ of Trade he sealed is void, the city stays hostile to his killer for three days, nobody trades in
  the meantime, and then a new Gar is enthroned.
- **Writ of Trade**: nobody in the city trades with you, and no bookie takes your bet, until you pay the Great Gar
  (right-click him with 8 emeralds in your inventory). The writ is good in that city for nine days.
- **Who lives where**: Rats live in the sewers and sleep by day in piles in little hay nooks in the tunnel walls. A rat
  caught out under the open sky runs for the nearest manhole and drops down it; at night the odd one pops up and
  dashes across town to another manhole. Ordinary cats hunt rats they catch in the open, and eat them; the sewers
  breed replacements. Bats play Bazz at night on or by a stage (a club, a roof deck); the rest fly about between
  rooftops, decks and roosts. By day they hang from roosts. Cat city archers (green tabards, bows) man the towers.
- **Cat cops and city guards** are the city's golems: they see off monsters. They also keep the law. Breaking the
  city's blocks, taking things out of its barrels and sacks, hitting a citizen or killing one are crimes, but only
  cops within 24 blocks notice. They surround you, circle and name a fine. Throw emeralds or raw fish at them
  (an emerald counts as two fish) and they leave you alone. Hit a cop, or don't pay in time, and they kill you.
  Right-click a cop to hear what you owe. A fallen cop is replaced in time while the city has citizens.
- **Bat towers** have roost posts sticking out of their sides, some under awnings. By day the bats fly up and hang
  from them upside down; at dusk they drop off and glide down.
- **Bazz clubs**: bat bands play all night. The style drifts every couple of minutes between bebop, lounge and
  dub (with echoed eeks). The barman sells Calf (the Cats' alcoholic milk), Calf Bite and fermented tuna.
- **The fighting pit**: the Bat bookie calls a bout every few minutes when someone's watching (a Cat against
  three Rats, Rat against Rat, or Cat against Cat). Click a stake in chat (or right-click the bookie) to bet; winners
  pay double. Beaten fighters tap out rather than die.
- **Streets and sewers**: smooth lanes of packed mud and mud brick flags (level from side to side) with a stone curb,
  weeds coming up through it, and tall street lamps. Yards and alleys are trodden earth and worn grass with weeds,
  vines up the walls and clutter. A brick sewer with a water channel runs under every street, all at one depth so
  they meet; where the land falls away it stops at an iron grate instead of breaking out. Grey stone manhole covers
  (they open by hand) appear only over a stretch of sewer that is really there, each with a ladder straight down to
  the walkway. A drain house leads to a Rat den.
- **Domum Ornamentum**: when it's installed (it comes with MineColonies), the city's roofs are smooth Domum shingles
  (steep 2:1 pairs, bell-cast flares, ridge caps) and its plaster walls real timber frames (chevron braces, crosses,
  framed panels). Every building also has a plain `_vanilla` twin used when Domum or the other decor mods are missing.
- **Bazz decks**: the Bazz house has a roof deck where the band plays under a striped canopy (they roost on the rods
  under it by day) and a rickety deck off the second floor for listening and drinking; the Bazz club has a rickety
  terrace on stilts. Bat towers have roost racks: beams held out from the walls, fence rods hanging under them,
  shaded by a shared awning.
- **Skyways**: pairs of houses across an alley joined by covered timber skyways, and a lived-in bridge (an arch on
  two towers over a passage, with a row of tiny houses on top).
- **Rowhomes**: terraces of joined houses (`tools/rcb_build.py`) with different heights, materials and roofs
  (trimmed gables facing or along the street, dormers, mansards, flared spires), bay windows, shop fronts with
  awnings, patched walls and propped-up holes, chimneys with enclosed fires.
- **Rats with jobs**: sweepers clean up litter (anything left lying in the street for two minutes), post rats,
  accountants and mutated engineers, each with their own trades.
- **Also**: the Watch House and fish stalls.

Find one: `/locate structure childrenofmu:rcb_city`. Build one here: `/place structure childrenofmu:rcb_city`.
The Old Traveller sells maps to them too.

## The Old Traveller

A rare wayside encounter: a little thatched cabin with a campfire out front (`traveler_hut_<race>`), home to an Old
Traveller of any race (rat, cat, bat, dog, frog or goxa), named like "Old Barnaby" and carrying a compass. They sell
treasure maps, marked with a red X, to the nearest Frog Town, Ithk Village and RCB City they know of (worked out a
few seconds apart once you're near, so the game never stalls), for emeralds plus
something their race fancies (a cat wants fish, a frog slimeballs, a rat bones, a bat spider eyes, a dog iron, a goxa
amethyst), and a few odd things picked up on the road. All the towns are rare now, so the maps matter.
Find one: `/locate structure childrenofmu:traveler_hut_frog` (or `_rat`, `_cat`, `_bat`, `_dog`, `_goxa`).

## Running the test game

1. Open this folder in IntelliJ IDEA (File > Open > pick the `ChildrenOfMu` folder). Let it finish importing. The first time takes several minutes while it downloads Minecraft.
2. In the Gradle panel (elephant icon, right side): `Tasks > mod development > runClient`, or use the `Client` run configuration at the top right.
3. Create a world: Creative, Allow Cheats ON. A Superflat world is handy for testing.

### Useful test controls
- Spawn eggs are in the **Children of Mu** creative tab.
- `F3 + F4`: switch between Creative and Survival (hold F3, tap F4 to cycle).
- `/gamemode survival` / `/gamemode creative`
- `/summon childrenofmu:ithk ~ ~2 ~`
- A specific profession: `/summon childrenofmu:ithk ~ ~2 ~ {Profession:"archivist"}` (or `transmuter`, `gatekeeper`, `curator`, `quiescent`)
- A Master-level Ithk with all its trades: `/summon childrenofmu:ithk ~ ~2 ~ {Profession:"gatekeeper",MerchantLevel:5}`
- `/kill @e[type=childrenofmu:ithk]` removes them all.
- Find the nearest village: `/locate structure childrenofmu:ithk_village` (then click the coordinates to teleport)
- Build a whole village right here: `/place structure childrenofmu:ithk_village` (it's about 150 blocks across)
- Build a tower right here: `/place structure childrenofmu:amethyst_tower` (stand in an open area; it's big)
- Build a Watcher: `/place structure childrenofmu:amethyst_watcher`
- Build an Athenaeum: `/place structure childrenofmu:athenaeum`
- Build a Nexus: `/place structure childrenofmu:nexus`
- Small pieces: `/place structure childrenofmu:hot_fount` (also `eye_statue`, `sleep_lid`, `attuner_shard`,
  `attuner_fork`, `attuner_ring`, `athanor`, `lens_arch`, `scrying_pool`, `mud_garden`, `star_perch`, `memory_cairn`,
  `golem_cradle`)
  (both only place in biomes they can generate in: plains, savanna, meadow or swamp, so use those or Superflat)
- Fly: in Creative, double-tap jump. The top room is about 50 blocks up.
- `/time set night`, `/weather clear`, `/tp ~ ~50 ~`

## Changing things
- Names: `src/main/java/com/childrenofmu/entity/ithk/IthkNames.java`
- Eye colors: `IthkEyeColors.java`
- Telepathy lines: `src/main/resources/assets/childrenofmu/lang/en_us.json`
- Trades: `src/main/java/com/childrenofmu/entity/ithk/IthkTrades.java`
- The tower: edit and run `python tools/make_amethyst_tower.py`. Heights, sizes and blocks are named at the top.
- The Watcher: edit and run `python tools/make_amethyst_watcher.py`.
- The Athenaeum: edit and run `python tools/make_athenaeum.py`.
- The Nexus: edit and run `python tools/make_nexus.py`.
- Shared pieces and the stair/slab tapering live in `tools/ithk_build.py` (every generator uses it).
- New simple blocks (stairs/slabs): add them in `tools/make_block_assets.py` and `ModBlocks.java`, then run the script.
- How often villages appear: `spacing`/`separation` in `src/main/resources/data/childrenofmu/worldgen/structure_set/amethyst_tower.json`
  (in chunks; bigger = rarer). Which biomes: `data/childrenofmu/tags/worldgen/biome/has_structure/amethyst_tower.json`.
- Costumes: edit and run `python tools/make_ithk_costumes.py`. Colors and animation speeds are in `client/model/IthkCostumeModel.java`.
- Item textures: edit the pixel grids in `tools/make_item_textures.py` and run it.
- Ethereal duration, village radius, telepathy range: top of `IthkEntity.java`
- Bob height and speed: top of `client/renderer/IthkRenderer.java`
- Sphere shape and textures: edit and run `python tools/make_ithk_assets.py` (needs Pillow). It regenerates the geometry and both textures.

## Building a jar for the modpack
Gradle panel: `Tasks > build > build`. The jar appears in `build/libs/`. Copy it into the modpack's `mods` folder.
