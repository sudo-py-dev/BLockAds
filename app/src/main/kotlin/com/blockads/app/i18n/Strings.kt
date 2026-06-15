package com.blockads.app.i18n

import androidx.compose.runtime.compositionLocalOf

object Strings {
    private val registry: Map<String, LocalizedStrings> =
        mapOf(
            "en" to StringsEn,
            "he" to StringsHe,
            "ar" to StringsAr,
            "fr" to StringsFr,
            "de" to StringsDe,
            "es" to StringsEs,
        )

    fun resolve(languageTag: String): LocalizedStrings = registry[languageTag.take(2).lowercase()] ?: registry.getValue("en")

    val all: Map<String, LocalizedStrings> = registry
}

val LocalStrings = compositionLocalOf<LocalizedStrings> { StringsEn }
