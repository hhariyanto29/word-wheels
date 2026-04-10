use word_wheel_core::GameApp;

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
