use serde::{Deserialize, Serialize};
use std::collections::{HashMap, HashSet};
use std::sync::{Mutex, OnceLock};

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

#[derive(Debug, Clone, Default, Serialize, Deserialize)]
pub struct PersonalModel {
    #[serde(default)]
    pub words: HashMap<String, u32>,
    #[serde(default)]
    pub bigrams: HashMap<String, HashMap<String, u32>>,
    #[serde(default)]
    pub blocked: HashSet<String>,
}

impl PersonalModel {
    pub fn learn(&mut self, prev_word: &str, word: &str, boost: u32) {
        let trimmed = word.trim();
        if trimmed.chars().count() < 2 {
            return;
        }
        let lower = trimmed.to_lowercase();
        self.blocked.remove(&lower);
        self.words
            .entry(trimmed.to_string())
            .and_modify(|c| *c = c.saturating_add(boost))
            .or_insert(boost);
        let prev = prev_word.trim().to_lowercase();
        if !prev.is_empty() {
            self.bigrams
                .entry(prev)
                .or_default()
                .entry(trimmed.to_string())
                .and_modify(|c| *c = c.saturating_add(boost))
                .or_insert(boost);
        }
    }

    pub fn block(&mut self, word: &str) {
        let lower = word.trim().to_lowercase();
        if lower.is_empty() {
            return;
        }
        self.words.retain(|k, _| k.to_lowercase() != lower);
        self.blocked.insert(lower);
    }

    fn is_blocked(&self, word: &str) -> bool {
        self.blocked.contains(&word.to_lowercase())
    }
}

#[derive(Debug, Clone, Copy, PartialEq, Eq)]
pub enum CasingStyle {
    Lower,
    Title,
    AllCaps,
}

#[derive(Debug, Clone)]
struct WordEntry {
    canonical: String,
    lower: String,
    clean: String,
    log_freq: f64,
}

/// High-performance Rust suggestion index using prefix buckets,
/// log-scaled Zipf frequencies, SymSpell delete sets, and layout-aware spatial distances.
#[derive(Debug, Clone, Default)]
pub struct SuggestionIndex {
    entries: Vec<WordEntry>,
    exact_clean: HashMap<String, Vec<usize>>,
    prefix1: HashMap<char, Vec<usize>>,
    prefix2: HashMap<(char, char), Vec<usize>>,
    symspell_deletes: HashMap<String, Vec<u32>>,
    bigrams: HashMap<String, HashMap<String, f64>>,
    trigrams: HashMap<String, HashMap<String, f64>>,
}

impl SuggestionIndex {
    /// Builds a fast runtime index from a word frequency list (`word\tfreq` lines)
    /// and one or more JSON ngram models.
    pub fn build(word_tsv: &str, json_overlays: &[&str]) -> Self {
        let mut word_counts: HashMap<String, u64> = HashMap::new();
        let mut bigrams: HashMap<String, HashMap<String, f64>> = HashMap::new();
        let mut trigrams: HashMap<String, HashMap<String, f64>> = HashMap::new();

        for line in word_tsv.lines() {
            let mut parts = line.split_whitespace();
            if let (Some(w), Some(c_str)) = (parts.next(), parts.next()) {
                if let Ok(c) = c_str.parse::<u64>() {
                    word_counts
                        .entry(w.to_string())
                        .and_modify(|e| *e = (*e).max(c))
                        .or_insert(c);
                }
            }
        }

        for json_str in json_overlays {
            if json_str.trim().is_empty() {
                continue;
            }
            if let Ok(model) = serde_json::from_str::<DictionaryModel>(json_str) {
                for (w, c) in model.words {
                    let c64 = u64::from(c);
                    word_counts
                        .entry(w)
                        .and_modify(|e| *e = (*e).max(c64))
                        .or_insert(c64);
                }
                for (k, next_map) in model.bigrams {
                    let key_lower = k.to_lowercase();
                    let target = bigrams.entry(key_lower).or_default();
                    for (nw, sc) in next_map {
                        let log_s = (f64::from(sc) + 1.0).log10() * 100.0;
                        target
                            .entry(nw)
                            .and_modify(|e| *e = e.max(log_s))
                            .or_insert(log_s);
                    }
                }
                for (k, next_map) in model.trigrams {
                    let key_lower = k.to_lowercase();
                    let target = trigrams.entry(key_lower).or_default();
                    for (nw, sc) in next_map {
                        let log_s = (f64::from(sc) + 1.0).log10() * 100.0;
                        target
                            .entry(nw)
                            .and_modify(|e| *e = e.max(log_s))
                            .or_insert(log_s);
                    }
                }
            }
        }

        let mut entries: Vec<WordEntry> = word_counts
            .into_iter()
            .map(|(canonical, freq)| {
                let lower = canonical.to_lowercase();
                let clean = strip_accents(&lower);
                let log_freq = ((freq as f64) + 1.0).log10() * 100.0;
                WordEntry {
                    canonical,
                    lower,
                    clean,
                    log_freq,
                }
            })
            .collect();

        // Sort descending by frequency so prefix buckets naturally iterate highest-frequency first.
        entries.sort_by(|a, b| {
            b.log_freq
                .partial_cmp(&a.log_freq)
                .unwrap_or(std::cmp::Ordering::Equal)
        });

        let mut exact_clean: HashMap<String, Vec<usize>> = HashMap::new();
        let mut prefix1: HashMap<char, Vec<usize>> = HashMap::new();
        let mut prefix2: HashMap<(char, char), Vec<usize>> = HashMap::new();
        let mut symspell_deletes: HashMap<String, Vec<u32>> = HashMap::new();

        for (idx, entry) in entries.iter().enumerate() {
            exact_clean
                .entry(entry.clean.clone())
                .or_default()
                .push(idx);
            let mut chars = entry.clean.chars();
            if let Some(c1) = chars.next() {
                prefix1.entry(c1).or_default().push(idx);
                if let Some(c2) = chars.next() {
                    prefix2.entry((c1, c2)).or_default().push(idx);
                }
            }

            // Build SymSpell distance-1 delete index for top 15,000 entries of length >= 3
            if idx < 15_000 {
                let c_chars: Vec<char> = entry.clean.chars().collect();
                if c_chars.len() >= 3 {
                    let idx_u32 = idx as u32;
                    for skip in 0..c_chars.len() {
                        let del: String = c_chars
                            .iter()
                            .enumerate()
                            .filter(|&(i, _)| i != skip)
                            .map(|(_, &c)| c)
                            .collect();
                        let list = symspell_deletes.entry(del).or_default();
                        if list.len() < 24 && !list.contains(&idx_u32) {
                            list.push(idx_u32);
                        }
                    }
                }
            }
        }

        Self {
            entries,
            exact_clean,
            prefix1,
            prefix2,
            symspell_deletes,
            bigrams,
            trigrams,
        }
    }

    pub fn query(
        &self,
        context_words: &[String],
        prefix: &str,
        mode: &str,
        layout_id: &str,
        capitalize_hint: bool,
        max_results: usize,
    ) -> Vec<SuggestionCandidate> {
        self.query_with_personal(
            context_words,
            prefix,
            mode,
            layout_id,
            capitalize_hint,
            max_results,
            None,
        )
    }

    #[allow(clippy::too_many_arguments)]
    pub fn query_with_personal(
        &self,
        context_words: &[String],
        prefix: &str,
        mode: &str,
        layout_id: &str,
        capitalize_hint: bool,
        max_results: usize,
        personal: Option<&PersonalModel>,
    ) -> Vec<SuggestionCandidate> {
        let is_advanced = mode == "advanced_autocomplete";
        let trimmed_prefix = prefix.trim();
        let norm_lower = trimmed_prefix.to_lowercase();
        let clean_prefix = strip_accents(&norm_lower);

        let casing = detect_casing(trimmed_prefix, capitalize_hint);

        // Clean Start: if no prefix and no preceding context words, return empty
        // so the toolbar displays tools cleanly (never dumping raw stopwords like "je - est - tu").
        if clean_prefix.is_empty() {
            if context_words.is_empty() {
                return Vec::new();
            }
            return self.query_next_word(context_words, is_advanced, casing, max_results, personal);
        }

        let is_blocked = |w: &str| personal.is_some_and(|p| p.is_blocked(w));
        let personal_word_boost = |w: &str| -> f64 {
            personal
                .and_then(|p| {
                    p.words
                        .iter()
                        .find(|(pw, _)| {
                            pw.eq_ignore_ascii_case(w) || pw.to_lowercase() == w.to_lowercase()
                        })
                        .map(|(_, &cnt)| (f64::from(cnt) + 1.0).log10() * 95.0)
                })
                .unwrap_or(0.0)
        };

        let mut candidates: Vec<(String, f64, bool)> = Vec::new();
        let mut seen_idx: HashSet<usize> = HashSet::new();

        // 0. Check personal / learned custom vocabulary
        if let Some(p) = personal {
            for (p_word, &cnt) in &p.words {
                if p.is_blocked(p_word) {
                    continue;
                }
                let p_lower = p_word.to_lowercase();
                let p_clean = strip_accents(&p_lower);
                let boost = (f64::from(cnt) + 1.0).log10() * 140.0;
                if p_clean == clean_prefix {
                    let mut score = 500.0 + boost;
                    score += self.context_boost(&p_lower, context_words, is_advanced, personal);
                    candidates.push((p_word.clone(), score, true));
                } else if p_clean.starts_with(&clean_prefix) {
                    let len_diff = p_clean
                        .chars()
                        .count()
                        .saturating_sub(clean_prefix.chars().count());
                    let mut score = 460.0 + boost - (len_diff as f64) * 12.0;
                    score += self.context_boost(&p_lower, context_words, is_advanced, personal);
                    candidates.push((p_word.clone(), score, false));
                }
            }
        }

        // 1. Exact lowercase/accent-stripped match lookup
        if let Some(exact_indices) = self.exact_clean.get(&clean_prefix) {
            for &idx in exact_indices {
                if seen_idx.insert(idx) {
                    let entry = &self.entries[idx];
                    if is_blocked(&entry.canonical) {
                        continue;
                    }
                    let is_exact_diacritic = entry.lower == norm_lower;
                    let mut score = entry.log_freq + if is_exact_diacritic { 320.0 } else { 260.0 };
                    score += self.context_boost(&entry.lower, context_words, is_advanced, personal);
                    score += personal_word_boost(&entry.canonical);
                    candidates.push((entry.canonical.clone(), score, true));
                }
            }
        }

        // 2. Prefix lookup from sorted 2-char / 1-char bucket
        let mut p_chars = clean_prefix.chars();
        let bucket_opt: Option<&Vec<usize>> = match (p_chars.next(), p_chars.next()) {
            (Some(c1), Some(c2)) => self.prefix2.get(&(c1, c2)),
            (Some(c1), None) => self.prefix1.get(&c1),
            _ => None,
        };

        if let Some(bucket) = bucket_opt {
            let mut prefix_hits = 0usize;
            for &idx in bucket {
                if seen_idx.contains(&idx) {
                    continue;
                }
                let entry = &self.entries[idx];
                if entry.clean.starts_with(&clean_prefix) {
                    seen_idx.insert(idx);
                    if is_blocked(&entry.canonical) {
                        continue;
                    }
                    let len_diff = entry
                        .clean
                        .chars()
                        .count()
                        .saturating_sub(clean_prefix.chars().count());
                    let exact_diacritic_prefix = entry.lower.starts_with(&norm_lower);
                    let mut score = entry.log_freq
                        + if exact_diacritic_prefix { 180.0 } else { 140.0 }
                        - (len_diff as f64) * 14.0;
                    score += self.context_boost(&entry.lower, context_words, is_advanced, personal);
                    score += personal_word_boost(&entry.canonical);
                    candidates.push((entry.canonical.clone(), score, false));
                    prefix_hits += 1;
                    // Bucket is pre-sorted by frequency; inspecting top 60 prefix hits is plenty.
                    if prefix_hits >= 60 {
                        break;
                    }
                }
            }
        }

        // 3. Advanced Autocomplete: SymSpell distance-1 typo & spatial key proximity
        let prefix_len = clean_prefix.chars().count();
        if is_advanced && prefix_len >= 3 {
            let p_vec: Vec<char> = clean_prefix.chars().collect();
            let mut probe_keys: Vec<String> = Vec::with_capacity(p_vec.len() + 1);
            probe_keys.push(clean_prefix.clone());
            for skip in 0..p_vec.len() {
                let del: String = p_vec
                    .iter()
                    .enumerate()
                    .filter(|&(i, _)| i != skip)
                    .map(|(_, &c)| c)
                    .collect();
                probe_keys.push(del);
            }

            let mut fuzzy_checked = 0usize;
            for key in probe_keys {
                if let Some(hit_indices) = self.symspell_deletes.get(&key) {
                    for &idx_u32 in hit_indices {
                        let idx = idx_u32 as usize;
                        if seen_idx.contains(&idx) {
                            continue;
                        }
                        let entry = &self.entries[idx];
                        if is_blocked(&entry.canonical) {
                            continue;
                        }
                        let w_len = entry.clean.chars().count();
                        if w_len.abs_diff(prefix_len) > 1 {
                            continue;
                        }
                        let dist = spatial_damerau_distance(&clean_prefix, &entry.clean, layout_id);
                        if dist <= 1.15 {
                            seen_idx.insert(idx);
                            let mut score = entry.log_freq + (1.2 - dist) * 190.0;
                            score +=
                                self.context_boost(&entry.lower, context_words, true, personal);
                            score += personal_word_boost(&entry.canonical);
                            candidates.push((entry.canonical.clone(), score, false));
                            fuzzy_checked += 1;
                            if fuzzy_checked >= 40 {
                                break;
                            }
                        }
                    }
                }
                if fuzzy_checked >= 40 {
                    break;
                }
            }
        }

        candidates.sort_by(|a, b| b.1.partial_cmp(&a.1).unwrap_or(std::cmp::Ordering::Equal));

        let mut results = Vec::new();
        for (word, score, is_exact) in candidates {
            let cased = apply_casing(&word, casing);
            if !results
                .iter()
                .any(|r: &SuggestionCandidate| r.word.eq_ignore_ascii_case(&cased))
            {
                results.push(SuggestionCandidate {
                    word: cased,
                    confidence: score,
                    is_exact,
                });
                if results.len() >= max_results {
                    break;
                }
            }
        }

        results
    }

    fn query_next_word(
        &self,
        context_words: &[String],
        is_advanced: bool,
        casing: CasingStyle,
        max_results: usize,
        personal: Option<&PersonalModel>,
    ) -> Vec<SuggestionCandidate> {
        let mut next_candidates: Vec<(String, f64)> = Vec::new();
        let is_blocked = |w: &str| personal.is_some_and(|p| p.is_blocked(w));

        if is_advanced && context_words.len() >= 2 {
            let last_two = format!(
                "{} {}",
                context_words[context_words.len() - 2].to_lowercase(),
                context_words[context_words.len() - 1].to_lowercase()
            );
            if let Some(tri) = self.trigrams.get(&last_two) {
                for (w, s) in tri {
                    if !is_blocked(w) {
                        next_candidates.push((w.clone(), *s * 2.2));
                    }
                }
            }
        }

        if let Some(prev) = context_words.last() {
            let prev_lower = prev.to_lowercase();
            // User's learned bigram transitions get top personalization weight
            if let Some(p) = personal {
                if let Some(p_bi) = p.bigrams.get(&prev_lower) {
                    for (w, &cnt) in p_bi {
                        if !is_blocked(w) {
                            let s = (f64::from(cnt) + 1.0).log10() * 180.0 + 480.0;
                            next_candidates.push((w.clone(), s));
                        }
                    }
                }
            }
            if let Some(bi) = self.bigrams.get(&prev_lower) {
                for (w, s) in bi {
                    if !is_blocked(w)
                        && !next_candidates
                            .iter()
                            .any(|(ew, _)| ew.eq_ignore_ascii_case(w))
                    {
                        next_candidates.push((w.clone(), *s * 1.4));
                    }
                }
            }
        }

        // Do not pad with unrelated random unigrams when context produces no bigram/trigram predictions
        if next_candidates.is_empty() {
            return Vec::new();
        }

        next_candidates.sort_by(|a, b| b.1.partial_cmp(&a.1).unwrap_or(std::cmp::Ordering::Equal));

        let mut results = Vec::new();
        for (word, score) in next_candidates {
            let cased = apply_casing(&word, casing);
            if !results
                .iter()
                .any(|r: &SuggestionCandidate| r.word.eq_ignore_ascii_case(&cased))
            {
                results.push(SuggestionCandidate {
                    word: cased,
                    confidence: score,
                    is_exact: false,
                });
                if results.len() >= max_results {
                    break;
                }
            }
        }
        results
    }

    fn context_boost(
        &self,
        word_lower: &str,
        context_words: &[String],
        is_advanced: bool,
        personal: Option<&PersonalModel>,
    ) -> f64 {
        if !is_advanced || context_words.is_empty() {
            return 0.0;
        }
        let mut boost = 0.0;
        if context_words.len() >= 2 {
            let last_two = format!(
                "{} {}",
                context_words[context_words.len() - 2].to_lowercase(),
                context_words[context_words.len() - 1].to_lowercase()
            );
            if let Some(tri) = self.trigrams.get(&last_two) {
                if let Some(s) = tri.get(word_lower) {
                    boost += *s * 1.5;
                }
            }
        }
        if let Some(prev) = context_words.last() {
            let prev_lower = prev.to_lowercase();
            if let Some(p) = personal {
                if let Some(p_bi) = p.bigrams.get(&prev_lower) {
                    for (pw, &cnt) in p_bi {
                        if pw.to_lowercase() == word_lower {
                            boost += (f64::from(cnt) + 1.0).log10() * 140.0 + 120.0;
                        }
                    }
                }
            }
            if let Some(bi) = self.bigrams.get(&prev_lower) {
                if let Some(s) = bi.get(word_lower) {
                    boost += *s * 0.9;
                }
            }
        }
        boost
    }
}

/// Detects intended casing style from user input and active shift/auto-cap state.
pub fn detect_casing(prefix: &str, capitalize_hint: bool) -> CasingStyle {
    let letters: Vec<char> = prefix.chars().filter(|c| c.is_alphabetic()).collect();
    if letters.len() >= 2 && letters.iter().all(|c| c.is_uppercase()) {
        CasingStyle::AllCaps
    } else if letters.first().is_some_and(|c| c.is_uppercase()) || capitalize_hint {
        CasingStyle::Title
    } else {
        CasingStyle::Lower
    }
}

/// Formats a canonical dictionary word into the target casing style.
pub fn apply_casing(canonical: &str, style: CasingStyle) -> String {
    match style {
        CasingStyle::AllCaps => canonical.to_uppercase(),
        CasingStyle::Title => {
            let mut chars = canonical.chars();
            match chars.next() {
                None => String::new(),
                Some(first) => {
                    let mut out = first.to_uppercase().to_string();
                    out.push_str(chars.as_str());
                    out
                }
            }
        }
        CasingStyle::Lower => canonical.to_string(),
    }
}

/// Normalizes accented characters for accent-insensitive prefix matching.
pub fn strip_accents(s: &str) -> String {
    s.chars()
        .map(|c| match c {
            'à' | 'á' | 'â' | 'ã' | 'ä' | 'å' => 'a',
            'è' | 'é' | 'ê' | 'ë' => 'e',
            'ì' | 'í' | 'î' | 'ï' => 'i',
            'ò' | 'ó' | 'ô' | 'õ' | 'ö' => 'o',
            'ù' | 'ú' | 'û' | 'ü' => 'u',
            'ç' => 'c',
            'ñ' => 'n',
            'ÿ' => 'y',
            other => other,
        })
        .collect()
}

/// Computes physical 2D key substitution cost based on QWERTY/AZERTY proximity.
fn key_substitution_cost(a: char, b: char, layout_id: &str) -> f64 {
    if a == b {
        return 0.0;
    }
    let pos_a = key_coords(a, layout_id);
    let pos_b = key_coords(b, layout_id);
    if let (Some((ra, ca)), Some((rb, cb))) = (pos_a, pos_b) {
        let dr = ra - rb;
        let dc = ca - cb;
        let dist_sq = dr * dr + dc * dc;
        if dist_sq <= 2.1 {
            // Adjacent key on virtual layout ("fat-finger" tolerance)
            return 0.35;
        }
    }
    1.0
}

fn key_coords(ch: char, layout_id: &str) -> Option<(f64, f64)> {
    let rows: [&str; 3] = if layout_id.contains("fr") {
        ["azertyuiop", "qsdfghjklm", "wxcvbn"]
    } else {
        ["qwertyuiop", "asdfghjkl", "zxcvbnm"]
    };
    let offsets = [0.0_f64, 0.25, 0.75];
    for (r_idx, row) in rows.iter().enumerate() {
        if let Some(c_idx) = row.chars().position(|c| c == ch) {
            return Some((r_idx as f64, (c_idx as f64) + offsets[r_idx]));
        }
    }
    None
}

/// Layout-aware Damerau-Levenshtein distance with adjacent key discount.
pub fn spatial_damerau_distance(a: &str, b: &str, layout_id: &str) -> f64 {
    let a_chars: Vec<char> = a.chars().collect();
    let b_chars: Vec<char> = b.chars().collect();
    let m = a_chars.len();
    let n = b_chars.len();

    let mut dp = vec![vec![0.0_f64; n + 1]; m + 1];
    for (i, row) in dp.iter_mut().enumerate().take(m + 1) {
        row[0] = i as f64;
    }
    for (j, cell) in dp[0].iter_mut().enumerate().take(n + 1) {
        *cell = j as f64;
    }

    for i in 1..=m {
        for j in 1..=n {
            let sub_cost = key_substitution_cost(a_chars[i - 1], b_chars[j - 1], layout_id);
            let mut best = (dp[i - 1][j] + 1.0)
                .min(dp[i][j - 1] + 1.0)
                .min(dp[i - 1][j - 1] + sub_cost);

            if i > 1
                && j > 1
                && a_chars[i - 1] == b_chars[j - 2]
                && a_chars[i - 2] == b_chars[j - 1]
            {
                best = best.min(dp[i - 2][j - 2] + 0.65);
            }
            dp[i][j] = best;
        }
    }
    dp[m][n]
}

/// UniFFI export for querying a single JSON dictionary directly.
pub fn get_suggestions_from_json(
    dict_json: &str,
    context_words: Vec<String>,
    prefix: String,
    mode: String,
    max_results: u32,
) -> Vec<SuggestionCandidate> {
    let index = SuggestionIndex::build("", &[dict_json]);
    index.query(
        &context_words,
        &prefix,
        &mode,
        "azerty_fr",
        false,
        max_results as usize,
    )
}

#[derive(Debug, Clone, Deserialize)]
pub struct SwipePoint {
    pub x: f64,
    pub y: f64,
}

#[derive(Debug, Clone, Deserialize)]
pub struct KeyCenter {
    pub ch: String,
    pub x: f64,
    pub y: f64,
}

fn dist2d(a: (f64, f64), b: (f64, f64)) -> f64 {
    let dx = a.0 - b.0;
    let dy = a.1 - b.1;
    (dx * dx + dy * dy).sqrt()
}

fn point_to_segment_dist(p: (f64, f64), a: (f64, f64), b: (f64, f64)) -> f64 {
    let abx = b.0 - a.0;
    let aby = b.1 - a.1;
    let ab_len2 = abx * abx + aby * aby;
    if ab_len2 <= 1e-6 {
        return dist2d(p, a);
    }
    let t = (((p.0 - a.0) * abx + (p.1 - a.1) * aby) / ab_len2).clamp(0.0, 1.0);
    let proj = (a.0 + t * abx, a.1 + t * aby);
    dist2d(p, proj)
}

fn resample_swipe_path(raw: &[SwipePoint], target_count: usize) -> Option<(Vec<(f64, f64)>, f64)> {
    if raw.len() < 2 || target_count < 2 {
        return None;
    }
    let mut cleaned: Vec<(f64, f64)> = Vec::with_capacity(raw.len());
    for pt in raw {
        let p = (pt.x, pt.y);
        if cleaned.last().is_none_or(|&prev| dist2d(prev, p) >= 2.0) {
            cleaned.push(p);
        }
    }
    if cleaned.len() < 2 {
        cleaned.push((raw[raw.len() - 1].x, raw[raw.len() - 1].y));
    }

    let mut cum_lens = Vec::with_capacity(cleaned.len());
    cum_lens.push(0.0);
    for pair in cleaned.windows(2) {
        let seg = dist2d(pair[0], pair[1]);
        let prev = *cum_lens.last().unwrap_or(&0.0);
        cum_lens.push(prev + seg);
    }
    let total_len = *cum_lens.last().unwrap_or(&0.0);
    if total_len < 12.0 {
        return None;
    }

    let mut out = Vec::with_capacity(target_count);
    let mut seg_idx = 0usize;
    for step in 0..target_count {
        let target_d = (step as f64 / (target_count - 1) as f64) * total_len;
        while seg_idx + 1 < cum_lens.len() && cum_lens[seg_idx + 1] < target_d {
            seg_idx += 1;
        }
        if seg_idx + 1 >= cleaned.len() {
            out.push(cleaned[cleaned.len() - 1]);
        } else {
            let d0 = cum_lens[seg_idx];
            let d1 = cum_lens[seg_idx + 1];
            let span = (d1 - d0).max(1e-6);
            let t = ((target_d - d0) / span).clamp(0.0, 1.0);
            let p0 = cleaned[seg_idx];
            let p1 = cleaned[seg_idx + 1];
            out.push((p0.0 + (p1.0 - p0.0) * t, p0.1 + (p1.1 - p0.1) * t));
        }
    }
    Some((out, total_len))
}

fn estimate_key_unit(centers: &HashMap<char, (f64, f64)>) -> f64 {
    let mut min_dists: Vec<f64> = centers
        .iter()
        .filter_map(|(&ch_a, &pa)| {
            centers
                .iter()
                .filter(|(&ch_b, _)| ch_b != ch_a)
                .map(|(_, &pb)| dist2d(pa, pb))
                .min_by(|x, y| x.partial_cmp(y).unwrap_or(std::cmp::Ordering::Equal))
        })
        .collect();
    if min_dists.is_empty() {
        return 80.0;
    }
    min_dists.sort_by(|x, y| x.partial_cmp(y).unwrap_or(std::cmp::Ordering::Equal));
    min_dists[min_dists.len() / 2].max(24.0)
}

/// FUTO/LatinIME-inspired inflection detector: extracts sharp turning points (corners)
/// along the resampled gesture path so candidate words that match user pivot keys win.
fn detect_inflection_points(resampled: &[(f64, f64)], key_unit: f64) -> Vec<((f64, f64), f64)> {
    let n = resampled.len();
    if n < 5 {
        return Vec::new();
    }
    let min_arm = key_unit * 0.18;
    let mut raw_corners: Vec<(usize, (f64, f64), f64)> = Vec::new();
    for i in 2..(n - 2) {
        let p_prev = resampled[i - 2];
        let p_curr = resampled[i];
        let p_next = resampled[i + 2];
        let ux = p_curr.0 - p_prev.0;
        let uy = p_curr.1 - p_prev.1;
        let vx = p_next.0 - p_curr.0;
        let vy = p_next.1 - p_curr.1;
        let u_len = (ux * ux + uy * uy).sqrt();
        let v_len = (vx * vx + vy * vy).sqrt();
        if u_len < min_arm || v_len < min_arm {
            continue;
        }
        let cos_theta = ((ux * vx + uy * vy) / (u_len * v_len)).clamp(-1.0, 1.0);
        if cos_theta < 0.72 {
            let sharpness = (0.75 - cos_theta).clamp(0.05, 1.5);
            if let Some(last) = raw_corners.last_mut() {
                if i <= last.0 + 2 {
                    if sharpness > last.2 {
                        *last = (i, p_curr, sharpness);
                    }
                    continue;
                }
            }
            raw_corners.push((i, p_curr, sharpness));
        }
    }
    raw_corners
        .into_iter()
        .map(|(_, pt, weight)| (pt, weight))
        .collect()
}

#[allow(clippy::too_many_arguments)]
fn score_swipe_word(
    clean_word: &str,
    log_freq: f64,
    context_bonus: f64,
    centers: &HashMap<char, (f64, f64)>,
    resampled: &[(f64, f64)],
    inflections: &[((f64, f64), f64)],
    total_len: f64,
    key_unit: f64,
) -> Option<f64> {
    let mut dedup_pts: Vec<(f64, f64)> = Vec::new();
    let mut prev_ch: Option<char> = None;
    for ch in clean_word.chars() {
        if !ch.is_alphabetic() {
            continue;
        }
        if Some(ch) == prev_ch {
            continue;
        }
        let &pt = centers.get(&ch)?;
        dedup_pts.push(pt);
        prev_ch = Some(ch);
    }

    if dedup_pts.is_empty() {
        return None;
    }
    if dedup_pts.len() == 1 {
        // Double-letter words on one key require short localized swipe
        let d_start = dist2d(dedup_pts[0], resampled[0]);
        let d_end = dist2d(dedup_pts[0], resampled[resampled.len() - 1]);
        if d_start > key_unit * 0.9 || d_end > key_unit * 0.9 || total_len > key_unit * 1.4 {
            return None;
        }
        return Some(log_freq * 8.0 + context_bonus - ((d_start + d_end) / key_unit) * 14.0);
    }

    let k = dedup_pts.len();
    let m = resampled.len();
    let start_d = dist2d(dedup_pts[0], resampled[0]);
    let end_d = dist2d(dedup_pts[k - 1], resampled[m - 1]);
    if start_d > key_unit * 1.4 || end_d > key_unit * 1.4 {
        return None;
    }

    // Monotone DP alignment of k key centers onto M resampled swipe points
    let mut dp = vec![f64::INFINITY; m];
    for (j, dp_slot) in dp.iter_mut().enumerate().take(m.min(3)) {
        *dp_slot = dist2d(dedup_pts[0], resampled[j]);
    }
    for &key_pt in dedup_pts.iter().skip(1) {
        let mut next = vec![f64::INFINITY; m];
        let mut running_best = f64::INFINITY;
        for (j, &swipe_pt) in resampled.iter().enumerate() {
            running_best = running_best.min(dp[j]);
            if running_best.is_finite() {
                next[j] = running_best + dist2d(key_pt, swipe_pt);
            }
        }
        dp = next;
    }
    let forward_sum = dp[m.saturating_sub(3)..m]
        .iter()
        .copied()
        .fold(f64::INFINITY, f64::min);
    if !forward_sum.is_finite() {
        return None;
    }
    let forward_avg = forward_sum / (k as f64);
    if forward_avg > key_unit * 1.4 {
        return None;
    }

    // Reverse fidelity: measure swipe points' distance to the word's polyline segments
    let mut reverse_sum = 0.0;
    for &sp in resampled {
        let mut best_seg_d = f64::INFINITY;
        for seg in dedup_pts.windows(2) {
            best_seg_d = best_seg_d.min(point_to_segment_dist(sp, seg[0], seg[1]));
        }
        reverse_sum += best_seg_d;
    }
    let reverse_avg = reverse_sum / (m as f64);
    if reverse_avg > key_unit * 1.5 {
        return None;
    }

    // Inflection/corner anchor alignment: sharp turns in user gesture should land near word keys
    let mut corner_penalty = 0.0;
    for &(corner_pt, weight) in inflections {
        let min_key_d = dedup_pts
            .iter()
            .map(|&kp| dist2d(kp, corner_pt))
            .fold(f64::INFINITY, f64::min);
        corner_penalty += (min_key_d / key_unit) * 11.0 * weight;
    }

    let ideal_len: f64 = dedup_pts.windows(2).map(|seg| dist2d(seg[0], seg[1])).sum();
    if total_len > ideal_len * 2.1 + key_unit * 1.2 || ideal_len > total_len * 2.1 + key_unit * 1.2
    {
        return None;
    }
    let len_diff = (total_len - ideal_len).abs();

    let sigma2 = 2.0 * (key_unit * 0.68).powi(2);
    let gaussian_endpoints =
        ((-start_d * start_d / sigma2).exp() + (-end_d * end_d / sigma2).exp()) * 8.0;

    let score = log_freq * 3.0 + context_bonus * 0.25 + gaussian_endpoints
        - (forward_avg / key_unit) * 24.0
        - (reverse_avg / key_unit) * 26.0
        - ((start_d + end_d) / key_unit) * 18.0
        - corner_penalty
        - (len_diff / key_unit) * 7.5;

    Some(score)
}

#[allow(clippy::too_many_arguments)]
pub fn decode_swipe_with_overlay(
    base: &SuggestionIndex,
    overlay: Option<&SuggestionIndex>,
    personal: Option<&PersonalModel>,
    context_words: &[String],
    centers_list: &[KeyCenter],
    swipe_points: &[SwipePoint],
    capitalize_hint: bool,
    max_results: usize,
) -> Vec<SuggestionCandidate> {
    let mut centers: HashMap<char, (f64, f64)> = HashMap::new();
    for kc in centers_list {
        let clean = strip_accents(&kc.ch.to_lowercase());
        if let Some(ch) = clean.chars().next() {
            if ch.is_alphabetic() {
                centers.insert(ch, (kc.x, kc.y));
            }
        }
    }
    if centers.is_empty() {
        return Vec::new();
    }

    let key_unit = estimate_key_unit(&centers);
    let Some((resampled, total_len)) = resample_swipe_path(swipe_points, 28) else {
        return Vec::new();
    };
    if total_len < key_unit * 0.45 {
        return Vec::new();
    }

    let start_pt = resampled[0];
    let end_pt = resampled[resampled.len() - 1];

    let mut start_keys: Vec<(char, f64)> = centers
        .iter()
        .map(|(&ch, &pt)| (ch, dist2d(pt, start_pt)))
        .filter(|&(_, d)| d <= key_unit * 2.1)
        .collect();
    start_keys.sort_by(|a, b| a.1.partial_cmp(&b.1).unwrap_or(std::cmp::Ordering::Equal));
    start_keys.truncate(4);

    let end_set: HashSet<char> = {
        let mut ek: Vec<(char, f64)> = centers
            .iter()
            .map(|(&ch, &pt)| (ch, dist2d(pt, end_pt)))
            .filter(|&(_, d)| d <= key_unit * 2.1)
            .collect();
        ek.sort_by(|a, b| a.1.partial_cmp(&b.1).unwrap_or(std::cmp::Ordering::Equal));
        ek.truncate(4);
        ek.into_iter().map(|(ch, _)| ch).collect()
    };

    if start_keys.is_empty() || end_set.is_empty() {
        return Vec::new();
    }
    let start_chars: HashSet<char> = start_keys.iter().map(|&(ch, _)| ch).collect();

    let is_blocked = |word: &str| personal.is_some_and(|p| p.is_blocked(word));
    let inflections = detect_inflection_points(&resampled, key_unit);
    let mut best_by_lower: HashMap<String, (String, f64)> = HashMap::new();

    let mut score_entry =
        |canonical: &str, clean: &str, log_freq: f64, ctx_source: &SuggestionIndex| {
            if clean.chars().count() < 2 || is_blocked(canonical) {
                return;
            }
            let last_alpha = clean.chars().rev().find(|c| c.is_alphabetic());
            if !last_alpha.is_some_and(|c| end_set.contains(&c)) {
                return;
            }
            let lower = canonical.to_lowercase();
            let ctx_bonus = ctx_source.context_boost(&lower, context_words, true, personal);
            if let Some(score) = score_swipe_word(
                clean,
                log_freq,
                ctx_bonus,
                &centers,
                &resampled,
                &inflections,
                total_len,
                key_unit,
            ) {
                best_by_lower
                    .entry(lower)
                    .and_modify(|existing| {
                        if score > existing.1 {
                            *existing = (canonical.to_string(), score);
                        }
                    })
                    .or_insert_with(|| (canonical.to_string(), score));
            }
        };

    for &start_ch in &start_chars {
        if let Some(indices) = base.prefix1.get(&start_ch) {
            for &idx in indices {
                let entry = &base.entries[idx];
                score_entry(&entry.canonical, &entry.clean, entry.log_freq, base);
            }
        }
        if let Some(ov) = overlay {
            if let Some(indices) = ov.prefix1.get(&start_ch) {
                for &idx in indices {
                    let entry = &ov.entries[idx];
                    // Give active dialect packs a slight boost over obscure generic entries
                    score_entry(&entry.canonical, &entry.clean, entry.log_freq + 0.6, ov);
                }
            }
        }
    }

    if let Some(p) = personal {
        for (word, &cnt) in &p.words {
            let clean = strip_accents(&word.to_lowercase());
            let first_ch = clean.chars().next();
            if first_ch.is_some_and(|c| start_chars.contains(&c)) {
                let boost_freq = (f64::from(cnt) + 1.0).log10() + 5.4;
                score_entry(word, &clean, boost_freq, base);
            }
        }
    }

    let casing = if capitalize_hint {
        CasingStyle::Title
    } else {
        CasingStyle::Lower
    };

    let mut ranked: Vec<(String, f64)> = best_by_lower.into_values().collect();
    ranked.sort_by(|a, b| b.1.partial_cmp(&a.1).unwrap_or(std::cmp::Ordering::Equal));

    ranked
        .into_iter()
        .take(max_results.max(1))
        .map(|(word, score)| SuggestionCandidate {
            word: apply_casing(&word, casing),
            confidence: score,
            is_exact: false,
        })
        .collect()
}

// ---------------------------------------------------------------------------
// Global cached engine + Android JNI entrypoints
// ---------------------------------------------------------------------------

#[derive(Default)]
struct EngineCache {
    en: Option<SuggestionIndex>,
    fr: Option<SuggestionIndex>,
    dialect_overlay: Option<SuggestionIndex>,
    personal: PersonalModel,
}

static GLOBAL_ENGINE: OnceLock<Mutex<EngineCache>> = OnceLock::new();

fn engine_mutex() -> &'static Mutex<EngineCache> {
    GLOBAL_ENGINE.get_or_init(|| Mutex::new(EngineCache::default()))
}

#[allow(clippy::too_many_arguments)]
pub fn query_with_dialect_overlay(
    base: &SuggestionIndex,
    overlay: Option<&SuggestionIndex>,
    context_words: &[String],
    prefix: &str,
    mode: &str,
    layout_id: &str,
    capitalize_hint: bool,
    max_results: usize,
    personal: Option<&PersonalModel>,
) -> Vec<SuggestionCandidate> {
    let mut combined = base.query_with_personal(
        context_words,
        prefix,
        mode,
        layout_id,
        capitalize_hint,
        max_results,
        personal,
    );
    if let Some(ov) = overlay {
        let ov_hits = ov.query_with_personal(
            context_words,
            prefix,
            mode,
            layout_id,
            capitalize_hint,
            max_results,
            personal,
        );
        for hit in ov_hits {
            if let Some(existing) = combined
                .iter_mut()
                .find(|c| c.word.eq_ignore_ascii_case(&hit.word))
            {
                if hit.confidence > existing.confidence {
                    existing.confidence = hit.confidence;
                }
                existing.is_exact = existing.is_exact || hit.is_exact;
            } else {
                combined.push(hit);
            }
        }
        combined.sort_by(|a, b| {
            b.confidence
                .partial_cmp(&a.confidence)
                .unwrap_or(std::cmp::Ordering::Equal)
        });
        combined.truncate(max_results);
    }
    combined
}

#[no_mangle]
pub extern "system" fn Java_com_thamnis_thkb_engine_NativeSuggestionBridge_nativeInitDictionaries(
    mut env: jni::JNIEnv,
    _class: jni::objects::JClass,
    en_tsv: jni::objects::JString,
    en_common_json: jni::objects::JString,
    fr_tsv: jni::objects::JString,
    fr_common_json: jni::objects::JString,
    initial_overlay_json: jni::objects::JString,
) -> jni::sys::jboolean {
    let read_str = |env: &mut jni::JNIEnv, s: jni::objects::JString| -> String {
        env.get_string(&s)
            .map(|js| js.to_string_lossy().into_owned())
            .unwrap_or_default()
    };

    let en_tsv_s = read_str(&mut env, en_tsv);
    let en_json_s = read_str(&mut env, en_common_json);
    let fr_tsv_s = read_str(&mut env, fr_tsv);
    let fr_json_s = read_str(&mut env, fr_common_json);
    let overlay_s = read_str(&mut env, initial_overlay_json);

    let en_idx = SuggestionIndex::build(&en_tsv_s, &[&en_json_s]);
    let fr_idx = SuggestionIndex::build(&fr_tsv_s, &[&fr_json_s]);
    let ov_idx = if overlay_s.trim().is_empty() {
        None
    } else {
        Some(SuggestionIndex::build("", &[&overlay_s]))
    };

    if let Ok(mut guard) = engine_mutex().lock() {
        guard.en = Some(en_idx);
        guard.fr = Some(fr_idx);
        guard.dialect_overlay = ov_idx;
        1
    } else {
        0
    }
}

#[no_mangle]
pub extern "system" fn Java_com_thamnis_thkb_engine_NativeSuggestionBridge_nativeSyncDialects(
    mut env: jni::JNIEnv,
    _class: jni::objects::JClass,
    overlay_json: jni::objects::JString,
) -> jni::sys::jboolean {
    let overlay_s: String = env
        .get_string(&overlay_json)
        .map(|s| s.to_string_lossy().into_owned())
        .unwrap_or_default();

    let ov_idx = if overlay_s.trim().is_empty() {
        None
    } else {
        Some(SuggestionIndex::build("", &[&overlay_s]))
    };

    if let Ok(mut guard) = engine_mutex().lock() {
        guard.dialect_overlay = ov_idx;
        1
    } else {
        0
    }
}

#[allow(clippy::too_many_arguments)]
#[no_mangle]
pub extern "system" fn Java_com_thamnis_thkb_engine_NativeSuggestionBridge_nativeQuerySuggestions(
    mut env: jni::JNIEnv,
    _class: jni::objects::JClass,
    layout_id: jni::objects::JString,
    context_joined: jni::objects::JString,
    prefix: jni::objects::JString,
    mode: jni::objects::JString,
    _ivorian_enabled: jni::sys::jboolean,
    capitalize_hint: jni::sys::jboolean,
    max_results: jni::sys::jint,
) -> jni::sys::jstring {
    let layout_s: String = env
        .get_string(&layout_id)
        .map(|s| s.to_string_lossy().into_owned())
        .unwrap_or_default();
    let ctx_s: String = env
        .get_string(&context_joined)
        .map(|s| s.to_string_lossy().into_owned())
        .unwrap_or_default();
    let prefix_s: String = env
        .get_string(&prefix)
        .map(|s| s.to_string_lossy().into_owned())
        .unwrap_or_default();
    let mode_s: String = env
        .get_string(&mode)
        .map(|s| s.to_string_lossy().into_owned())
        .unwrap_or_else(|_| "advanced_autocomplete".to_string());

    let context_words: Vec<String> = ctx_s
        .split('\u{001F}')
        .filter(|s| !s.is_empty())
        .map(ToString::to_string)
        .collect();

    let is_fr = layout_s.contains("fr") || layout_s.contains("ci");

    let candidates = if let Ok(guard) = engine_mutex().lock() {
        let index_opt = if is_fr {
            guard.fr.as_ref()
        } else {
            guard.en.as_ref()
        };
        index_opt
            .map(|idx| {
                query_with_dialect_overlay(
                    idx,
                    guard.dialect_overlay.as_ref(),
                    &context_words,
                    &prefix_s,
                    &mode_s,
                    &layout_s,
                    capitalize_hint != 0,
                    max_results.max(1) as usize,
                    Some(&guard.personal),
                )
            })
            .unwrap_or_default()
    } else {
        Vec::new()
    };

    let json = serde_json::to_string(&candidates).unwrap_or_else(|_| "[]".to_string());
    env.new_string(json)
        .map(|s| s.into_raw())
        .unwrap_or(std::ptr::null_mut())
}

#[no_mangle]
pub extern "system" fn Java_com_thamnis_thkb_engine_NativeSuggestionBridge_nativeSyncPersonalDictionary(
    mut env: jni::JNIEnv,
    _class: jni::objects::JClass,
    personal_json: jni::objects::JString,
) -> jni::sys::jboolean {
    let json_s: String = env
        .get_string(&personal_json)
        .map(|s| s.to_string_lossy().into_owned())
        .unwrap_or_default();

    let model: PersonalModel = serde_json::from_str(&json_s).unwrap_or_default();
    if let Ok(mut guard) = engine_mutex().lock() {
        guard.personal = model;
        1
    } else {
        0
    }
}

#[no_mangle]
pub extern "system" fn Java_com_thamnis_thkb_engine_NativeSuggestionBridge_nativeLearnWord(
    mut env: jni::JNIEnv,
    _class: jni::objects::JClass,
    prev_word: jni::objects::JString,
    word: jni::objects::JString,
    weight_boost: jni::sys::jint,
) {
    let prev_s: String = env
        .get_string(&prev_word)
        .map(|s| s.to_string_lossy().into_owned())
        .unwrap_or_default();
    let word_s: String = env
        .get_string(&word)
        .map(|s| s.to_string_lossy().into_owned())
        .unwrap_or_default();

    if let Ok(mut guard) = engine_mutex().lock() {
        guard
            .personal
            .learn(&prev_s, &word_s, weight_boost.max(1) as u32);
    }
}

#[no_mangle]
pub extern "system" fn Java_com_thamnis_thkb_engine_NativeSuggestionBridge_nativeBlockWord(
    mut env: jni::JNIEnv,
    _class: jni::objects::JClass,
    word: jni::objects::JString,
) {
    let word_s: String = env
        .get_string(&word)
        .map(|s| s.to_string_lossy().into_owned())
        .unwrap_or_default();

    if let Ok(mut guard) = engine_mutex().lock() {
        guard.personal.block(&word_s);
    }
}

#[allow(clippy::too_many_arguments)]
#[no_mangle]
pub extern "system" fn Java_com_thamnis_thkb_engine_NativeSuggestionBridge_nativeDecodeSwipe(
    mut env: jni::JNIEnv,
    _class: jni::objects::JClass,
    layout_id: jni::objects::JString,
    context_joined: jni::objects::JString,
    key_centers_json: jni::objects::JString,
    swipe_points_json: jni::objects::JString,
    capitalize_hint: jni::sys::jboolean,
    max_results: jni::sys::jint,
) -> jni::sys::jstring {
    let read_str = |env: &mut jni::JNIEnv, s: jni::objects::JString| -> String {
        env.get_string(&s)
            .map(|js| js.to_string_lossy().into_owned())
            .unwrap_or_default()
    };

    let layout_s = read_str(&mut env, layout_id);
    let ctx_s = read_str(&mut env, context_joined);
    let centers_s = read_str(&mut env, key_centers_json);
    let points_s = read_str(&mut env, swipe_points_json);

    let context_words: Vec<String> = ctx_s
        .split('\u{001F}')
        .filter(|s| !s.is_empty())
        .map(ToString::to_string)
        .collect();

    let centers: Vec<KeyCenter> = serde_json::from_str(&centers_s).unwrap_or_default();
    let points: Vec<SwipePoint> = serde_json::from_str(&points_s).unwrap_or_default();
    let is_fr = layout_s.contains("fr") || layout_s.contains("ci");

    let candidates = if let Ok(guard) = engine_mutex().lock() {
        let index_opt = if is_fr {
            guard.fr.as_ref()
        } else {
            guard.en.as_ref()
        };
        index_opt
            .map(|idx| {
                decode_swipe_with_overlay(
                    idx,
                    guard.dialect_overlay.as_ref(),
                    Some(&guard.personal),
                    &context_words,
                    &centers,
                    &points,
                    capitalize_hint != 0,
                    max_results.max(1) as usize,
                )
            })
            .unwrap_or_default()
    } else {
        Vec::new()
    };

    let json = serde_json::to_string(&candidates).unwrap_or_else(|_| "[]".to_string());
    env.new_string(json)
        .map(|s| s.into_raw())
        .unwrap_or(std::ptr::null_mut())
}

#[cfg(test)]
mod tests {
    use super::*;

    fn sample_index() -> SuggestionIndex {
        let tsv = "de\t8000000\nje\t7500000\nest\t6500000\ntu\t4000000\nbonjour\t150000\nbon\t200000\nmanger\t120000\nfaire\t250000\n";
        let ci_json = r#"{
            "words": {
                "môgô": 80000,
                "enjaillement": 72000,
                "sciencer": 70000,
                "ya": 60000,
                "fohi": 67000,
                "Abidjan": 90000
            },
            "bigrams": {
                "ya": { "fohi": 990 }
            },
            "trigrams": {
                "il faut": { "sciencer": 980 }
            }
        }"#;
        SuggestionIndex::build(tsv, &[ci_json])
    }

    #[test]
    fn clean_start_returns_empty_on_empty_context() {
        let idx = sample_index();
        let results = idx.query(&[], "", "advanced_autocomplete", "azerty_fr", true, 3);
        assert!(results.is_empty());
    }

    #[test]
    fn case_alignment_matches_lowercase_and_preserves_display_casing() {
        let idx = sample_index();

        let lower = idx.query(&[], "bonj", "autocomplete", "azerty_fr", false, 3);
        assert_eq!(lower[0].word, "bonjour");

        let title = idx.query(&[], "Bonj", "autocomplete", "azerty_fr", false, 3);
        assert_eq!(title[0].word, "Bonjour");

        let autocap = idx.query(&[], "bonj", "autocomplete", "azerty_fr", true, 3);
        assert_eq!(autocap[0].word, "Bonjour");

        let caps = idx.query(&[], "BONJ", "autocomplete", "azerty_fr", false, 3);
        assert_eq!(caps[0].word, "BONJOUR");

        // Canonical proper noun preserved even in lowercase prefix
        let city = idx.query(&[], "abidj", "autocomplete", "azerty_fr", false, 3);
        assert_eq!(city[0].word, "Abidjan");
    }

    #[test]
    fn symspell_spatial_fixes_adjacent_typo() {
        let idx = sample_index();
        // 'p' is right next to 'o' on AZERTY/QWERTY: "bpnjour" -> "bonjour"
        let results = idx.query(
            &[],
            "bpnjour",
            "advanced_autocomplete",
            "azerty_fr",
            false,
            3,
        );
        assert!(!results.is_empty());
        assert_eq!(results[0].word, "bonjour");
    }

    #[test]
    fn advanced_autocomplete_trigram_and_bigram() {
        let idx = sample_index();
        let ctx = vec!["il".to_string(), "faut".to_string()];
        let tri = idx.query(&ctx, "", "advanced_autocomplete", "azerty_fr", false, 3);
        assert_eq!(tri[0].word, "sciencer");

        let bi = idx.query(
            &["ya".to_string()],
            "",
            "advanced_autocomplete",
            "azerty_fr",
            false,
            3,
        );
        assert_eq!(bi[0].word, "fohi");
    }

    #[test]
    fn strips_accents_for_nouchi_lookup() {
        let idx = sample_index();
        let res = idx.query(&[], "mogo", "autocomplete", "azerty_fr", false, 3);
        assert_eq!(res[0].word, "môgô");
    }

    #[test]
    fn personal_dictionary_learns_custom_words_bigrams_and_blocks() {
        let idx = sample_index();
        let mut personal = PersonalModel::default();
        personal.learn("bonjour", "Thamnis", 10);

        // Custom unknown word "Thamnis" should now be suggested on "tham"
        let prefix_res = idx.query_with_personal(
            &[],
            "tham",
            "advanced_autocomplete",
            "azerty_fr",
            false,
            3,
            Some(&personal),
        );
        assert!(!prefix_res.is_empty());
        assert_eq!(prefix_res[0].word, "Thamnis");

        // Learned bigram "bonjour -> Thamnis" should predict next word
        let next_res = idx.query_with_personal(
            &["Bonjour".to_string()],
            "",
            "advanced_autocomplete",
            "azerty_fr",
            false,
            3,
            Some(&personal),
        );
        assert!(!next_res.is_empty());
        assert_eq!(next_res[0].word, "Thamnis");

        // Blocking "bonjour" removes it cleanly from predictions
        personal.block("bonjour");
        let after_block = idx.query_with_personal(
            &[],
            "bonj",
            "autocomplete",
            "azerty_fr",
            false,
            3,
            Some(&personal),
        );
        assert!(after_block.iter().all(|c| c.word != "bonjour"));
    }

    #[test]
    fn dialect_overlay_merges_multiple_jargons() {
        let base = sample_index();
        let cs_and_med = r#"{
            "words": { "kubernetes": 85000, "ordonnance": 88000 },
            "bigrams": { "pull": { "request": 990 } }
        }"#;
        let overlay = SuggestionIndex::build("", &[cs_and_med]);

        let hits = query_with_dialect_overlay(
            &base,
            Some(&overlay),
            &[],
            "kuber",
            "advanced_autocomplete",
            "qwerty_en",
            false,
            3,
            None,
        );
        assert!(!hits.is_empty());
        assert_eq!(hits[0].word, "kubernetes");

        let next_hits = query_with_dialect_overlay(
            &base,
            Some(&overlay),
            &["pull".to_string()],
            "",
            "advanced_autocomplete",
            "qwerty_en",
            false,
            3,
            None,
        );
        assert!(!next_hits.is_empty());
        assert_eq!(next_hits[0].word, "request");
    }

    fn azerty_centers() -> Vec<KeyCenter> {
        let rows = [
            ("azertyuiop", 0.0, 0.0),
            ("qsdfghjklm", 0.0, 100.0),
            ("wxcvbn", 150.0, 200.0),
        ];
        let mut out = Vec::new();
        for (row, x0, y) in rows {
            for (i, ch) in row.chars().enumerate() {
                out.push(KeyCenter {
                    ch: ch.to_string(),
                    x: x0 + (i as f64) * 100.0 + 50.0,
                    y: y + 50.0,
                });
            }
        }
        out
    }

    fn trace_keys(centers: &[KeyCenter], keys: &str) -> Vec<SwipePoint> {
        let map: HashMap<char, (f64, f64)> = centers
            .iter()
            .filter_map(|kc| kc.ch.chars().next().map(|c| (c, (kc.x, kc.y))))
            .collect();
        let mut anchors = Vec::new();
        for ch in keys.chars() {
            if let Some(&pt) = map.get(&ch) {
                anchors.push(pt);
            }
        }
        let mut pts = Vec::new();
        for pair in anchors.windows(2) {
            for step in 0..=4 {
                let t = step as f64 / 4.0;
                pts.push(SwipePoint {
                    x: pair[0].0 + (pair[1].0 - pair[0].0) * t + 4.0,
                    y: pair[0].1 + (pair[1].1 - pair[0].1) * t - 3.0,
                });
            }
        }
        pts
    }

    #[test]
    fn swipe_decoder_matches_bonjour_and_dialect_words() {
        let idx = sample_index();
        let centers = azerty_centers();

        let bonjour_path = trace_keys(&centers, "bonjour");
        let bonjour_hits =
            decode_swipe_with_overlay(&idx, None, None, &[], &centers, &bonjour_path, false, 3);
        assert!(!bonjour_hits.is_empty());
        assert_eq!(bonjour_hits[0].word, "bonjour");

        // Dialect overlay word with accents ("môgô") traced smoothly across m -> o -> g -> o
        let mogo_path = trace_keys(&centers, "mogo");
        let mogo_hits =
            decode_swipe_with_overlay(&idx, None, None, &[], &centers, &mogo_path, false, 3);
        assert!(!mogo_hits.is_empty());
        assert_eq!(mogo_hits[0].word, "môgô");
    }
}
