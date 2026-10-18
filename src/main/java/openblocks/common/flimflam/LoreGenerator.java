package openblocks.common.flimflam;

import static openblocks.util.Words.alt;
import static openblocks.util.Words.capitalize;
import static openblocks.util.Words.capitalizeFully;
import static openblocks.util.Words.opt;
import static openblocks.util.Words.range;
import static openblocks.util.Words.seq;
import static openblocks.util.Words.sub;
import static openblocks.util.Words.word;

import java.util.Map;
import net.minecraft.util.RandomSource;
import openblocks.util.Words.Generator;

/**
 * Port of legacy {@code LoreGenerator}: silly item histories and hero names for the lore and rename flim flams.
 */
public class LoreGenerator {

	private static final Generator heroGenerator = createHeroGenerator();
	private static final Generator loreGenerator = createLoreGenerator();

	public static String generateLore(RandomSource random, String playerName, String itemName) {
		return generate(loreGenerator, random, Map.of("player", playerName, "item", itemName));
	}

	public static String generateName(RandomSource random) {
		return generate(heroGenerator, random, Map.of());
	}

	private static String generate(Generator generator, RandomSource random, Map<String, String> params) {
		final String result = generator.generate(random, params).replaceAll("\\s+", " ").trim();
		return result.isEmpty()? result : Character.toUpperCase(result.charAt(0)) + result.substring(1);
	}

	private static Generator createHeroGenerator() {
		Generator heroesPrefix = alt("Grunnar", "Hermann", "Sven", "Grarg", "Blarf", "Hans", "Nathan", "Oglaf", "Eric", "Bob", "Banan", "Alaric");
		Generator heroesPostfix = alt("ish", "ilde", "monkeybutt", "son", "shvili", "berg", "bert", "us");
		Generator heroName = word(heroesPrefix, opt(0.6f, heroesPostfix));
		Generator heroOptional = alt("slightly", "sometimes", "mistakenly", "somehow", "part-time");
		Generator heroAdj = alt("insane", "brave", "smelly", "philosophical", "jumping", "toothless", "burning", "heroic", "shy", "narcoleptic", "manly", "girly", "non-euclidian", "euphoric", "misanthropic", "ambivalent", "fictional", "fetishist");
		Generator heroClass = alt("babycrusher", "wrestler", "nitpicker", "barber", "anesthesiologist", "sharpshooter", "plumber", "insurance salesman", "clown", "empiricist", "defenestrator", "visigoth", "nipple twister");
		Generator classicHeroes = capitalizeFully(seq(heroName, "the", seq(opt(0.2f, heroOptional), heroAdj, heroClass, opt(0.2f, word("(lvl. ", range(1, 11), ")")))));

		Generator firstName = alt("Bill", "Juliet", "Nigel", "Steve", "Parsnip", "Cucumber", "Ludwig", "Markus", "Sven", "Clark", "Carl", "Throatwobbler", "Raymond", "Nancy", "Brian", "Brunhilda", "Richard", "Rupert");
		Generator lastNameComponent = alt("Smith", "Weston", "Banana", "Drum", "Forklift", "Ampersand", "Fruitbat", "Fhtagn", "Svenson", "Stein", "Gutenabend", "Mangrove", "Bigglesworth", "Larch", "Semicolon", "Wurst", "Nixon", "Baden", "Priapus");
		Generator lastName = alt(lastNameComponent, word(lastNameComponent, "-", lastNameComponent));
		Generator pseudonym = alt("Duckie", "Nosepicker", "Snort", "Bomber", "Ouch", "Anvil", "Halfslab", "Radiator", "Barbie", "Biggles", "Income Tax", "Not In Face", "Tea Time", "Twerk", "Mutalisk", "Bueno", "Sixpack", "Yellow Snow");
		Generator namePrefix = alt("Dr.", "Rev.", "Ms", "Mr", "Prof.", "Hon.", "Sgt.", "Cmdr.", "Sir", "Lady", "Comrade", "His Magnificence", "Her Holiness", "The Right Honourable");
		Generator middleName = alt("W.", "T.", "F.");
		Generator nameSuffix = alt("M.Sc", "Ph.D", "OBE", "Jr.", "Sr.", "III", "II", "Esq.");

		final Generator middleStuff = seq(opt(0.1f, middleName), opt(0.6f, word("\"", pseudonym, "\"")));
		Generator modernHeroes = seq(opt(0.4f, namePrefix),
				firstName,
				middleStuff,
				opt(0.3f, alt("von", "de", "van", "van de", "de la")),
				lastName,
				opt(0.2f, word(" ", nameSuffix)));

		return alt(classicHeroes, modernHeroes);
	}

	private static Generator createLoreGenerator() {
		Generator adj1 = alt("overpowered", "misspelled", "store-brand", "unsettling", "unremarkable", "sleazy", "boring", "golden", "junky", "ergonomic", "low voltage", "many-angled");
		Generator adj2 = alt("cursed", "legendary", "unique", "penultimate", "awesome", "suboptimal", "mighty", "ridiculously", "slightly");
		Generator adjs = seq(opt(0.7f, adj2), adj1);
		Generator parts = alt("codpiece", "loincloth", "tootbrush", "dental floss", "eggbeater", "rubber chicken with a pulley in the middle", "shovel", "hammoc", "panties", "spatula", "fedora");

		Generator placeAdj = alt("deadly", "dreadful", "boring", "cheap", "backwater", "tax-free", "gluten-free", "dark", "evil", "misunderstood");
		Generator kingdomAdjective = alt("loathing", "meat", "potatoes", "hydrocarbonates", "sweden", "slighlty unpleasant things", "herpaderp", "sobbing", "knitting");
		Generator kingdomish = seq(opt(0.4f, placeAdj), alt("kingdom", "cave", "gorge", "convention", "pit", "bazaar", "land"));
		Generator placeWithAdj = seq(kingdomish, "of", kingdomAdjective, opt(0.2f, seq("and", kingdomAdjective)));
		Generator mountainName = alt("lard", "butter", "rotten eggs", "brimstone", "newts", "doom", "croc", "flipflop");
		Generator mountain = seq(opt(0.6f, placeAdj), "Mt.", mountainName);
		final Generator hardcodedPlaces = alt("dalania", "prussia", "foobaria", "hot dog stand", "abyssinia", "zanzibar", "eastasia", "freedonia", "latveria", "woolloomooloo", "breslau", "uzbekistan", "north korea", "lower intestine", "hyperborea");
		Generator places = capitalizeFully(alt(placeWithAdj, mountain, hardcodedPlaces));

		Generator otherPeople = alt("youtube personalities", "dwarves", "villagers", "elves", "tax collectors", "quality testers", "boring people");
		Generator actor = seq(alt(heroGenerator, seq(otherPeople, "of", places)));

		Generator story = alt("that nobody cares about", seq("that previously belonged to", actor));
		Generator epicLoot = seq(opt(0.5f, adjs), parts, story);

		Generator created = seq(alt("repurposed from", "originally bundled with ", "forged from", "not to be mistaken with"), epicLoot);
		Generator loaned = seq("loaned to", actor);
		Generator forgotten = seq("forgotten in", alt("post office", "loo", "deep hole", "hurry"));
		Generator origin = alt(created, seq(alt("stolen", loaned, "imagined", forgotten, "found behind couch"), "by", heroGenerator));

		Generator itemModifier = alt("replica of");
		Generator itemAction = alt("beating", "bleeding", "winds", "things", word(sub("item", "thing"), "ing"), "cooking", "looting", "scrubing", "backpain", "hernia");
		Generator itemType = alt(sub("item", "gizmo"), alt("gizmo", "thingmajig", "doodad", "tat", "thingie"));
		Generator item = capitalize(seq(opt(0.9f, adjs), itemType, opt(0.9f, seq("of", itemAction))));
		Generator fullItem = seq(opt(0.1f, itemModifier), item, opt(0.05f, seq("(TM)")));

		Generator taunt = alt("wimp", "noob", "git", "fool", "that scoundrel", "scumbag");
		Generator playerGet = seq(alt("stolen from", "found in", "bought in", "dug out in", "smuggled from"), places);
		Generator ownerInfo = seq(playerGet, "by", opt(0.3f, seq(taunt, "named")), sub("player", "Frank"));

		Generator randomItems = alt("bananas", "grapes", "hairpins", "corks", "shuffling", "squash", "penguins");
		Generator organizationSpeciality = alt(randomItems, seq(randomItems, "and", randomItems));

		Generator university = capitalizeFully(seq("university of", alt(hardcodedPlaces, organizationSpeciality)));

		Generator institutish = alt("institute", "council", "committee");
		Generator institute = capitalizeFully(seq(institutish, "of", organizationSpeciality));

		Generator foundationFirst = alt("lick", "pick", "poke", "prod", "smell", "ring", "steal", "hug", "kick", "fwap");
		Generator foundationSecond = alt("fish", "sauce", "leopard", "pick", "smell", "mayonaise", "steal", "grave", "derp");
		Generator foundation = seq(word(capitalizeFully(foundationFirst), "-a-", capitalizeFully(foundationSecond)), "foundation");

		Generator organization = alt(university, foundation, institute);

		Generator restoredInfo = seq("restored by", organization);
		Generator recent = seq("Recently", alt(ownerInfo, restoredInfo));

		Generator extra = alt("$1.99 each", "5 quids in plain wrapper", "Accept no substitues", "Made in China", "Batteries not included", "Patent pending");
		return word(fullItem, opt(0.5f, seq(",", origin)), opt(0.5f, seq(".", recent)), opt(0.2f, seq(".", extra)));
	}
}
