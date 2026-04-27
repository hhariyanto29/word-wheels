import SwiftUI

/// One sector of the lottery wheel.
struct SpinSector: Identifiable {
    let id = UUID()
    let coins: Int
    let hints: Int
    let color: Color
    let weight: Int  // probability weight

    var label: String {
        switch (coins, hints) {
        case let (c, h) where c > 0 && h > 0: return "+\(c)\n+\(h)💡"
        case let (c, _) where c > 0:           return "+\(c)"
        case let (_, h) where h > 0:           return "+\(h)💡"
        default:                                return "—"
        }
    }
}

/// Default 8-sector wheel mirroring the Android version. Casual-friendly:
/// small wins are common, rarer 50-coin / 3-hint jackpots stay exciting.
let defaultSpinSectors: [SpinSector] = [
    SpinSector(coins: 5,  hints: 0, color: Color(hex: 0xFF50A0E6), weight: 28),
    SpinSector(coins: 10, hints: 0, color: Color(hex: 0xFFFFB400), weight: 22),
    SpinSector(coins: 15, hints: 0, color: Color(hex: 0xFF50A0E6), weight: 16),
    SpinSector(coins: 0,  hints: 1, color: Color(hex: 0xFF32C850), weight: 12),
    SpinSector(coins: 20, hints: 0, color: Color(hex: 0xFFFFB400), weight: 10),
    SpinSector(coins: 25, hints: 0, color: Color(hex: 0xFF50A0E6), weight: 6),
    SpinSector(coins: 50, hints: 0, color: Color(hex: 0xFFE65050), weight: 4),
    SpinSector(coins: 0,  hints: 3, color: Color(hex: 0xFF32C850), weight: 2),
]

/// Modal lottery wheel. Tap "SPIN" → wheel spins to a weighted-random
/// sector → reward reported via `onSpinResult`. Tapping background after
/// the spin closes the dialog.
struct SpinWheelDialog: View {
    let sectors: [SpinSector]
    let onSpinResult: (SpinSector) -> Void
    let onDismiss: () -> Void

    @State private var hasSpun = false
    @State private var resultIndex = -1
    @State private var rotation: Double = 0  // degrees

    init(sectors: [SpinSector] = defaultSpinSectors,
         onSpinResult: @escaping (SpinSector) -> Void,
         onDismiss: @escaping () -> Void) {
        self.sectors = sectors
        self.onSpinResult = onSpinResult
        self.onDismiss = onDismiss
    }

    var body: some View {
        ZStack {
            Color.black.opacity(0.78)
                .ignoresSafeArea()
                .onTapGesture { if hasSpun { onDismiss() } }

            VStack(spacing: 16) {
                Text(hasSpun ? "Nice!" : "Daily Spin")
                    .font(.system(size: 26, weight: .bold))
                    .foregroundColor(.white)

                if hasSpun, let sec = sectors.indices.contains(resultIndex)
                                ? sectors[resultIndex] : nil {
                    Text(rewardSummary(sec))
                        .font(.system(size: 14))
                        .foregroundColor(GameColors.starYellow)
                } else {
                    Text("Tap below to spin — once per day")
                        .font(.system(size: 14))
                        .foregroundColor(GameColors.starYellow)
                }

                ZStack {
                    WheelShape(sectors: sectors)
                        .rotationEffect(.degrees(rotation))
                        .animation(
                            .timingCurve(0.05, 0.9, 0.4, 1.0, duration: 3.2),
                            value: rotation,
                        )
                        .frame(maxWidth: .infinity)
                        .aspectRatio(1, contentMode: .fit)

                    // Pointer (does not rotate)
                    Triangle()
                        .fill(Color(hex: 0xFFFFDC50))
                        .overlay(Triangle().stroke(Color.black.opacity(0.4), lineWidth: 3))
                        .frame(width: 36, height: 36)
                        .offset(y: -GameSpinWheelTopOffset)
                        .frame(maxHeight: .infinity, alignment: .top)
                }
                .padding(.horizontal, 4)

                Button(action: spinTapped) {
                    Text(hasSpun ? "TAP TO CLOSE" : "SPIN")
                        .font(.system(size: 18, weight: .bold))
                        .foregroundColor(.white)
                        .padding(.horizontal, 36).padding(.vertical, 14)
                        .background(RoundedRectangle(cornerRadius: 28)
                            .fill(hasSpun
                                  ? Color(hex: 0xFF555555)
                                  : Color(hex: 0xFF32C850)))
                }
                .buttonStyle(.plain)
                .disabled(hasSpun)
            }
            .padding(20)
            .frame(maxWidth: 360)
            .background(RoundedRectangle(cornerRadius: 20).fill(GameColors.completeBg))
            .padding(.horizontal, 24)
        }
    }

    private func spinTapped() {
        guard !hasSpun else { onDismiss(); return }

        let totalWeight = sectors.reduce(0) { $0 + $1.weight }
        var pick = Int.random(in: 0..<totalWeight)
        var idx = 0
        for (i, s) in sectors.enumerated() {
            if pick < s.weight { idx = i; break }
            pick -= s.weight
        }
        resultIndex = idx
        hasSpun = true

        // Wheel rotates clockwise; sector i is centred at i*sweep relative
        // to the pointer at top. 5 full turns + the negative offset lands
        // sector idx under the pointer.
        let sweep = 360.0 / Double(sectors.count)
        rotation = 360 * 5 - Double(idx) * sweep

        // Notify the caller after the animation finishes.
        DispatchQueue.main.asyncAfter(deadline: .now() + 3.3) {
            onSpinResult(sectors[idx])
        }
    }

    private func rewardSummary(_ sec: SpinSector) -> String {
        switch (sec.coins, sec.hints) {
        case let (c, h) where c > 0 && h > 0:
            return "+\(c) coins and +\(h) hints"
        case let (c, _) where c > 0:
            return "+\(c) coins"
        case let (_, h) where h > 0:
            return "+\(h) hint" + (h > 1 ? "s" : "")
        default: return ""
        }
    }
}

/// Vertical offset so the pointer sits flush against the wheel's top edge.
private let GameSpinWheelTopOffset: CGFloat = -2

/// Pie-chart-style wheel with N coloured sectors and centred labels.
private struct WheelShape: View {
    let sectors: [SpinSector]

    var body: some View {
        GeometryReader { geo in
            let radius = min(geo.size.width, geo.size.height) / 2 * 0.96
            let center = CGPoint(x: geo.size.width / 2, y: geo.size.height / 2)
            let sweep = 360.0 / Double(sectors.count)

            ZStack {
                ForEach(0..<sectors.count, id: \.self) { i in
                    let start = Double(i) * sweep - 90 - sweep / 2
                    let end = start + sweep
                    SectorShape(
                        center: center,
                        radius: radius,
                        startDeg: start,
                        endDeg: end,
                    )
                    .fill(sectors[i].color)
                    .overlay(
                        SectorShape(
                            center: center,
                            radius: radius,
                            startDeg: start,
                            endDeg: end,
                        ).stroke(Color.white.opacity(0.5), lineWidth: 3)
                    )

                    // Sector label
                    let mid = (start + end) / 2 * .pi / 180
                    let lx = center.x + cos(mid) * radius * 0.6
                    let ly = center.y + sin(mid) * radius * 0.6
                    Text(sectors[i].label)
                        .font(.system(size: 18, weight: .bold))
                        .foregroundColor(.white)
                        .multilineTextAlignment(.center)
                        .position(x: lx, y: ly)
                }

                // Centre hub
                Circle()
                    .fill(GameColors.submitBg)
                    .frame(width: radius * 0.28, height: radius * 0.28)
                    .position(center)
                    .overlay(
                        Circle()
                            .stroke(Color.white, lineWidth: 3)
                            .frame(width: radius * 0.28, height: radius * 0.28)
                            .position(center)
                    )
            }
        }
    }
}

private struct SectorShape: Shape {
    let center: CGPoint
    let radius: CGFloat
    let startDeg: Double
    let endDeg: Double

    func path(in rect: CGRect) -> Path {
        var p = Path()
        p.move(to: center)
        p.addArc(
            center: center,
            radius: radius,
            startAngle: .degrees(startDeg),
            endAngle: .degrees(endDeg),
            clockwise: false,
        )
        p.closeSubpath()
        return p
    }
}

private struct Triangle: Shape {
    func path(in rect: CGRect) -> Path {
        var p = Path()
        p.move(to: CGPoint(x: rect.midX, y: rect.maxY))
        p.addLine(to: CGPoint(x: rect.minX + rect.width * 0.18, y: rect.minY))
        p.addLine(to: CGPoint(x: rect.maxX - rect.width * 0.18, y: rect.minY))
        p.closeSubpath()
        return p
    }
}
