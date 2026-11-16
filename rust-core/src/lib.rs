pub mod layout;
pub mod search;

uniffi::setup_scaffolding!();

/// Returns the core engine version string.
#[uniffi::export]
pub fn core_version() -> String {
    env!("CARGO_PKG_VERSION").to_string()
}

/// Looks up accent/variant characters for a given base key from a JSON accent map.
#[uniffi::export]
pub fn lookup_accents(accents_json: &str, key: &str) -> Vec<String> {
    layout::lookup_accents_from_json(accents_json, key)
}

/// Searches a JSON kaomoji catalog by query substring (case-insensitive).
#[uniffi::export]
pub fn search_kaomoji(kaomoji_json: &str, query: &str) -> Vec<search::KaomojiEntry> {
    search::search_kaomoji_from_json(kaomoji_json, query)
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn returns_non_empty_version() {
        assert!(!core_version().is_empty());
    }
}
