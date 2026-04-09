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
            // All levels use "spine + branches" pattern for guaranteed connectivity.
            // Every word shares at least one cell with another word.
            // Level 1: CAST spine across, CAT/SAT branch down (cols 0 and 2, non-adjacent)
            // Intersections: C(1,0), S(1,2) — all verified
            // Col 0: C,A,T = CAT ✓  Col 2: S,A,T = SAT ✓  Row 1: C,A,S,T = CAST ✓
            1 => Self {
                rows: 5,
                cols: 5,
                letters: vec!['C', 'A', 'T', 'S'],
                words: vec![
                    PlacedWord { word: "CAST".into(), row: 1, col: 0, dir: Dir::Across },
                    PlacedWord { word: "CAT".into(), row: 1, col: 0, dir: Dir::Down },
                    PlacedWord { word: "SAT".into(), row: 1, col: 2, dir: Dir::Down },
                ],
            },

            // Level 2: SPINE across, SIN/NIP down (spaced cols 0,3), PEN crosses NIP
            // Every contiguous run verified: Row1=SPINE, Col0=SIN, Col3=NIP, Row3=PEN
            2 => Self {
                rows: 6,
                cols: 6,
                letters: vec!['S', 'P', 'I', 'N', 'E'],
                words: vec![
                    PlacedWord { word: "SPINE".into(), row: 1, col: 0, dir: Dir::Across },
                    PlacedWord { word: "SIN".into(), row: 1, col: 0, dir: Dir::Down },
                    PlacedWord { word: "NIP".into(), row: 1, col: 3, dir: Dir::Down },
                    PlacedWord { word: "PEN".into(), row: 3, col: 3, dir: Dir::Across },
                ],
            },

            // Level 3: HASTE across, HATE/SEAT down (spaced cols 0,2), EAT crosses both
            // Every contiguous run verified: Row1=HASTE, Col0=HATE, Col2=SEAT, Row4=EAT
            3 => Self {
                rows: 7,
                cols: 7,
                letters: vec!['H', 'A', 'S', 'T', 'E'],
                words: vec![
                    PlacedWord { word: "HASTE".into(), row: 1, col: 0, dir: Dir::Across },
                    PlacedWord { word: "HATE".into(), row: 1, col: 0, dir: Dir::Down },
                    PlacedWord { word: "SEAT".into(), row: 1, col: 2, dir: Dir::Down },
                    PlacedWord { word: "EAT".into(), row: 4, col: 0, dir: Dir::Across },
                ],
            },

            // Level 4: SWARM across, SAW/RAM down (spaced cols 0,3), WARM crosses both
            // Every contiguous run verified: Row1=SWARM, Col0=SAW, Col3=RAM, Row3=WARM
            4 => Self {
                rows: 7,
                cols: 7,
                letters: vec!['W', 'A', 'R', 'M', 'S'],
                words: vec![
                    PlacedWord { word: "SWARM".into(), row: 1, col: 0, dir: Dir::Across },
                    PlacedWord { word: "SAW".into(), row: 1, col: 0, dir: Dir::Down },
                    PlacedWord { word: "RAM".into(), row: 1, col: 3, dir: Dir::Down },
                    PlacedWord { word: "WARM".into(), row: 3, col: 0, dir: Dir::Across },
                ],
            },

            // Level 5: CARES across, CARE/SEAR down (spaced cols 0,4), ERA crosses CARE
            // Every contiguous run verified: Row1=CARES, Col0=CARE, Col4=SEAR, Row4=ERA
            5 => Self {
                rows: 7,
                cols: 7,
                letters: vec!['C', 'A', 'R', 'E', 'S'],
                words: vec![
                    PlacedWord { word: "CARES".into(), row: 1, col: 0, dir: Dir::Across },
                    PlacedWord { word: "CARE".into(), row: 1, col: 0, dir: Dir::Down },
                    PlacedWord { word: "SEAR".into(), row: 1, col: 4, dir: Dir::Down },
                    PlacedWord { word: "ERA".into(), row: 4, col: 0, dir: Dir::Across },
                ],
            },

            // Level 6: GRINS across, GIN/SIR down (spaced cols 0,4), RID crosses SIR, DIG down from RID
            // Every contiguous run verified: Row1=GRINS, Col0=GIN, Col4=SIR, Row3(4-6)=RID, Col6=DIG
            6 => Self {
                rows: 7,
                cols: 7,
                letters: vec!['G', 'R', 'I', 'N', 'D', 'S'],
                words: vec![
                    PlacedWord { word: "GRINS".into(), row: 1, col: 0, dir: Dir::Across },
                    PlacedWord { word: "GIN".into(), row: 1, col: 0, dir: Dir::Down },
                    PlacedWord { word: "SIR".into(), row: 1, col: 4, dir: Dir::Down },
                    PlacedWord { word: "RID".into(), row: 3, col: 4, dir: Dir::Across },
                    PlacedWord { word: "DIG".into(), row: 3, col: 6, dir: Dir::Down },
                ],
            },

            // Level 7: PLANETS across, PETAL/NETS/SLANT down (spaced cols 0,3,6), ANTS crosses PETAL+NETS
            // Every contiguous run verified: Row1=PLANETS, Col0=PETAL, Col3=NETS, Col6=SLANT, Row4=ANTS
            7 => Self {
                rows: 8,
                cols: 8,
                letters: vec!['P', 'L', 'A', 'N', 'E', 'T', 'S'],
                words: vec![
                    PlacedWord { word: "PLANETS".into(), row: 1, col: 0, dir: Dir::Across },
                    PlacedWord { word: "PETAL".into(), row: 1, col: 0, dir: Dir::Down },
                    PlacedWord { word: "NETS".into(), row: 1, col: 3, dir: Dir::Down },
                    PlacedWord { word: "SLANT".into(), row: 1, col: 6, dir: Dir::Down },
                    PlacedWord { word: "ANTS".into(), row: 4, col: 0, dir: Dir::Across },
                ],
            },

            // Level 8: CRANES across, CARED/NEARS/SANE down (spaced cols 0,3,5), DENS+END cross
            // Every contiguous run verified: Row1=CRANES, Col0=CARED, Col3=NEARS, Col5=SANE, Row5=DENS, Col1(5-7)=END
            8 => Self {
                rows: 8,
                cols: 8,
                letters: vec!['C', 'R', 'A', 'N', 'E', 'S', 'D'],
                words: vec![
                    PlacedWord { word: "CRANES".into(), row: 1, col: 0, dir: Dir::Across },
                    PlacedWord { word: "CARED".into(), row: 1, col: 0, dir: Dir::Down },
                    PlacedWord { word: "NEARS".into(), row: 1, col: 3, dir: Dir::Down },
                    PlacedWord { word: "SANE".into(), row: 1, col: 5, dir: Dir::Down },
                    PlacedWord { word: "DENS".into(), row: 5, col: 0, dir: Dir::Across },
                    PlacedWord { word: "END".into(), row: 5, col: 1, dir: Dir::Down },
                ],
            },

            // Level 9: STORED across, SORT/REST/DOSE down (spaced cols 0,3,5), ROES crosses SORT+REST
            // Every contiguous run verified: Row1=STORED, Col0=SORT, Col3=REST, Col5=DOSE, Row3=ROES
            9 => Self {
                rows: 8,
                cols: 8,
                letters: vec!['S', 'T', 'O', 'R', 'E', 'D'],
                words: vec![
                    PlacedWord { word: "STORED".into(), row: 1, col: 0, dir: Dir::Across },
                    PlacedWord { word: "SORT".into(), row: 1, col: 0, dir: Dir::Down },
                    PlacedWord { word: "REST".into(), row: 1, col: 3, dir: Dir::Down },
                    PlacedWord { word: "DOSE".into(), row: 1, col: 5, dir: Dir::Down },
                    PlacedWord { word: "ROES".into(), row: 3, col: 0, dir: Dir::Across },
                ],
            },

            // Level 10: TRAINS across, TEARS/INSET/SIREN down (spaced cols 0,3,5), ANTS+SENT cross
            // Every contiguous run verified: Row1=TRAINS, Col0=TEARS, Col3=INSET, Col5=SIREN, Row3=ANTS, Row5=SENT
            _ => Self {
                rows: 9,
                cols: 9,
                letters: vec!['T', 'R', 'A', 'I', 'N', 'S', 'E'],
                words: vec![
                    PlacedWord { word: "TRAINS".into(), row: 1, col: 0, dir: Dir::Across },
                    PlacedWord { word: "TEARS".into(), row: 1, col: 0, dir: Dir::Down },
                    PlacedWord { word: "INSET".into(), row: 1, col: 3, dir: Dir::Down },
                    PlacedWord { word: "SIREN".into(), row: 1, col: 5, dir: Dir::Down },
                    PlacedWord { word: "ANTS".into(), row: 3, col: 0, dir: Dir::Across },
                    PlacedWord { word: "SENT".into(), row: 5, col: 0, dir: Dir::Across },
                ],
            },
        }
    }

    fn bonus_words(&self) -> Vec<&'static str> {
        match self.letters.as_slice() {
            ['C', 'A', 'T', 'S'] => vec!["ACT", "ACTS", "SCAT", "CATS"],
            ['S', 'P', 'I', 'N', 'E'] => vec!["PIN", "PIE", "SIP", "SNIP", "PIES", "PENS", "SINE", "PINE"],
            ['H', 'A', 'S', 'T', 'E'] => vec!["HEAT", "SET", "HAT", "TEA", "ATE", "ETA", "EAST", "EATS"],
            ['W', 'A', 'R', 'M', 'S'] => vec!["WARMS", "WARS", "ARMS", "MARS", "ARM", "MAR", "RAW", "WAR"],
            ['C', 'A', 'R', 'E', 'S'] => vec!["RACE", "CARS", "EARS", "ACE", "ARC", "SCAR", "ARCS", "ACRE", "ACES"],
            ['G', 'R', 'I', 'N', 'D', 'S'] => vec!["GRINDS", "GRID", "RIND", "DING", "RING", "GRIN", "RINGS"],
            ['P', 'L', 'A', 'N', 'E', 'T', 'S'] => vec!["PLAN", "PLANT", "LEAN", "PLATE", "STEAL", "LANE", "TALE", "PELT"],
            ['C', 'R', 'A', 'N', 'E', 'S', 'D'] => vec!["DANCE", "SCARE", "RACED", "CEDAR", "CANE", "ACRE", "RAN"],
            ['S', 'T', 'O', 'R', 'E', 'D'] => vec!["RODE", "DOER", "STORE", "DOTES", "TROD", "ODES", "SORE", "TORE"],
            ['T', 'R', 'A', 'I', 'N', 'S', 'E'] => vec!["STARE", "RETAIN", "SATIRE", "INSERT", "STAIN", "RAIN", "RISE", "TIRE"],
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
    words_toward_hint: u32,
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
            hints_left: 5,
            words_toward_hint: 0,
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
                self.coins = self.coins.saturating_add(2);
                self.words_toward_hint += 1;
                let hint_msg = if self.words_toward_hint >= 10 {
                    self.words_toward_hint = 0;
                    self.hints_left += 1;
                    " +1 hint!"
                } else {
                    ""
                };
                self.clear_selection();
                return format!("Found: {} (+2 pts){}", guess, hint_msg);
            } else {
                self.clear_selection();
                return "Already found.".to_string();
            }
        }

        if self.bonus_words.contains(&guess) {
            if self.bonus_found.insert(guess.clone()) {
                self.coins = self.coins.saturating_add(2);
                self.words_toward_hint += 1;
                let hint_msg = if self.words_toward_hint >= 10 {
                    self.words_toward_hint = 0;
                    self.hints_left += 1;
                    " +1 hint!"
                } else {
                    ""
                };
                self.clear_selection();
                return format!("Bonus: {} (+2 pts){}", guess, hint_msg);
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
            return "No hints left! Find more words to earn hints.".to_string();
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
            self.hints_left -= 1;
            return "Revealed a letter!".to_string();
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

pub struct GameApp {
    game: GameState,
    current_level: usize,
    status: String,
    drag_active: bool,
    bg_texture: Option<egui::TextureHandle>,
}

impl GameApp {
    pub fn new() -> Self {
        Self {
            game: GameState::new(1, 200),
            current_level: 1,
            status: String::new(),
            drag_active: false,
            bg_texture: None,
        }
    }

    fn go_to_level(&mut self, level_num: usize) {
        let coins = self.game.coins;
        let words_toward_hint = self.game.words_toward_hint;
        let hints_left = self.game.hints_left;
        self.current_level = level_num;
        self.game = GameState::new(level_num, coins);
        self.game.words_toward_hint = words_toward_hint;
        self.game.hints_left = hints_left;
        self.status = String::new();
        self.drag_active = false;
    }

    fn load_bg_texture(&mut self, ctx: &egui::Context) {
        if self.bg_texture.is_some() {
            return;
        }
        let img_bytes = include_bytes!("../assets/background.jpg");
        if let Ok(img) = image::load_from_memory(img_bytes) {
            let rgba = img.to_rgba8();
            let size = [rgba.width() as usize, rgba.height() as usize];
            let pixels = rgba.into_vec();
            let color_image = egui::ColorImage::from_rgba_unmultiplied(size, &pixels);
            self.bg_texture = Some(ctx.load_texture(
                "background",
                color_image,
                egui::TextureOptions::LINEAR,
            ));
        }
    }
}

/// Paints a soft drop shadow behind a rounded rectangle.
fn paint_drop_shadow(painter: &egui::Painter, rect: egui::Rect, rounding: f32, layers: u8, spread: f32) {
    let base_alpha = 35u8;
    for i in 0..layers {
        let expand = spread * (i as f32 + 1.0) / layers as f32;
        let alpha = base_alpha.saturating_sub((base_alpha as f32 * i as f32 / layers as f32) as u8);
        let shadow_rect = rect.expand(expand);
        let offset = egui::vec2(0.0, expand * 0.5);
        let shifted = egui::Rect::from_min_size(
            shadow_rect.min + offset,
            shadow_rect.size(),
        );
        painter.rect_filled(shifted, rounding + expand * 0.5, egui::Color32::from_rgba_premultiplied(0, 0, 0, alpha));
    }
}

/// Paints a soft circular drop shadow.
fn paint_circle_shadow(painter: &egui::Painter, center: egui::Pos2, radius: f32, layers: u8, spread: f32) {
    let base_alpha = 30u8;
    for i in 0..layers {
        let expand = spread * (i as f32 + 1.0) / layers as f32;
        let alpha = base_alpha.saturating_sub((base_alpha as f32 * i as f32 / layers as f32) as u8);
        let shifted_center = center + egui::vec2(0.0, expand * 0.4);
        painter.circle_filled(shifted_center, radius + expand, egui::Color32::from_rgba_premultiplied(0, 0, 0, alpha));
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

        // Load background texture on first frame
        self.load_bg_texture(ctx);

        egui::CentralPanel::default()
            .frame(egui::Frame::NONE)
            .show(ctx, |ui| {
                let full_rect = ui.max_rect();
                let painter = ui.painter();

                // --- BACKGROUND IMAGE ---
                if let Some(tex) = &self.bg_texture {
                    let uv = egui::Rect::from_min_max(
                        egui::pos2(0.0, 0.0),
                        egui::pos2(1.0, 1.0),
                    );
                    painter.image(tex.id(), full_rect, uv, egui::Color32::WHITE);
                } else {
                    paint_gradient_bg(painter, full_rect);
                }

                let margin = 16.0;
                let content_rect = full_rect.shrink(margin);
                let mut cursor_y = content_rect.min.y + 8.0;

                // --- TOP BAR ---
                let bar_h = 44.0;
                let bar_rect = egui::Rect::from_min_size(
                    egui::pos2(content_rect.min.x, cursor_y),
                    egui::vec2(content_rect.width(), bar_h),
                );
                paint_drop_shadow(painter, bar_rect, 22.0, 5, 8.0);
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

                // Grid backdrop panel for contrast
                let grid_backdrop = egui::Rect::from_min_size(
                    egui::pos2(grid_x0 - 12.0, grid_y0 - 12.0),
                    egui::vec2(grid_total_w + 24.0, grid_total_h + 24.0),
                );
                paint_drop_shadow(painter, grid_backdrop, 16.0, 5, 10.0);
                painter.rect_filled(grid_backdrop, 16.0, egui::Color32::from_rgba_premultiplied(10, 25, 60, 180));
                painter.rect_stroke(grid_backdrop, 16.0, egui::Stroke::new(1.0, egui::Color32::from_rgba_premultiplied(255, 255, 255, 25)), egui::StrokeKind::Outside);

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

                        // Cell drop shadow
                        paint_drop_shadow(painter, cell_rect, 6.0, 3, 3.0);

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
                    paint_drop_shadow(painter, word_rect, 18.0, 4, 6.0);
                    painter.rect_filled(
                        word_rect,
                        18.0,
                        egui::Color32::from_rgba_premultiplied(255, 255, 255, 200),
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
                    let status_rect = egui::Rect::from_center_size(
                        egui::pos2(content_rect.center().x, cursor_y + 10.0),
                        egui::vec2(self.status.len() as f32 * 9.0 + 30.0, 28.0),
                    );
                    paint_drop_shadow(painter, status_rect, 14.0, 3, 4.0);
                    painter.rect_filled(status_rect, 14.0, egui::Color32::from_rgba_premultiplied(0, 0, 0, 140));
                    painter.text(
                        status_rect.center(),
                        egui::Align2::CENTER_CENTER,
                        &self.status,
                        egui::FontId::proportional(15.0),
                        egui::Color32::WHITE,
                    );
                    cursor_y += 32.0;
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

                // White circle background with shadow
                paint_circle_shadow(painter, wheel_center, wheel_radius, 6, 12.0);
                painter.circle_filled(wheel_center, wheel_radius, WHEEL_BG);
                painter.circle_stroke(
                    wheel_center,
                    wheel_radius,
                    egui::Stroke::new(2.5, egui::Color32::from_rgba_premultiplied(255, 255, 255, 120)),
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
                paint_circle_shadow(painter, hint_center, hint_size / 2.0, 4, 6.0);
                painter.circle_filled(hint_center, hint_size / 2.0, HINT_BTN_BG);
                painter.circle_stroke(hint_center, hint_size / 2.0, egui::Stroke::new(1.5, egui::Color32::from_rgba_premultiplied(255, 255, 255, 40)));
                painter.text(
                    egui::pos2(hint_center.x, hint_center.y - 4.0),
                    egui::Align2::CENTER_CENTER,
                    "\u{1F4A1}",
                    egui::FontId::proportional(22.0),
                    egui::Color32::WHITE,
                );
                // Hint count badge
                {
                    let badge_pos = egui::pos2(hint_center.x + 16.0, hint_center.y - 16.0);
                    let badge_color = if self.game.hints_left > 0 {
                        egui::Color32::from_rgb(50, 180, 80)
                    } else {
                        egui::Color32::from_rgb(120, 120, 120)
                    };
                    painter.circle_filled(badge_pos, 10.0, badge_color);
                    painter.text(
                        badge_pos,
                        egui::Align2::CENTER_CENTER,
                        format!("{}", self.game.hints_left),
                        egui::FontId::proportional(12.0),
                        egui::Color32::WHITE,
                    );
                }
                // Hint progress bar (words_toward_hint / 10)
                {
                    let bar_w = hint_size;
                    let bar_h = 6.0;
                    let bar_x = hint_center.x - bar_w / 2.0;
                    let bar_y = hint_center.y + hint_size / 2.0 + 4.0;
                    let bar_rect = egui::Rect::from_min_size(
                        egui::pos2(bar_x, bar_y),
                        egui::vec2(bar_w, bar_h),
                    );
                    painter.rect_filled(bar_rect, 3.0, egui::Color32::from_rgb(40, 40, 60));
                    let progress = self.game.words_toward_hint as f32 / 10.0;
                    if progress > 0.0 {
                        let fill_rect = egui::Rect::from_min_size(
                            bar_rect.min,
                            egui::vec2(bar_w * progress, bar_h),
                        );
                        painter.rect_filled(fill_rect, 3.0, egui::Color32::from_rgb(80, 220, 120));
                    }
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
                paint_drop_shadow(painter, submit_rect, 20.0, 4, 6.0);
                painter.rect_filled(
                    submit_rect,
                    20.0,
                    if submit_resp.hovered() {
                        egui::Color32::from_rgb(60, 140, 210)
                    } else {
                        SUBMIT_BG
                    },
                );
                painter.rect_stroke(
                    submit_rect,
                    20.0,
                    egui::Stroke::new(1.5, egui::Color32::from_rgba_premultiplied(255, 255, 255, 50)),
                    egui::StrokeKind::Outside,
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
                paint_circle_shadow(painter, back_center, hint_size / 2.0, 4, 6.0);
                painter.circle_filled(back_center, hint_size / 2.0, HINT_BTN_BG);
                painter.circle_stroke(back_center, hint_size / 2.0, egui::Stroke::new(1.5, egui::Color32::from_rgba_premultiplied(255, 255, 255, 40)));
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
                    paint_drop_shadow(painter, overlay, 16.0, 6, 16.0);
                    painter.rect_filled(
                        overlay,
                        16.0,
                        egui::Color32::from_rgba_premultiplied(20, 40, 80, 245),
                    );
                    painter.rect_stroke(
                        overlay,
                        16.0,
                        egui::Stroke::new(2.0, egui::Color32::from_rgb(100, 160, 255)),
                        egui::StrokeKind::Outside,
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
                        "+10 pts bonus!",
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
                    paint_drop_shadow(painter, next_rect, 19.0, 4, 6.0);
                    painter.rect_filled(
                        next_rect,
                        19.0,
                        if next_resp.hovered() {
                            GEM_GREEN
                        } else {
                            egui::Color32::from_rgb(40, 180, 70)
                        },
                    );
                    painter.rect_stroke(
                        next_rect,
                        19.0,
                        egui::Stroke::new(1.5, egui::Color32::from_rgba_premultiplied(255, 255, 255, 60)),
                        egui::StrokeKind::Outside,
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

// Android entry point
#[cfg(target_os = "android")]
#[no_mangle]
fn android_main(app: winit::platform::android::activity::AndroidApp) {
    android_logger::init_once(
        android_logger::Config::default().with_max_level(log::LevelFilter::Debug),
    );

    log::info!("Word Wheel: android_main starting");

    let options = eframe::NativeOptions {
        android_app: Some(app),
        renderer: eframe::Renderer::Glow,
        ..Default::default()
    };

    if let Err(e) = eframe::run_native(
        "Word Wheel",
        options,
        Box::new(|_cc| Ok(Box::new(GameApp::new()))),
    ) {
        log::error!("eframe::run_native failed: {e}");
    }
}
