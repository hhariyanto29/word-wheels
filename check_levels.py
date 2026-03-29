#!/usr/bin/env python3
"""Check all levels for contiguous non-word sequences in rows and columns."""

# Common English words (3+ letters) for validation
VALID_WORDS = {
    # 3-letter
    "ACE", "ACT", "ADD", "AGE", "AID", "AIM", "AIR", "ALE", "ALL", "AND",
    "ANT", "APE", "ARC", "ARE", "ARK", "ARM", "ART", "ATE", "AWE",
    "BAD", "BAG", "BAN", "BAR", "BAT", "BED", "BET", "BIG", "BIT", "BOW",
    "BOX", "BOY", "BUD", "BUG", "BUN", "BUS", "BUT", "BUY",
    "CAB", "CAN", "CAP", "CAR", "CAT", "COP", "COT", "COW", "CRY", "CUB",
    "CUP", "CUR", "CUT",
    "DAD", "DAM", "DAY", "DEN", "DEW", "DID", "DIG", "DIM", "DIN", "DIP",
    "DOC", "DOE", "DOG", "DOT", "DRY", "DUB", "DUD", "DUE", "DUG", "DUN",
    "DUO", "DYE",
    "EAR", "EAT", "EEL", "EGG", "ELF", "ELK", "ELM", "EMU", "END", "ERA",
    "ERR", "EVE", "EWE", "EYE",
    "FAN", "FAR", "FAT", "FAX", "FED", "FEE", "FEN", "FEW", "FIG", "FIN",
    "FIR", "FIT", "FIX", "FLY", "FOB", "FOE", "FOG", "FOP", "FOR", "FOX",
    "FRY", "FUN", "FUR",
    "GAB", "GAG", "GAP", "GAS", "GAY", "GEL", "GEM", "GET", "GIG", "GIN",
    "GNU", "GOB", "GOD", "GOT", "GUM", "GUN", "GUT", "GUY", "GYM",
    "HAD", "HAM", "HAS", "HAT", "HAY", "HEN", "HER", "HEW", "HID", "HIM",
    "HIP", "HIS", "HIT", "HOB", "HOG", "HOP", "HOT", "HOW", "HUB", "HUE",
    "HUG", "HUM", "HUT",
    "ICE", "ICY", "ILL", "IMP", "INK", "INN", "ION", "IRE", "IRK", "ITS",
    "IVY",
    "JAB", "JAG", "JAM", "JAR", "JAW", "JAY", "JET", "JIG", "JOB", "JOG",
    "JOT", "JOY", "JUG", "JUT",
    "KEG", "KEN", "KEY", "KID", "KIN", "KIT",
    "LAB", "LAD", "LAG", "LAP", "LAW", "LAY", "LEA", "LED", "LEG", "LET",
    "LID", "LIE", "LIP", "LIT", "LOG", "LOT", "LOW",
    "MAD", "MAN", "MAP", "MAR", "MAT", "MAW", "MAX", "MAY", "MEN", "MET",
    "MID", "MIX", "MOB", "MOD", "MOM", "MOP", "MOW", "MUD", "MUG", "MUM",
    "NAB", "NAG", "NAP", "NET", "NEW", "NIL", "NIT", "NOD", "NOR", "NOT",
    "NOW", "NUB", "NUN", "NUT",
    "OAK", "OAR", "OAT", "ODD", "ODE", "OFF", "OFT", "OIL", "OLD", "ONE",
    "OPT", "ORB", "ORE", "OUR", "OUT", "OWE", "OWL", "OWN",
    "PAD", "PAL", "PAN", "PAP", "PAT", "PAW", "PAY", "PEA", "PEG", "PEN",
    "PEP", "PER", "PET", "PEW", "PIE", "PIG", "PIN", "PIT", "PLY", "POD",
    "POP", "POT", "POW", "PRY", "PUB", "PUG", "PUN", "PUP", "PUS", "PUT",
    "RAG", "RAM", "RAN", "RAP", "RAT", "RAW", "RAY", "RED", "REF", "RIB",
    "RID", "RIG", "RIM", "RIP", "ROB", "ROD", "ROE", "ROT", "ROW", "RUB",
    "RUG", "RUM", "RUN", "RUT", "RYE",
    "SAD", "SAG", "SAP", "SAT", "SAW", "SAY", "SEA", "SET", "SEW", "SHE",
    "SHY", "SIN", "SIP", "SIR", "SIS", "SIT", "SIX", "SKI", "SKY", "SLY",
    "SOB", "SOD", "SON", "SOP", "SOT", "SOW", "SOY", "SPA", "SPY", "STY",
    "SUB", "SUM", "SUN", "SUP",
    "TAB", "TAD", "TAG", "TAN", "TAP", "TAR", "TAT", "TAX", "TEA", "TEN",
    "THE", "TIE", "TIN", "TIP", "TOE", "TON", "TOO", "TOP", "TOT", "TOW",
    "TOY", "TUB", "TUG", "TUN", "TWO",
    "URN", "USE",
    "VAN", "VAT", "VET", "VIA", "VIE", "VOW",
    "WAD", "WAG", "WAR", "WAX", "WAY", "WEB", "WED", "WET", "WHO", "WHY",
    "WIG", "WIN", "WIT", "WOE", "WOK", "WON", "WOO", "WOW",
    "YAK", "YAM", "YAP", "YAW", "YEA", "YES", "YET", "YEW", "YIN",
    "ZAP", "ZEN", "ZIP", "ZIT", "ZOO",
    # 4-letter
    "ABLE", "ACHE", "ACID", "ACRE", "ACTS", "AGED", "AIDE", "ALSO", "ANTS",
    "ARCH", "AREA", "ARMS", "ARMY", "ARTS",
    "BACK", "BAIT", "BAKE", "BALD", "BALE", "BALL", "BAND", "BANE", "BANG",
    "BANK", "BARE", "BARK", "BARN", "BASE", "BASS", "BATH", "BATS",
    "BEAD", "BEAM", "BEAN", "BEAR", "BEAT", "BEDS", "BEEN", "BEER", "BELL",
    "BELT", "BEND", "BENT", "BEST", "BIRD", "BITE", "BLOW", "BLUE", "BLUR",
    "BOAT", "BODY", "BOLD", "BOLT", "BOMB", "BOND", "BONE", "BOOK", "BOOM",
    "BOOT", "BORE", "BORN", "BOSS", "BOTH", "BOUT", "BOWL", "BRED", "BULK",
    "BULL", "BUMP", "BURN", "BURY", "BUSH", "BUST", "BUSY",
    "CAFE", "CAGE", "CAKE", "CALF", "CALL", "CALM", "CAME", "CAMP", "CANE",
    "CAPE", "CARD", "CARE", "CARS", "CART", "CASE", "CASH", "CAST", "CATS",
    "CAVE", "CELL", "CHIN", "CHIP", "CHOP", "CITE", "CITY", "CLAD", "CLAM",
    "CLAN", "CLAP", "CLAY", "CLIP", "CLOT", "CLUB", "CLUE", "COAL", "COAT",
    "CODE", "COIL", "COIN", "COLD", "COLT", "COME", "CONE", "COOK", "COOL",
    "COPE", "COPY", "CORD", "CORE", "CORK", "CORN", "COST", "COSY",
    "COUP", "CRAB", "CREW", "CROP", "CROW", "CURB", "CURE", "CURL", "CUTE",
    "DALE", "DAME", "DAMP", "DARE", "DARK", "DARN", "DART", "DASH", "DATA",
    "DATE", "DAWN", "DAYS", "DEAD", "DEAF", "DEAL", "DEAR", "DEBT", "DECK",
    "DEED", "DEEM", "DEEP", "DEER", "DEFT", "DEMO", "DENT", "DENS", "DENY",
    "DESK", "DIAL", "DICE", "DIED", "DIET", "DIGS", "DIME", "DINE", "DIRE",
    "DIRT", "DISC", "DISH", "DISK", "DOCK", "DOES", "DOME", "DONE", "DOOM",
    "DOOR", "DOSE", "DOTE", "DOTS", "DOWN", "DOZE", "DRAG", "DRAW", "DREW",
    "DRIP", "DROP", "DRUG", "DRUM", "DUAL", "DUCK", "DUEL", "DULL", "DUMB",
    "DUMP", "DUNE", "DUNG", "DUNK", "DUSK", "DUST", "DUTY",
    "EACH", "EARL", "EARN", "EARS", "EASE", "EAST", "EASY", "EATS", "EDGE",
    "EDIT", "ELSE", "EMIT", "ENDS", "EPIC", "EVEN", "EVER", "EVIL", "EXAM",
    "FACE", "FACT", "FADE", "FAIL", "FAIR", "FAKE", "FALL", "FAME", "FANG",
    "FARE", "FARM", "FAST", "FATE", "FAWN", "FEAR", "FEAT", "FEED", "FEEL",
    "FEET", "FELL", "FELT", "FERN", "FEST", "FILE", "FILL", "FILM", "FIND",
    "FINE", "FIRE", "FIRM", "FISH", "FIST", "FLAG", "FLAP", "FLAT", "FLAW",
    "FLEA", "FLED", "FLEW", "FLIP", "FLIT", "FLOG", "FLOP", "FLOW", "FOAM",
    "FOIL", "FOLD", "FOLK", "FOND", "FONT", "FOOD", "FOOL", "FOOT", "FORE",
    "FORK", "FORM", "FORT", "FOUL", "FOUR", "FREE", "FROM", "FUEL", "FULL",
    "FUME", "FUND", "FURY", "FUSE", "FUSS",
    "GAIN", "GAIT", "GALE", "GAME", "GANG", "GAPE", "GARB", "GASH", "GASP",
    "GATE", "GAVE", "GAZE", "GEAR", "GERM", "GIFT", "GILD", "GIST", "GIVE",
    "GLAD", "GLEE", "GLEN", "GLIB", "GLOW", "GLUE", "GLUM", "GNAT", "GNAW",
    "GOAL", "GOAT", "GOES", "GOLD", "GOLF", "GONE", "GOOD", "GORE", "GRAB",
    "GRAM", "GRAY", "GREW", "GREY", "GRID", "GRIM", "GRIN", "GRIP", "GRIT",
    "GROW", "GULF", "GURU",
    "HACK", "HAIL", "HAIR", "HALE", "HALF", "HALL", "HALT", "HAND", "HANG",
    "HARD", "HARE", "HARM", "HARP", "HASH", "HAST", "HATE", "HAUL", "HAVE",
    "HAWK", "HAZE", "HAZY", "HEAD", "HEAL", "HEAP", "HEAR", "HEAT", "HEED",
    "HEEL", "HELD", "HELM", "HELP", "HERB", "HERD", "HERE", "HERO", "HERS",
    "HIDE", "HIGH", "HIKE", "HILL", "HILT", "HIND", "HINT", "HIRE", "HITS",
    "HIVE", "HOAX", "HOLD", "HOLE", "HOLY", "HOME", "HONE", "HOOD", "HOOK",
    "HOOP", "HOPE", "HORN", "HOSE", "HOST", "HOUR", "HOWL", "HUGE", "HULL",
    "HUMP", "HUNG", "HUNT", "HURL", "HURT", "HUSH",
    "IDEA", "INCH", "INTO", "IRON", "ISLE", "ITEM",
    "JACK", "JADE", "JAIL", "JAMB", "JARS", "JAZZ", "JEAN", "JEER", "JERK",
    "JEST", "JOBS", "JOIN", "JOKE", "JOLT", "JOWL", "JUMP", "JUNE", "JUNK",
    "JURY", "JUST", "JUTE",
    "KEEN", "KEEP", "KEPT", "KICK", "KILL", "KIND", "KING", "KISS", "KITE",
    "KNACK", "KNEE", "KNEW", "KNIT", "KNOB", "KNOT", "KNOW",
    "LACE", "LACK", "LAID", "LAIN", "LAIR", "LAKE", "LAMB", "LAME", "LAMP",
    "LAND", "LANE", "LAPS", "LARD", "LARK", "LASH", "LASS", "LAST", "LATE",
    "LAWN", "LAZY", "LEAD", "LEAF", "LEAK", "LEAN", "LEAP", "LEFT", "LEND",
    "LENS", "LENT", "LESS", "LEST", "LEVY", "LIAR", "LICE", "LICK", "LIED",
    "LIEU", "LIFE", "LIFT", "LIKE", "LIMB", "LIME", "LIMP", "LINE", "LINK",
    "LINT", "LION", "LIPS", "LIST", "LIVE", "LOAD", "LOAF", "LOAM", "LOAN",
    "LOCK", "LOFT", "LOGO", "LONE", "LONG", "LOOK", "LOOM", "LOOP", "LORD",
    "LORE", "LOSE", "LOSS", "LOST", "LOTS", "LOUD", "LOVE", "LUCK", "LUMP",
    "LURE", "LURK", "LUSH", "LUST",
    "MACE", "MADE", "MAIL", "MAIN", "MAKE", "MALE", "MALL", "MALT", "MANE",
    "MANY", "MAPS", "MARE", "MARK", "MARS", "MASH", "MASK", "MASS", "MAST",
    "MATE", "MAZE", "MEAD", "MEAL", "MEAN", "MEAT", "MEET", "MELD", "MELT",
    "MEMO", "MEND", "MENU", "MERE", "MESH", "MESS", "MILD", "MILE", "MILK",
    "MILL", "MIME", "MIND", "MINE", "MINT", "MIRE", "MISS", "MIST", "MITE",
    "MITT", "MOAN", "MOAT", "MOCK", "MODE", "MOLD", "MOLE", "MOLT", "MONK",
    "MOOD", "MOON", "MOOR", "MOOT", "MORE", "MORN", "MOSS", "MOST", "MOTH",
    "MOVE", "MUCH", "MUCK", "MUFF", "MULE", "MULL", "MUSE", "MUSH", "MUST",
    "MUTE",
    "NAIL", "NAME", "NAVE", "NEAR", "NEAT", "NECK", "NEED", "NEST", "NETS",
    "NEWS", "NEXT", "NICE", "NINE", "NITS", "NODE", "NONE", "NOON", "NORM",
    "NOSE", "NOTE", "NOUN", "NUDE", "NULL",
    "OATH", "OBEY", "ODES", "ODDS", "OMEN", "OMIT", "ONCE", "ONLY", "ONTO",
    "OPAL", "OPEN", "OPTS", "ORAL", "ORCA", "ORES",
    "PACE", "PACK", "PACT", "PAGE", "PAID", "PAIL", "PAIN", "PAIR", "PALE",
    "PALM", "PANE", "PANG", "PARE", "PARK", "PART", "PASS", "PAST", "PATH",
    "PAVE", "PAWN", "PEAK", "PEAL", "PEAR", "PEAT", "PECK", "PEEL", "PEER",
    "PELT", "PEND", "PENS", "PENT", "PERK", "PEST", "PICK", "PIER", "PIKE",
    "PILE", "PILL", "PINE", "PINK", "PINT", "PIPE", "PITH", "PITS", "PITY",
    "PLAN", "PLAY", "PLEA", "PLOD", "PLOT", "PLOW", "PLOY", "PLUG", "PLUM",
    "PLUS", "POCK", "POEM", "POET", "POLE", "POLL", "POLO", "POND", "PONY",
    "POOL", "POOR", "POPE", "PORE", "PORK", "PORT", "POSE", "POST", "POUR",
    "PRAY", "PREY", "PROD", "PROP", "PROW", "PULL", "PULP", "PUMP", "PUNK",
    "PURE", "PUSH",
    "RACE", "RACK", "RAFT", "RAGE", "RAID", "RAIL", "RAIN", "RAKE", "RAMP",
    "RANG", "RANK", "RANT", "RARE", "RASH", "RATE", "RAVE", "RAYS", "READ",
    "REAL", "REAM", "REAP", "REAR", "REED", "REEF", "REEL", "REIN", "RELY",
    "REND", "RENT", "REST", "RICE", "RICH", "RIDE", "RIFT", "RIGS", "RILE",
    "RILL", "RIME", "RIND", "RING", "RINK", "RIOT", "RISE", "RISK", "RITE",
    "ROAD", "ROAM", "ROAR", "ROBE", "ROCK", "RODE", "RODS", "ROES", "ROLE",
    "ROLL", "ROOF", "ROOM", "ROOT", "ROPE", "ROSE", "ROSY", "ROTE", "ROUT",
    "ROVE", "RUDE", "RUIN", "RULE", "RUMP", "RUNG", "RUSE", "RUSH", "RUST",
    "SAGE", "SAID", "SAIL", "SAKE", "SALE", "SALT", "SAME", "SAND", "SANE",
    "SANG", "SANK", "SASH", "SAVE", "SCAR", "SEAL", "SEAM", "SEAR", "SEAT",
    "SECT", "SEED", "SEEK", "SEEM", "SEEN", "SELF", "SELL", "SEND", "SENT",
    "SHED", "SHIN", "SHIP", "SHOE", "SHOP", "SHOT", "SHOW", "SHUT", "SICK",
    "SIDE", "SIFT", "SIGH", "SIGN", "SILK", "SILL", "SILT", "SINE", "SING",
    "SINK", "SIRE", "SITE", "SIZE", "SKIP", "SKIT", "SLAB", "SLAG", "SLAM",
    "SLAP", "SLAT", "SLED", "SLEW", "SLID", "SLIM", "SLIP", "SLIT", "SLOB",
    "SLOT", "SLOW", "SLUG", "SLUM", "SNAP", "SNIP", "SNOB", "SNUG", "SOAK",
    "SOAP", "SOAR", "SOCK", "SODA", "SOFA", "SOFT", "SOIL", "SOLD", "SOLE",
    "SOME", "SONG", "SOON", "SOOT", "SORE", "SORT", "SOUL", "SOUR", "SPAN",
    "SPAR", "SPIN", "SPIT", "SPOT", "SPUR", "STAB", "STAG", "STAR", "STAY",
    "STEM", "STEP", "STEW", "STIR", "STOP", "STOW", "STUB", "STUD", "STUN",
    "SUIT", "SULK", "SUNG", "SUNK", "SURE", "SURF", "SWAN", "SWAP", "SWAY",
    "SWIM",
    "TABS", "TACK", "TACT", "TAIL", "TAKE", "TALE", "TALK", "TALL", "TAME",
    "TANG", "TANK", "TAPE", "TAPS", "TARN", "TART", "TASK", "TEAR", "TEEM",
    "TELL", "TEMP", "TEND", "TENS", "TENT", "TERM", "TERN", "TEST", "TEXT",
    "THAN", "THAT", "THEM", "THEN", "THEY", "THIN", "THIS", "TICK", "TIDE",
    "TIDY", "TIED", "TIER", "TIES", "TIFF", "TILE", "TILL", "TILT", "TIME",
    "TINE", "TINY", "TIRE", "TOAD", "TOED", "TOIL", "TOLD", "TOLL", "TOMB",
    "TOME", "TONE", "TOOK", "TOOL", "TOPS", "TORE", "TORN", "TORT", "TOSS",
    "TOUR", "TOWN", "TRAP", "TRAY", "TREE", "TREK", "TRIM", "TRIO", "TRIP",
    "TROD", "TROT", "TRUE", "TSAR", "TUBE", "TUCK", "TUFT", "TUNA", "TUNE",
    "TURF", "TURN", "TWIN", "TYPE",
    "UNDO", "UNIT", "UNTO", "UPON", "URGE", "USED", "USER",
    "VAIN", "VALE", "VANE", "VARY", "VASE", "VAST", "VEIL", "VEIN", "VENT",
    "VERB", "VERY", "VEST", "VETO", "VICE", "VIEW", "VILE", "VINE", "VOID",
    "VOLT", "VOTE", "VOWED",
    "WADE", "WAGE", "WAIL", "WAIT", "WAKE", "WALK", "WALL", "WAND", "WANT",
    "WARD", "WARM", "WARN", "WARP", "WART", "WARY", "WASH", "WASP", "WAVE",
    "WAVY", "WAXY", "WAYS", "WEAK", "WEAN", "WEAR", "WEED", "WEEK", "WEEP",
    "WELD", "WELL", "WENT", "WERE", "WEST", "WHAT", "WHEN", "WHIM", "WHIP",
    "WHOM", "WICK", "WIDE", "WIFE", "WILD", "WILL", "WILT", "WILY", "WIMP",
    "WIND", "WINE", "WING", "WINK", "WIPE", "WIRE", "WISE", "WISH", "WISP",
    "WITH", "WITS", "WOKE", "WOLF", "WOMB", "WOOD", "WOOL", "WORD", "WORE",
    "WORK", "WORM", "WORN", "WOVE", "WRAP", "WRIT",
    "YARD", "YARN", "YEAR", "YELL", "YOGA", "YOKE", "YOLK", "YOUR",
    "ZEAL", "ZERO", "ZEST", "ZINC", "ZONE", "ZOOM",
    # 5-letter
    "CARES", "CARED", "CRANE", "CRANES", "DANCE",
    "GRINDS", "GRIND", "GRINS",
    "HASTE", "HEART",
    "INSET",
    "LANCE", "LANES",
    "NEARS", "NERVE",
    "PETAL", "PLANT", "PLANE", "PLANETS",
    "RESTS", "RIND", "RINDS", "RINGS",
    "SCARE", "SPINE", "STORE", "STORED", "STARE", "STEAL",
    "TEARS", "TENTS", "TIRES", "TIRED", "TREAD", "TRAINS", "TRAIN",
    "WARMS",
    # Additional needed words
    "HEAT", "SEAT", "EAST", "EATS",
    "SWAM", "MARS",
    "ARCS", "ACES", "SCAT",
    "DING", "GRIN",
    "LEAN", "PLATE", "SLANT", "PELT",
    "RACED", "CEDAR", "SANE",
    "RODE", "DOSE", "DOER", "DOTES",
    "RETAIN", "SATIRE", "INSERT", "STAIN", "RAIN",
    "ROES", "SORE", "SORT", "TROD", "ODES", "REST", "TORE",
    "NEST", "ANTS", "RISE", "NITS", "SITE", "TIRE",
    "TENT", "LANE", "NETS", "TALE",
    "ACRE", "EARS",
    "WARMS", "ARMS",
}

# Add some more that might appear
VALID_WORDS.update({
    "PETAL", "PETS", "PEST", "PELT", "PENS",
    "SEI",  # a type of whale - borderline
})

LEVELS = [
    # Level 1
    {
        "num": 1, "rows": 5, "cols": 5,
        "words": [
            ("CAST", 1, 0, "A"), ("CAT", 1, 0, "D"), ("SAT", 1, 2, "D"),
        ]
    },
    # Level 2
    {
        "num": 2, "rows": 6, "cols": 6,
        "words": [
            ("SPINE", 1, 0, "A"), ("SIN", 1, 0, "D"), ("NIP", 1, 3, "D"),
            ("PEN", 3, 3, "A"),
        ]
    },
    # Level 3
    {
        "num": 3, "rows": 7, "cols": 7,
        "words": [
            ("HASTE", 1, 0, "A"), ("HATE", 1, 0, "D"), ("SEAT", 1, 2, "D"),
            ("EAT", 4, 0, "A"),
        ]
    },
    # Level 4
    {
        "num": 4, "rows": 7, "cols": 7,
        "words": [
            ("SWARM", 1, 0, "A"), ("SAW", 1, 0, "D"), ("RAM", 1, 3, "D"),
            ("WARM", 3, 0, "A"),
        ]
    },
    # Level 5
    {
        "num": 5, "rows": 7, "cols": 7,
        "words": [
            ("CARES", 1, 0, "A"), ("CARE", 1, 0, "D"), ("SEAR", 1, 4, "D"),
            ("ERA", 4, 0, "A"),
        ]
    },
    # Level 6
    {
        "num": 6, "rows": 7, "cols": 7,
        "words": [
            ("GRINS", 1, 0, "A"), ("GIN", 1, 0, "D"), ("SIR", 1, 4, "D"),
            ("RID", 3, 4, "A"), ("DIG", 3, 6, "D"),
        ]
    },
    # Level 7
    {
        "num": 7, "rows": 8, "cols": 8,
        "words": [
            ("PLANETS", 1, 0, "A"), ("PETAL", 1, 0, "D"), ("NETS", 1, 3, "D"),
            ("SLANT", 1, 6, "D"), ("ANTS", 4, 0, "A"),
        ]
    },
    # Level 8
    {
        "num": 8, "rows": 8, "cols": 8,
        "words": [
            ("CRANES", 1, 0, "A"), ("CARED", 1, 0, "D"), ("NEARS", 1, 3, "D"),
            ("SANE", 1, 5, "D"), ("DENS", 5, 0, "A"), ("END", 5, 1, "D"),
        ]
    },
    # Level 9
    {
        "num": 9, "rows": 8, "cols": 8,
        "words": [
            ("STORED", 1, 0, "A"), ("SORT", 1, 0, "D"), ("REST", 1, 3, "D"),
            ("DOSE", 1, 5, "D"), ("ROES", 3, 0, "A"),
        ]
    },
    # Level 10
    {
        "num": 10, "rows": 9, "cols": 9,
        "words": [
            ("TRAINS", 1, 0, "A"), ("TEARS", 1, 0, "D"), ("INSET", 1, 3, "D"),
            ("SIREN", 1, 5, "D"), ("ANTS", 3, 0, "A"), ("SENT", 5, 0, "A"),
        ]
    },
]


def build_grid(level):
    grid = {}
    for word, row, col, direction in level["words"]:
        for i, ch in enumerate(word):
            if direction == "A":
                r, c = row, col + i
            else:
                r, c = row + i, col
            if (r, c) in grid and grid[(r, c)] != ch:
                print(f"  !! CONFLICT at ({r},{c}): '{grid[(r,c)]}' vs '{ch}' (word '{word}')")
            grid[(r, c)] = ch
    return grid


def get_contiguous_runs(grid, rows, cols):
    """Find all contiguous runs of 2+ filled cells in rows and columns."""
    runs = []

    # Check rows
    for r in range(rows):
        run = []
        for c in range(cols):
            if (r, c) in grid:
                run.append((r, c, grid[(r, c)]))
            else:
                if len(run) >= 2:
                    runs.append(("row", r, run[:]))
                run = []
        if len(run) >= 2:
            runs.append(("row", r, run[:]))

    # Check columns
    for c in range(cols):
        run = []
        for r in range(rows):
            if (r, c) in grid:
                run.append((r, c, grid[(r, c)]))
            else:
                if len(run) >= 2:
                    runs.append(("col", c, run[:]))
                run = []
        if len(run) >= 2:
            runs.append(("col", c, run[:]))

    return runs


def check_level(level):
    num = level["num"]
    rows = level["rows"]
    cols = level["cols"]
    grid = build_grid(level)

    print(f"\n{'='*60}")
    print(f"LEVEL {num} ({rows}x{cols})")
    print(f"{'='*60}")

    # Print grid
    min_r = min(r for r, c in grid)
    max_r = max(r for r, c in grid)
    min_c = min(c for r, c in grid)
    max_c = max(c for r, c in grid)

    print("\nGrid:")
    print("    " + "  ".join(f"{c}" for c in range(min_c, max_c + 1)))
    for r in range(min_r, max_r + 1):
        row_str = f"{r}   "
        for c in range(min_c, max_c + 1):
            if (r, c) in grid:
                row_str += f"{grid[(r,c)]}  "
            else:
                row_str += ".  "
        print(row_str)

    # Check contiguous runs
    runs = get_contiguous_runs(grid, rows, cols)
    issues = []

    # Build set of actual words placed in this level
    placed_words_set = set()
    for word, row, col, direction in level["words"]:
        placed_words_set.add(word)

    print("\nContiguous runs:")
    for direction, idx, run in runs:
        word = "".join(ch for _, _, ch in run)
        is_valid = word in VALID_WORDS
        is_placed = word in placed_words_set
        status = "✓ (placed word)" if is_placed else ("✓ (valid word)" if is_valid else "❌ NOT A WORD")
        label = f"  {direction} {idx}: {word}"
        print(f"{label:30s} {status}")
        if not is_valid and not is_placed:
            issues.append((direction, idx, word))

    if issues:
        print(f"\n  ⚠️  ISSUES FOUND: {len(issues)}")
        for d, i, w in issues:
            print(f"    - {d} {i}: '{w}' is not a valid word")
    else:
        print(f"\n  ✅ All contiguous runs are valid words")

    return issues


if __name__ == "__main__":
    all_issues = {}
    for level in LEVELS:
        issues = check_level(level)
        if issues:
            all_issues[level["num"]] = issues

    print(f"\n{'='*60}")
    print("SUMMARY")
    print(f"{'='*60}")
    if all_issues:
        print(f"Issues found in {len(all_issues)} level(s):")
        for num, issues in all_issues.items():
            for d, i, w in issues:
                print(f"  Level {num}: {d} {i} = '{w}'")
    else:
        print("All levels pass! No non-word contiguous sequences found.")
