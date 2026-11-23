use std::net::{Ipv4Addr, UdpSocket};
use std::thread;
use th_kb_companion::{
    build_discovery_reply, dispatch_command, parse_remote_command, OsInputBackend, RemoteCommand,
    DEFAULT_CONTROL_PORT, DEFAULT_DISCOVERY_PORT,
};

fn detect_lan_ip() -> Option<String> {
    let sock = UdpSocket::bind("0.0.0.0:0").ok()?;
    sock.connect("8.8.8.8:80").ok()?;
    Some(sock.local_addr().ok()?.ip().to_string())
}

fn main() {
    let args: Vec<String> = std::env::args().collect();
    let dry_run = args.iter().any(|a| a == "--dry-run");
    let host_name = std::env::var("HOSTNAME")
        .or_else(|_| std::env::var("COMPUTERNAME"))
        .unwrap_or_else(|_| "Desktop".to_string());
    let lan_ip = detect_lan_ip().unwrap_or_else(|| "<this-PC-IP>".to_string());

    println!("================================================");
    println!("  th-kb Companion v{}", env!("CARGO_PKG_VERSION"));
    println!("  Host Name  : {}", host_name.trim());
    println!("  Wi-Fi IP   : {}:{}", lan_ip, DEFAULT_CONTROL_PORT);
    println!("  Discovery  : UDP :{} + 224.0.0.167", DEFAULT_DISCOVERY_PORT);
    if dry_run {
        println!("  Mode       : --dry-run (logging packets only)");
    }
    println!("  Open th-kb Remote on your phone to connect!");
    println!("================================================");

    // Discovery beacon thread (responds to unicast, broadcast, and 224.0.0.167 multicast)
    let discovery_reply = build_discovery_reply(&host_name, DEFAULT_CONTROL_PORT);
    thread::spawn(move || {
        let Ok(sock) = UdpSocket::bind(("0.0.0.0", DEFAULT_DISCOVERY_PORT)) else {
            return;
        };
        let _ = sock.join_multicast_v4(&Ipv4Addr::new(224, 0, 0, 167), &Ipv4Addr::UNSPECIFIED);
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
    let pong_reply = format!("THKB_PONG:{}", host_name.trim());
    let mut backend = OsInputBackend::new(dry_run);
    let mut buf = [0u8; 8192];
    loop {
        if let Ok((len, src)) = control_sock.recv_from(&mut buf) {
            if let Ok(raw) = std::str::from_utf8(&buf[..len]) {
                match parse_remote_command(raw) {
                    Ok(RemoteCommand::Ping) => {
                        let _ = control_sock.send_to(pong_reply.as_bytes(), src);
                    }
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
