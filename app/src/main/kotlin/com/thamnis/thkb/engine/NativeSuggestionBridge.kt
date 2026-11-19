package com.thamnis.thkb.engine

internal object NativeSuggestionBridge {
    val isLoaded: Boolean = try {
        System.loadLibrary("th_kb_core")
        true
    } catch (_: UnsatisfiedLinkError) {
        false
    } catch (_: SecurityException) {
        false
    }

    external fun nativeInitDictionaries(
        enTsv: String,
        enCommonJson: String,
        frTsv: String,
        frCommonJson: String,
        initialOverlayJson: String,
    ): Boolean

    external fun nativeSyncDialects(overlayJson: String): Boolean

    external fun nativeQuerySuggestions(
        layoutId: String,
        contextJoined: String,
        prefix: String,
        mode: String,
        ivorianEnabled: Boolean,
        capitalizeHint: Boolean,
        maxResults: Int,
    ): String

    external fun nativeSyncPersonalDictionary(personalJson: String): Boolean

    external fun nativeLearnWord(
        prevWord: String,
        word: String,
        weightBoost: Int,
    )

    external fun nativeBlockWord(word: String)

    external fun nativeDecodeSwipe(
        layoutId: String,
        contextJoined: String,
        keyCentersJson: String,
        swipePointsJson: String,
        capitalizeHint: Boolean,
        maxResults: Int,
    ): String

    external fun nativeSearchEmojis(query: String): String

    external fun nativeAnalyzePcmRms(pcmArray: ShortArray, length: Int): Float
}
