package ca.bcparkspot.app.ui.theme

import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Test

class ColorLiteralScanTest {

    private val mainDir = File("src/main")
    private val paletteFile = "java/ca/bcparkspot/app/ui/theme/Palette.kt"
    private val colorsXml = Regex("res/values(-night)?/colors\\.xml")
    private val kotlinLiteral = Regex(
        listOf(
            """Color\(\s*0x""",
            """Color\(\s*\d""",
            """Color\(\s*(alpha|red|green|blue)\s*=""",
            """\bColor\.(White|Black|Red|Green|Blue|Yellow|Cyan|Magenta|Gray|DarkGray|LightGray)\b""",
            """\bColor\.(WHITE|BLACK|RED|GREEN|BLUE|YELLOW|CYAN|MAGENTA|GRAY|DKGRAY|LTGRAY|argb|rgb|parseColor|valueOf)\b""",
            """\b0x[0-9A-Fa-f]{8}\b""",
            """color\s*=\s*MaterialTheme\.colorScheme\.outline(?![\w.])"""
        ).joinToString("|")
    )
    private val xmlLiteral = Regex("""#[0-9A-Fa-f]{6,8}\b""")

    private fun patternFor(file: File, rel: String): Regex? = when {
        rel == paletteFile -> null
        file.extension == "kt" -> kotlinLiteral
        file.extension == "xml" && !colorsXml.matches(rel) -> xmlLiteral
        else -> null
    }

    @Test
    fun `colour literals live only in the palette and colors xml, and outline is never text`() {
        assertTrue("run from the app module: ${mainDir.absolutePath} is missing", mainDir.isDirectory)
        var scanned = 0
        val offenders = mainDir.walkTopDown().filter { it.isFile }.flatMap { file ->
            val rel = file.relativeTo(mainDir).invariantSeparatorsPath
            val pattern = patternFor(file, rel) ?: return@flatMap emptySequence()
            scanned++
            file.readLines().withIndex().filter { pattern.containsMatchIn(it.value) }.map { "$rel:${it.index + 1}: ${it.value.trim()}" }.asSequence()
        }.toList()
        assertTrue("no source files were scanned", scanned > 0)
        assertTrue(offenders.joinToString("\n"), offenders.isEmpty())
    }

    @Test
    fun `the scan catches every literal form it claims to`() {
        val planted = listOf(
            "Color(0xFF123456)", "Color(alpha = 0.5f, red = 1f, green = 0f, blue = 0f)", "Color.White",
            "android.graphics.Color.WHITE", "Color.argb(80, 0, 0, 0)", "val c = 0x50000000",
            "Text(\"x\", color = MaterialTheme.colorScheme.outline)"
        )
        planted.forEach { assertTrue("missed: $it", kotlinLiteral.containsMatchIn(it)) }
        listOf("Color.Transparent", "MaterialTheme.colorScheme.outline.copy(alpha = Alpha.LOW)", "tint = MaterialTheme.colorScheme.outlineVariant")
            .forEach { assertTrue("false alarm: $it", !kotlinLiteral.containsMatchIn(it)) }
        assertTrue(xmlLiteral.containsMatchIn("android:fillColor=\"#134E4A\""))
    }
}
