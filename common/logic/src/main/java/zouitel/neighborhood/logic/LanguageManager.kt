package zouitel.neighborhood.logic

import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import androidx.core.text.TextUtilsCompat
import android.view.View
import java.util.Locale

object LanguageManager {

    enum class AppLanguage(val tag: String, val displayName: String) {
        // ── A ──────────────────────────────────────────────
        ALBANIAN("sq",    "Shqip"),
        ARABIC("ar",      "العربية"),
        ARMENIAN("hy",    "Հայերեն"),

        // ── B ──────────────────────────────────────────────
        BENGALI("bn",     "বাংলা"),
        BULGARIAN("bg",   "Български"),

        // ── C ──────────────────────────────────────────────
        CHINESE_SIMPLIFIED("zh-CN",    "中文（简体）"),
        CHINESE_TRADITIONAL("zh-TW",   "中文（繁體）"),
        CROATIAN("hr",    "Hrvatski"),
        CZECH("cs",       "Čeština"),

        // ── D ──────────────────────────────────────────────
        DANISH("da",      "Dansk"),
        DUTCH("nl",       "Nederlands"),

        // ── E ──────────────────────────────────────────────
        ENGLISH("en",     "English"),
        ESTONIAN("et",    "Eesti"),

        // ── F ──────────────────────────────────────────────
        FILIPINO("fil",   "Filipino"),
        FINNISH("fi",     "Suomi"),
        FRENCH("fr",      "Français"),

        // ── G ──────────────────────────────────────────────
        GEORGIAN("ka",    "ქართული"),
        GERMAN("de",      "Deutsch"),
        GREEK("el",       "Ελληνικά"),

        // ── H ──────────────────────────────────────────────
        HINDI("hi",       "हिन्दी"),

        // ── I ──────────────────────────────────────────────
        ICELANDIC("is",   "Íslenska"),
        INDONESIAN("id",  "Bahasa Indonesia"),   // resource folder: values-in
        IRISH("ga",       "Gaeilge"),
        ITALIAN("it",     "Italiano"),

        // ── J ──────────────────────────────────────────────
        JAPANESE("ja",    "日本語"),

        // ── K ──────────────────────────────────────────────
        KHMER("km",       "ភាសាខ្មែរ"),
        KOREAN("ko",      "한국어"),

        // ── L ──────────────────────────────────────────────
        LATVIAN("lv",     "Latviešu"),
        LITHUANIAN("lt",  "Lietuvių"),
        LUXEMBOURGISH("lb","Lëtzebuergesch"),

        // ── M ──────────────────────────────────────────────
        MALAY("ms",       "Bahasa Melayu"),
        MALTESE("mt",     "Malti"),
        MONGOLIAN("mn",   "Монгол"),

        // ── N ──────────────────────────────────────────────
        NEPALI("ne",      "नेपाली"),
        NORWEGIAN("no",   "Norsk"),

        // ── P ──────────────────────────────────────────────
        PORTUGUESE_BR("pt-BR", "Português (Brasil)"),
        PORTUGUESE_PT("pt-PT", "Português (Portugal)"),

        // ── Q ──────────────────────────────────────────────
        QUECHUA("qu",     "Runasimi"),

        // ── R ──────────────────────────────────────────────
        ROMANIAN("ro",    "Română"),
        RUSSIAN("ru",     "Русский"),

        // ── S ──────────────────────────────────────────────
        SERBIAN("sr",     "Српски"),
        SLOVAK("sk",      "Slovenčina"),
        SPANISH("es",     "Español"),
        SWEDISH("sv",     "Svenska"),

        // ── T ──────────────────────────────────────────────
        THAI("th",        "ภาษาไทย"),
        TURKISH("tr",     "Türkçe"),

        // ── U ──────────────────────────────────────────────
        UKRAINIAN("uk",   "Українська"),
        UZBEK("uz",       "O'zbek"),

        // ── V ──────────────────────────────────────────────
        VIETNAMESE("vi",  "Tiếng Việt");

        companion object {
            /** All languages sorted alphabetically by their display name. */
            val sortedByName: List<AppLanguage>
                get() = entries.sortedBy { it.displayName }

            /** Look up a language by its BCP-47 tag (case-insensitive). */
            fun fromTag(tag: String): AppLanguage? =
                entries.find { it.tag.equals(tag, ignoreCase = true) }
        }
    }

    // ───────────────────────────────────────────────────────

    /**
     * Apply [language] to the whole app.
     * No Activity restart needed — AppCompat handles the recreation.
     */
    fun setLanguage(language: AppLanguage) {
        val localeList = LocaleListCompat.forLanguageTags(language.tag)
        AppCompatDelegate.setApplicationLocales(localeList)
    }

    /**
     * Return the currently active [AppLanguage].
     *
     * Matching priority:
     *  1. Exact BCP-47 tag match  (e.g. "zh-CN" → CHINESE_SIMPLIFIED)
     *  2. Base-language match     (e.g. "en-US" → ENGLISH)
     *  3. Fallback                (→ ENGLISH)
     */
    fun getCurrentLanguage(): AppLanguage {
        val locales = AppCompatDelegate.getApplicationLocales()
        if (locales.isEmpty) return AppLanguage.ENGLISH

        val firstTag = locales
            .toLanguageTags()
            .split(",")
            .firstOrNull()
            ?.trim()
            ?: return AppLanguage.ENGLISH

        // 1 — exact match
        AppLanguage.fromTag(firstTag)?.let { return it }

        // 2 — base-language match (strip region, e.g. "en-US" → "en")
        val baseTag = firstTag.substringBefore("-")
        return AppLanguage.fromTag(baseTag) ?: AppLanguage.ENGLISH
    }

    /**
     * Convenience: return true if the current locale is Right-To-Left.
     */
    fun isCurrentLanguageRtl(): Boolean {
        val locale = Locale.forLanguageTag(getCurrentLanguage().tag)
        return TextUtilsCompat.getLayoutDirectionFromLocale(locale) == View.LAYOUT_DIRECTION_RTL
    }
}