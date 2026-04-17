import SwiftUI

/// Crossword grid. Cell size is derived from available width and height so
/// the grid scales cleanly from small phones (cells shrink to 22 pt) up to
/// iPads (cells cap at 56 pt).
struct CrosswordGrid: View {
    let level: Level
    let visible: [Cell: Character]
    let usedCells: Set<Cell>

    private let gap: CGFloat = 3
    private let padding: CGFloat = 12
    private let minCell: CGFloat = 22
    private let maxCell: CGFloat = 56

    var body: some View {
        GeometryReader { geo in
            let w = geo.size.width
            let h = geo.size.height
            let widthBudget = w - padding * 2 - gap * CGFloat(level.cols - 1)
            let widthCell = max(widthBudget / CGFloat(level.cols), minCell)
            let heightBudget = h - padding * 2 - gap * CGFloat(level.rows - 1)
            let heightCell = h > 0 ? max(heightBudget / CGFloat(level.rows), minCell) : widthCell
            let cellSize = min(widthCell, heightCell, maxCell)

            VStack(spacing: gap) {
                ForEach(0..<level.rows, id: \.self) { r in
                    HStack(spacing: gap) {
                        ForEach(0..<level.cols, id: \.self) { c in
                            let cell = Cell(row: r, col: c)
                            if usedCells.contains(cell) {
                                GridCellView(letter: visible[cell], size: cellSize)
                            } else {
                                Color.clear.frame(width: cellSize, height: cellSize)
                            }
                        }
                    }
                }
            }
            .padding(padding)
            .background(
                RoundedRectangle(cornerRadius: 16, style: .continuous)
                    .fill(GameColors.gridBackdrop)
            )
            .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .center)
        }
    }
}

private struct GridCellView: View {
    let letter: Character?
    let size: CGFloat

    var body: some View {
        ZStack {
            RoundedRectangle(cornerRadius: 6)
                .fill(letter != nil ? GameColors.cellFilled : GameColors.cellEmpty)
            if let letter {
                Text(String(letter))
                    .font(.system(size: size * 0.48, weight: .bold))
                    .foregroundColor(GameColors.cellFoundText)
            }
        }
        .frame(width: size, height: size)
    }
}
