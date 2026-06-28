package com.clipboardreader.reader

import com.clipboardreader.Prefs
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LanguageDetectorTest {

    @Test
    fun cyrillic_is_detected() {
        assertTrue(LanguageDetector.hasCyrillic("Привет мир"))
        assertTrue(LanguageDetector.hasCyrillic("mixed Salut и текст"))
    }

    @Test
    fun latin_is_not_cyrillic() {
        assertFalse(LanguageDetector.hasCyrillic("Salut lume"))
        assertFalse(LanguageDetector.hasCyrillic("Bună ziua, ce mai faci?"))
    }

    @Test
    fun auto_picks_russian_for_cyrillic() {
        assertEquals(LanguageDetector.LOCALE_RU, LanguageDetector.localeFor("Привет", Prefs.LANG_AUTO))
    }

    @Test
    fun auto_picks_romanian_for_latin() {
        assertEquals(LanguageDetector.LOCALE_RO, LanguageDetector.localeFor("Bună", Prefs.LANG_AUTO))
    }

    @Test
    fun manual_override_wins_over_script() {
        assertEquals(LanguageDetector.LOCALE_RO, LanguageDetector.localeFor("Привет", Prefs.LANG_RO))
        assertEquals(LanguageDetector.LOCALE_RU, LanguageDetector.localeFor("Salut", Prefs.LANG_RU))
    }
}
