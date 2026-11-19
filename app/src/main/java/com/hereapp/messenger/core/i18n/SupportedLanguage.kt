package com.hereapp.messenger.core.i18n

data class SupportedLanguage(
    val code: String,
    val nativeName: String,
    val englishName: String,
    val isRtl: Boolean = false,
    val isFullyTranslated: Boolean = false
)

object LanguageCatalog {
    val all: List<SupportedLanguage> = listOf(
        SupportedLanguage("en", "English", "English", isRtl = false, isFullyTranslated = true),
        SupportedLanguage("ar", "العربية", "Arabic", isRtl = true, isFullyTranslated = true),
        SupportedLanguage("fr", "Français", "French", isRtl = false, isFullyTranslated = true),
        SupportedLanguage("es", "Español", "Spanish", isRtl = false, isFullyTranslated = true),
        SupportedLanguage("de", "Deutsch", "German", isRtl = false, isFullyTranslated = true),
        SupportedLanguage("tr", "Türkçe", "Turkish", isRtl = false, isFullyTranslated = true),
        SupportedLanguage("pt", "Português", "Portuguese", isRtl = false),
        SupportedLanguage("it", "Italiano", "Italian", isRtl = false),
        SupportedLanguage("ru", "Русский", "Russian", isRtl = false),
        SupportedLanguage("uk", "Українська", "Ukrainian", isRtl = false),
        SupportedLanguage("zh", "中文", "Chinese", isRtl = false),
        SupportedLanguage("ja", "日本語", "Japanese", isRtl = false),
        SupportedLanguage("ko", "한국어", "Korean", isRtl = false),
        SupportedLanguage("hi", "हिन्दी", "Hindi", isRtl = false),
        SupportedLanguage("bn", "বাংলা", "Bengali", isRtl = false),
        SupportedLanguage("ur", "اردو", "Urdu", isRtl = true),
        SupportedLanguage("fa", "فارسی", "Persian", isRtl = true),
        SupportedLanguage("he", "עברית", "Hebrew", isRtl = true),
        SupportedLanguage("id", "Bahasa Indonesia", "Indonesian", isRtl = false),
        SupportedLanguage("ms", "Bahasa Melayu", "Malay", isRtl = false),
        SupportedLanguage("vi", "Tiếng Việt", "Vietnamese", isRtl = false),
        SupportedLanguage("th", "ไทย", "Thai", isRtl = false),
        SupportedLanguage("nl", "Nederlands", "Dutch", isRtl = false),
        SupportedLanguage("pl", "Polski", "Polish", isRtl = false),
        SupportedLanguage("sv", "Svenska", "Swedish", isRtl = false),
        SupportedLanguage("no", "Norsk", "Norwegian", isRtl = false),
        SupportedLanguage("da", "Dansk", "Danish", isRtl = false),
        SupportedLanguage("fi", "Suomi", "Finnish", isRtl = false),
        SupportedLanguage("el", "Ελληνικά", "Greek", isRtl = false),
        SupportedLanguage("cs", "Čeština", "Czech", isRtl = false),
        SupportedLanguage("ro", "Română", "Romanian", isRtl = false),
        SupportedLanguage("hu", "Magyar", "Hungarian", isRtl = false),
        SupportedLanguage("sw", "Kiswahili", "Swahili", isRtl = false)
    )

    fun findByCode(code: String): SupportedLanguage =
        all.firstOrNull { it.code == code } ?: all.first()
}
