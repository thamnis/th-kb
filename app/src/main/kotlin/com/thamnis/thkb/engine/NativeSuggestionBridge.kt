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
        frCiJson: String,
    ): Boolean

    external fun nativeQuerySuggestions(
        layoutId: String,
        contextJoined: String,
        prefix: String,
        mode: String,
        ivorianEnabled: Boolean,
        capitalizeHint: Boolean,
        maxResults: Int,
    ): String
}
