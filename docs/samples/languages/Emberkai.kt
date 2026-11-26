// Check Kotlin colors: classes and objects, enum entries, annotations, named arguments,
// extension and suspend functions, captured variables, and string templates.
package emberkai

enum class Tone { WARM, COOL }

@JvmInline
value class Hex(val value: Int)

data class Swatch(val name: String, val hex: Hex, val tone: Tone = Tone.WARM)

sealed interface Palette {
    val swatches: List<Swatch>
}

abstract class BasePalette : Palette

object Emberkai : BasePalette() {
    override val swatches = listOf(Swatch("ember", Hex(0xfd9353)), Swatch(name = "teal", hex = Hex(0x5ad4e6), tone = Tone.COOL))
}

fun Swatch.describe(upper: Boolean = false): String {
    val label = if (upper) name.uppercase() else name
    return "$label #${"%06x".format(hex.value)} ${tone.name.lowercase()}"
}

fun <T : Palette> T.count(predicate: (Swatch) -> Boolean): Int {
    var matches = 0
    swatches.forEach { if (predicate(it)) matches++ }
    return matches
}

fun main() {
    println(Emberkai.swatches.joinToString("\n") { it.describe(upper = true) })
    println(Emberkai.count { swatch -> swatch.tone == Tone.WARM })
}
