use std::net::UdpSocket;
use std::thread;
use th_kb_companion::{
    build_discovery_reply, dispatch_command, parse_remote_command, OsInputBackend,
    DEFAULT_CONTROL_PORT, DEFAULT_DISCOVERY_PORT,
};

fn main() {
    let args: Vec<String> = std::env::args().collect();
    let dry_run = args.iter().any(|a| a == "--dry-run");
    let host_name = std::env::var("HOSTNAME")
        .or_else(|_| std::env::var("COMPUTERNAME"))
        .unwrap_or_else(|_| "Desktop".to_string());

    println!(
        "th-kb-companion listening on UDP :{} (discovery UDP :{}{} )",
        DEFAULT_CONTROL_PORT,
        DEFAULT_DISCOVERY_PORT,
        if dry_run { ", dry-run" } else { "" }
    );

    // Discovery beacon thread
    let discovery_reply = build_discovery_reply(&host_name, DEFAULT_CONTROL_PORT);
    thread::spawn(move || {
        let Ok(sock) = UdpSocket::bind(("0.0.0.0", DEFAULT_DISCOVERY_PORT)) else {
            return;
        };
        let mut buf = [0u8; 256];
        while let Ok((len, src)) = sock.recv_from(&mut buf) {
            if let Ok(msg) = std::str::from_utf8(&buf[..len]) {
                if msg.trim() == "THKB_DISCOVER" {
                    let _ = sock.send_to(discovery_reply.as_bytes(), src);
                }
            }
        }
    });

    let control_sock =
        UdpSocket::bind(("0.0.0.0", DEFAULT_CONTROL_PORT)).expect("bind control UDP port");
    let mut backend = OsInputBackend::new(dry_run);
    let mut buf = [0u8; 8192];
    loop {
        if let Ok((len, _src)) = control_sock.recv_from(&mut buf) {
            if let Ok(raw) = std::str::from_utf8(&buf[..len]) {
                match parse_remote_command(raw) {
                    Ok(cmd) => {
                        if dry_run {
                            println!("cmd: {:?}", cmd);
                        }
                        dispatch_command(&cmd, &mut backend);
                    }
                    Err(e) => eprintln!("invalid packet: {e}"),
                }
            }
        }
    }
}
