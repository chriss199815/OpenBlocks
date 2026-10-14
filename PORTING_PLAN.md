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

## Welle 2 – Fluid und XP sowie die bekanntesten Features — ✅ erledigt (2026-10-13)
Fortschritt:
- [x] XP Juice + XP Bucket, `FluidXpUtils`.
- [x] Tank: `TankBlockEntity` (Säule füllen/leeren, Ausgleich mit Nachbarn), Data Component `tank_contents`, Fluid-Capability für Block und Item, verbundener Rahmen über `TankModel` (`ModelData`), Fluid per BER, Item per BEWLR, Licht über Blockstate `light`, Config `tanks.*`, GameTests.
- [x] XP Drain: zieht XP von Spielern (max. 4/Tick) und Orbs in jede Fluid-Capability darunter, GameTests.
- [x] XP Shower: mit Redstone aktiv, gibt XP aus dem Block dahinter als `NoFlyXpOrb` aus (folgt Spielern nicht, über `XpOrbTargetingEvent`), Partikel `liquid_spray` (später auch für den Sprinkler), GameTests.
- [x] XP Bottler: erstes Menü + Screen. Vorlage für weitere GUIs: `MachineMenu` (Spielerinventar + Shift-Klick), `TankWidget`, GUI-Hintergrund im Vanilla-Stil (per Skript erzeugt), Fortschrittspfeil aus Vanilla-Sprites, Fortschritt über `ContainerData`, Tank über BE-Update-Paket. Seitenkonfiguration in Welle 3 nachgerüstet (siehe dort).
- [x] Grave: `LivingDropsEvent` (niedrige Priorität) legt die Drops im nächsten Tick in ein Grab nahe dem Todesort, Erdhügel-Variante, Name per BER, Schaufel gräbt aus (selten Gewitter), Skelette/Fledermäuse, Spielregel `openblocks:spawn_graves`, API-Events `GraveDropsEvent`/`GraveSpawnEvent`, Config `graves.*`, GameTests. **Offen:** Inventar-Backups (`storeContents`) kommen mit dem Befehl `ob_inventory` in Welle 4; `minimalPosY`/`maximalPosY` entfallen (Weltgrenzen); anders als im Original wird zuerst ein freier Platz mit festem Boden gesucht, der Erdblock unter dem Grab nur als Notlösung (z. B. über Lava/Void).
- **XP Juice** als `FluidType` + Source/Flowing + Bucket, dazu `FluidXpUtils` portieren.
- Tank (Fluid-Capability, verbundene Tanks, BER fürs Füllbild, Tankinhalt bleibt beim Abbauen über eine Data Component erhalten).
- XP Drain, XP Shower, XP Bottler (erstes Menü + Screen → Vorlage für alle weiteren GUIs).
- Grave (Inventar beim Tod sichern über `LivingDropsEvent`, BER).
- [x] Hang Glider + Glider Wing: synchronisiertes Player-Attachment `hang_glider` (Hand) statt Entity, Physik inkl. Thermik im Player-Tick (beide Seiten), Rendering über `RenderPlayerEvent` (Körper gekippt, Gleiter über dem Spieler) und `RenderLevelStageEvent` (First Person), Item in der Hand ausgeblendet per `BakedModelWrapper`, Config `hangglider.enableThermal`, GameTests.  Akustisches Variometer → Welle 5.
- [x] Sleeping Bag: Rechtsklick schläft am aktuellen Ort (Vanilla-Prüfungen für Nacht/Monster/Dimension nachgebaut), Spawnpunkt bleibt, `CanContinueSleepingEvent` erlaubt Schlafen ohne Bett, synchronisiertes Attachment `sleeping_bag` für das Schlafsack-Modell (`RenderLayer`). Anders als im Original wird er nicht im Brust-Slot getragen, die Rüstung bleibt an. GameTests.
- [x] Advancements und Trigger → verschoben zu ihren Features: „Stack Overflow“ (`dev_null_stacked`) mit Dev Null in Welle 3, „Zoinks!“ (`brick_dropped`) mit Brick Drops in Welle 4. Andere eigene Advancements hatte das Original nicht.

## Welle 3 – Maschinen und Inventare
Vacuum Hopper (Seitenkonfiguration), Auto Anvil, Auto Enchantment Table, Block Breaker/Placer, Item Dropper, Donation Station, Drawing Table + Stencil, Fan, Flag, Bear Trap, Sprinkler, Paint Mixer, Paint Can, Paint Brush, Canvas/Canvas Glass, Luggage, Dev Null (+ Advancement „Stack Overflow“), Cursor. (XP Bucket bereits in Welle 2 erledigt.)
- Hier werden die Code-animierten BERs gebaut, die die Armatures ersetzen.
- Canvas: Farbdaten pro Seite in der BE, Rendering über ein eigenes `BakedModel` mit `ModelData`.

Fortschritt:
- [x] Vacuum Hopper: saugt Items und XP-Kugeln im Umkreis von 3 Blöcken an (nur serverseitig, Bewegung per `hasImpulse` synchronisiert), 10 Slots + XP-Tank (5 Level), gibt alle 10 Ticks an die konfigurierten Seiten aus. Schleichen mit leeren Händen schaltet das Ansaugen ab. Seitenkonfiguration als wiederverwendbare Bausteine: `SideConfigTabs` (aufklappbare Tabs, JEI-Exclusion-Areas) + `SideSelectorWidget` (drehbarer 3D-Block, Klick schaltet Seite), Umschalten über das Vanilla-Paket für Menü-Buttons (`clickMenuButton`). Stutzen per `VacuumHopperModel` (`ModelData`). Capabilities wie im Original nur auf Ausgabeseiten (nur Entnahme). Anders als im Original werden XP-Kugeln nur genommen, wenn sie komplett in den Tank passen, und zusammengelegte Kugeln zählen voll (Access Transformer für `ExperienceOrb.count`). GameTests.
- [x] XP Bottler – Seitenkonfiguration: Tabs Auto-Extract (Glasflaschen), Auto-Drink (XP-Fluid) und Auto-Eject (XP-Flaschen) mit `SideSelectorWidget` und Checkbox im Tab-Kopf (`SideConfigTabs.Tab#withCheckbox`). Capabilities nur auf den konfigurierten Seiten. Anders als im Original sind neue Bottler auf allen Seiten offen und „Auto-Drink“ ist an, damit sie sich ohne Konfiguration wie Vanilla-Maschinen verhalten. Gemeinsamer Helfer `SideBits` für Seiten-Bitmasken. GameTests.
- [x] Item Dropper: wirft bei steigender Redstone-Flanke ein Item gerade aus der Vorderseite (6 Richtungen, Platzierung wie beim Vanilla-Dropper), Geschwindigkeit per Vanilla-Slider in 0,1er-Schritten, optional skaliert mit der Signalstärke. Flanke über den Vanilla-Blockstate `triggered` statt eines Felds in der BE. Einstellungen über `clickMenuButton` und `ContainerData` (auch das Maximum, weil die Common-Config nicht zum Client synchronisiert wird). GUI in Dispenser-Größe statt 300 px breit (per Skript aus dem Vanilla-Dispenser erzeugt). Config `itemdropper.maxItemDropSpeed`. Anders als im Original ohne Fake-Player/`ItemTossEvent`. GameTests.
- [x] Block Breaker + Block Placer: gemeinsame Basis `BlockManipulatorBlock`/`BlockManipulatorBlockEntity` (Blockstate `powered`, Arbeit per Block-Event am Tick-Ende). Anders als im Original (Wiederholung jeden Tick, nur durch `actionLimit` begrenzt) höchstens eine Aktion alle 10 Ticks, solange bestromt in diesem Takt weiter; Config `breaker.cooldown`/`placer.cooldown`. Fake-Player über `FakePlayerFactory`. Breaker baut über `ServerPlayerGameMode.destroyBlock` ab (feuert `BreakEvent`/`BlockDropsEvent`), wählt das erste passende Werkzeug wie die alten `toolProbes` und legt die Drops über `BlockDropsEvent` in das Inventar dahinter. Placer platziert über `useItemOn` (feuert `RightClickBlock`), GUI mit dem Vanilla-Dispenser-Hintergrund. Anders als im Original baut der Breaker keine Flüssigkeiten ab und hat kein Dummy-Inventar mehr. GameTests.
- [x] Auto Anvil: kombiniert Werkzeug und Modifikator über das Vanilla-`AnvilMenu` mit Fake-Player (inkl. `AnvilUpdateEvent`), bezahlt mit XP aus dem Tank (45 Level), alle 40 Ticks ein Versuch. Seitenkonfiguration (Werkzeug, Modifikator, Ausgabe, XP) mit neuen gemeinsamen Bausteinen: `AutoSlotConfig` (Seiten + Auto-Flags, Button-IDs, NBT), `SidedItemHandler` (Ein-/Ausgabe-Slots je Seite) und `MachineTransfer` (Items/Fluid mit Nachbarn tauschen); der XP Bottler nutzt sie jetzt auch, `OBFakePlayer` wird mit Breaker/Placer geteilt. Modell per Datagen aus dem alten JSON, ausgerichtet wie der Vanilla-Amboss; GUI im Layout des Vanilla-Ambosses (per Skript erzeugt). Anders als im Original bleiben eigene Namen erhalten (der Vanilla-Amboss würde sie ohne Namensfeld entfernen), und „Auto-Drink“ ist standardmäßig an. GameTests.
- [x] Auto Enchantment Table: verzaubert wie der Vanilla-Zaubertisch (gleiche Bücherregal-Prüfung über `EnchantingTableBlock.BOOKSHELF_OFFSETS`, Kosten über `EnchantmentHelper`, Tag `in_enchanting_table`, `EnchantmentLevelSetEvent`, `Item.applyEnchantments`), mit wählbarer Option 1–3 (Button mit Vanilla-Level-Icons) und Leistungsgrenze per Slider; bezahlt mit Lapis (`c:gems/lapis`) und XP aus dem Tank (30 Level). Seed pro Tisch wie im Original. Buch per `BlockEntityRenderer`, der den Vanilla-`EnchantTableRenderer` mit einem Vanilla-Buchzustand nutzt; Partikel von Bücherregalen wie beim Vanilla-Tisch. Seitenkonfiguration mit `AutoSlotConfig` (Werkzeug, Lapis, Ausgabe, XP). Anders als im Original nur ein Gegenstand im Werkzeug-Slot (wie Vanilla), „Auto-Drink“ standardmäßig an, Tisch leuchtet (Licht 7) wie der Vanilla-Tisch. GameTests.
- [x] Donation Station: Sparschwein (`piggy.obj` über den NeoForge-OBJ-Loader, Texturen per `#piggy1`/`#piggy2` im generierten Modell) mit einem Slot, Item-Capability auf allen Seiten. Die GUI zeigt Name und Autoren der Mod, die das Item hinzugefügt hat (`getCreatorModId` + `IModInfo`), lange Texte gekürzt mit Tooltip statt halber Schriftgröße. Der Button öffnet den Link wie ein Chat-Link (Bestätigung je nach Option „Chat-Links bestätigen“). Spendenlinks weiter per IMC `donateUrl`; die fest eingebaute Liste alter 1.12-Mods entfällt, stattdessen wird als Rückfall die `displayURL` der Mod angeboten („Mod-Seite besuchen“). GUI-Hintergrund per Skript aus dem Vanilla-Dispenser erzeugt. GameTests.
- [x] Drawing Table + Stencil: Die 15 Muster werden je ein eigenes Item (`<muster>_stencil`, alle heißen „Schablone“, das Muster steht im Tooltip wie bei Vanilla-Bannervorlagen). Texturen erzeugt der Datagen-Provider `OBStencilTextureProvider` aus `stencil.png` (ersetzt den alten `StencilTextureManager` zur Laufzeit). Unprepared Stencil und Sketching Pencil als eigene Items (statt `MetasGeneric`). Der Ausgabe-Slot spiegelt nur die Eingabe (Item-Capability mit beiden Slots); anders als im Original zeigt er wie der Vanilla-Steinsäger eine Schablone und jede Entnahme verbraucht ein Eingabe-Item (Shift-Klick wiederholt), und fertige Schablonen werden als Eingabe angenommen, um sie umzuzeichnen, Musterauswahl über `clickMenuButton`, Muster per BE-Update-Paket zum Client. GUI in Vanilla-Größe mit ◀/▶ neben der Ausgabe und dem Musternamen darunter (Hintergrund per Skript aus Dispenser + Ofenpfeil). **Offen:** Glyphen-Modus kommt mit Glyph (Welle 4). GameTests.
- [x] Paint Mixer + Paint Can + Paint Brush: Farbe und Restmenge als Data Component `paint` (statt NBT). Mixer mischt aus Cyan/Magenta/Gelb/Schwarz (`c:dyes/*`) eine Dose in beliebiger Farbe (CMYK-Verbrauch wie im Original, 300 Ticks), Milcheimer wird zur Dose; GUI mit Farbfeld, Sättigungsbalken, Hex-Feld, Vanilla-Palette, Tintenständen, Farbe per `clickMenuButton` (Farbe als Button-ID). Dose im Mixer per `BlockEntityRenderer` gefärbt und wackelnd statt Armature/ASM. Dose behält Farbe/Menge beim Abbauen (Loot kopiert die Component), Pinsel tunkt in die Dose (24 Anwendungen). Pinsel färbt Vanilla-Blöcke (Wolle, Teppich, Terrakotta, Beton, Trockenbeton, Glas, Scheiben) per Block-Tausch (`DyedBlocks`), anders als im Original immer in die nächstliegende der 16 Farben (das Original verlangte fast exakte Treffer, selbst #000000 blieb wirkungslos). Pinsel-Rezepte mit Farbstoff je Farbe. GameTests.
- [x] Canvas + Canvas Glass + Squeegee: BE speichert den ersetzten Block und je Seite Grundfarbe, Farblagen und aufgelegte Schablone (`CanvasSide`, Codec). Pinsel (Config `canvas.paintbrushReplacesBlocks`) und Schablone machen geeignete Blöcke zum Canvas (`canvas.replaceBlacklist`; keine BEs, Türen/Betten, zusätzlich keine unzerstörbaren oder wassergefüllten Blöcke); nicht volle Blöcke werden Canvas Glass. Der Canvas verhält sich wie der ersetzte Block (Form, Härte, Werkzeug, Drops, Licht, Klang, Pick-Block, `getAppearance`, Redstone). Darstellung: `CanvasModel` zeigt den ersetzten Block, `CanvasRenderer` malt Farbe und Schablonen mit dynamischen 16×16-Texturen darüber (ersetzt den Texturpool, Config `canvasTexturePoolSize` entfällt). Leere Hand dreht die Schablone, Schleichen nimmt sie ab; Squeegee wischt eine Seite (Schleichen: alle) und stellt den Block wieder her, sobald keine Farbe mehr drauf ist. Squeegee-Rezept mit Vanilla-Schwamm, bis der OpenBlocks-Schwamm portiert ist. Redstone-Signal, Leitfähigkeit und Komparatorwert kommen vom ersetzten Block. GameTests (dabei Wetter-Abhängigkeit der Gleiter-/Schlafsack-Tests behoben).
- [x] Fan: bläst Entities in einem Kegel (Öffnung 1,2 rad, Spitze 1,1 Blöcke hinter dem Ventilator) mit linear abnehmender Kraft, skaliert mit dem Redstone-Signal; Config `fan.fanForce`/`fanRange`/`isRedstoneActivated`. Winkel frei wie im Original (Kopfrichtung beim Platzieren, Rechtsklick ±10°, Schleichen dreht zurück), deshalb zeichnet `FanRenderer` Rahmen und Flügel komplett (statt Armature/Eval-Modell); Modelle `fan_frame`/`fan_blades` per Datagen aus den alten JSONs, `fan` (beides) für Item und Partikel. Server bewegt Mobs/Items (Bewegung per `hasImpulse` synchronisiert), der Client den eigenen Spieler (mit der Client-Config, wie im Original und beim Gleiter). Rezept mit Glatter-Stein-Stufe (alte Stufe mit Meta 0). Anders als im Original hält jeder angrenzende Block den Ventilator (auch Wand/Decke, nicht ersetzbare Blöcke), und er fällt ab, sobald keiner mehr da ist (vorher nur fester Block darunter, nur beim Platzieren geprüft), und die Kegelachse liegt genau in der Blockmitte (vorher um einen halben Block versetzt). GameTests.
- [x] Flag: ein Block pro Farbe (`<farbe>_flag`, alle heißen „Flagge“, Farbe per Tint wie beim Aufzug) statt Metadaten + BE. Auf dem Boden zeigt sie in Blickrichtung, Rechtsklick dreht um ±10° (Blockstate `rotation`, 36 Stufen; je Winkel ein Modell mit NeoForge-Root-Transform statt Eval-Modell/Armature), an Wänden schräg abstehend; nicht an Decken. Hält auf festen Blöcken, Zäunen/Mauern und auf anderen Flaggen (stapelbar, mit Flagge in der Hand wird platziert statt gedreht), fällt ab, wenn der Halt verschwindet. Rechtsklick in die Luft bzw. Schleichen + Rechtsklick auf einen Block schaltet die Farbe im Stapel weiter. Rezepte mit Teppich je Farbe + `c:rods/wooden`. Anders als im Original stehen alle 16 Farben im Creative-Tab (vorher nur Lime). GameTests.

## Welle 4 – komplexe Features
Trophy (+15 Verhaltensweisen), Cannon, Projector + Cartographer + Height Map, Guide/Builder Guide (+ `shapes/`), Imaginary-Blöcke + Glasses, Crane Backpack + Magnet, Golden Egg, Golden Eye (Vanilla-Locate-API), Sky Block, Glyph, Sonic Glasses, Village Highlighter, Enchantments + FlimFlams, Befehle (Brigadier: flimflam, luck, inventory; `inventory` inkl. Grab-Backups), Brick Drops (+ Advancement „Zoinks!“).

## Welle 5 (optional)
Handbuch über Patchouli, akustisches Variometer für den Hang Glider, eventuell Integrationen (CC: Tweaked Peripherals), Übersetzungen nachpflegen.

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
