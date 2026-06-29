package com.clipboardreader

import android.content.Context
import android.content.SharedPreferences

/** Tiny SharedPreferences wrapper for the handful of user settings. */
object Prefs {
    private const val NAME = "citestemi_prefs"

    const val KEY_BUBBLE = "bubble"
    const val LANG_AUTO = "auto"
    const val LANG_RO = "ro"
    const val LANG_RU = "ru"

    private fun sp(ctx: Context) = ctx.getSharedPreferences(NAME, Context.MODE_PRIVATE)

    fun langPref(ctx: Context): String = sp(ctx).getString("lang", LANG_AUTO) ?: LANG_AUTO
    fun setLangPref(ctx: Context, value: String) = sp(ctx).edit().putString("lang", value).apply()

    fun rate(ctx: Context): Float = sp(ctx).getFloat("rate", 1.0f)
    fun setRate(ctx: Context, value: Float) = sp(ctx).edit().putFloat("rate", value).apply()

    fun bubbleEnabled(ctx: Context): Boolean = sp(ctx).getBoolean(KEY_BUBBLE, false)
    fun setBubbleEnabled(ctx: Context, value: Boolean) = sp(ctx).edit().putBoolean(KEY_BUBBLE, value).apply()

    fun registerChangeListener(ctx: Context, l: SharedPreferences.OnSharedPreferenceChangeListener) =
        sp(ctx).registerOnSharedPreferenceChangeListener(l)

    fun unregisterChangeListener(ctx: Context, l: SharedPreferences.OnSharedPreferenceChangeListener) =
        sp(ctx).unregisterOnSharedPreferenceChangeListener(l)
}
