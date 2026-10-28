package com.childrenofmu.entity.folk;

import javax.annotation.Nullable;

import net.minecraft.network.chat.Component;
import net.minecraft.util.RandomSource;

/**
 * Names for the folk, so a town is full of somebodies rather than "Frog Folk". So far only the Frog folk have them:
 * fussy, damp, faintly medieval names, with a title for the jobs that come with one ("Sergeant Wilbur Mudgley",
 * "Treasurer Hortense Bogwhistle", "Old Ambrose Reedmantle"). Froglets get a little name ("Lil Pip").
 */
public final class FolkNames {
    private static final String[] FROG_FIRST = {
            "Wilbur", "Mortimer", "Agnes", "Tobias", "Gwendolyn", "Ebenezer", "Hortense", "Bartholomew", "Prudence",
            "Cuthbert", "Mildred", "Ignatius", "Euphemia", "Thaddeus", "Winifred", "Rupert", "Dorcas", "Ambrose",
            "Hepzibah", "Silas", "Nettie", "Percival", "Ophelia", "Jasper", "Bertha", "Gideon", "Mungo", "Petronella",
            "Ribbald", "Tansy", "Horace", "Edwina", "Fergus", "Lavinia", "Osgood", "Maud", "Gerald", "Wendell",
            "Clementine", "Barnaby", "Algernon", "Beatrix", "Cornelius", "Drusilla", "Eustace", "Fenella", "Godfrey",
            "Hilda", "Isadora", "Jethro", "Keziah", "Leopold", "Marjorie", "Norbert", "Olive", "Phineas", "Quentin",
            "Rosalind", "Septimus", "Tabitha", "Ulric", "Violet", "Wilfred", "Yolanda", "Zebedee", "Aldous", "Bunty",
            "Cyril", "Delphine", "Erasmus", "Florian", "Griselda", "Humphrey", "Imelda", "Jocasta", "Lancelot",
            "Matilda", "Neville", "Orville", "Pansy", "Reginald", "Sybil", "Tarquin", "Ursula", "Vernon", "Wolfgang",
            "Augustus", "Belinda", "Clarence", "Dulcie", "Egbert", "Fanny", "Gilbert", "Harriet", "Ivo", "Jemima",
            "Kenelm", "Lettice", "Marmaduke", "Nigella", "Oswald", "Philippa", "Rowena", "Sidney", "Theodora",
            "Bathsheba", "Crispin", "Dymphna", "Elspeth", "Fabian", "Gertrude", "Hamish", "Inigo", "Lucretia",
            "Mabel", "Nathaniel", "Obadiah", "Peregrine", "Roderick", "Sophronia", "Tristram", "Valentine",
            "Wilhelmina", "Barnabus", "Cressida", "Doris", "Enoch", "Felicity", "Grimsby", "Hubert", "Ida", "Josiah",
            "Kitty", "Lionel", "Morwenna", "Nelda", "Ottoline", "Piers", "Rhoda", "Solomon", "Thomasina", "Uriah",
            "Verity", "Walpurga"};
    private static final String[] FROG_LAST = {
            "Puddlesworth", "Mudgley", "Croakwell", "Bogwhistle", "Reedmantle", "Duckweed", "Sloggins", "Marshbottom",
            "Splotch", "Fenwick", "Gloopley", "Hopkirk", "Tadwell", "Bulrush", "Damplow", "Squelchworth", "Mirefoot",
            "Gullet", "Spawnsley", "Bubblethorpe", "Murkins", "Ponderwade", "Newtley", "Dankworth", "Leapingwell",
            "Flybottom", "Soggins", "Wartleby", "Sedgewick", "Pondsby", "Lilyblather", "Moistly", "Bogsworth",
            "Croaker", "Dampier", "Fenbridge", "Glubb", "Hopworthy", "Lilygrove", "Mossbottom", "Mudlark",
            "Newtwhistle", "Oozely", "Pondbury", "Quagmire", "Reedley", "Slimbridge", "Tadpolewaite", "Wetherby",
            "Algaeworth", "Bladderwrack", "Cattail", "Dewlap", "Driftwood", "Eelsworth", "Frogmorton", "Gnatcatcher",
            "Hopscotch", "Jumpwell", "Kelpie", "Leapfrogge", "Marshwallow", "Mistlethwaite", "Mudpuddle", "Nettlebed",
            "Ooze-Perkins", "Peatbog", "Plopworth", "Puddlefoot", "Quillwort", "Rainsby", "Ribbitson", "Rushlight",
            "Sludgely", "Soggibottom", "Splashworth", "Squitch", "Stagnant", "Swampleigh", "Tadcaster", "Toadflax",
            "Treacleby", "Underpond", "Waddleworth", "Waterbutt", "Weedsley", "Wetfoot", "Wiggleby", "Bogswallow",
            "Dribbleton", "Flotsam", "Gloaming", "Lilypond", "Mirkwood", "Mosswick", "Muckleby", "Paddlesworth",
            "Pondscum", "Rushmore", "Sumpter", "Tidewell", "Wallow", "Worts", "Bubbleby", "Croakington", "Dampwick",
            "Gurgle", "Hoppenstall", "Jetsam", "Moatley", "Plish", "Quagley", "Spittlewick", "Thistlewade"};
    private static final String[] FROGLET = {
            "Pip", "Tad", "Dottie", "Splish", "Nim", "Wiggs", "Minnow", "Plop", "Bibby", "Sprat", "Tilly", "Hop",
            "Squib", "Dib", "Wobble", "Bloop", "Pebble", "Twig", "Dewy", "Fizz", "Kip", "Midge", "Puddle", "Nibs",
            "Sprout", "Bubbles", "Skip", "Dinky", "Bean", "Gloop", "Tiddle", "Wren", "Moss", "Noodle", "Pickle",
            "Speck", "Button", "Dot", "Jinx", "Tumble", "Fidget", "Smidge", "Plink", "Ripple", "Lolly", "Sniff",
            "Zip", "Boing", "Bitsy"};
    private static final String[] GUARD_TITLE = {"Constable", "Corporal", "Sergeant", "Lance-Constable", "Watchman",
            "Captain"};
    private static final String[] ELDER_TITLE = {"Old", "Granny", "Grandad", "Venerable", "Nana", "Gaffer", "Auntie"};

    // ---- RCB City: Cats (lazy aristocrats of violence), Rats (short lives, many of them), Bats (jazz) ----
    private static final String[] CAT_FIRST = {
            "Tibbles", "Mogsworth", "Gristle", "Sardine", "Pounce", "Velvet", "Tomkin", "Grimalkin", "Smudge",
            "Ginger", "Mittens", "Bartholomew", "Purrcival", "Claudius", "Snagglepaw", "Nimbus", "Marmalade", "Socks",
            "Tybalt", "Felicity", "Duchess", "Cosmo", "Oswin", "Biscuit", "Muffin", "Shadow", "Ravioli", "Whiskers",
            "Clawdia", "Ambrose", "Tuna", "Sable", "Moggy", "Pudding", "Lucifer", "Saffron", "Bagheera", "Jasper",
            "Kipper", "Fishbone", "Scratch", "Slink", "Morrigan", "Tallow", "Rasputin", "Augustina"};
    private static final String[] CAT_LAST = {
            "Fishbreath", "Ninelives", "Clawsworth", "Purrington", "Gutterpaw", "Mousebane", "Highwhisker",
            "Velvetpaw", "Scratchley", "Tunaby", "Hissop", "Furbottom", "Pouncefoot", "Ratcatcher", "Sardineworth",
            "Milkwater", "Calfsley", "Nap-a-lot", "Sunwarmer", "Rooftopple", "Mogg", "Tailtwist", "Snarlington",
            "Whiskerfield", "Clawhammer", "Cushionsby", "Knocksoff", "Purrloin"};
    private static final String[] COP_TITLE = {"Constable", "Sergeant", "Corporal", "Lance-Constable", "Captain"};
    private static final String[] RAT_NAME = {
            "Nibs", "Skritch", "Gnawley", "Squeak", "Tatters", "Crumb", "Bodger", "Grub", "Scuttle", "Whisk",
            "Mangy", "Dregs", "Pockets", "Smudger", "Twitch", "Rind", "Sniffy", "Patch", "Gristle", "Mouldy", "Scabbers",
            "Nipper", "Fidget", "Ragsy", "Soot", "Pip", "Dribble", "Grubby", "Slops", "Tick", "Wick", "Chitter", "Dodger"};
    private static final String[] BAT_FIRST = {
            "Smokey", "Velma", "Duke", "Satchmo", "Ella", "Bix", "Lulu", "Dizzy", "Fats", "Cootie", "Ruby", "Slim",
            "Mo", "Bunny", "Count", "Bessie", "Sugar", "Thelonious", "Hazel", "Jelly", "Moonie", "Sonny", "Dolores",
            "Lester", "Pepper", "Vic", "Lady", "Echo", "Flit", "Nocturne"};
    private static final String[] BAT_LAST = {
            "Wingsworth", "Eekins", "Duskley", "Flutterby", "Nightingale", "Belfry", "Cavendish", "Hangwell",
            "Moth", "Echolalia", "Batterly", "Swoop", "Leatherwing", "Upsidedown", "Rafters", "Gloaming", "Squeaker",
            "Blackwood", "Vespers", "Murmur"};
    private static final String[] RAT_GRADE = {"the Lesser", "the Younger", "the Quick", "Junior", "the Unlucky",
            "the Spare", "the Last"};
    private static final String[] FIGHT_EPITHET = {"Gutsy", "Mad", "Iron-Jaw", "Killer", "Lucky", "Big", "Two-Tail",
            "One-Ear", "Bonecruncher", "Slasher", "Stumpy", "Mangler", "Butcher", "Scrapper", "Gnasher", "Wild"};

    private FolkNames() {}

    /** A Rat's name: a short name and a number, because there are always more of them. "Nibs the 47th". */
    private static String ratName(RandomSource r) {
        if (r.nextInt(5) == 0) {
            return pick(RAT_NAME, r) + " " + pick(RAT_GRADE, r);
        }
        int n = 2 + r.nextInt(r.nextBoolean() ? 30 : 300);
        String suffix = (n % 100 >= 11 && n % 100 <= 13) ? "th" : switch (n % 10) {
            case 1 -> "st";
            case 2 -> "nd";
            case 3 -> "rd";
            default -> "th";
        };
        return pick(RAT_NAME, r) + " the " + n + suffix;
    }

    /** Names over a nameplate's comfortable length fall back to something shorter. */
    private static final int MAX_NAME = 18;

    private static String fit(String name, String shorter) {
        return name.length() <= MAX_NAME ? name : shorter;
    }

    /** A name for the pit: "Mangler Tibbles", "Gutsy Nibs". */
    public static Component fighterName(FolkRace race, RandomSource r) {
        String name = race == FolkRace.CAT ? pick(CAT_FIRST, r) : pick(RAT_NAME, r);
        return Component.literal(fit(pick(FIGHT_EPITHET, r) + " " + name, name));
    }

    @Nullable
    private static Component cityName(FolkRace race, String variant, RandomSource r) {
        return switch (race) {
            case CAT -> {
                if ("gar".equals(variant)) {
                    yield Component.literal(FolkGar.PLAIN_TITLE);   // gets its city's name once it's in the world
                }
                String first = pick(CAT_FIRST, r);
                String title = "cop".equals(variant) ? pick(COP_TITLE, r) : "guard".equals(variant)
                        ? (r.nextBoolean() ? "Guardsman" : "Watchman") : "archer".equals(variant)
                        ? (r.nextBoolean() ? "Bowman" : "Archer") : null;
                // The law goes by title and first name ("Sergeant Tibbles"); everyone else by their full name.
                yield Component.literal(fit(title != null ? title + " " + first : first + " " + pick(CAT_LAST, r), first));
            }
            case RAT -> Component.literal(fit("accountant".equals(variant) ? "Mr. " + pick(RAT_NAME, r) : ratName(r), pick(RAT_NAME, r)));
            case BAT -> {
                String first = pick(BAT_FIRST, r);
                yield Component.literal(fit(first + " " + pick(BAT_LAST, r), first));
            }
            default -> null;
        };
    }

    /** A fresh name for this folk, or null if its race goes unnamed for now. */
    @Nullable
    public static Component name(FolkRace race, String variant, RandomSource random) {
        if (race == FolkRace.CAT || race == FolkRace.RAT || race == FolkRace.BAT) {
            return cityName(race, variant, random);
        }
        if (race != FolkRace.FROG) {
            return null;
        }
        if ("froglet".equals(variant)) {
            return Component.literal("Lil " + pick(FROGLET, random));
        }
        String name = pick(FROG_FIRST, random) + " " + pick(FROG_LAST, random);
        String title = switch (variant) {
            case "guard" -> pick(GUARD_TITLE, random);
            case "elder" -> pick(ELDER_TITLE, random);
            case "potioneer" -> random.nextBoolean() ? "Apothecary" : null;
            case "chitinier" -> random.nextInt(3) == 0 ? "Ma\u00eetre" : "Chef";
            case "cook" -> random.nextInt(3) == 0 ? "Cook" : null;
            case "schoolmarm" -> random.nextBoolean() ? "Miss" : "Madam";
            default -> null;
        };
        return Component.literal(title == null ? name : title + " " + name);
    }

    /**
     * A name short enough for the trading window (which adds " - Apprentice" and the like after it): the title
     * dropped, and a long surname cut to its initial ("Bartholomew T.").
     */
    public static String shortName(String full) {
        String[] words = full.split(" ");
        java.util.List<String> titles = new java.util.ArrayList<>(java.util.List.of(GUARD_TITLE));
        titles.addAll(java.util.List.of(ELDER_TITLE));
        titles.addAll(java.util.List.of(COP_TITLE));
        titles.addAll(java.util.List.of("Guardsman", "Watchman", "Mr.", "Apothecary", "Chef", "Ma\u00eetre", "Cook", "Treasurer", "Teacher", "Miss", "Madam"));
        int i = words.length >= 3 && titles.contains(words[0]) ? 1 : 0;
        String first = words[i];
        String rest = String.join(" ", java.util.Arrays.copyOfRange(words, i + 1, words.length));
        String name = rest.isEmpty() ? first : first + " " + rest;
        return name.length() <= 19 || rest.isEmpty() ? name : first + " " + rest.charAt(0) + ".";
    }

    /** The Old Traveller, whatever their race: "Old Barnaby". */
    public static Component travellerName(RandomSource random) {
        return Component.literal("Old " + pick(FROG_FIRST, random));
    }

    private static String pick(String[] options, RandomSource random) {
        return options[random.nextInt(options.length)];
    }
}
