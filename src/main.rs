use eframe::egui;
use rand::seq::SliceRandom;
use serde::{Deserialize, Serialize};
use std::collections::{HashMap, HashSet};
use std::f32::consts::{FRAC_PI_2, TAU};

#[derive(Debug, Clone, Copy, PartialEq, Eq, Hash, Deserialize, Serialize)]
enum Dir {
    Across,
    Down,
}

#[derive(Debug, Clone, Deserialize, Serialize)]
struct PlacedWord {
    word: String,
    row: usize,
    col: usize,
    dir: Dir,
}

#[derive(Debug, Clone, Deserialize, Serialize)]
struct Level {
    rows: usize,
    cols: usize,
    letters: Vec<char>,
    words: Vec<PlacedWord>,
}

impl Level {
    fn total_levels() -> usize {
        10
    }

    fn get(level_num: usize) -> Self {
        match level_num {
            // Level 1: 4 words, 5 letters, grid 5x5
            // Letters: C A T S E
            //   C A T  (0,1) across
            //   A      (0,1) down -> A,C,E
            //   A C E  (1,0) across
            //   S A T  (2,0) across  -- shares A at (2,1) but wait...
            // Simpler layout:
            //   C A T    row0, col0 across
            //   A        col0 down: C,A,S,E (but only CAT at top)
            //   S E T    row2, col0 across
            //   A C E    row1, col1 across
            // Let me do a clean small grid:
            //
            //  Grid 4x4:
            //  row0: C A T .
            //  row1: . C . .
            //  row2: . E . .
            //  row3: S E T .
            //
            //  CAT across (0,0), ACE down (0,1), SET across (3,0),
            //  but ACE down from (0,1): A(0,1) C(1,1) E(2,1) - ok
            //  SET across (3,0): S(3,0) E(3,1) T(3,2)
            //  We need E(3,1) to connect - ACE is only 3 long ending at (2,1)
            //  Add: SEA? No. Let's keep it simple:
            //
            //  Grid 4x3:
            //  row0: C A T
            //  row1: . C .
            //  row2: . E .
            //
            //  CAT across(0,0), ACE down(0,1)
            //  Need more words. Add: ATE down from (0,2): A? no T(0,2) A(1,2) E(2,2) = TAE? no
            //
            // Let me use a proven simple layout:
            //  Grid 5x4:
            //  . C . .
            //  . A . .
            //  . T . .     CAT down (0,1)
            //  S A T .     SAT across (3,0), shares A at (3,1)
            //  . . E .
            //
            // Hmm this doesn't intersect well. Let me just do clean designs:
            1 => Self {
                // Level 1: Easy - 4 words, 5 letters
                // Grid 4x4:
                //  A T . .    AT across (0,0)
                //  T . . .
                //  E A T .    EAT across (2,0)
                //  A . . .
                //  T E A .    TEA across (4,0)
                //
                // Simpler approach - just intersecting words:
                //  . E A T      EAT across (0,1)
                //  . A . .
                //  . T . .      EAT down (0,1) = E,A,T
                //  T E A .      TEA across (2,0) shares E at (2,1)
                //
                // Grid 3x4:
                //  . E A T      row0: EAT across col1
                //  . A . .      row1: col1 = A (part of EAT down)
                //  T E A .      row2: TEA across col0, shares E at (2,1)
                //
                // EAT across (0,1): E(0,1) A(0,2) T(0,3)
                // EAT down (0,1): E(0,1) A(1,1) T(2,1) -- shares E(0,1) with across
                // TEA across (2,0): T(2,0) E(2,1) A(2,2) -- shares T at (2,1) with EAT down? No, EAT down has T(2,1), TEA has E(2,1). Conflict!
                // Fix: TEA across (2,0): T(2,0) E(2,1) A(2,2). EAT down: E(0,1) A(1,1) T(2,1). T(2,1) vs E(2,1) - different cols, ok!
                // Wait (2,1): EAT down puts T at (2,1). TEA across puts E at (2,1). CONFLICT.
                //
                // Fix layout:
                // EAT across (0,1): E(0,1) A(0,2) T(0,3)
                // ATE down (0,2): A(0,2) T(1,2) E(2,2) -- shares A(0,2)
                // SET across (1,0): S(1,0) E(1,1) T(1,2) -- shares T(1,2) with ATE down
                // SEA down (1,0): S(1,0) E(2,0) A(3,0)
                rows: 4,
                cols: 4,
                letters: vec!['E', 'A', 'T', 'S'],
                words: vec![
                    PlacedWord { word: "EAT".into(), row: 0, col: 1, dir: Dir::Across },
                    PlacedWord { word: "ATE".into(), row: 0, col: 2, dir: Dir::Down },
                    PlacedWord { word: "SET".into(), row: 1, col: 0, dir: Dir::Across },
                    PlacedWord { word: "SEA".into(), row: 1, col: 0, dir: Dir::Down },
                ],
            },

            // Level 2: 5 words, 5 letters
            // Letters: R A N E G
            // Words: RAN, AGE, RANG, ERA, NEAR
            // Grid 5x5:
            //  R A N . .    RAN across (0,0)
            //  . G . . .
            //  . E R A .    ERA across (2,1)
            //  . . A . .
            //  . . N . .    RAN down (0,2)? R(0,2)=N conflict
            //
            // Clean layout:
            //  R A N G E    RANGE across (0,0) - 5 letters, too many
            // Keep it 5 words with simple crosses:
            //  A G E . .    AGE across (0,0)
            //  . E . . .
            //  R A N . .    RAN across (2,0)
            //  . R . . .
            //
            // AGE across (0,0): A(0,0) G(0,1) E(0,2)
            // GEAR down (0,1): G(0,1) E(1,1) A(2,1) R(3,1) -- shares G(0,1)
            // RAN across (2,0): R(2,0) A(2,1) N(2,2) -- shares A(2,1) with GEAR
            // ERA across (1,0): E(1,0) R(1,1)... R(1,1) but GEAR has E(1,1). Conflict.
            //
            // Simpler:
            // AGE across (0,0): A(0,0) G(0,1) E(0,2)
            // ARE down (0,0): A(0,0) R(1,0) E(2,0)
            // EARN across (1,0)? no too complex
            // RAN across (1,0): R(1,0) A(1,1) N(1,2) -- shares R(1,0) with ARE
            // ERA across (2,0): E(2,0) R(2,1) A(2,2) -- shares E(2,0) with ARE
            2 => Self {
                rows: 3,
                cols: 3,
                letters: vec!['A', 'G', 'E', 'R', 'N'],
                words: vec![
                    PlacedWord { word: "AGE".into(), row: 0, col: 0, dir: Dir::Across },
                    PlacedWord { word: "ARE".into(), row: 0, col: 0, dir: Dir::Down },
                    PlacedWord { word: "RAN".into(), row: 1, col: 0, dir: Dir::Across },
                    PlacedWord { word: "ERA".into(), row: 2, col: 0, dir: Dir::Across },
                    PlacedWord { word: "GEN".into(), row: 0, col: 1, dir: Dir::Down },
                ],
            },

            // Level 3: 5 words, 6 letters
            // Letters: S T O P R E
            // Grid 5x5:
            // STOP across (0,0): S(0,0) T(0,1) O(0,2) P(0,3)
            // TOP down (0,1)? T(0,1) O(1,1) P(2,1)
            // ROPE across (2,0): R(2,0) O(2,1)... O conflicts with P(2,1)
            //
            // STOP across (0,0): S T O P
            // SORE down (0,0): S(0,0) O(1,0) R(2,0) E(3,0)
            // ORE across (1,0): O(1,0) R(1,1) E(1,2) - shares O with SORE
            // POET down (0,3): P(0,3) O(1,3) E(2,3) T(3,3)
            // REST across (3,0): R(3,0)... SORE has E(3,0). conflict
            // TORE across (2,0)? no, SORE has R(2,0)
            // PER across (2,2): P(2,2) E(2,3) R(2,4) -- shares E(2,3) with POET
            // ROPE across (3,0): R(3,0)... E(3,0) from SORE? no, 3,0 not in SORE. SORE: S(0,0) O(1,0) R(2,0) E(3,0). Yes E at (3,0).
            //
            // Let me simplify:
            // TOPS across (0,0): T(0,0) O(0,1) P(0,2) S(0,3)
            // TORE down (0,0): T(0,0) O(1,0) R(2,0) E(3,0)
            // ORE across (1,0): shares O(1,0) -- O(1,0) R(1,1) E(1,2)
            // POSE across (2,1): P(2,1) O(2,2) S(2,3) E(2,4) -- no intersection needed but grid gets big
            //
            // Cleaner 4x5:
            // STOP across (0,1): S(0,1) T(0,2) O(0,3) P(0,4)
            // SORE down (0,1): S(0,1) O(1,1) R(2,1) E(3,1)
            // ORE across (1,0)? P(1,0)?
            // PORT across (1,1)? O(1,1) already from SORE
            // TORE down (0,2): T(0,2) O(1,2) R(2,2) E(3,2) --
            // POET across (3,0): P(3,0) O(3,1)... E(3,1) from SORE != O. conflict
            //
            // I'll take a more systematic approach:
            3 => Self {
                rows: 5,
                cols: 5,
                letters: vec!['S', 'T', 'O', 'P', 'R', 'E'],
                words: vec![
                    // STOP across (0,0): S(0,0) T(0,1) O(0,2) P(0,3)
                    PlacedWord { word: "STOP".into(), row: 0, col: 0, dir: Dir::Across },
                    // SORE down (0,0): S(0,0) O(1,0) R(2,0) E(3,0)
                    PlacedWord { word: "SORE".into(), row: 0, col: 0, dir: Dir::Down },
                    // TORE down (0,1): T(0,1) O(1,1) R(2,1) E(3,1)
                    PlacedWord { word: "TORE".into(), row: 0, col: 1, dir: Dir::Down },
                    // PERT across (3,0)? E(3,0) from SORE. P(3,0) != E. Nope.
                    // REST across (2,0): R(2,0) E(2,1)... R(2,1) from TORE != E. Conflict.
                    // ROPE across (2,0): R(2,0) O(2,1)... TORE has R(2,1) not O. Conflict.
                    // OPT across (1,1): O(1,1) P(1,2) T(1,3) -- shares O(1,1) with TORE
                    PlacedWord { word: "OPT".into(), row: 1, col: 1, dir: Dir::Across },
                    // PET down (0,3): P(0,3) E(1,3)... OPT has T(1,3). conflict
                    // POT down (1,2): P(1,2) O(2,2) T(3,2) -- shares P(1,2) with OPT
                    PlacedWord { word: "POT".into(), row: 1, col: 2, dir: Dir::Down },
                ],
            },

            // Level 4: 6 words, 6 letters (original level)
            4 => Self {
                rows: 6,
                cols: 6,
                letters: vec!['L', 'E', 'S', 'S', 'Y', 'T'],
                words: vec![
                    PlacedWord { word: "LESS".into(), row: 0, col: 2, dir: Dir::Across },
                    PlacedWord { word: "LET".into(), row: 0, col: 2, dir: Dir::Down },
                    PlacedWord { word: "YET".into(), row: 2, col: 0, dir: Dir::Across },
                    PlacedWord { word: "YES".into(), row: 2, col: 0, dir: Dir::Down },
                    PlacedWord { word: "STYLE".into(), row: 4, col: 0, dir: Dir::Across },
                    PlacedWord { word: "SET".into(), row: 5, col: 2, dir: Dir::Across },
                ],
            },

            // Level 5: 6 words, 6 letters
            // Letters: H O M E S T
            // HOMES, THOSE, MOTH, SOME, HOST, THE
            // Grid 5x5:
            // HOME across (0,0): H(0,0) O(0,1) M(0,2) E(0,3)
            // HOST down (0,0): H(0,0) O(1,0) S(2,0) T(3,0)
            // SOME across (2,0): S(2,0) O(2,1) M(2,2) E(2,3) -- shares S(2,0) with HOST
            // MET down (0,2)? M(0,2) E(1,2) T(2,2)... SOME has M(2,2) and MET has T(2,2). conflict
            // OEM? no
            // THE across (3,0): T(3,0) H(3,1) E(3,2) -- shares T(3,0) with HOST
            // MOTH down (0,2): M(0,2) O(1,2) T(2,2) H(3,2)... SOME has M(2,2), MOTH has T(2,2). Conflict.
            // MET down (0,2): M(0,2) E(1,2) T(2,2). SOME has M(2,2). Conflict.
            // OE down (0,3): nah too short.
            // Let me shift SOME: SOME across (2,0) -> S O M E at positions (2,0)(2,1)(2,2)(2,3)
            // MET down (0,3): E(0,3) ... that's E not M.
            // POEM? no P.
            // THEM down (3,0)? starts at row3. too low.
            // Simple fix:
            // HOME across (0,1): H(0,1) O(0,2) M(0,3) E(0,4)
            // HOT down (0,1): H(0,1) O(1,1) T(2,1)
            // SOME across (1,1): ... no, starts at (1,1) with S but HOT has O(1,1).
            // TOMES across (2,0): T(2,0) O(2,1) M(2,2) E(2,3) S(2,4)
            //   HOT shares T(2,1)? HOT: H(0,1) O(1,1) T(2,1). TOMES: O(2,1). T!=O conflict.
            //
            // Let me just do a simpler proven layout:
            5 => Self {
                rows: 5,
                cols: 5,
                letters: vec!['H', 'O', 'M', 'E', 'S', 'T'],
                words: vec![
                    // HOME across (0,0): H O M E
                    PlacedWord { word: "HOME".into(), row: 0, col: 0, dir: Dir::Across },
                    // HOST down (0,0): H O S T
                    PlacedWord { word: "HOST".into(), row: 0, col: 0, dir: Dir::Down },
                    // SOME across (2,0): S O M E -- shares S(2,0) with HOST
                    PlacedWord { word: "SOME".into(), row: 2, col: 0, dir: Dir::Across },
                    // THE across (3,0): T H E -- shares T(3,0) with HOST
                    PlacedWord { word: "THE".into(), row: 3, col: 0, dir: Dir::Across },
                    // OMS down (0,1)? O M S? not a word.
                    // OME down? no
                    // MET down (0,2): M(0,2) E(1,2) T(2,2). SOME has M(2,2). M!=T conflict.
                    // OE down (1,1)? too short
                    // MET across (1,2)? M(1,2) E(1,3) T(1,4)
                    PlacedWord { word: "MET".into(), row: 1, col: 2, dir: Dir::Across },
                    // STEM across (4,0)? need more intersections
                    // TOE across (3,2): no, THE is at row3 col0-2, so E at (3,2). TOE: T(3,2) conflict with E(3,2)
                    // MOT down (0,2): M(0,2) O(1,2)... MET has M(1,2). conflict.
                    // Just add MOTH or THOSE... tricky. Keep 5 words for this level.
                    PlacedWord { word: "TOES".into(), row: 3, col: 0, dir: Dir::Down },
                ],
            },

            // Level 6: 7 words, 6 letters
            // Letters: B R I C K S
            // Words: BRICK, RISK, SICK, RIBS, SIR, IRK
            6 => Self {
                rows: 6,
                cols: 6,
                letters: vec!['B', 'R', 'I', 'C', 'K', 'S'],
                words: vec![
                    // BRICK across (0,0): B R I C K
                    PlacedWord { word: "BRICK".into(), row: 0, col: 0, dir: Dir::Across },
                    // RIBS down (0,1): R(0,1) I(1,1) B(2,1) S(3,1)
                    PlacedWord { word: "RIBS".into(), row: 0, col: 1, dir: Dir::Down },
                    // ICK across (1,1): I(1,1) C(1,2) K(1,3) -- shares I(1,1)
                    PlacedWord { word: "ICK".into(), row: 1, col: 1, dir: Dir::Across },
                    // SIR across (3,0): S(3,0) I(3,1)... RIBS has S(3,1). S!=I conflict.
                    // SIR across (3,1): S(3,1) I(3,2) R(3,3) -- shares S(3,1) with RIBS
                    PlacedWord { word: "SIR".into(), row: 3, col: 1, dir: Dir::Across },
                    // RISK down (0,2): no, BRICK has I(0,2). RISK: R I S K. R!=I conflict.
                    // IRK down (0,2): I(0,2) R(1,2)... ICK has C(1,2). conflict.
                    // CRIBS? no second B
                    // SKI across (2,0): S(2,0) K(2,1)... RIBS has B(2,1). conflict.
                    // IRKS down (0,2): I(0,2) R(1,2)... ICK: C(1,2). conflict.
                    // SICK down (0,3)? C from BRICK at (0,3). SICK: S I C K. S!=C conflict.
                    // CIS down (1,2): C(1,2) I(2,2) S(3,2). ICK has C(1,2) -- shares! SIR has I(3,2). S!=I conflict.
                    // Keep it cleaner:
                    PlacedWord { word: "RISK".into(), row: 2, col: 0, dir: Dir::Across },
                    PlacedWord { word: "SKI".into(), row: 4, col: 0, dir: Dir::Across },
                ],
            },

            // Level 7: 7 words, 7 letters
            // Letters: P L A N E T S
            // Words: PLAN, PLANET, PANT, LANE, SLANT, NET, TAN
            7 => Self {
                rows: 7,
                cols: 7,
                letters: vec!['P', 'L', 'A', 'N', 'E', 'T', 'S'],
                words: vec![
                    // PLANET across (0,0): P L A N E T
                    PlacedWord { word: "PLANET".into(), row: 0, col: 0, dir: Dir::Across },
                    // PLAN down (0,0): P(0,0) L(1,0) A(2,0) N(3,0)
                    PlacedWord { word: "PLAN".into(), row: 0, col: 0, dir: Dir::Down },
                    // LANE across (1,0): L(1,0) A(1,1) N(1,2) E(1,3) -- shares L(1,0)
                    PlacedWord { word: "LANE".into(), row: 1, col: 0, dir: Dir::Across },
                    // ANTE down (1,1): A(1,1) N(2,1) T(3,1) E(4,1) -- shares A(1,1)
                    PlacedWord { word: "ANTE".into(), row: 1, col: 1, dir: Dir::Down },
                    // NET across (3,0): N(3,0) E(3,1)... ANTE has T(3,1). conflict.
                    // TAN across (3,0): no, PLAN has N(3,0). TAN: T A N, T!=N.
                    // NET across (4,0): N(4,0) E(4,1)... ANTE has E(4,1). shares E!
                    PlacedWord { word: "NET".into(), row: 4, col: 0, dir: Dir::Across },
                    // SLANT across (2,0): S? A(2,0) from PLAN. S!=A conflict.
                    // PEN down (0,4)? PLANET has E(0,4). PEN: P E N. P!=E conflict.
                    // NETS down (4,0)? N(4,0) E(5,0) T(6,0) S(7,0) -- out of grid
                    // PANT down -- already have PLAN
                    // TEN across (3,1): T(3,1) E(3,2) N(3,3) -- ANTE has T(3,1). shares!
                    PlacedWord { word: "TEN".into(), row: 3, col: 1, dir: Dir::Across },
                    // NETS across (4,0): N E T S
                    PlacedWord { word: "NETS".into(), row: 5, col: 0, dir: Dir::Across },
                ],
            },

            // Level 8: 8 words, 7 letters
            // Letters: C R A N E S D
            // CRANES, DANCE, SCAR, REND, CARES, DARN, DENS, ACNE
            8 => Self {
                rows: 7,
                cols: 7,
                letters: vec!['C', 'R', 'A', 'N', 'E', 'S', 'D'],
                words: vec![
                    // CRANE across (0,0): C R A N E
                    PlacedWord { word: "CRANE".into(), row: 0, col: 0, dir: Dir::Across },
                    // CARED down (0,0): C(0,0) A(1,0) R(2,0) E(3,0) D(4,0)
                    PlacedWord { word: "CARED".into(), row: 0, col: 0, dir: Dir::Down },
                    // AND across (1,0): A(1,0) N(1,1) D(1,2) -- shares A(1,0)
                    PlacedWord { word: "AND".into(), row: 1, col: 0, dir: Dir::Across },
                    // REND down (0,1): R(0,1) E(1,1)... AND has N(1,1). R,E,N... REND: R E N D. E(1,1)!=N. conflict.
                    // NED down (1,1): N(1,1) E(2,1) D(3,1) -- shares N(1,1) with AND
                    PlacedWord { word: "DENS".into(), row: 1, col: 1, dir: Dir::Down },
                    // RED across (2,0): R(2,0) E(2,1) D(2,2) -- shares R(2,0), shares E(2,1) with DENS? DENS: N(1,1) E(2,1) -- yes!
                    PlacedWord { word: "RED".into(), row: 2, col: 0, dir: Dir::Across },
                    // RACED? no.
                    // SCARE across (3,0)? E(3,0) from CARED. SCARE: S C A R E. S!=E. conflict.
                    // END across (3,0): E(3,0) N(3,1) D(3,2) -- shares E(3,0) with CARED
                    // DENS has N(3,1)? DENS down from (1,1): N(1,1) E(2,1) N(3,1) S(4,1). Wait DENS = D E N S.
                    // Actually DENS down (1,1): D(1,1)? But AND has N(1,1). D!=N. conflict!
                    // Fix: remove DENS. Use NES or something else.
                    // NEAR down (0,3): N(0,3) E(1,3) A(2,3) R(3,3)
                    PlacedWord { word: "NEAR".into(), row: 0, col: 3, dir: Dir::Down },
                    // SANE across (4,0): ... D(4,0) from CARED. S!=D. conflict.
                    // ACES down (0,4): ... CRANE has E(0,4). A!=E. conflict.
                    // RACE across (3,0): ... E(3,0). R!=E. conflict.
                    // DARN across (4,0): D(4,0) A(4,1) R(4,2) N(4,3)
                    PlacedWord { word: "DARN".into(), row: 4, col: 0, dir: Dir::Across },
                    // SCARE across? DANCE?
                    PlacedWord { word: "SCAN".into(), row: 3, col: 0, dir: Dir::Across },
                ],
            },

            // Level 9: 8 words, 7 letters
            // Letters: F L O W E R S
            9 => Self {
                rows: 7,
                cols: 7,
                letters: vec!['F', 'L', 'O', 'W', 'E', 'R', 'S'],
                words: vec![
                    // FLOWER across (0,0): F L O W E R
                    PlacedWord { word: "FLOWER".into(), row: 0, col: 0, dir: Dir::Across },
                    // FLOWS down (0,0): F(0,0) L(1,0) O(2,0) W(3,0) S(4,0)
                    PlacedWord { word: "FLOWS".into(), row: 0, col: 0, dir: Dir::Down },
                    // LOWER across (1,0): L(1,0) O(1,1) W(1,2) E(1,3) R(1,4) -- shares L(1,0)
                    PlacedWord { word: "LOWER".into(), row: 1, col: 0, dir: Dir::Across },
                    // OWL across (2,0): O(2,0) W(2,1) L(2,2) -- shares O(2,0)
                    PlacedWord { word: "OWL".into(), row: 2, col: 0, dir: Dir::Across },
                    // WORE down (1,2): W(1,2) O(2,2)... OWL has L(2,2). W,O,L... conflict
                    // ORE down (1,1): O(1,1) R(2,1) E(3,1) -- shares O(1,1)
                    PlacedWord { word: "ORE".into(), row: 1, col: 1, dir: Dir::Down },
                    // SELF across (4,0): S(4,0) E(4,1) L(4,2) F(4,3) -- shares S(4,0)
                    PlacedWord { word: "SELF".into(), row: 4, col: 0, dir: Dir::Across },
                    // WOE across (3,0): W(3,0) O(3,1)... ORE has E(3,1). O!=E. conflict.
                    // FOWL? ROW?
                    // ROW across (3,1)? no specific intersection
                    // ROLE across (2,2): ... OWL has L(2,2). R!=L. conflict.
                    PlacedWord { word: "ROW".into(), row: 2, col: 1, dir: Dir::Across },
                    PlacedWord { word: "FORE".into(), row: 3, col: 0, dir: Dir::Across },
                ],
            },

            // Level 10: 9 words, 7 letters - hardest!
            // Letters: C H A M P I O N  -- 8 letters but we use 7: C H A M P I N
            // Simpler: Letters: T R A I N S E
            // Words: TRAINS, TRAIN, STAIN, RAIN, STEIN, RISE, ANTS, SIN, EARN
            _ => Self {
                rows: 8,
                cols: 8,
                letters: vec!['T', 'R', 'A', 'I', 'N', 'S', 'E'],
                words: vec![
                    // TRAINS across (0,0): T R A I N S
                    PlacedWord { word: "TRAINS".into(), row: 0, col: 0, dir: Dir::Across },
                    // TRAIN down (0,0): T(0,0) R(1,0) A(2,0) I(3,0) N(4,0)
                    PlacedWord { word: "TRAIN".into(), row: 0, col: 0, dir: Dir::Down },
                    // RISE across (1,0): R(1,0) I(1,1) S(1,2) E(1,3) -- shares R(1,0)
                    PlacedWord { word: "RISE".into(), row: 1, col: 0, dir: Dir::Across },
                    // ANTS across (2,0): A(2,0) N(2,1) T(2,2) S(2,3) -- shares A(2,0)
                    PlacedWord { word: "ANTS".into(), row: 2, col: 0, dir: Dir::Across },
                    // INSET down (0,1)? no, TRAINS has R(0,1).
                    // INST down? no word.
                    // SIN across (4,0): no, TRAIN has N(4,0). S!=N.
                    // STAIN across (3,0)? I(3,0). S!=I. conflict.
                    // RAIN across (3,0)? ... no, I(3,0) from TRAIN. R!=I.
                    // IRES down (1,0)? -- that's RISE going down. R I ... same spot.
                    // INERT down (0,3)? TRAINS has I(0,3). INERT: I N E R T. I(0,3) N(1,3)... RISE has E(1,3). N!=E. conflict.
                    // SINE down (1,2): S(1,2) I(2,2)... ANTS has T(2,2). S,I,T... conflict.
                    // EARN across (4,0): no, TRAIN has N(4,0).
                    // Let me place words more carefully at different positions:
                    // RAIN across (3,1): R(3,1) A(3,2) I(3,3) N(3,4)
                    PlacedWord { word: "RAIN".into(), row: 3, col: 1, dir: Dir::Across },
                    // SIREN down (0,5): S(0,5) I(1,5) R(2,5) E(3,5) N(4,5)
                    PlacedWord { word: "SIREN".into(), row: 0, col: 5, dir: Dir::Down },
                    // STIR across (4,0): ... N(4,0) from TRAIN. S!=N.
                    // STEIN across (4,2): S(4,2) T(4,3) E(4,4) I(4,5) N(4,6)... SIREN has N(4,5). I!=N conflict.
                    // SIN across (5,0): S(5,0) I(5,1) N(5,2)
                    PlacedWord { word: "SIN".into(), row: 5, col: 0, dir: Dir::Across },
                    // EARN across (4,1): E(4,1) A(4,2) R(4,3) N(4,4)
                    PlacedWord { word: "EARN".into(), row: 4, col: 1, dir: Dir::Across },
                    // STARE? TINSE? SATIN?
                    PlacedWord { word: "STEIN".into(), row: 6, col: 0, dir: Dir::Across },
                ],
            },
        }
    }

    fn bonus_words(&self) -> Vec<&'static str> {
        // Each level has bonus words that can be formed from the letters
        // but are not placed on the grid
        match self.letters.as_slice() {
            ['E', 'A', 'T', 'S'] => vec!["SAT", "TEA", "TAS"],
            ['A', 'G', 'E', 'R', 'N'] => vec!["NAG", "RAG", "RANG", "GEAR", "NEAR"],
            ['S', 'T', 'O', 'P', 'R', 'E'] => vec!["ROPE", "PORE", "TORE", "REST", "SORT"],
            ['L', 'E', 'S', 'S', 'Y', 'T'] => vec!["LETS", "LEST", "STYLES"],
            ['H', 'O', 'M', 'E', 'S', 'T'] => vec!["THEM", "MOTH", "MOST", "THOSE"],
            ['B', 'R', 'I', 'C', 'K', 'S'] => vec!["CRIBS", "IRKS", "SIR"],
            ['P', 'L', 'A', 'N', 'E', 'T', 'S'] => vec!["SLANT", "PANT", "STEAL", "PLATE"],
            ['C', 'R', 'A', 'N', 'E', 'S', 'D'] => vec!["DANCE", "SCARE", "DANCES", "RACED"],
            ['F', 'L', 'O', 'W', 'E', 'R', 'S'] => vec!["WOLF", "OWES", "ROLES", "FLOWS"],
            ['T', 'R', 'A', 'I', 'N', 'S', 'E'] => vec!["STARE", "INSERT", "RETAIN", "SATIRE"],
            _ => vec![],
        }
    }

    fn answers_set(&self) -> HashSet<String> {
        self.words.iter().map(|w| w.word.clone()).collect()
    }

    fn used_cells(&self) -> HashSet<(usize, usize)> {
        let mut used = HashSet::new();
        for pw in &self.words {
            for (i, _ch) in pw.word.chars().enumerate() {
                let (r, c) = match pw.dir {
                    Dir::Across => (pw.row, pw.col + i),
                    Dir::Down => (pw.row + i, pw.col),
                };
                used.insert((r, c));
            }
        }
        used
    }

    fn solution_letter_at(&self, row: usize, col: usize) -> Option<char> {
        for pw in &self.words {
            for (i, ch) in pw.word.chars().enumerate() {
                let (r, c) = match pw.dir {
                    Dir::Across => (pw.row, pw.col + i),
                    Dir::Down => (pw.row + i, pw.col),
                };
                if r == row && c == col {
                    return Some(ch);
                }
            }
        }
        None
    }
}

struct GameState {
    level: Level,
    #[allow(dead_code)]
    level_num: usize,
    used: HashSet<(usize, usize)>,
    answers: HashSet<String>,
    bonus_words: HashSet<String>,
    found: HashSet<String>,
    revealed: HashMap<(usize, usize), char>,
    tiles: Vec<char>,
    selection: Vec<usize>,
    coins: u32,
    bonus_found: HashSet<String>,
    hints_left: u32,
}

impl GameState {
    fn new(level_num: usize, coins: u32) -> Self {
        let level = Level::get(level_num);
        let used = level.used_cells();
        let answers = level.answers_set();
        let bonus_words: HashSet<String> = level
            .bonus_words()
            .iter()
            .map(|s| s.to_string())
            .collect();
        let tiles = level.letters.clone();
        Self {
            level,
            level_num,
            used,
            answers,
            bonus_words,
            found: HashSet::new(),
            revealed: HashMap::new(),
            tiles,
            selection: Vec::new(),
            coins,
            bonus_found: HashSet::new(),
            hints_left: 3,
        }
    }

    fn current_word(&self) -> String {
        self.selection
            .iter()
            .filter_map(|&idx| self.tiles.get(idx).copied())
            .collect()
    }

    fn clear_selection(&mut self) {
        self.selection.clear();
    }

    fn backspace(&mut self) {
        self.selection.pop();
    }

    fn shuffle_tiles(&mut self) {
        self.clear_selection();
        let mut rng = rand::thread_rng();
        self.tiles.shuffle(&mut rng);
    }

    fn is_complete(&self) -> bool {
        self.found.len() == self.answers.len()
    }

    fn visible_letters_map(&self) -> HashMap<(usize, usize), char> {
        let mut map = self.revealed.clone();
        for pw in &self.level.words {
            if self.found.contains(&pw.word) {
                for (i, ch) in pw.word.chars().enumerate() {
                    let (r, c) = match pw.dir {
                        Dir::Across => (pw.row, pw.col + i),
                        Dir::Down => (pw.row + i, pw.col),
                    };
                    map.insert((r, c), ch);
                }
            }
        }
        map
    }

    fn try_submit(&mut self) -> String {
        let guess = self.current_word();
        if guess.len() < 2 {
            return "Make a longer word.".to_string();
        }

        if self.answers.contains(&guess) {
            if self.found.insert(guess.clone()) {
                self.coins = self.coins.saturating_add(5);
                self.clear_selection();
                return format!("Found: {} (+5 coins)", guess);
            } else {
                self.clear_selection();
                return "Already found.".to_string();
            }
        }

        if self.bonus_words.contains(&guess) {
            if self.bonus_found.insert(guess.clone()) {
                self.coins = self.coins.saturating_add(1);
                self.clear_selection();
                return format!("Bonus: {} (+1 coin)", guess);
            } else {
                self.clear_selection();
                return "Bonus already counted.".to_string();
            }
        }

        self.clear_selection();
        "No match.".to_string()
    }

    fn hint_reveal_random_letter(&mut self) -> String {
        if self.hints_left == 0 {
            return "No hints left!".to_string();
        }
        let hint_cost = 10;
        if self.coins < hint_cost {
            return format!("Not enough coins (need {}).", hint_cost);
        }

        let visible = self.visible_letters_map();
        let mut candidates: Vec<(usize, usize)> = self
            .used
            .iter()
            .copied()
            .filter(|pos| !visible.contains_key(pos))
            .collect();

        if candidates.is_empty() {
            return "No letters left to reveal.".to_string();
        }

        let mut rng = rand::thread_rng();
        candidates.shuffle(&mut rng);
        let (r, c) = candidates[0];

        if let Some(ch) = self.level.solution_letter_at(r, c) {
            self.revealed.insert((r, c), ch);
            self.coins -= hint_cost;
            self.hints_left -= 1;
            return format!("Revealed a letter (-{} coins).", hint_cost);
        }

        "Hint failed.".to_string()
    }
}

// -- Colors --
const BG_TOP: egui::Color32 = egui::Color32::from_rgb(60, 100, 200);
const BG_BOTTOM: egui::Color32 = egui::Color32::from_rgb(140, 180, 240);
const CELL_FILLED: egui::Color32 = egui::Color32::from_rgb(25, 55, 120);
const CELL_EMPTY: egui::Color32 = egui::Color32::from_rgb(200, 215, 240);
const CELL_FOUND_TEXT: egui::Color32 = egui::Color32::WHITE;
const TOPBAR_BG: egui::Color32 = egui::Color32::from_rgba_premultiplied(20, 40, 80, 220);
const GEM_GREEN: egui::Color32 = egui::Color32::from_rgb(50, 200, 80);
const BADGE_BLUE: egui::Color32 = egui::Color32::from_rgb(80, 160, 230);
const WHEEL_BG: egui::Color32 = egui::Color32::from_rgba_premultiplied(255, 255, 255, 230);
const TILE_SELECTED_BG: egui::Color32 = egui::Color32::from_rgb(80, 160, 230);
const LINE_COLOR: egui::Color32 = egui::Color32::from_rgb(80, 160, 230);
const LETTER_COLOR: egui::Color32 = egui::Color32::from_rgb(30, 30, 30);
const HINT_BTN_BG: egui::Color32 = egui::Color32::from_rgba_premultiplied(40, 40, 40, 180);
const SHUFFLE_ICON_COLOR: egui::Color32 = egui::Color32::from_rgb(150, 160, 175);
const SUBMIT_BG: egui::Color32 = egui::Color32::from_rgb(80, 160, 230);

struct GameApp {
    game: GameState,
    current_level: usize,
    status: String,
    drag_active: bool,
}

impl GameApp {
    fn new() -> Self {
        Self {
            game: GameState::new(1, 200),
            current_level: 1,
            status: String::new(),
            drag_active: false,
        }
    }

    fn go_to_level(&mut self, level_num: usize) {
        let coins = self.game.coins;
        self.current_level = level_num;
        self.game = GameState::new(level_num, coins);
        self.status = String::new();
        self.drag_active = false;
    }
}

fn paint_gradient_bg(painter: &egui::Painter, rect: egui::Rect) {
    let steps = 40;
    let step_h = rect.height() / steps as f32;
    for i in 0..steps {
        let t = i as f32 / steps as f32;
        let r = BG_TOP.r() as f32 + (BG_BOTTOM.r() as f32 - BG_TOP.r() as f32) * t;
        let g = BG_TOP.g() as f32 + (BG_BOTTOM.g() as f32 - BG_TOP.g() as f32) * t;
        let b = BG_TOP.b() as f32 + (BG_BOTTOM.b() as f32 - BG_TOP.b() as f32) * t;
        let color = egui::Color32::from_rgb(r as u8, g as u8, b as u8);
        let strip = egui::Rect::from_min_size(
            egui::pos2(rect.min.x, rect.min.y + i as f32 * step_h),
            egui::vec2(rect.width(), step_h + 1.0),
        );
        painter.rect_filled(strip, 0.0, color);
    }
}

impl eframe::App for GameApp {
    fn update(&mut self, ctx: &egui::Context, _frame: &mut eframe::Frame) {
        // Use custom fonts with larger default size
        let mut style = (*ctx.style()).clone();
        style.text_styles.insert(
            egui::TextStyle::Body,
            egui::FontId::proportional(16.0),
        );
        style.text_styles.insert(
            egui::TextStyle::Heading,
            egui::FontId::proportional(20.0),
        );
        style.text_styles.insert(
            egui::TextStyle::Button,
            egui::FontId::proportional(16.0),
        );
        ctx.set_style(style);

        let _screen = ctx.screen_rect();

        egui::CentralPanel::default()
            .frame(egui::Frame::none())
            .show(ctx, |ui| {
                let full_rect = ui.max_rect();
                let painter = ui.painter();

                // --- GRADIENT BACKGROUND ---
                paint_gradient_bg(painter, full_rect);

                let margin = 16.0;
                let content_rect = full_rect.shrink(margin);
                let mut cursor_y = content_rect.min.y + 8.0;

                // --- TOP BAR ---
                let bar_h = 44.0;
                let bar_rect = egui::Rect::from_min_size(
                    egui::pos2(content_rect.min.x, cursor_y),
                    egui::vec2(content_rect.width(), bar_h),
                );
                painter.rect_filled(bar_rect, 22.0, TOPBAR_BG);

                // Gem + coins (left)
                let gem_x = bar_rect.min.x + 14.0;
                let gem_cy = bar_rect.center().y;
                painter.circle_filled(egui::pos2(gem_x + 8.0, gem_cy), 10.0, GEM_GREEN);
                painter.text(
                    egui::pos2(gem_x + 8.0, gem_cy),
                    egui::Align2::CENTER_CENTER,
                    "\u{25C6}",
                    egui::FontId::proportional(14.0),
                    egui::Color32::WHITE,
                );
                painter.text(
                    egui::pos2(gem_x + 26.0, gem_cy),
                    egui::Align2::LEFT_CENTER,
                    format!("{}", self.game.coins),
                    egui::FontId::proportional(17.0),
                    egui::Color32::WHITE,
                );

                // Word count badge (center-left)
                let badge_x = bar_rect.min.x + 110.0;
                let badge_w = 60.0;
                let badge_rect = egui::Rect::from_min_size(
                    egui::pos2(badge_x, bar_rect.min.y + 8.0),
                    egui::vec2(badge_w, bar_h - 16.0),
                );
                painter.rect_filled(badge_rect, 14.0, BADGE_BLUE);
                painter.text(
                    egui::pos2(badge_rect.min.x + 12.0, badge_rect.center().y),
                    egui::Align2::LEFT_CENTER,
                    "W",
                    egui::FontId::proportional(14.0),
                    egui::Color32::WHITE,
                );
                painter.text(
                    egui::pos2(badge_rect.max.x - 10.0, badge_rect.center().y),
                    egui::Align2::RIGHT_CENTER,
                    format!(
                        "{}/{}",
                        self.game.found.len(),
                        self.game.answers.len()
                    ),
                    egui::FontId::proportional(14.0),
                    egui::Color32::WHITE,
                );

                // Level indicator (right)
                let level_x = bar_rect.max.x - 50.0;
                let level_badge_rect = egui::Rect::from_min_size(
                    egui::pos2(level_x, bar_rect.min.y + 8.0),
                    egui::vec2(44.0, bar_h - 16.0),
                );
                painter.rect_filled(level_badge_rect, 14.0, egui::Color32::from_rgba_premultiplied(255, 255, 255, 40));
                painter.text(
                    level_badge_rect.center(),
                    egui::Align2::CENTER_CENTER,
                    format!("Lv.{}", self.current_level),
                    egui::FontId::proportional(14.0),
                    egui::Color32::WHITE,
                );

                cursor_y += bar_h + 16.0;

                // --- CROSSWORD GRID ---
                let grid_area_w = content_rect.width();
                let cell_size = ((grid_area_w - 10.0) / self.game.level.cols as f32)
                    .min(48.0)
                    .max(34.0);
                let gap = 3.0;
                let grid_total_w =
                    cell_size * self.game.level.cols as f32 + gap * (self.game.level.cols as f32 - 1.0);
                let grid_total_h =
                    cell_size * self.game.level.rows as f32 + gap * (self.game.level.rows as f32 - 1.0);
                let grid_x0 = content_rect.min.x + (content_rect.width() - grid_total_w) / 2.0;
                let grid_y0 = cursor_y;

                let visible = self.game.visible_letters_map();

                for r in 0..self.game.level.rows {
                    for c in 0..self.game.level.cols {
                        if !self.game.used.contains(&(r, c)) {
                            continue;
                        }

                        let x = grid_x0 + c as f32 * (cell_size + gap);
                        let y = grid_y0 + r as f32 * (cell_size + gap);
                        let cell_rect = egui::Rect::from_min_size(
                            egui::pos2(x, y),
                            egui::vec2(cell_size, cell_size),
                        );

                        let has_letter = visible.contains_key(&(r, c));
                        let bg = if has_letter { CELL_FILLED } else { CELL_EMPTY };
                        painter.rect_filled(cell_rect, 6.0, bg);

                        if let Some(ch) = visible.get(&(r, c)) {
                            painter.text(
                                cell_rect.center(),
                                egui::Align2::CENTER_CENTER,
                                ch.to_string(),
                                egui::FontId::proportional(cell_size * 0.48),
                                CELL_FOUND_TEXT,
                            );
                        }
                    }
                }

                cursor_y = grid_y0 + grid_total_h + 14.0;

                // --- CURRENT WORD DISPLAY ---
                let word = self.game.current_word();
                if !word.is_empty() {
                    let word_display_h = 36.0;
                    let word_w = word.len() as f32 * 28.0 + 20.0;
                    let word_rect = egui::Rect::from_min_size(
                        egui::pos2(
                            content_rect.min.x + (content_rect.width() - word_w) / 2.0,
                            cursor_y,
                        ),
                        egui::vec2(word_w, word_display_h),
                    );
                    painter.rect_filled(
                        word_rect,
                        18.0,
                        egui::Color32::from_rgba_premultiplied(255, 255, 255, 180),
                    );
                    painter.text(
                        word_rect.center(),
                        egui::Align2::CENTER_CENTER,
                        &word,
                        egui::FontId::proportional(20.0),
                        egui::Color32::from_rgb(30, 30, 30),
                    );
                    cursor_y += word_display_h + 6.0;
                } else {
                    cursor_y += 6.0;
                }

                // --- STATUS MESSAGE ---
                if !self.status.is_empty() {
                    painter.text(
                        egui::pos2(content_rect.center().x, cursor_y + 10.0),
                        egui::Align2::CENTER_CENTER,
                        &self.status,
                        egui::FontId::proportional(15.0),
                        egui::Color32::WHITE,
                    );
                    cursor_y += 26.0;
                }

                // --- LETTER WHEEL AREA ---
                // Position wheel in lower portion of screen
                let available_h = content_rect.max.y - cursor_y;
                let wheel_diameter = available_h.min(content_rect.width()).min(320.0).max(200.0);
                let wheel_radius = wheel_diameter / 2.0;
                let wheel_center = egui::pos2(
                    content_rect.center().x,
                    cursor_y + available_h / 2.0 - 20.0,
                );

                // White circle background
                painter.circle_filled(wheel_center, wheel_radius, WHEEL_BG);
                painter.circle_stroke(
                    wheel_center,
                    wheel_radius,
                    egui::Stroke::new(2.0, egui::Color32::from_rgb(220, 225, 235)),
                );

                // Tile positions
                let n = self.game.tiles.len().max(1);
                let tile_orbit = wheel_radius * 0.6;
                let tile_r = wheel_radius * 0.17;
                let mut positions: Vec<egui::Pos2> = Vec::with_capacity(n);
                for i in 0..n {
                    let t = i as f32 / n as f32;
                    let ang = t * TAU - FRAC_PI_2;
                    let pos =
                        wheel_center + egui::vec2(ang.cos() * tile_orbit, ang.sin() * tile_orbit);
                    positions.push(pos);
                }

                // Selection lines
                if self.game.selection.len() >= 2 {
                    for pair in self.game.selection.windows(2) {
                        let a = positions[pair[0]];
                        let b = positions[pair[1]];
                        painter.line_segment(
                            [a, b],
                            egui::Stroke::new(4.0, LINE_COLOR),
                        );
                    }
                }

                // Draw tiles and handle interaction (click + drag)
                let pointer = ctx.input(|i| i.pointer.clone());
                let pointer_pos = pointer.interact_pos();
                let primary_down = pointer.primary_down();
                let primary_pressed = pointer.primary_pressed();
                let primary_released = pointer.primary_released();

                if primary_pressed {
                    // Check if press started on a tile
                    if let Some(pos) = pointer_pos {
                        for i in 0..n {
                            let dist = (pos - positions[i]).length();
                            if dist <= tile_r + 5.0 {
                                if !self.game.selection.contains(&i) {
                                    self.game.selection.clear();
                                    self.game.selection.push(i);
                                    self.drag_active = true;
                                }
                                break;
                            }
                        }
                    }
                }

                if primary_down && self.drag_active {
                    if let Some(pos) = pointer_pos {
                        for i in 0..n {
                            let dist = (pos - positions[i]).length();
                            if dist <= tile_r + 5.0 && !self.game.selection.contains(&i) {
                                self.game.selection.push(i);
                                break;
                            }
                        }
                    }
                }

                if primary_released && self.drag_active {
                    self.drag_active = false;
                    // Auto-submit on release if word is long enough
                    if self.game.current_word().len() >= 2 {
                        self.status = self.game.try_submit();
                    } else {
                        self.game.clear_selection();
                    }
                }

                // Also handle simple clicks (tap without drag)
                for i in 0..n {
                    let pos = positions[i];
                    let tile_rect = egui::Rect::from_center_size(
                        pos,
                        egui::vec2(tile_r * 2.0, tile_r * 2.0),
                    );
                    let id = ui.make_persistent_id(("tile", i));
                    let resp = ui.interact(tile_rect, id, egui::Sense::click());

                    let selected = self.game.selection.contains(&i);
                    if selected {
                        painter.circle_filled(pos, tile_r, TILE_SELECTED_BG);
                    }

                    // Letter
                    painter.text(
                        pos,
                        egui::Align2::CENTER_CENTER,
                        self.game.tiles[i].to_string(),
                        egui::FontId::proportional(tile_r * 1.1),
                        if selected {
                            egui::Color32::WHITE
                        } else {
                            LETTER_COLOR
                        },
                    );

                    // Click-based selection (fallback for non-drag)
                    if resp.clicked() && !self.drag_active {
                        if !selected {
                            self.game.selection.push(i);
                        }
                    }
                }

                // Shuffle icon in center of wheel
                let shuffle_rect = egui::Rect::from_center_size(
                    wheel_center,
                    egui::vec2(36.0, 36.0),
                );
                let shuffle_id = ui.make_persistent_id("shuffle_btn");
                let shuffle_resp = ui.interact(shuffle_rect, shuffle_id, egui::Sense::click());
                painter.text(
                    wheel_center,
                    egui::Align2::CENTER_CENTER,
                    "\u{21C4}",
                    egui::FontId::proportional(26.0),
                    if shuffle_resp.hovered() {
                        egui::Color32::from_rgb(100, 110, 130)
                    } else {
                        SHUFFLE_ICON_COLOR
                    },
                );
                if shuffle_resp.clicked() {
                    self.game.shuffle_tiles();
                }

                // --- BOTTOM BUTTONS ---
                let btn_y = wheel_center.y + wheel_radius + 16.0;

                // Hint button (left)
                let hint_size = 54.0;
                let hint_center = egui::pos2(content_rect.min.x + 40.0, btn_y);
                let hint_rect = egui::Rect::from_center_size(
                    hint_center,
                    egui::vec2(hint_size, hint_size),
                );
                let hint_id = ui.make_persistent_id("hint_btn");
                let hint_resp = ui.interact(hint_rect, hint_id, egui::Sense::click());
                painter.circle_filled(hint_center, hint_size / 2.0, HINT_BTN_BG);
                painter.text(
                    egui::pos2(hint_center.x, hint_center.y - 4.0),
                    egui::Align2::CENTER_CENTER,
                    "\u{1F4A1}",
                    egui::FontId::proportional(22.0),
                    egui::Color32::WHITE,
                );
                // Hint count badge
                if self.game.hints_left > 0 {
                    let badge_pos = egui::pos2(hint_center.x + 16.0, hint_center.y - 16.0);
                    painter.circle_filled(badge_pos, 10.0, egui::Color32::from_rgb(220, 50, 50));
                    painter.text(
                        badge_pos,
                        egui::Align2::CENTER_CENTER,
                        format!("{}", self.game.hints_left),
                        egui::FontId::proportional(12.0),
                        egui::Color32::WHITE,
                    );
                }
                if hint_resp.clicked() {
                    self.status = self.game.hint_reveal_random_letter();
                }

                // Submit button (center-bottom, for non-drag users)
                let submit_w = 100.0;
                let submit_h = 40.0;
                let submit_rect = egui::Rect::from_center_size(
                    egui::pos2(content_rect.center().x, btn_y),
                    egui::vec2(submit_w, submit_h),
                );
                let submit_id = ui.make_persistent_id("submit_btn");
                let submit_resp = ui.interact(submit_rect, submit_id, egui::Sense::click());
                painter.rect_filled(
                    submit_rect,
                    20.0,
                    if submit_resp.hovered() {
                        egui::Color32::from_rgb(60, 140, 210)
                    } else {
                        SUBMIT_BG
                    },
                );
                painter.text(
                    submit_rect.center(),
                    egui::Align2::CENTER_CENTER,
                    "Submit",
                    egui::FontId::proportional(16.0),
                    egui::Color32::WHITE,
                );
                if submit_resp.clicked() {
                    self.status = self.game.try_submit();
                }

                // Backspace button (right)
                let back_center = egui::pos2(content_rect.max.x - 40.0, btn_y);
                let back_rect = egui::Rect::from_center_size(
                    back_center,
                    egui::vec2(hint_size, hint_size),
                );
                let back_id = ui.make_persistent_id("back_btn");
                let back_resp = ui.interact(back_rect, back_id, egui::Sense::click());
                painter.circle_filled(back_center, hint_size / 2.0, HINT_BTN_BG);
                painter.text(
                    back_center,
                    egui::Align2::CENTER_CENTER,
                    "\u{232B}",
                    egui::FontId::proportional(22.0),
                    egui::Color32::WHITE,
                );
                if back_resp.clicked() {
                    self.game.backspace();
                }

                // --- COMPLETION ---
                if self.game.is_complete() {
                    // Dim overlay behind popup
                    painter.rect_filled(
                        full_rect,
                        0.0,
                        egui::Color32::from_rgba_premultiplied(0, 0, 0, 120),
                    );

                    let is_last_level = self.current_level >= Level::total_levels();
                    let popup_h = if is_last_level { 180.0 } else { 160.0 };
                    let overlay = egui::Rect::from_center_size(
                        full_rect.center(),
                        egui::vec2(280.0, popup_h),
                    );
                    painter.rect_filled(
                        overlay,
                        16.0,
                        egui::Color32::from_rgba_premultiplied(20, 40, 80, 245),
                    );
                    painter.rect_stroke(
                        overlay,
                        16.0,
                        egui::Stroke::new(2.0, egui::Color32::from_rgb(100, 160, 255)),
                    );

                    let title = if is_last_level {
                        "All Levels Complete!"
                    } else {
                        "Level Complete!"
                    };
                    painter.text(
                        egui::pos2(overlay.center().x, overlay.min.y + 30.0),
                        egui::Align2::CENTER_CENTER,
                        title,
                        egui::FontId::proportional(22.0),
                        egui::Color32::WHITE,
                    );

                    // Stars / reward display
                    painter.text(
                        egui::pos2(overlay.center().x, overlay.min.y + 58.0),
                        egui::Align2::CENTER_CENTER,
                        "+10 coins bonus!",
                        egui::FontId::proportional(15.0),
                        egui::Color32::from_rgb(255, 220, 80),
                    );

                    // Next level / replay button
                    let btn_label = if is_last_level { "Play Again (Lv.1)" } else { "Next Level" };
                    let btn_w = if is_last_level { 160.0 } else { 130.0 };
                    let next_rect = egui::Rect::from_center_size(
                        egui::pos2(overlay.center().x, overlay.max.y - 45.0),
                        egui::vec2(btn_w, 38.0),
                    );
                    let next_id = ui.make_persistent_id("next_btn");
                    let next_resp = ui.interact(next_rect, next_id, egui::Sense::click());
                    painter.rect_filled(
                        next_rect,
                        19.0,
                        if next_resp.hovered() {
                            GEM_GREEN
                        } else {
                            egui::Color32::from_rgb(40, 180, 70)
                        },
                    );
                    painter.text(
                        next_rect.center(),
                        egui::Align2::CENTER_CENTER,
                        btn_label,
                        egui::FontId::proportional(16.0),
                        egui::Color32::WHITE,
                    );
                    if next_resp.clicked() {
                        self.game.coins += 10; // Level complete bonus
                        if is_last_level {
                            self.go_to_level(1);
                        } else {
                            self.go_to_level(self.current_level + 1);
                        }
                    }
                }

                // Bonus words display (small, bottom)
                if !self.game.bonus_found.is_empty() {
                    let bonus_text = format!(
                        "Bonus: {}",
                        self.game
                            .bonus_found
                            .iter()
                            .cloned()
                            .collect::<Vec<_>>()
                            .join(", ")
                    );
                    painter.text(
                        egui::pos2(content_rect.center().x, content_rect.max.y - 8.0),
                        egui::Align2::CENTER_BOTTOM,
                        bonus_text,
                        egui::FontId::proportional(12.0),
                        egui::Color32::from_rgba_premultiplied(255, 255, 255, 160),
                    );
                }
            });

        // Request continuous repaint for smooth interaction
        ctx.request_repaint();
    }
}

fn main() -> eframe::Result<()> {
    let options = eframe::NativeOptions {
        viewport: egui::ViewportBuilder::default()
            .with_inner_size([390.0, 844.0])
            .with_min_inner_size([360.0, 700.0])
            .with_max_inner_size([430.0, 932.0])
            .with_title("Word Wheel"),
        ..Default::default()
    };
    eframe::run_native(
        "Word Wheel",
        options,
        Box::new(|_cc| Ok(Box::new(GameApp::new()))),
    )
}
