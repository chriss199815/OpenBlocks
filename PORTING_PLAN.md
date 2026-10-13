# Plan: OpenBlocks → NeoForge 1.21.1

## Context
OpenBlocks ist eine Mod für MC 1.12.2. Sie läuft auf Forge 14.23 mit ForgeGradle 2.3 und Java 8. Es gibt 381 Klassen, die stark an das Submodul **OpenModsLib** gekoppelt sind (564 Klassen, eigenes Sync/RPC/GUI/ASM/Animation-Framework). Ziel ist eine spielbare Version für **NeoForge 1.21.1**.

Entscheidungen:
- OpenModsLib wird **nicht** portiert. Jedes Subsystem wird durch moderne Vanilla- oder NeoForge-Mittel ersetzt, nur kleine Helfer werden übernommen.
- Der Port erfolgt **in Wellen**. Nach jeder Welle gibt es einen spielbaren Stand.

Praktisch ist das ein Neuaufbau, der den alten Code als Referenz nutzt. Ein automatisches „Hochziehen“ ist nicht möglich: Zwischen 1.12 und 1.21 haben sich Registries, Rendering, NBT/Data Components, Netzwerk, GUIs, Fluids und Enchantments grundlegend geändert. Beide Projekte stehen unter MIT, also darf der Code übernommen werden. Die Copyright-Hinweise bleiben in `LICENSE` erhalten.

## Repo-Strategie
- Neuer Branch `port/1.21.1`. Der alte Stand bleibt auf `master` als Referenz.
- Den alten Code nach `legacy/` verschieben (wird nicht kompiliert). So kann man beim Portieren jede Klasse direkt nebeneinander vergleichen. Das Submodul `OpenModsLib` bleibt dort ebenfalls als Referenz liegen und wird aus `settings.gradle` entfernt.
- Neues Build-Gerüst nach dem NeoForge-MDK: ModDevGradle, Gradle 8.x, Java 21, Parchment-Mappings, `neoforge.mods.toml`.
- Mod-ID `openblocks` bleibt. Falls die Mod veröffentlicht wird, vorher Name und Mod-ID klären (z. B. „OpenBlocks Reborn“), um Konflikte mit dem Original zu vermeiden.

## Ersatz-Tabelle: OpenModsLib → NeoForge 1.21.1
| Alt (OpenModsLib/Forge 1.12) | Neu |
|---|---|
| `@RegisterBlock`/`@RegisterItem` + `ModStartupHelper` (`OpenBlocks.java` ~273/398/725) | `DeferredRegister.Blocks/Items/BlockEntityTypes/EntityTypes/MenuTypes/DataComponents`, eigene Klassen `OBBlocks`, `OBItems` … |
| `Config.java` + `@ConfigProperty` (106 Felder) | `ModConfigSpec` (Common/Server/Client). „Feature deaktivieren“ über Config plus Ausblenden aus Creative-Tab und Rezepten (Registrierung bleibt immer aktiv) |
| `openmods.sync` (SyncableInt, SyncMap, `sync.drops`) | `getUpdateTag`/`ClientboundBlockEntityDataPacket`, im Menü `ContainerData`/`DataSlot`, Drop-Daten über **Data Components** und `BlockItem` component copying |
| `openmods.network.rpc` + `openblocks/rpc/*` (7 Interfaces) | je ein `CustomPacketPayload` + `StreamCodec`, registriert über `RegisterPayloadHandlersEvent` |
| `openmods.network.event` (Elevator/Guide/PlayerAction) | Client → Server-Payloads |
| `openmods.gui.component` (Widget-Toolkit) | Vanilla `AbstractContainerScreen` + `AbstractContainerMenu`. Wiederkehrende Widgets (Tank-Anzeige, Seitenauswahl, Level-Slider) als kleine eigene Widget-Klassen |
| `openmods.inventory` | `ItemStackHandler`/`SimpleContainer`, Item-Inventare über `DataComponents.CONTAINER` bzw. eigene Komponente |
| `openmods.liquids` | `FluidTank` + `Capabilities.FluidHandler.BLOCK/ITEM` |
| `openmods.fakeplayer` | `FakePlayerFactory` von NeoForge |
| `openmods.movement` (ASM-Sprung/Sneak-Hook für den Aufzug) | Client-Tick-Event prüft Input → Payload an den Server |
| Forge-Animation-API (`armatures/`, `asms/`, `openmods:eval`) | `BlockEntityRenderer` mit Code-Animation (Rotation/Translation von Model-Parts). Kein GeckoLib nötig |
| `forge_marker`-Blockstates, Custom Model Loader (`magic-*`) | Vanilla-Blockstates per **Datagen**. `magic-*`-Modelle → NeoForge `IUnbakedGeometry`/`BakedModel`-Wrapper oder BER |
| `openmods.renderer.shaders` (Guide) | `RenderType`-Linien/Quads im `RenderLevelStageEvent`, ohne eigene Shader |
| `.lang`-Dateien (16) | `lang/*.json` per Konvertierungsskript |
| `forge:ore_shaped`/OreDict (≈420 Stellen) | Vanilla-Rezepte + `c:`-Tags, erzeugt per Datagen |
| Custom `IRecipe` (`CustomRecipesSetup.java`, 7 Klassen) | `CustomRecipe` + `RecipeSerializer` |
| Enchantments (Explosive, FlimFlam, LastStand) | Datapack-Enchantments (1.21 data-driven) + Event-Handler, die per `EnchantmentHelper` den Level abfragen |
| `infobook`, `fixers`, Legacy-IDs | **entfallen** (Handbuch evtl. später über Patchouli) |
| ComputerCraft/OpenPeripheral (`integration/`) | **entfällt vorerst** |
| NEI-Plugin (`NEIOpenBlocksConfig`, versteckt Canvas Glass + Height Map) | **JEI**-Plugin `openblocks.integration.jei.OpenBlocksJeiPlugin` (optional, nur API zur Compile-Zeit). Später: JEI-Kategorien/Beschreibungen für Custom-Rezepte (Crayon-Mix, Map-Clone, Golden-Eye-Recharge …), da `CustomRecipe` in JEI sonst nicht sichtbar ist |

Kleine Helfer aus `openmods.utils`/`geometry`/`colors` (z. B. Farb-Enums, Rotations-Helfer) werden bei Bedarf in ein internes Paket `openblocks.util` kopiert, nicht vorab.

## Phase 0 – Fundament — ✅ erledigt (2026-10-13)
Erledigt:
- [x] Branch `port/1.21.1`; alter Code inkl. Submodul `OpenModsLib` nach `legacy/` verschoben (nicht kompiliert).
- [x] Build: NeoForge 21.1.259, ModDevGradle 2.0.148, Parchment 2024.11.17, Gradle 8.14.3, Java 21; Runs `client`, `server`, `gameTestServer`, `data`.
- [x] `neoforge.mods.toml` aus Template (`src/main/templates`), JEI als optionale Client-Abhängigkeit.
- [x] `OpenBlocks`-Hauptklasse, Registries `OBBlocks`, `OBItems`, `OBBlockEntities`, `OBCreativeTabs`.
- [x] `OBConfig` (leer), `OBNetwork` (leerer Registrar), `OBDataGenerators` (leer).
- [x] JEI-Plugin `OpenBlocksJeiPlugin` als Ersatz für das NEI-Plugin (Hide-Liste noch leer).
- [x] Assets: 16 `.lang` → JSON (Keys `block.openblocks.*`/`item.openblocks.*`), Texturen nach `textures/block`/`item`, Sounds übernommen.
- [x] `CLAUDE.md` + dieser Plan im Repo.
- [x] Verifiziert: `./gradlew build` ok, `runClient` startet, OpenBlocks + JEI in der Mod-Liste.

Verschoben in Welle 1 (erst bei Bedarf anlegen):
- Weitere Registries `OBEntities`, `OBMenus`, `OBComponents`, `OBSounds`, `OBFluids`.
- Datagen-Provider (Blockstates/Modelle, Loot, Rezepte, Tags, Lang).
- Basisklassen (`OBBlockEntity` mit `setChangedAndSync()`, `HorizontalRotatableBlock`).

Hinweis: Der Creative-Tab bleibt unsichtbar, solange er leer ist (Vanilla blendet leere Tabs aus).

## Welle 1 – einfache Blöcke und Items (erster spielbarer Release) — 🚧 in Arbeit
Fortschritt:
- [x] Elevator + Rotating Elevator: je 16 Blöcke pro Farbe (`<farbe>_elevator`, `<farbe>_elevator_rotating`) statt Metadaten/TileEntity, Färben per Rechtsklick mit Farbstoff, Config `elevator.*`, `ElevatorCheckEvent`-API, Payload statt `ElevatorActionEvent`, GameTests. Dazu Grundgerüst: Datagen (Blockstates/Modelle, Loot, Rezepte, Block-Tags, GameTest-Strukturen), `OBSounds`, Client-Einstieg `OpenBlocksClient`.
Ladder, Rope Ladder, Sponge, Scaffolding, Heal, Big Button (Stein/Holz), Target, Path, Elevator + Rotating Elevator, Wrench, Pedometer, Slimalyzer, Sponge-on-a-Stick, Tasty Clay, generische Items (`MetasGeneric` → einzelne Items statt Metadaten).
- Metadaten-Items werden in einzelne Items aufgeteilt (es gibt keine Item-Metadaten mehr).
- Für jeden Block: Blockstate, Modell und Loot-Table per Datagen, Rezept mit Tags.

## Welle 2 – Fluid und XP sowie die bekanntesten Features
- **XP Juice** als `FluidType` + Source/Flowing + Bucket, dazu `FluidXpUtils` portieren.
- Tank (Fluid-Capability, verbundene Tanks, BER fürs Füllbild, Tankinhalt bleibt beim Abbauen über eine Data Component erhalten).
- XP Drain, XP Shower, XP Bottler (erstes Menü + Screen → Vorlage für alle weiteren GUIs).
- Grave (Inventar beim Tod sichern über `LivingDropsEvent`, BER).
- Hang Glider: als Item und Player-Attachment statt Entity; Rendering über `RenderPlayerEvent`, Physik im Player-Tick.
- Sleeping Bag.
- Advancements und Trigger.

## Welle 3 – Maschinen und Inventare
Vacuum Hopper (Seitenkonfiguration), Auto Anvil, Auto Enchantment Table, Block Breaker/Placer, Item Dropper, Donation Station, Drawing Table + Stencil, Fan, Flag, Bear Trap, Sprinkler, Paint Mixer, Paint Can, Paint Brush, Canvas/Canvas Glass, Luggage, Dev Null, Cursor, XP Bucket.
- Hier werden die Code-animierten BERs gebaut, die die Armatures ersetzen.
- Canvas: Farbdaten pro Seite in der BE, Rendering über ein eigenes `BakedModel` mit `ModelData`.

## Welle 4 – komplexe Features
Trophy (+15 Verhaltensweisen), Cannon, Projector + Cartographer + Height Map, Guide/Builder Guide (+ `shapes/`), Imaginary-Blöcke + Glasses, Crane Backpack + Magnet, Golden Egg, Golden Eye (Vanilla-Locate-API), Sky Block, Glyph, Sonic Glasses, Village Highlighter, Enchantments + FlimFlams, Befehle (Brigadier: flimflam, luck, inventory), Brick Drops.

## Welle 5 (optional)
Handbuch über Patchouli, eventuell Integrationen (CC: Tweaked Peripherals), Übersetzungen nachpflegen.

## Vorgehen je Feature (wiederholbares Muster)
1. Alte Klassen in `legacy/` lesen (Block, BE, Renderer, GUI, Config-Werte, Rezept).
2. Neu schreiben mit 1.21-APIs gemäß Ersatz-Tabelle.
3. Datagen-Einträge ergänzen und `./gradlew runData` ausführen.
4. GameTest (wo sinnvoll) und manueller Test im Client.
5. Commit pro Feature.

## Verifikation
- `./gradlew build`: kompiliert ohne Fehler.
- `./gradlew runData`: generierte Ressourcen sind aktuell und eingecheckt.
- `./gradlew runGameTestServer`: automatische Tests für die Logik, z. B.:
  - Tank füllt und leert sich korrekt.
  - Der XP Drain zieht XP ab.
  - Das Grave enthält nach dem Tod das Inventar.
  - Der Aufzug teleportiert.
- `./gradlew runClient`: manuelle Checkliste pro Welle (Rezept im Rezeptbuch, Platzieren/Abbauen/Drops, GUI, Rendering, Multiplayer-Sync über `runServer` + zweiten Client).
- Optional später: GitHub Actions für Build + GameTests.
