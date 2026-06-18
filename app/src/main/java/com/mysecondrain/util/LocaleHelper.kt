package com.mysecondrain.util

import android.content.Context
import android.content.res.Configuration
import java.util.Locale

object LocaleHelper {

    fun setLocale(context: Context, language: String): Context {
        val locale = when (language) {
            "বাংলা" -> Locale("bn")
            else    -> Locale("en")
        }
        Locale.setDefault(locale)
        val config = Configuration(context.resources.configuration)
        config.setLocale(locale)
        return context.createConfigurationContext(config)
    }

    fun getCurrentLanguage(context: Context): String {
        return when (context.resources.configuration.locales[0].language) {
            "bn"  -> "বাংলা"
            else  -> "English"
        }
    }
}