package com.clipboardreader.reader

import com.clipboardreader.Prefs
import java.util.Locale

/** Picks the TTS locale for a piece of text. Two languages only: Romanian + Russian, by script. */
object LanguageDetector {
    val LOCALE_RO: Locale = Locale("ro", "RO")
    val LOCALE_RU: Locale = Locale("ru", "RU")

    /** True if the text contains any Cyrillic letter (covers the Russian alphabet). */
    fun hasCyrillic(text: String): Boolean =
        text.any { it in 'Ѐ'..'ӿ' || it in 'Ԁ'..'ԯ' }

    /**
     * @param override one of [Prefs.LANG_AUTO], [Prefs.LANG_RO], [Prefs.LANG_RU]
     */
    fun localeFor(text: String, override: String): Locale = when (override) {
        Prefs.LANG_RO -> LOCALE_RO
        Prefs.LANG_RU -> LOCALE_RU
        else -> if (hasCyrillic(text)) LOCALE_RU else LOCALE_RO
    }
}
