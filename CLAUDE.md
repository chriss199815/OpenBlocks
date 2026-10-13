# OpenBlocks – Port auf NeoForge 1.21.1

Vollständiger Plan: [PORTING_PLAN.md](PORTING_PLAN.md)

## Projektstatus
- Laufender Port von MC 1.12.2 (Forge) auf **NeoForge 1.21.1** (21.1.x), Java 21, ModDevGradle 2, Parchment-Mappings, Gradle 8.14.
- Arbeitsbranch: `port/1.21.1`. `master` = unveränderter 1.12.2-Stand.
- Alter Code liegt in `legacy/` (wird NICHT kompiliert), nur als Referenz. Nie aus `legacy/` importieren.
- `legacy/OpenModsLib` (Submodul) wird **nicht** portiert – Ersatz siehe unten.
- Port in Wellen: 0 Fundament · 1 einfache Blöcke/Items · 2 XP-Fluid/Tank/Grave/Glider · 3 Maschinen/GUIs · 4 komplexe Features · 5 optional (Handbuch, Integrationen).

## Befehle
- `./gradlew build` – kompilieren
- `./gradlew runClient` / `runServer` – Spiel starten
- `./gradlew runData` – Datagen; Ausgabe in `src/generated/resources` wird eingecheckt
- `./gradlew runGameTestServer` – GameTests (ohne Tests bricht der Server mit „No test functions were given!“ ab – das ist erwartet)

## Code-Navigation
- Für Strukturfragen (wer ruft was auf, welche Klassen hängen von welcher OpenModsLib-Klasse ab, alle Abhängigkeiten eines Features in `legacy/`) **zuerst die repo-graph-Tools verwenden**, danach Grep/Read.
- Vor jedem Feature-Port per repo-graph alle Teile der alten Implementierung in `legacy/` ermitteln: Block, TileEntity, Renderer, GUI/Container, RPC-Interface, Config-Werte, Rezepte, Lang-Keys.

## Struktur
- `openblocks.OpenBlocks` – Mod-Einstieg, verdrahtet alle Registries/Listener
- `openblocks.registry.*` – `DeferredRegister`s (`OBBlocks`, `OBItems`, `OBBlockEntities`, `OBCreativeTabs`, später `OBEntities`, `OBMenus`, `OBComponents`, `OBSounds`, `OBFluids`)
- `openblocks.OBConfig` – `ModConfigSpec`
- `openblocks.network.OBNetwork` – Payload-Registrierung
- `openblocks.data.*` – Datagen-Provider
- `openblocks.client.*` – nur Client-Code (Renderer, Screens, Input)
- `openblocks.integration.jei` – JEI-Plugin (Nachfolger des alten NEI-Plugins)
- `openblocks.util` – bei Bedarf aus OpenModsLib übernommene Helfer

## Konventionen
- Registrierung nur über `DeferredRegister` in `openblocks.registry`.
- Keine handgeschriebenen Blockstate-/Modell-/Loot-/Rezept-/Tag-JSONs → immer Datagen.
- Rezepte nutzen `c:`-Tags statt OreDict/konkreter Items, wo möglich.
- Item-Daten nur über Data Components (keine NBT-Tags auf ItemStacks).
- Netzwerk: ein `CustomPacketPayload` + `StreamCodec` pro Aktion, registriert in `OBNetwork`.
- Client-only Code nur unter `openblocks.client`, registriert über Client-Events.
- Config-Werte in `OBConfig` – nur übernehmen, was das aktuelle Feature braucht.
- OpenModsLib-Helfer nur bei Bedarf nach `openblocks.util` kopieren, nicht vorab.
- Metadaten-Items (`MetasGeneric` u. ä.) werden zu einzelnen Items.
- Lang-Keys: `block.openblocks.<id>`, `item.openblocks.<id>` (die Lang-JSONs wurden aus den alten `.lang`-Dateien konvertiert; Keys bei Bedarf an neue IDs anpassen).
- Texturen: `textures/block`, `textures/item` (Singular).
- JEI ist optional (`compileOnly` API, `runtimeOnly` im Dev). Kein harter Import von JEI-Klassen außerhalb von `openblocks.integration.jei`.

## Ersatz OpenModsLib → NeoForge (Kurzform)
sync → `getUpdateTag`/`ContainerData`/Data Components · rpc/network.event → Payloads · gui.component → Vanilla Screen/Menu + eigene Widgets · inventory → `ItemStackHandler` · liquids → `FluidTank` + Capabilities · fakeplayer → `FakePlayerFactory` · Armatures/ASM-Animation → `BlockEntityRenderer` mit Code-Animation · shaders → `RenderLevelStageEvent` · NEI → JEI · infobook, fixers, CC/OpenPeripheral → entfallen.

## Workflow pro Feature
1. Alte Implementierung in `legacy/` analysieren (repo-graph).
2. Mit 1.21-APIs neu schreiben.
3. Datagen ergänzen, `./gradlew runData`.
4. `./gradlew build`, ggf. GameTest, manueller Test in `runClient`.
5. Änderungen zusammenfassen und **auf Freigabe durch den User warten – nie ohne Freigabe committen**.
6. Nach Freigabe: ein Commit pro Feature.

## Lizenz
MIT (OpenMods Team). Copyright-Hinweise in `LICENSE` beibehalten.
