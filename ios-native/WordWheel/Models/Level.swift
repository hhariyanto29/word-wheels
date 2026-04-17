import Foundation

enum Dir {
    case across
    case down
}

struct PlacedWord {
    let word: String
    let row: Int
    let col: Int
    let dir: Dir
}

struct Cell: Hashable {
    let row: Int
    let col: Int
}

struct Level {
    static let totalLevels = 10

    let rows: Int
    let cols: Int
    let letters: [Character]
    let words: [PlacedWord]

    var answersSet: Set<String> { Set(words.map { $0.word }) }

    var usedCells: Set<Cell> {
        var used = Set<Cell>()
        for pw in words {
            for i in 0..<pw.word.count {
                let cell: Cell
                switch pw.dir {
                case .across: cell = Cell(row: pw.row, col: pw.col + i)
                case .down:   cell = Cell(row: pw.row + i, col: pw.col)
                }
                used.insert(cell)
            }
        }
        return used
    }

    func solutionLetter(row: Int, col: Int) -> Character? {
        for pw in words {
            for (i, ch) in pw.word.enumerated() {
                let r: Int, c: Int
                switch pw.dir {
                case .across: r = pw.row; c = pw.col + i
                case .down:   r = pw.row + i; c = pw.col
                }
                if r == row && c == col { return ch }
            }
        }
        return nil
    }

    var bonusWords: [String] {
        switch String(letters) {
        case "CATS":     return ["ACT", "ACTS", "SCAT", "CATS"]
        case "SPINE":    return ["PIN", "PIE", "SIP", "SNIP", "PIES", "PENS", "SINE", "PINE"]
        case "HASTE":    return ["HEAT", "SET", "HAT", "TEA", "ATE", "ETA", "EAST", "EATS"]
        case "WARMS":    return ["WARMS", "WARS", "ARMS", "MARS", "ARM", "MAR", "RAW", "WAR"]
        case "CARES":    return ["RACE", "CARS", "EARS", "ACE", "ARC", "SCAR", "ARCS", "ACRE", "ACES"]
        case "GRINDS":   return ["GRINDS", "GRID", "RIND", "DING", "RING", "GRIN", "RINGS"]
        case "PLANETS":  return ["PLAN", "PLANT", "LEAN", "PLATE", "STEAL", "LANE", "TALE", "PELT"]
        case "CRANESD":  return ["DANCE", "SCARE", "RACED", "CEDAR", "CANE", "ACRE", "RAN"]
        case "STORED":   return ["RODE", "DOER", "STORE", "DOTES", "TROD", "ODES", "SORE", "TORE"]
        case "TRAINSE":  return ["STARE", "RETAIN", "SATIRE", "INSERT", "STAIN", "RAIN", "RISE", "TIRE"]
        default:         return []
        }
    }

    static func get(_ levelNum: Int) -> Level {
        switch levelNum {
        case 1:
            return Level(rows: 5, cols: 5, letters: ["C","A","T","S"], words: [
                PlacedWord(word: "CAST", row: 1, col: 0, dir: .across),
                PlacedWord(word: "CAT",  row: 1, col: 0, dir: .down),
                PlacedWord(word: "SAT",  row: 1, col: 2, dir: .down),
            ])
        case 2:
            return Level(rows: 6, cols: 6, letters: ["S","P","I","N","E"], words: [
                PlacedWord(word: "SPINE", row: 1, col: 0, dir: .across),
                PlacedWord(word: "SIN",   row: 1, col: 0, dir: .down),
                PlacedWord(word: "NIP",   row: 1, col: 3, dir: .down),
                PlacedWord(word: "PEN",   row: 3, col: 3, dir: .across),
            ])
        case 3:
            return Level(rows: 7, cols: 7, letters: ["H","A","S","T","E"], words: [
                PlacedWord(word: "HASTE", row: 1, col: 0, dir: .across),
                PlacedWord(word: "HATE",  row: 1, col: 0, dir: .down),
                PlacedWord(word: "SEAT",  row: 1, col: 2, dir: .down),
                PlacedWord(word: "EAT",   row: 4, col: 0, dir: .across),
            ])
        case 4:
            return Level(rows: 7, cols: 7, letters: ["W","A","R","M","S"], words: [
                PlacedWord(word: "SWARM", row: 1, col: 0, dir: .across),
                PlacedWord(word: "SAW",   row: 1, col: 0, dir: .down),
                PlacedWord(word: "RAM",   row: 1, col: 3, dir: .down),
                PlacedWord(word: "WARM",  row: 3, col: 0, dir: .across),
            ])
        case 5:
            return Level(rows: 7, cols: 7, letters: ["C","A","R","E","S"], words: [
                PlacedWord(word: "CARES", row: 1, col: 0, dir: .across),
                PlacedWord(word: "CARE",  row: 1, col: 0, dir: .down),
                PlacedWord(word: "SEAR",  row: 1, col: 4, dir: .down),
                PlacedWord(word: "ERA",   row: 4, col: 0, dir: .across),
            ])
        case 6:
            return Level(rows: 7, cols: 7, letters: ["G","R","I","N","D","S"], words: [
                PlacedWord(word: "GRINS", row: 1, col: 0, dir: .across),
                PlacedWord(word: "GIN",   row: 1, col: 0, dir: .down),
                PlacedWord(word: "SIR",   row: 1, col: 4, dir: .down),
                PlacedWord(word: "RID",   row: 3, col: 4, dir: .across),
                PlacedWord(word: "DIG",   row: 3, col: 6, dir: .down),
            ])
        case 7:
            return Level(rows: 8, cols: 8, letters: ["P","L","A","N","E","T","S"], words: [
                PlacedWord(word: "PLANETS", row: 1, col: 0, dir: .across),
                PlacedWord(word: "PETAL",   row: 1, col: 0, dir: .down),
                PlacedWord(word: "NETS",    row: 1, col: 3, dir: .down),
                PlacedWord(word: "SLANT",   row: 1, col: 6, dir: .down),
                PlacedWord(word: "ANTS",    row: 4, col: 0, dir: .across),
            ])
        case 8:
            return Level(rows: 8, cols: 8, letters: ["C","R","A","N","E","S","D"], words: [
                PlacedWord(word: "CRANES", row: 1, col: 0, dir: .across),
                PlacedWord(word: "CARED",  row: 1, col: 0, dir: .down),
                PlacedWord(word: "NEARS",  row: 1, col: 3, dir: .down),
                PlacedWord(word: "SANE",   row: 1, col: 5, dir: .down),
                PlacedWord(word: "DENS",   row: 5, col: 0, dir: .across),
                PlacedWord(word: "END",    row: 5, col: 1, dir: .down),
            ])
        case 9:
            return Level(rows: 8, cols: 8, letters: ["S","T","O","R","E","D"], words: [
                PlacedWord(word: "STORED", row: 1, col: 0, dir: .across),
                PlacedWord(word: "SORT",   row: 1, col: 0, dir: .down),
                PlacedWord(word: "REST",   row: 1, col: 3, dir: .down),
                PlacedWord(word: "DOSE",   row: 1, col: 5, dir: .down),
                PlacedWord(word: "ROES",   row: 3, col: 0, dir: .across),
            ])
        default:
            return Level(rows: 9, cols: 9, letters: ["T","R","A","I","N","S","E"], words: [
                PlacedWord(word: "TRAINS", row: 1, col: 0, dir: .across),
                PlacedWord(word: "TEARS",  row: 1, col: 0, dir: .down),
                PlacedWord(word: "INSET",  row: 1, col: 3, dir: .down),
                PlacedWord(word: "SIREN",  row: 1, col: 5, dir: .down),
                PlacedWord(word: "ANTS",   row: 3, col: 0, dir: .across),
                PlacedWord(word: "SENT",   row: 5, col: 0, dir: .across),
            ])
        }
    }
}
