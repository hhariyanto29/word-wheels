use eframe::egui;
use word_wheel_crossword::GameApp;

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
