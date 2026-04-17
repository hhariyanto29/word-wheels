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

    var body: some View {
        GeometryReader { geo in
            let size = geo.size
            let center = CGPoint(x: size.width / 2, y: size.height / 2)
            let radius = min(size.width, size.height) / 2 * 0.95
            let tileOrbit = radius * 0.6
            let tileR = radius * 0.17
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
                        // First hit starts the selection
                        if selection.isEmpty {
                            let h = hitTest(
                                size: size,
                                positions: positions,
                                tileR: tileR,
                                point: value.location,
                            )
                            if let h {
                                selection = [h]
                                sound.play(.select)
                            }
                            return
                        }
                        let h = hitTest(
                            size: size,
                            positions: positions,
                            tileR: tileR,
                            point: value.location,
                        )
                        if let h, !selection.contains(h) {
                            selection.append(h)
                            sound.play(.select)
                        }
                    }
                    .onEnded { _ in onSubmit() }
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
        for (i, p) in positions.enumerated() {
            let dx = p.x - point.x
            let dy = p.y - point.y
            if sqrt(dx * dx + dy * dy) <= tileR + 12 { return i }
        }
        return nil
    }
}
