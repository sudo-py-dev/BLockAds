package com.blockads.app

import com.blockads.app.i18n.LocalizedStrings
import com.blockads.app.i18n.Strings
import com.blockads.app.i18n.StringsEn
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import kotlin.reflect.full.memberProperties

class StringsTest {
    private fun allStringValues(strings: LocalizedStrings): List<String> =
        LocalizedStrings::class.memberProperties.map { prop ->
            prop.getter.call(strings) as String
        }

    @Test
    fun `English strings are all non-blank`() {
        allStringValues(StringsEn).forEach { value ->
            assertFalse(value.isBlank(), "English string is blank: $value")
        }
    }

    @Test
    fun `all locale implementations have non-blank values`() {
        Strings.all.forEach { (tag, impl) ->
            allStringValues(impl).forEach { value ->
                assertFalse(value.isBlank(), "Locale '$tag' has blank string: $value")
            }
        }
    }

    @Test
    fun `unknown locale tag falls back to English`() {
        val result = Strings.resolve("xx")
        assertEquals(StringsEn, result)
    }

    @Test
    fun `known locale tags resolve correctly`() {
        assertNotNull(Strings.resolve("he"))
        assertNotNull(Strings.resolve("ar"))
        assertNotNull(Strings.resolve("fr"))
        assertNotNull(Strings.resolve("de"))
        assertNotNull(Strings.resolve("es"))
    }

    @Test
    fun `legacy Hebrew tag iw resolves`() {
        val result = Strings.resolve("iw")
        assertNotNull(result)
    }

    @Test
    fun `language tag with region resolves correctly`() {
        // "en-US" → should strip to "en" and match English
        val result = Strings.resolve("en-US")
        assertEquals(StringsEn, result)
    }
}
