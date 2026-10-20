OpenBlocks
=========

An open source random collection of blocks and miscellaneous cool stuff.

This is an unofficial port of [OpenBlocks](https://github.com/OpenMods/OpenBlocks) from Minecraft 1.12.2 (Forge) to **Minecraft 1.21.1 (NeoForge)**. The original mod was created by the OpenMods team; this port is maintained by chriss199815 and is not affiliated with or endorsed by the original authors. Please report bugs of this port [here](https://github.com/chriss199815/OpenBlocks/issues), not to the OpenMods team.

> **Status: alpha.** All features of the original are ported, but the port is new and may still contain bugs. Back up your worlds. Worlds from 1.12.2 cannot be converted.


About this port: made with AI
-----------
This port was written almost entirely by an AI, guided, reviewed and tested by me. The AI read the original 1.12.2 code and rewrote each feature for the NeoForge 1.21.1 APIs; I decided what to port and how, tested every feature in game and approved each change before it was committed.

What this means for you:

* The code was not written line by line by a human. It has been tested in game and with automated GameTests, but bugs the original never had are possible.
* Textures, models and sounds are the original assets from OpenBlocks. Most translations were carried over from the original; missing and new strings were translated by the AI.
* If you run into a problem, please open an issue. Reports help a lot.


Requirements
-----------
* Minecraft 1.21.1
* NeoForge 21.1 or newer

Optional integrations:

* [JEI](https://modrinth.com/mod/jei): recipes, including examples for the special crafting recipes
* [Patchouli](https://modrinth.com/mod/patchouli): the in-game manual (Info Book)
* [CC: Tweaked](https://modrinth.com/mod/cc-tweaked): peripherals for several blocks and a magnet turtle upgrade


What does it contain?
-----------
* A combination ladder and trapdoor, also known as the **Jaded Ladder**
* A creative-only **healer** block that slowly replenishes your health and food when you stand on it
* A **guide block** and **builder's guide** to assist in various constructions
* Colorable **elevator blocks** (also in a rotating variant) to quickly travel between floors. Requires XP by default
* Archery **targets** for a shooting range
* **Player graves**, safekeeping for your last death
* Colored **flags** for various purposes
* Liquid **tanks**: portable, practical, and pliable any-size liquid containment
* Hang **gliders**, with an optional acoustic variometer
* Random **trophies** that may or may not do odd things when right-clicked
* **Bear traps**: like venus fly traps, but snappier
* **Luggage**, the traveling sentient chest
* **Sprinklers**, to accelerate the growth of crops and other growables
* **Item cannons** to shoot items around for transport
* **Vacuum hoppers**: like regular hoppers, but not limited to the top, and they suck up XP
* **Sonic glasses**: echolocation for Steves
* **Sponges** (and the sponge on a stick) for getting rid of liquids fast
* The Redstone Configurable Pulse Lightweight Mega Touch Sensor, also known as the **BIG BUTTON**
* Blocks drawn by **pencils** and **crayons** that are only seen by the imaginative eye
* **Fans**: the power of an industrial fan in the size of a desk fan
* A wearable **crane** for picking up blocks and entities
* **XP Bottler**, **XP Drain** and **XP Shower** for liquid XP
* **Magnet turtles** (with CC: Tweaked)
* **Village highlighter**: shows beds, workstations, bells and villager ranges of nearby villages (reworked, since villages work differently since 1.14)
* The **Slimalyzer**, which detects slime chunks
* **Paths**, an alternative to gravel roads
* The **Block Breaker**, **Block Placer** and **Item Dropper**
* The **Auto Anvil** and **Auto Enchantment Table**, using liquid XP to work automatically
* The **Sleeping Bag**: sleep on the go without resetting your spawn point
* The **Rope Ladder**: a magic ladder that doesn't need support and rolls itself down to the ground
* The **Donation Station**: tells you which mod a block or item comes from
* The **Projector**, **Cartographer** and height maps
* The **Sky Block**, **Golden Egg**, **Golden Eye**, **Glyphs**, **Scaffolding**, **Pedometer**, **Cursor**, **Dev Null** and more
* Three enchantments (Explosive, Last Stand, Flim Flam) with their random flim flam pranks
* A painting/staining system for blocks, including:
  * A **Paint Mixer**: makes paint using dyes and milk. Millions of colors possible
  * A paint **brush** to paint white **canvases** and other blocks
  * **Stencils** that are made using the **Drawing Table** to paint specific areas of blocks
  * A **Squeegee** that cleans paint off of blocks


Compiling
--------------
You need Java 21 and git.

```
git clone https://github.com/chriss199815/OpenBlocks.git
cd OpenBlocks
git checkout port/1.21.1
./gradlew build
```

On Windows use `gradlew.bat build`. The finished jar is in `build/libs`.

Other useful tasks:

* `./gradlew runClient` / `./gradlew runServer`: start the game in the dev environment
* `./gradlew runData`: run the data generators (output in `src/generated/resources`)
* `./gradlew runGameTestServer`: run the GameTests

The old 1.12.2 code is kept in `legacy/` as a reference only; it is not compiled. The original OpenModsLib library is not needed anymore.


Credits
-
* The OpenMods team (Mikee, NeverCast, boq, Lyqyd and all contributors) for the original OpenBlocks and all its assets
* Port to NeoForge 1.21.1: chriss199815


License
-

OpenBlocks is open source under the MIT license, see [LICENSE](LICENSE). The original copyright notice of the OpenMods team is kept.
