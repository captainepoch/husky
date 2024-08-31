package com.keylesspalace.tusky.util

enum class PostFormat(val formatValue: String) {
    PLAIN("text/plain"),
    MARKDOWN("text/markdown"),
    BBCODE("text/bbcode"),
    HTML("text/html");

    companion object {
        fun getFormat(formatValue: String?): PostFormat {
            return when (formatValue) {
                MARKDOWN.formatValue -> MARKDOWN
                BBCODE.formatValue -> BBCODE
                HTML.formatValue -> HTML
                else -> PLAIN
            }
        }
    }
}
