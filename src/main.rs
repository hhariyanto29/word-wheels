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
    word: String, // store UPPERCASE
    row: usize,
    col: usize,
    dir: Dir,
}

#[derive(Debug, Clone, Deserialize, Serialize)]
struct Level {
    rows: usize,
    cols: usize,
    letters: Vec<char>, // each tile is a letter; duplicates are separate tiles
    words: Vec<PlacedWord>,
}

impl Level {
    fn sample() -> Self {
        // Letters: L E S S Y T
        // Target words (all can be formed from the letters):
        // LESS, LET, YET, YES, STYLE, SET
        //
        // Layout (rows=6, cols=6):
        // - LESS across at (0,2)
        // - LET  down   at (0,2)
        // - YET  across at (2,0) ending at (2,2)
        // - YES  down   at (2,0) down to (4,0)
        // - STYLE across at (4,0) to (4,4)
        // - SET  across at (5,2) to (5,4)
        Self {
            rows: 6,
            cols: 6,
            letters: vec!['L', 'E', 'S', 'S', 'Y', 'T'],
            words: vec![
                PlacedWord {
                    word: "LESS".to_string(),
                    row: 0,
                    col: 2,
                    dir: Dir::Across,
                },
                PlacedWord {
                    word: "LET".to_string(),
                    row: 0,
                    col: 2,
                    dir: Dir::Down,
                },
                PlacedWord {
                    word: "YET".to_string(),
                    row: 2,
                    col: 0,
                    dir: Dir::Across,
                },
                PlacedWord {
                    word: "YES".to_string(),
                    row: 2,
                    col: 0,
                    dir: Dir::Down,
                },
                PlacedWord {
                    word: "STYLE".to_string(),
                    row: 4,
                    col: 0,
                    dir: Dir::Across,
                },
                PlacedWord {
                    word: "SET".to_string(),
                    row: 5,
                    col: 2,
                    dir: Dir::Across,
                },
            ],
        }
    }

    fn answers_set(&self) -> HashSet<String> {
        self.words.iter().map(|w| w.word.clone()).collect()
    }

    fn used_cells(&self) -> HashSet<(usize, usize)> {
        let mut used = HashSet::new();
        for pw in &self.words {
            for (i, ch) in pw.word.chars().enumerate() {
                let (r, c) = match pw.dir {
                    Dir::Across => (pw.row, pw.col + i),
                    Dir::Down => (pw.row + i, pw.col),
                };
                let _ = ch; // (kept for clarity)
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
    used: HashSet<(usize, usize)>,
    answers: HashSet<String>,

    // Progress
    found: HashSet<String>,
    revealed: HashMap<(usize, usize), char>, // hint reveals single letters

    // Letter wheel / input
    tiles: Vec<char>,        // current tile order (shuffled)
    selection: Vec<usize>,   // indices into tiles

    // Simple economy
    coins: u32,

    // Bonus words (tiny demo dictionary)
    bonus_found: HashSet<String>,
}

impl GameState {
    fn new(level: Level) -> Self {
        let used = level.used_cells();
        let answers = level.answers_set();
        let tiles = level.letters.clone();

        Self {
            level,
            used,
            answers,
            found: HashSet::new(),
            revealed: HashMap::new(),
            tiles,
            selection: Vec::new(),
            coins: 50,
            bonus_found: HashSet::new(),
        }
    }

    fn current_word(&self) -> String {
        let mut s = String::new();
        for &idx in &self.selection {
            if let Some(ch) = self.tiles.get(idx) {
                s.push(*ch);
            }
        }
        s
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
                return format!("✅ Found: {} (+5 coins)", guess);
            } else {
                self.clear_selection();
                return "Already found.".to_string();
            }
        }

        // Tiny demo “bonus dictionary”:
        // In a real game you'd load a proper word list for your chosen language.
        const BONUS: &[&str] = &["LETS", "LEST", "STYLES"];
        if BONUS.contains(&guess.as_str()) {
            if self.bonus_found.insert(guess.clone()) {
                self.coins = self.coins.saturating_add(1);
                self.clear_selection();
                return format!("✨ Bonus: {} (+1 coin)", guess);
            } else {
                self.clear_selection();
                return "Bonus already counted.".to_string();
            }
        }

        self.clear_selection();
        "No match.".to_string()
    }

    fn hint_reveal_random_letter(&mut self) -> String {
        let hint_cost = 10;
        if self.coins < hint_cost {
            return format!("Not enough coins (need {}).", hint_cost);
        }

        // Build set of currently visible cells
        let visible = self.visible_letters_map();

        // Find unrevealed used cells
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
            return format!("💡 Revealed a letter (-{} coins).", hint_cost);
        }

        "Hint failed (no solution letter found).".to_string()
    }
}

struct GameApp {
    game: GameState,
    status: String,
}

impl GameApp {
    fn new() -> Self {
        Self {
            game: GameState::new(Level::sample()),
            status: "Click letters to build a word, then Submit.".to_string(),
        }
    }
}

impl eframe::App for GameApp {
    fn update(&mut self, ctx: &egui::Context, _frame: &mut eframe::Frame) {
        egui::TopBottomPanel::top("top_bar").show(ctx, |ui| {
            ui.horizontal(|ui| {
                ui.heading("Word Wheel Crossword (Rust MVP)");
                ui.with_layout(egui::Layout::right_to_left(egui::Align::Center), |ui| {
                    ui.label(format!("Coins: {}", self.game.coins));
                    ui.separator();
                    ui.label(format!(
                        "Found: {}/{}",
                        self.game.found.len(),
                        self.game.answers.len()
                    ));
                });
            });
        });

        egui::CentralPanel::default().show(ctx, |ui| {
            ui.add_space(8.0);

            // --- GRID ---
            let cell = egui::vec2(44.0, 44.0);
            let gap = egui::vec2(6.0, 6.0);
            let visible = self.game.visible_letters_map();

            ui.vertical_centered(|ui| {
                ui.label(egui::RichText::new("Puzzle").strong());
                ui.add_space(6.0);

                egui::Grid::new("grid")
                    .spacing(gap)
                    .show(ui, |ui| {
                        for r in 0..self.game.level.rows {
                            for c in 0..self.game.level.cols {
                                let (rect, _resp) =
                                    ui.allocate_exact_size(cell, egui::Sense::hover());

                                if self.game.used.contains(&(r, c)) {
                                    let rounding = 8.0;
                                    let painter = ui.painter();

                                    painter.rect_filled(
                                        rect,
                                        rounding,
                                        ui.visuals().widgets.inactive.bg_fill,
                                    );
                                    painter.rect_stroke(
                                        rect,
                                        rounding,
                                        egui::Stroke::new(
                                            1.0,
                                            ui.visuals().widgets.inactive.bg_stroke.color,
                                        ),
                                    );

                                    if let Some(ch) = visible.get(&(r, c)) {
                                        painter.text(
                                            rect.center(),
                                            egui::Align2::CENTER_CENTER,
                                            ch.to_string(),
                                            egui::FontId::proportional(22.0),
                                            ui.visuals().text_color(),
                                        );
                                    }
                                }
                            }
                            ui.end_row();
                        }
                    });

                ui.add_space(10.0);

                // --- CURRENT WORD ---
                let w = self.game.current_word();
                ui.label(
                    egui::RichText::new(if w.is_empty() { "—".to_string() } else { w })
                        .size(22.0),
                );

                ui.add_space(10.0);

                // --- LETTER WHEEL ---
                let wheel_size = egui::vec2(300.0, 300.0);
                let (rect, _resp) = ui.allocate_exact_size(wheel_size, egui::Sense::hover());
                let painter = ui.painter();

                let center = rect.center();
                let n = self.game.tiles.len().max(1);
                let radius = 105.0;
                let tile_r = 26.0;

                // Compute tile positions
                let mut positions: Vec<egui::Pos2> = Vec::with_capacity(n);
                for i in 0..n {
                    let t = i as f32 / n as f32;
                    let ang = t * TAU - FRAC_PI_2;
                    let pos = center + egui::vec2(ang.cos() * radius, ang.sin() * radius);
                    positions.push(pos);
                }

                // Draw selection lines
                if self.game.selection.len() >= 2 {
                    for pair in self.game.selection.windows(2) {
                        let a = positions[pair[0]];
                        let b = positions[pair[1]];
                        painter.line_segment(
                            [a, b],
                            egui::Stroke::new(3.0, ui.visuals().widgets.active.bg_fill),
                        );
                    }
                }

                // Draw tiles & handle clicks
                for i in 0..n {
                    let pos = positions[i];
                    let tile_rect = egui::Rect::from_center_size(
                        pos,
                        egui::vec2(tile_r * 2.0, tile_r * 2.0),
                    );

                    let id = ui.make_persistent_id(("tile", i));
                    let resp = ui.interact(tile_rect, id, egui::Sense::click());

                    let selected = self.game.selection.contains(&i);
                    let fill = if selected {
                        ui.visuals().widgets.active.bg_fill
                    } else {
                        ui.visuals().widgets.inactive.bg_fill
                    };

                    painter.circle_filled(pos, tile_r, fill);
                    painter.circle_stroke(
                        pos,
                        tile_r,
                        egui::Stroke::new(1.0, ui.visuals().widgets.inactive.bg_stroke.color),
                    );

                    painter.text(
                        pos,
                        egui::Align2::CENTER_CENTER,
                        self.game.tiles[i].to_string(),
                        egui::FontId::proportional(22.0),
                        ui.visuals().text_color(),
                    );

                    if resp.clicked() {
                        if !selected {
                            self.game.selection.push(i);
                        }
                    }
                }

                ui.add_space(12.0);

                // --- CONTROLS ---
                ui.horizontal(|ui| {
                    if ui.button("Backspace").clicked() {
                        self.game.backspace();
                    }
                    if ui.button("Clear").clicked() {
                        self.game.clear_selection();
                    }
                    if ui.button("Shuffle").clicked() {
                        self.game.shuffle_tiles();
                    }
                    if ui.button("Hint (-10)").clicked() {
                        self.status = self.game.hint_reveal_random_letter();
                    }
                    if ui.button("Submit").clicked() {
                        self.status = self.game.try_submit();
                    }
                });

                ui.add_space(10.0);
                ui.label(&self.status);

                if self.game.is_complete() {
                    ui.add_space(10.0);
                    ui.label(egui::RichText::new("🎉 Level complete!").strong().size(18.0));
                    if ui.button("Restart Level").clicked() {
                        self.game = GameState::new(Level::sample());
                        self.status = "Restarted.".to_string();
                    }
                }

                ui.add_space(10.0);
                ui.label(format!(
                    "Bonus found: {}",
                    self.game.bonus_found.iter().cloned().collect::<Vec<_>>().join(", ")
                ));
            });
        });
    }
}

fn main() -> eframe::Result<()> {
    let options = eframe::NativeOptions::default();
    eframe::run_native(
        "Word Wheel Crossword (Prototype)",
        options,
        Box::new(|_cc| Ok(Box::new(GameApp::new()))),
    )
}
