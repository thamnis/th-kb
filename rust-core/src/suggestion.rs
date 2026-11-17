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

// ---------------------------------------------------------------------------
// Global cached engine + Android JNI entrypoints
// ---------------------------------------------------------------------------

#[derive(Default)]
struct EngineCache {
    en: Option<SuggestionIndex>,
    fr: Option<SuggestionIndex>,
    fr_ci: Option<SuggestionIndex>,
    personal: PersonalModel,
}

static GLOBAL_ENGINE: OnceLock<Mutex<EngineCache>> = OnceLock::new();

fn engine_mutex() -> &'static Mutex<EngineCache> {
    GLOBAL_ENGINE.get_or_init(|| Mutex::new(EngineCache::default()))
}

#[no_mangle]
pub extern "system" fn Java_com_thamnis_thkb_engine_NativeSuggestionBridge_nativeInitDictionaries(
    mut env: jni::JNIEnv,
    _class: jni::objects::JClass,
    en_tsv: jni::objects::JString,
    en_common_json: jni::objects::JString,
    fr_tsv: jni::objects::JString,
    fr_common_json: jni::objects::JString,
    fr_ci_json: jni::objects::JString,
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
    let fr_ci_s = read_str(&mut env, fr_ci_json);

    let en_idx = SuggestionIndex::build(&en_tsv_s, &[&en_json_s]);
    let fr_idx = SuggestionIndex::build(&fr_tsv_s, &[&fr_json_s]);
    let fr_ci_idx = SuggestionIndex::build(&fr_tsv_s, &[&fr_json_s, &fr_ci_s]);

    if let Ok(mut guard) = engine_mutex().lock() {
        guard.en = Some(en_idx);
        guard.fr = Some(fr_idx);
        guard.fr_ci = Some(fr_ci_idx);
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
    ivorian_enabled: jni::sys::jboolean,
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
    let use_ci = is_fr && ivorian_enabled != 0;

    let candidates = if let Ok(guard) = engine_mutex().lock() {
        let index_opt = if use_ci {
            guard.fr_ci.as_ref().or(guard.fr.as_ref())
        } else if is_fr {
            guard.fr.as_ref()
        } else {
            guard.en.as_ref()
        };
        index_opt
            .map(|idx| {
                idx.query_with_personal(
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
}
