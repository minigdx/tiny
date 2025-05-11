import kotlinx.browser.document
import org.w3c.dom.HTMLDivElement
import kotlin.test.Test
import kotlin.test.assertEquals

class MainTest {
    @Test
    fun highlight_example() {
        val elt = document.createElement("div") as HTMLDivElement
        elt.innerHTML =
            """<div>-- this is a comment</div>
<div>    function hello()</div>
<div>        print("hello")</div>
<div>    end</div>"""

        elt.innerHTML = highlight(elt.innerText)

        assertEquals(
            """
            <div><em class="code_comment">-- this is a comment</em></div>
            <div>    <strong class="code_keyword">function</strong> hello()</div>
            <div>        print(<strong class="code_string">"hello"</strong>)</div>
            <div>    <strong class="code_keyword">end</strong></div>
            """.trimIndent().trim(),
            elt.innerHTML,
        )
    }

    @Test
    fun highlight_comment() {
        val elt = document.createElement("div") as HTMLDivElement
        elt.innerHTML = "<div>-- this is a comment</div>"
        elt.innerHTML = highlight(elt.innerText)

        assertEquals("<div><em class=\"code_comment\">-- this is a comment</em></div>", elt.innerHTML)
    }

    @Test
    fun highlight_keyword() {
        val elt = document.createElement("div") as HTMLDivElement
        elt.innerHTML =
            """<div>function hello()</div>
<div>end</div>"""

        elt.innerHTML = highlight(elt.innerText)

        assertEquals(
            """<div><strong class="code_keyword">function</strong> hello()</div>
<div><strong class="code_keyword">end</strong></div>""",
            elt.innerHTML,
        )
    }

    @Test
    fun highlight_string() {
        val elt = document.createElement("div") as HTMLDivElement
        elt.innerHTML = "<div>\"hello world\"</div>"
        elt.innerHTML = highlight(elt.innerText)

        assertEquals(
            """<div>
                |<strong class="code_string">"hello world"</strong>
                |</div>
            """.trimMargin().replace("\n", ""),
            elt.innerHTML,
        )
    }

    @Test
    fun highlight_number() {
        val elt = document.createElement("div") as HTMLDivElement
        elt.innerHTML = "<div>3</div>"
        elt.innerHTML = highlight(elt.innerText)

        assertEquals(
            """<div>
                |<em class="code_number">3</em>
                |</div>
            """.trimMargin().replace("\n", ""),
            elt.innerHTML,
        )
    }

    @Test
    fun highlight_extra_line_return() {
        val elt = document.createElement("div") as HTMLDivElement
        elt.innerHTML = "<div></div>\n" +
            "<div></div>" +
            "<div><em class=\"code_number\">3</em></div>"
        elt.innerHTML = highlight(extractText(elt))

        assertEquals(
            "<div><br></div>\n" +
                "<div><br></div>\n" +
                "<div><em class=\"code_number\">3</em></div>",
            elt.innerHTML,
        )
    }

    @Test
    fun extractText() {
        val content = """<div>-- Update the code to update the game!</div>
<div></div>
<div>function _init()</div>
<div></div>
<div>end</div>
<div></div>
<div>function _update()</div>
<div></div>
<div>end</div>
<div></div>
<div>function _draw()</div>
<div></div>
<div>end</div>"""

        val expected = """-- Update the code to update the game!

function _init()

end

function _update()

end

function _draw()

end"""

        val div = document.createElement("div") as HTMLDivElement
        div.innerHTML = content

        assertEquals(expected, extractText(div))
    }
}
