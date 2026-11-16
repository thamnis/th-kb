use serde::Deserialize;

/// A single kaomoji entry returned to Kotlin via UniFFI.
#[derive(Debug, Clone, PartialEq, Eq, Deserialize, uniffi::Record)]
pub struct KaomojiEntry {
    pub text: String,
    pub category: String,
    pub tags: Vec<String>,
}

/// Searches kaomoji entries by matching `query` against `category` or any `tag`
/// (case-insensitive). Empty queries return all entries.
pub fn search_kaomoji_from_json(kaomoji_json: &str, query: &str) -> Vec<KaomojiEntry> {
    let Ok(entries) = serde_json::from_str::<Vec<KaomojiEntry>>(kaomoji_json) else {
        return Vec::new();
    };
    let needle = query.trim().to_lowercase();
    if needle.is_empty() {
        return entries;
    }
    entries
        .into_iter()
        .filter(|entry| {
            entry.category.to_lowercase().contains(&needle)
                || entry
                    .tags
                    .iter()
                    .any(|t| t.to_lowercase().contains(&needle))
        })
        .collect()
}

#[cfg(test)]
mod tests {
    use super::*;

    const SAMPLE: &str = r#"[
        {"text": "(^_^)", "category": "happy", "tags": ["smile", "joy"]},
        {"text": "¯\\_(ツ)_/¯", "category": "shrug", "tags": ["dunno", "meh"]}
    ]"#;

    #[test]
    fn empty_query_returns_all() {
        assert_eq!(search_kaomoji_from_json(SAMPLE, "").len(), 2);
    }

    #[test]
    fn matches_tag_case_insensitively() {
        let res = search_kaomoji_from_json(SAMPLE, "SMILE");
        assert_eq!(res.len(), 1);
        assert_eq!(res[0].text, "(^_^)");
    }
}
