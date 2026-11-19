use serde::{Deserialize, Serialize};

/// A single kaomoji entry returned to Kotlin via UniFFI.
#[derive(Debug, Clone, PartialEq, Eq, Serialize, Deserialize, uniffi::Record)]
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
    let needle = crate::suggestion::strip_accents(&query.trim().to_lowercase());
    if needle.is_empty() {
        return entries;
    }
    entries
        .into_iter()
        .filter(|entry| {
            crate::suggestion::strip_accents(&entry.category.to_lowercase()).contains(&needle)
                || entry
                    .tags
                    .iter()
                    .any(|t| crate::suggestion::strip_accents(&t.to_lowercase()).contains(&needle))
        })
        .collect()
}

/// Bilingual (English + French + Ivorian/Nouchi) emoji keyword index.
const EMOJI_KEYWORD_CATALOG: &[(&str, &str)] = &[
    ("😂", "laugh rire lol mdr ptdr tears pleurer joie funny"),
    ("🤣", "rofl laugh rire mdr ptdr hilarious"),
    ("😀", "grin smile sourire happy heureux"),
    ("😊", "blush smile sourire cute mignon"),
    ("😍", "love heart eyes amour coeur adore kpata"),
    ("🥰", "love hearts amoureux affection calin"),
    ("😘", "kiss bisou baiser love amour"),
    ("😎", "cool sunglasses lunettes boss"),
    ("🥳", "party celebrate fete anniversaire enjaillement"),
    ("🤩", "star eyes wow super genial"),
    ("🤔", "think thinking reflechir hmm sciencer"),
    ("😅", "sweat relief ouf chaud"),
    ("😭", "cry sob pleurer triste sad"),
    ("😡", "angry mad colere furieux"),
    ("🤯", "mind blown explosion choc"),
    ("😴", "sleep tired dormir fatigue dodo"),
    ("🤧", "sneeze sick malade rhume"),
    ("❤️", "heart red love coeur amour rouge"),
    ("🧡", "orange heart coeur"),
    ("💛", "yellow heart coeur jaune"),
    ("💚", "green heart coeur vert"),
    ("💙", "blue heart coeur bleu"),
    ("💜", "purple heart coeur violet"),
    ("🖤", "black heart coeur noir"),
    ("🤍", "white heart coeur blanc"),
    ("💔", "broken heart coeur brise"),
    ("💯", "hundred 100 parfait top"),
    ("🔥", "fire hot flame feu chaud lit"),
    ("✨", "sparkles magic etoiles briller"),
    ("⭐", "star etoile favori"),
    ("👍", "thumbs up ok good oui bien super"),
    ("👎", "thumbs down no non mauvais"),
    ("🙏", "pray thanks please merci pardon stp svp"),
    ("👏", "clap bravo applaudir"),
    ("🙌", "hands celebrate hourra gloire"),
    ("🤝", "handshake deal accord ensemble"),
    ("💪", "muscle strong force fort"),
    ("👋", "wave hello bye salut coucou"),
    ("✌️", "peace victory paix victoire"),
    ("🤞", "fingers crossed chance"),
    ("👀", "eyes look voir regarder"),
    ("✅", "check done ok valide oui"),
    ("❌", "cross no error non erreur"),
    ("⚠️", "warning alert attention danger"),
    ("🎉", "tada party fete bravo"),
    ("🎂", "cake birthday gateau anniversaire"),
    ("🍕", "pizza food manger"),
    ("🍔", "burger food manger"),
    ("🍟", "fries frites manger"),
    ("🍗", "chicken poulet kedjenou braise"),
    ("🐟", "fish poisson garba braise"),
    ("🍌", "banana banane alloco foutou"),
    ("🍚", "rice riz attieke tchep"),
    ("🍺", "beer biere boisson"),
    ("☕", "coffee tea cafe chaud"),
    ("🚗", "car auto voiture route"),
    ("✈️", "plane flight avion voyage"),
    ("🏠", "home house maison"),
    ("💻", "laptop computer code pc tech"),
    ("📱", "phone mobile telephone"),
    ("💰", "money cash argent wave momo"),
    ("🎵", "music note musique son zouglou"),
    ("⚽", "soccer football ballon"),
    ("🏆", "trophy winner trophee champion"),
    ("🇨🇮", "cote divoire ivory coast ci abidjan babi"),
    ("🇫🇷", "france fr drapeau flag"),
    ("🇺🇸", "usa america us flag"),
    ("🇬🇧", "uk britain angleterre flag"),
    ("🇸🇳", "senegal dakar flag"),
    ("🇨🇲", "cameroon cameroun flag"),
    ("🇲🇱", "mali bamako flag"),
    ("🇧🇫", "burkina faso ouaga flag"),
];

/// Searches emojis across English, French, and Ivorian/Nouchi keywords.
pub fn search_emojis_bilingual(query: &str) -> Vec<String> {
    let needle = crate::suggestion::strip_accents(&query.trim().to_lowercase());
    if needle.is_empty() {
        return Vec::new();
    }
    let mut results = Vec::new();
    for &(emoji, tags) in EMOJI_KEYWORD_CATALOG {
        if tags
            .split_whitespace()
            .any(|t| t.starts_with(&needle) || t.contains(&needle))
        {
            results.push(emoji.to_string());
        }
    }
    results
}

/// Computes normalized 0..10 RMS decibel level from a 16-bit PCM audio frame.
pub fn analyze_pcm16_rms(samples: &[i16]) -> f32 {
    if samples.is_empty() {
        return 0.0;
    }
    let sum_sq: f64 = samples
        .iter()
        .map(|&s| {
            let v = f64::from(s) / 32768.0;
            v * v
        })
        .sum();
    let rms = (sum_sq / (samples.len() as f64)).sqrt();
    if rms <= 1e-5 {
        return 0.0;
    }
    let db = 20.0 * rms.log10();
    // Map [-45dBFS .. -5dBFS] into [0.0 .. 10.0]
    (((db + 45.0) / 4.0) as f32).clamp(0.0, 10.0)
}

#[no_mangle]
pub extern "system" fn Java_com_thamnis_thkb_engine_NativeSuggestionBridge_nativeSearchEmojis(
    mut env: jni::JNIEnv,
    _class: jni::objects::JClass,
    query: jni::objects::JString,
) -> jni::sys::jstring {
    let q: String = env
        .get_string(&query)
        .map(|s| s.to_string_lossy().into_owned())
        .unwrap_or_default();
    let hits = search_emojis_bilingual(&q);
    let json = serde_json::to_string(&hits).unwrap_or_else(|_| "[]".to_string());
    env.new_string(json)
        .map(|s| s.into_raw())
        .unwrap_or(std::ptr::null_mut())
}

#[no_mangle]
pub extern "system" fn Java_com_thamnis_thkb_engine_NativeSuggestionBridge_nativeAnalyzePcmRms(
    env: jni::JNIEnv,
    _class: jni::objects::JClass,
    pcm_array: jni::objects::JShortArray,
    length: jni::sys::jint,
) -> jni::sys::jfloat {
    let len = length.max(0) as usize;
    if len == 0 {
        return 0.0;
    }
    let mut buf = vec![0i16; len];
    if env.get_short_array_region(&pcm_array, 0, &mut buf).is_ok() {
        analyze_pcm16_rms(&buf)
    } else {
        0.0
    }
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

    #[test]
    fn bilingual_emoji_search_matches_en_fr_and_ci() {
        let fire_fr = search_emojis_bilingual("feu");
        assert!(fire_fr.contains(&"🔥".to_string()));

        let heart_fr = search_emojis_bilingual("coeur");
        assert!(heart_fr.contains(&"❤️".to_string()));

        let ci = search_emojis_bilingual("abidjan");
        assert!(ci.contains(&"🇨🇮".to_string()));
    }

    #[test]
    fn pcm16_rms_computes_expected_level() {
        assert_eq!(analyze_pcm16_rms(&[]), 0.0);
        let silence = vec![0i16; 160];
        assert_eq!(analyze_pcm16_rms(&silence), 0.0);
        let loud = vec![12000i16; 160];
        assert!(analyze_pcm16_rms(&loud) > 5.0);
    }
}
