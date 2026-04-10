use word_wheel_core::GameApp;

#[cfg(target_arch = "wasm32")]
use eframe::wasm_bindgen::JsCast as _;

/// Entry point for the web app.
#[cfg(target_arch = "wasm32")]
#[wasm_bindgen_futures::main]
async fn main() {
    let web_options = eframe::WebOptions::default();

    wasm_bindgen_futures::spawn_local(async {
        let start_result = eframe::WebRunner::new()
            .start(
                "word_wheel_canvas",
                web_options,
                Box::new(|_cc| Ok(Box::new(GameApp::new()))),
            )
            .await;

        // Remove loading text and show canvas
        let document = web_sys::window()
            .expect("No window")
            .document()
            .expect("No document");

        if let Some(loading) = document.get_element_by_id("loading_text") {
            match start_result {
                Ok(_) => loading.remove(),
                Err(e) => {
                    loading.set_inner_html(
                        &format!("<p>The app has crashed: {}</p>", e),
                    );
                }
            }
        }
    });
}
