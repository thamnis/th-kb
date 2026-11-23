use serde::{Deserialize, Serialize};
use std::process::Command;

pub const DEFAULT_CONTROL_PORT: u16 = 55677;
pub const DEFAULT_DISCOVERY_PORT: u16 = 55678;

#[derive(Debug, Clone, PartialEq, Serialize, Deserialize)]
#[serde(tag = "t", rename_all = "lowercase")]
pub enum RemoteCommand {
    Move { dx: f64, dy: f64 },
    Click { btn: String },
    Scroll { dy: f64 },
    Text { text: String },
    Key { key: String },
}

pub fn parse_remote_command(raw: &str) -> Result<RemoteCommand, serde_json::Error> {
    serde_json::from_str::<RemoteCommand>(raw.trim())
}

pub fn build_discovery_reply(host_name: &str, control_port: u16) -> String {
    let sanitized: String = host_name
        .trim()
        .chars()
        .map(|c| if c == ':' || c == '\n' || c == '\r' { '_' } else { c })
        .collect();
    let label = if sanitized.is_empty() {
        "Desktop"
    } else {
        sanitized.as_str()
    };
    format!("THKB_COMPANION:{label}:{control_port}")
}

pub trait InputBackend {
    fn move_pointer(&mut self, dx: i32, dy: i32);
    fn click(&mut self, button: &str);
    fn scroll(&mut self, steps: i32);
    fn type_text(&mut self, text: &str);
    fn press_key(&mut self, key: &str);
}

#[derive(Default)]
pub struct OsInputBackend {
    dry_run: bool,
}

impl OsInputBackend {
    pub fn new(dry_run: bool) -> Self {
        Self { dry_run }
    }

    fn run_first_available(&self, cmds: &[(&str, Vec<String>)]) {
        if self.dry_run {
            return;
        }
        for (bin, args) in cmds {
            if let Ok(status) = Command::new(bin).args(args).status() {
                if status.success() {
                    return;
                }
            }
        }
    }
}

impl InputBackend for OsInputBackend {
    fn move_pointer(&mut self, dx: i32, dy: i32) {
        let sx = dx.to_string();
        let sy = dy.to_string();
        self.run_first_available(&[
            ("xdotool", vec!["mousemove_relative".into(), "--".into(), sx.clone(), sy.clone()]),
            ("ydotool", vec!["mousemove".into(), "-x".into(), sx, "-y".into(), sy]),
        ]);
    }

    fn click(&mut self, button: &str) {
        let btn_code = match button {
            "right" => "3",
            "middle" => "2",
            _ => "1",
        };
        let ydo_code = match button {
            "right" => "0xC1",
            "middle" => "0xC2",
            _ => "0xC0",
        };
        self.run_first_available(&[
            ("xdotool", vec!["click".into(), btn_code.into()]),
            ("ydotool", vec!["click".into(), ydo_code.into()]),
        ]);
    }

    fn scroll(&mut self, steps: i32) {
        if steps == 0 {
            return;
        }
        let btn = if steps < 0 { "4" } else { "5" };
        let repeat = steps.abs().min(10).to_string();
        self.run_first_available(&[(
            "xdotool",
            vec!["click".into(), "--repeat".into(), repeat, btn.into()],
        )]);
    }

    fn type_text(&mut self, text: &str) {
        if text.is_empty() {
            return;
        }
        self.run_first_available(&[
            ("xdotool", vec!["type".into(), "--delay".into(), "4".into(), "--".into(), text.into()]),
            ("ydotool", vec!["type".into(), text.into()]),
        ]);
    }

    fn press_key(&mut self, key: &str) {
        let xkey = match key {
            "enter" => "Return",
            "backspace" => "BackSpace",
            "tab" => "Tab",
            "esc" => "Escape",
            "up" => "Up",
            "down" => "Down",
            "left" => "Left",
            "right" => "Right",
            "home" => "Home",
            "end" => "End",
            _ => return,
        };
        self.run_first_available(&[("xdotool", vec!["key".into(), xkey.into()])]);
    }
}

pub fn dispatch_command(cmd: &RemoteCommand, backend: &mut dyn InputBackend) {
    match cmd {
        RemoteCommand::Move { dx, dy } => {
            backend.move_pointer(dx.round() as i32, dy.round() as i32);
        }
        RemoteCommand::Click { btn } => backend.click(btn),
        RemoteCommand::Scroll { dy } => backend.scroll(dy.round() as i32),
        RemoteCommand::Text { text } => backend.type_text(text),
        RemoteCommand::Key { key } => backend.press_key(key),
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    #[derive(Default)]
    struct MockBackend {
        events: Vec<String>,
    }

    impl InputBackend for MockBackend {
        fn move_pointer(&mut self, dx: i32, dy: i32) {
            self.events.push(format!("move:{dx},{dy}"));
        }
        fn click(&mut self, button: &str) {
            self.events.push(format!("click:{button}"));
        }
        fn scroll(&mut self, steps: i32) {
            self.events.push(format!("scroll:{steps}"));
        }
        fn type_text(&mut self, text: &str) {
            self.events.push(format!("text:{text}"));
        }
        fn press_key(&mut self, key: &str) {
            self.events.push(format!("key:{key}"));
        }
    }

    #[test]
    fn parses_and_dispatches_all_remote_commands() {
        let mut backend = MockBackend::default();
        let payloads = [
            r#"{"t":"move","dx":12.4,"dy":-8.7}"#,
            r#"{"t":"click","btn":"right"}"#,
            r#"{"t":"scroll","dy":-3.0}"#,
            r#"{"t":"text","text":"Bonjour"}"#,
            r#"{"t":"key","key":"enter"}"#,
        ];
        for p in payloads {
            let cmd = parse_remote_command(p).expect("valid command");
            dispatch_command(&cmd, &mut backend);
        }
        assert_eq!(
            backend.events,
            vec![
                "move:12,-9",
                "click:right",
                "scroll:-3",
                "text:Bonjour",
                "key:enter",
            ]
        );
    }

    #[test]
    fn formats_clean_discovery_reply() {
        assert_eq!(
            build_discovery_reply("my:laptop\n", DEFAULT_CONTROL_PORT),
            "THKB_COMPANION:my_laptop:55677"
        );
    }
}
