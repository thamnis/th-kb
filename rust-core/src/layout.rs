use std::collections::HashMap;

/// Looks up accent/variant characters for `key` in a JSON object mapping
/// base keys (e.g. `"e"`) to arrays of variant strings (e.g. `["é", "è", "ê"]`).
pub fn lookup_accents_from_json(accents_json: &str, key: &str) -> Vec<String> {
    let Ok(map) = serde_json::from_str::<HashMap<String, Vec<String>>>(accents_json) else {
        return Vec::new();
    };
    map.get(key).cloned().unwrap_or_default()
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn finds_accents_for_existing_key() {
        let json = r#"{"e": ["é", "è", "ê", "ë"], "a": ["à", "â"]}"#;
        assert_eq!(
            lookup_accents_from_json(json, "e"),
            vec!["é", "è", "ê", "ë"]
        );
    }

    #[test]
    fn returns_empty_for_missing_or_invalid_json() {
        assert!(lookup_accents_from_json(r#"{"e": ["é"]}"#, "z").is_empty());
        assert!(lookup_accents_from_json("not-json", "e").is_empty());
    }
}
