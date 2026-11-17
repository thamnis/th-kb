pub mod layout;
pub mod search;
pub mod suggestion;

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

/// Queries text suggestion and autocomplete candidates from a JSON dictionary model.
#[uniffi::export]
pub fn get_suggestions(
    dict_json: &str,
    context_words: Vec<String>,
    prefix: String,
    mode: String,
    max_results: u32,
) -> Vec<suggestion::SuggestionCandidate> {
    suggestion::get_suggestions_from_json(dict_json, context_words, prefix, mode, max_results)
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn returns_non_empty_version() {
        assert!(!core_version().is_empty());
    }
}
