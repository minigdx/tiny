package com.github.minigdx.tiny.debugger

/**
 * Highlight the code with HTML tags.
 *
 * - String: <strong class="code_string">
 * - Comment: <em class="code_comment">
 * - Keyword: <strong class="code_keyword">
 * - Number: <em class="code_number">
 */
private val KEYWORD_PATTERN =
    listOf(
        "if", "else", "elif", "end", "while", "for", "in", "of",
        "continue", "break", "return", "function", "local", "do",
        "then", "repeat", "until", "not", "and", "or",
        "true", "false", "nil",
    ).joinToString("|")

fun highlight(content: String): String {
    return content
        // Create lines
        .split("\n").map { "<div>$it</div>" }.joinToString("\n")
        // Replace \n with <br /> to be selected correctly in the range
        .replace("<div></div>", "<div><br /></div>")
        // String
        .replace(Regex("(\".*?\")"), "<strong class=\"code_string\">\$1</strong>")
        // Comment
        .replace(Regex("--(.*)"), """<em class="code_comment">--$1</em>""")
        // Keyword
        .replace(
            Regex("\\b($KEYWORD_PATTERN)\\b"),
            """<strong class="code_keyword">$1</strong>""",
        )
        // Numbers
        .replace(Regex("\\b(\\d+)"), "<em class=\"code_number\">\$1</em>")
}
