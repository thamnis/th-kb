use std::collections::HashMap;
use serde::{Deserialize, Serialize};

#[derive(Debug, Clone, PartialEq, Serialize, Deserialize, uniffi::Record)]
pub struct SuggestionCandidate {
    pub word: String,
    pub confidence: f64,
    pub is_exact: bool,
}

#[derive(Debug, Clone, Default, Serialize, Deserialize)]
pub struct DictionaryModel {
    #[serde(default)]
    pub words: HashMap<String, u32>,
    #[serde(default)]
    pub bigrams: HashMap<String, HashMap<String, u32>>,
    #[serde(default)]
    pub trigrams: HashMap<String, HashMap<String, u32>>,
}

/// Computes Levenshtein edit distance between two strings.
pub fn edit_distance(a: &str, b: &str) -> usize {
    let a_chars: Vec<char> = a.chars().collect();
    let b_chars: Vec<char> = b.chars().collect();
    let (m, n) = (a_chars.len(), b_chars.len());

    let mut dp = vec![vec![0usize; n + 1]; m + 1];
    for i in 0..=m {
        dp[i][0] = i;
    }
    for j in 0..=n {
        dp[0][j] = j;
    }

    for i in 1..=m {
        for j in 1..=n {
            let cost = if a_chars[i - 1].to_lowercase().next() == b_chars[j - 1].to_lowercase().next() {
                0
            } else {
                1
            };
            dp[i][j] = (dp[i - 1][j] + 1)
                .min(dp[i][j - 1] + 1)
                .min(dp[i - 1][j - 1] + cost);
        }
    }
    dp[m][n]
}

/// Normalizes accented characters for fuzzy search (e.g. môgô -> mogo).
pub fn strip_accents(s: &str) -> String {
    s.chars().map(|c| match c {
        'à' | 'á' | 'â' | 'ã' | 'ä' | 'å' => 'a',
        'è' | 'é' | 'ê' | 'ë' => 'e',
        'ì' | 'í' | 'î' | 'ï' => 'i',
        'ò' | 'ó' | 'ô' | 'õ' | 'ö' => 'o',
        'ù' | 'ú' | 'û' | 'ü' => 'u',
        'ç' => 'c',
        'ñ' => 'n',
        other => other,
    }).collect()
}

/// Evaluates suggestions given dictionary model, preceding context words, and prefix.
pub fn query_suggestions(
    dict: &DictionaryModel,
    context_words: &[String],
    prefix: &str,
    mode: &str,
    max_results: usize,
) -> Vec<SuggestionCandidate> {
    let is_advanced = mode == "advanced_autocomplete";
    let norm_prefix = prefix.trim().to_lowercase();
    let norm_clean_prefix = strip_accents(&norm_prefix);

    let mut scored: Vec<(String, f64, bool)> = Vec::new();

    if norm_prefix.is_empty() {
        // Next-word prediction (Trigram sweet-spot -> Bigram -> Unigram)
        if is_advanced && context_words.len() >= 2 {
            let last_two = format!(
                "{} {}",
                context_words[context_words.len() - 2].to_lowercase(),
                context_words[context_words.len() - 1].to_lowercase()
            );
            if let Some(tri_matches) = dict.trigrams.get(&last_two) {
                for (word, count) in tri_matches {
                    scored.push((word.clone(), *count as f64 * 3.0, false));
                }
            }
        }

        if scored.len() < max_results && context_words.last().is_some() {
            let prev = context_words.last().unwrap().to_lowercase();
            if let Some(bi_matches) = dict.bigrams.get(&prev) {
                for (word, count) in bi_matches {
                    if !scored.iter().any(|(w, _, _)| w == word) {
                        scored.push((word.clone(), *count as f64 * 2.0, false));
                    }
                }
            }
        }

        if scored.len() < max_results {
            let mut top_unigrams: Vec<(&String, &u32)> = dict.words.iter().collect();
            top_unigrams.sort_by(|a, b| b.1.cmp(a.1));
            for (word, count) in top_unigrams.into_iter().take(max_results * 2) {
                if !scored.iter().any(|(w, _, _)| w == word) {
                    scored.push((word.clone(), *count as f64 * 0.5, false));
                }
            }
        }
    } else {
        // Prefix matching + Context boosting
        for (word, count) in &dict.words {
            let word_lower = word.to_lowercase();
            let word_clean = strip_accents(&word_lower);

            let is_exact = word_lower == norm_prefix;
            let starts_with_exact = word_lower.starts_with(&norm_prefix);
            let starts_with_clean = word_clean.starts_with(&norm_clean_prefix);

            if starts_with_exact || starts_with_clean {
                let mut score = *count as f64;
                if is_exact {
                    score += 5000.0;
                } else if starts_with_exact {
                    score += 2000.0;
                } else {
                    score += 1500.0;
                }

                // Shorter words closer to prefix get slight boost
                let len_diff = word_lower.len().saturating_sub(norm_prefix.len());
                score -= (len_diff as f64) * 10.0;

                // Contextual boost in advanced mode
                if is_advanced && context_words.len() >= 2 {
                    let last_two = format!(
                        "{} {}",
                        context_words[context_words.len() - 2].to_lowercase(),
                        context_words[context_words.len() - 1].to_lowercase()
                    );
                    if let Some(tri) = dict.trigrams.get(&last_two) {
                        if let Some(tri_count) = tri.get(word) {
                            score += (*tri_count as f64) * 4.0;
                        }
                    }
                }

                if is_advanced && context_words.last().is_some() {
                    let prev = context_words.last().unwrap().to_lowercase();
                    if let Some(bi) = dict.bigrams.get(&prev) {
                        if let Some(bi_count) = bi.get(word) {
                            score += (*bi_count as f64) * 2.5;
                        }
                    }
                }

                scored.push((word.clone(), score, is_exact));
            } else if is_advanced && norm_prefix.len() >= 3 {
                // Typo tolerance (Levenshtein distance <= 1)
                let dist = edit_distance(&norm_clean_prefix, &word_clean.chars().take(norm_clean_prefix.len() + 1).collect::<String>());
                if dist <= 1 {
                    let score = (*count as f64) * 0.4;
                    scored.push((word.clone(), score, false));
                }
            }
        }
    }

    // Sort descending by score
    scored.sort_by(|a, b| b.1.partial_cmp(&a.1).unwrap_or(std::cmp::Ordering::Equal));

    let mut results: Vec<SuggestionCandidate> = Vec::new();
    for (word, score, is_exact) in scored {
        if !results.iter().any(|r| r.word.eq_ignore_ascii_case(&word)) {
            results.push(SuggestionCandidate {
                word,
                confidence: score,
                is_exact,
            });
            if results.len() >= max_results {
                break
            }
        }
    }

    results
}

/// Uniffi export to query suggestions from a raw JSON dictionary model.
pub fn get_suggestions_from_json(
    dict_json: &str,
    context_words: Vec<String>,
    prefix: String,
    mode: String,
    max_results: u32,
) -> Vec<SuggestionCandidate> {
    let dict: DictionaryModel = match serde_json::from_str(dict_json) {
        Ok(d) => d,
        Err(_) => return Vec::new(),
    };

    query_suggestions(&dict, &context_words, &prefix, &mode, max_results as usize)
}

#[cfg(test)]
mod tests {
    use super::*;

    fn sample_model() -> DictionaryModel {
        let mut words = HashMap::new();
        words.insert("bonjour".to_string(), 900);
        words.insert("bon".to_string(), 1200);
        words.insert("môgô".to_string(), 800);
        words.insert("djossi".to_string(), 750);
        words.insert("enjaillement".to_string(), 700);
        words.insert("sciencer".to_string(), 680);
        words.insert("ya".to_string(), 600);
        words.insert("fohi".to_string(), 580);
        words.insert("chap".to_string(), 550);
        words.insert("chap-chap".to_string(), 520);

        let mut bigrams = HashMap::new();
        let mut ya_next = HashMap::new();
        ya_next.insert("fohi".to_string(), 950);
        bigrams.insert("ya".to_string(), ya_next);

        let mut chap_next = HashMap::new();
        chap_next.insert("chap-chap".to_string(), 900);
        bigrams.insert("chap".to_string(), chap_next);

        let mut trigrams = HashMap::new();
        let mut sci_next = HashMap::new();
        sci_next.insert("sciencer".to_string(), 980);
        trigrams.insert("il faut".to_string(), sci_next);

        DictionaryModel {
            words,
            bigrams,
            trigrams,
        }
    }

    #[test]
    fn autocomplete_prefix_matches_ivorian_nouchi() {
        let dict = sample_model();
        let suggestions = query_suggestions(&dict, &[], "enj", "autocomplete", 3);
        assert!(!suggestions.is_empty());
        assert_eq!(suggestions[0].word, "enjaillement");
    }

    #[test]
    fn advanced_autocomplete_boosts_trigram_context() {
        let dict = sample_model();
        let context = vec!["il".to_string(), "faut".to_string()];
        let suggestions = query_suggestions(&dict, &context, "", "advanced_autocomplete", 3);
        assert!(!suggestions.is_empty());
        assert_eq!(suggestions[0].word, "sciencer");
    }

    #[test]
    fn advanced_autocomplete_bigram_next_word() {
        let dict = sample_model();
        let context = vec!["ya".to_string()];
        let suggestions = query_suggestions(&dict, &context, "", "advanced_autocomplete", 3);
        assert!(!suggestions.is_empty());
        assert_eq!(suggestions[0].word, "fohi");
    }

    #[test]
    fn strips_accents_and_matches_mogo() {
        let dict = sample_model();
        let suggestions = query_suggestions(&dict, &[], "mogo", "advanced_autocomplete", 3);
        assert!(!suggestions.is_empty());
        assert_eq!(suggestions[0].word, "môgô");
    }
}
