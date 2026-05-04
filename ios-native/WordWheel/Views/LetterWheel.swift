import SwiftUI

/// The circular letter wheel. Drag across tiles to build a word; tapping the
/// center icon shuffles the tiles.
///
/// Mirrors `ui/LetterWheel.kt` on the Android side — Canvas for drawing,
/// gesture handlers for selection, and the same geometry constants so the
/// visual output is identical.
struct LetterWheel: View {
    let tiles: [Character]
    @Binding var selection: [Int]
    let onSubmit: () -> Void
    let onShuffle: () -> Void

    @EnvironmentObject private var sound: SoundManager
    @State private var wheelSize: CGSize = .zero
    /// Last drag location captured by the gesture handler — used to
    /// interpolate intermediate hit-tests so a fast slide doesn't skip
    /// past tiles whose centers fall between two consecutive events.
    @State private var lastDragPos: CGPoint? = nil

    var body: some View {
        GeometryReader { geo in
            let size = geo.size
            let center = CGPoint(x: size.width / 2, y: size.height / 2)
            let radius = min(size.width, size.height) / 2 * 0.99
            // tileOrbit + tileR = 0.94 → tile outer edge sits 5 % inside
            // the visible disc border. The previous 0.62 + 0.20 = 0.82
            // left a 17 % "moat" of empty white space ("jarak antara
            // border dan alphabetnya terlalu jauh" in user feedback).
            let tileOrbit = radius * 0.72
            let tileR = radius * 0.22
            let shuffleR = size.width * 0.09

            let positions: [CGPoint] = (0..<tiles.count).map { i in
                let angle = Double(i) / Double(max(tiles.count, 1)) * 2 * .pi - .pi / 2
                return CGPoint(
                    x: center.x + CGFloat(cos(angle)) * tileOrbit,
                    y: center.y + CGFloat(sin(angle)) * tileOrbit,
                )
            }

            ZStack {
                // Wheel background disc
                Circle()
                    .fill(GameColors.wheelBg)
                    .overlay(Circle().stroke(Color.white.opacity(0.47), lineWidth: 2.5))

                // Selection lines
                if selection.count >= 2 {
                    Path { p in
                        let pts = selection.compactMap { positions.indices.contains($0) ? positions[$0] : nil }
                        guard let first = pts.first else { return }
                        p.move(to: first)
                        for pt in pts.dropFirst() { p.addLine(to: pt) }
                    }
                    .stroke(GameColors.lineColor, style: StrokeStyle(lineWidth: 8, lineCap: .round, lineJoin: .round))
                }

                // Tiles
                ForEach(Array(tiles.enumerated()), id: \.offset) { i, ch in
                    let isSelected = selection.contains(i)
                    ZStack {
                        if isSelected {
                            Circle()
                                .fill(GameColors.tileSelectedBg)
                                .frame(width: tileR * 2, height: tileR * 2)
                        }
                        Text(String(ch))
                            .font(.system(size: tileR * 1.1, weight: .bold))
                            .foregroundColor(isSelected ? .white : GameColors.letterColor)
                    }
                    .position(positions.indices.contains(i) ? positions[i] : center)
                }

                // Center shuffle icon
                Text("\u{21C4}")
                    .font(.system(size: radius * 0.26, weight: .bold))
                    .foregroundColor(GameColors.shuffleIcon)
                    .position(center)
            }
            .contentShape(Rectangle())
            .simultaneousGesture(
                DragGesture(minimumDistance: 2)
                    .onChanged { value in
                        // First hit starts the selection.
                        if selection.isEmpty {
                            if let h = hitTest(size: size, positions: positions,
                                               tileR: tileR, point: value.location) {
                                selection = [h]
                                sound.play(.select)
                            }
                            lastDragPos = value.location
                            return
                        }
                        // Interpolate between the previous sample and the
                        // current one. Without this, fast swipes skip
                        // tiles whose centers fall between two events.
                        let from = lastDragPos ?? value.location
                        let to = value.location
                        let dx = to.x - from.x
                        let dy = to.y - from.y
                        let dist = sqrt(dx * dx + dy * dy)
                        // Half a tile-radius per step keeps us under the
                        // tile spacing so we can't skip a tile.
                        let stepLen = max(tileR * 0.5, 1)
                        let steps = max(1, min(12, Int(dist / stepLen)))
                        for s in 1...steps {
                            let t = CGFloat(s) / CGFloat(steps)
                            let p = CGPoint(x: from.x + dx * t, y: from.y + dy * t)
                            guard let h = hitTest(size: size, positions: positions,
                                                  tileR: tileR, point: p) else { continue }
                            if h == selection.last { continue }
                            // Backtrack: dragging back over the previous
                            // tile shortens the word — matches the feel
                            // of Wordscapes / Words of Wonders.
                            if selection.count >= 2,
                               h == selection[selection.count - 2] {
                                selection.removeLast()
                                sound.play(.select)
                            } else if !selection.contains(h) {
                                selection.append(h)
                                sound.play(.select)
                            }
                        }
                        lastDragPos = to
                    }
                    .onEnded { _ in
                        lastDragPos = nil
                        onSubmit()
                    }
            )
            .onTapGesture { location in
                // Center → shuffle
                let dx = location.x - center.x
                let dy = location.y - center.y
                if sqrt(dx * dx + dy * dy) <= shuffleR {
                    sound.play(.shuffle)
                    onShuffle()
                    return
                }
                if let h = hitTest(size: size, positions: positions, tileR: tileR, point: location),
                   !selection.contains(h) {
                    selection.append(h)
                    sound.play(.select)
                }
            }
            .onAppear { wheelSize = size }
            .onChange(of: size.width) { _ in wheelSize = size }
        }
        .aspectRatio(1, contentMode: .fit)
    }

    private func hitTest(size: CGSize, positions: [CGPoint], tileR: CGFloat, point: CGPoint) -> Int? {
        // Generous hit padding (~45% beyond the visible tile) keeps the
        // gesture forgiving when fingers slide quickly. The previous
        // +12pt constant was tight enough that fast swipes regularly
        // missed tiles between samples.
        let hitR = tileR * 1.45
        for (i, p) in positions.enumerated() {
            let dx = p.x - point.x
            let dy = p.y - point.y
            if sqrt(dx * dx + dy * dy) <= hitR { return i }
        }
        return nil
    }
}
