import java.util.*
import kotlin.math.min


/**
 * The Color class defines methods for creating and converting color ints.
 * Colors are represented as packed ints, made up of 4 bytes: alpha, red,
 * green, blue. The values are unpremultiplied, meaning any transparency is
 * stored solely in the alpha component, and not in the color components. The
 * components are stored as follows (alpha << 24) | (red << 16) |
 * (green << 8) | blue. Each component ranges between 0..255 with 0
 * meaning no contribution for that component, and 255 meaning 100%
 * contribution. Thus opaque-black would be 0xFF000000 (100% opaque but
 * no contributions from red, green, or blue), and opaque-white would be
 * 0xFFFFFFFF
 */
//object Color {
    const val BLACK = -0x1000000
    const val DKGRAY = -0xbbbbbc
    const val GRAY = -0x777778
    const val LTGRAY = -0x333334
    const val WHITE = -0x1
    const val RED = -0x10000
    const val GREEN = -0xff0100
    const val BLUE = -0xffff01
    const val YELLOW = -0x100
    const val CYAN = -0xff0001
    const val MAGENTA = -0xff01
    const val TRANSPARENT = 0

    /**
     * Return the alpha component of a color int. This is the same as saying
     * color >>> 24
     */
    fun alpha(color: Int): Int {
        return color ushr 24
    }

    /**
     * Return the red component of a color int. This is the same as saying
     * (color >> 16) & 0xFF
     */
    fun red(color: Int): Int {
        return color shr 16 and 0xFF
    }

    /**
     * Return the green component of a color int. This is the same as saying
     * (color >> 8) & 0xFF
     */
    fun green(color: Int): Int {
        return color shr 8 and 0xFF
    }

    /**
     * Return the blue component of a color int. This is the same as saying
     * color & 0xFF
     */
    fun blue(color: Int): Int {
        return color and 0xFF
    }

    /**
     * Return a color-int from red, green, blue components.
     * The alpha component is implicity 255 (fully opaque).
     * These component values should be [0..255], but there is no
     * range check performed, so if they are out of range, the
     * returned color is undefined.
     * @param red  Red component [0..255] of the color
     * @param green Green component [0..255] of the color
     * @param blue  Blue component [0..255] of the color
     */
    fun rgb(red: Int, green: Int, blue: Int): Int {
        return 0xFF shl 24 or (red shl 16) or (green shl 8) or blue
    }

    /**
     * Return a color-int from alpha, red, green, blue components.
     * These component values should be [0..255], but there is no
     * range check performed, so if they are out of range, the
     * returned color is undefined.
     * @param alpha Alpha component [0..255] of the color
     * @param red   Red component [0..255] of the color
     * @param green Green component [0..255] of the color
     * @param blue  Blue component [0..255] of the color
     */
    fun argb(alpha: Int, red: Int, green: Int, blue: Int): Int {
        return alpha shl 24 or (red shl 16) or (green shl 8) or blue
    }

    /**
     * Returns the hue component of a color int.
     *
     * @return A value between 0.0f and 1.0f
     *
     * @hide Pending API council
     */
    fun hue(color: Int): Float {
        val r = color shr 16 and 0xFF
        val g = color shr 8 and 0xFF
        val b = color and 0xFF
        val V = Math.max(b, Math.max(r, g))
        val temp = Math.min(b, Math.min(r, g))
        var H: Float
        if (V == temp) {
            H = 0f
        } else {
            val vtemp = (V - temp).toFloat()
            val cr = (V - r) / vtemp
            val cg = (V - g) / vtemp
            val cb = (V - b) / vtemp
            H = if (r == V) {
                cb - cg
            } else if (g == V) {
                2 + cr - cb
            } else {
                4 + cg - cr
            }
            H /= 6f
            if (H < 0) {
                H++
            }
        }
        return H
    }

    /**
     * Returns the saturation component of a color int.
     *
     * @return A value between 0.0f and 1.0f
     *
     * @hide Pending API council
     */
    fun saturation(color: Int): Float {
        val r = color shr 16 and 0xFF
        val g = color shr 8 and 0xFF
        val b = color and 0xFF
        val V = Math.max(b, Math.max(r, g))
        val temp = Math.min(b, Math.min(r, g))
        val S: Float
        S = if (V == temp) {
            0f
        } else {
            (V - temp) / V.toFloat()
        }
        return S
    }

    /**
     * Returns the brightness component of a color int.
     *
     * @return A value between 0.0f and 1.0f
     *
     * @hide Pending API council
     */
    fun brightness(color: Int): Float {
        val r = color shr 16 and 0xFF
        val g = color shr 8 and 0xFF
        val b = color and 0xFF
        val V = Math.max(b, Math.max(r, g))
        return V / 255f
    }

    /**
     * Parse the color string, and return the corresponding color-int.
     * If the string cannot be parsed, throws an IllegalArgumentException
     * exception. Supported formats are:
     * #RRGGBB
     * #AARRGGBB
     * 'red', 'blue', 'green', 'black', 'white', 'gray', 'cyan', 'magenta',
     * 'yellow', 'lightgray', 'darkgray', 'grey', 'lightgrey', 'darkgrey',
     * 'aqua', 'fuschia', 'lime', 'maroon', 'navy', 'olive', 'purple',
     * 'silver', 'teal'
     */
    fun parseColor(colorString: String): Int {
        if (colorString[0] == '#') {
            // Use a long to avoid rollovers on #ffXXXXXX
            var color = colorString.substring(1).toLong(16)
            if (colorString.length == 7) {
                // Set the alpha value
                color = color or -0x1000000
            } else require(colorString.length == 9) { "Unknown color" }
            return color.toInt()
        } else {
            val color = sColorNameMap[colorString.toLowerCase(Locale.ROOT)]
            if (color != null) {
                return color
            }
        }
        throw IllegalArgumentException("Unknown color")
    }

    /**
     * Convert HSB components to an ARGB color. Alpha set to 0xFF.
     * hsv[0] is Hue [0 .. 1)
     * hsv[1] is Saturation [0...1]
     * hsv[2] is Value [0...1]
     * If hsv values are out of range, they are pinned.
     * @param hsb  3 element array which holds the input HSB components.
     * @return the resulting argb color
     *
     * @hide Pending API council
     */
    fun HSBtoColor(hsb: FloatArray): Int {
        return HSBtoColor(hsb[0], hsb[1], hsb[2])
    }

    /**
     * Convert HSB components to an ARGB color. Alpha set to 0xFF.
     * hsv[0] is Hue [0 .. 1)
     * hsv[1] is Saturation [0...1]
     * hsv[2] is Value [0...1]
     * If hsv values are out of range, they are pinned.
     * @param h Hue component
     * @param s Saturation component
     * @param b Brightness component
     * @return the resulting argb color
     *
     * @hide Pending API council
     */
    fun HSBtoColor(h: Float, s: Float, b: Float): Int {
        var h = h
        var s = s
        var b = b
        h = h.coerceIn(0.0f, 1.0f)
        s = s.coerceIn(0.0f, 1.0f)
        b = b.coerceIn(0.0f, 1.0f)
        var red = 0.0f
        var green = 0.0f
        var blue = 0.0f
        val hf = (h - h.toInt()) * 6.0f
        val ihf = hf.toInt()
        val f = hf - ihf
        val pv = b * (1.0f - s)
        val qv = b * (1.0f - s * f)
        val tv = b * (1.0f - s * (1.0f - f))
        when (ihf) {
            0 -> {
                red = b
                green = tv
                blue = pv
            }
            1 -> {
                red = qv
                green = b
                blue = pv
            }
            2 -> {
                red = pv
                green = b
                blue = tv
            }
            3 -> {
                red = pv
                green = qv
                blue = b
            }
            4 -> {
                red = tv
                green = pv
                blue = b
            }
            5 -> {
                red = b
                green = pv
                blue = qv
            }
        }
        return -0x1000000 or ((red * 255.0f).toInt() shl 16) or
                ((green * 255.0f).toInt() shl 8) or (blue * 255.0f).toInt()
    }

    /**
     * Convert RGB components to HSV.
     * hsv[0] is Hue [0 .. 360)
     * hsv[1] is Saturation [0...1]
     * hsv[2] is Value [0...1]
     * @param red  red component value [0..255]
     * @param green  green component value [0..255]
     * @param blue  blue component value [0..255]
     * @param hsv  3 element array which holds the resulting HSV components.
     */
    fun RGBToHSV(red: Int, green: Int, blue: Int, hsv: FloatArray) {
        if (hsv.size < 3) {
            throw RuntimeException("3 components required for hsv")
        }
        nativeRGBToHSV(red, green, blue, hsv)
    }

    /**
     * Convert the argb color to its HSV components.
     * hsv[0] is Hue [0 .. 360)
     * hsv[1] is Saturation [0...1]
     * hsv[2] is Value [0...1]
     * @param color the argb color to convert. The alpha component is ignored.
     * @param hsv  3 element array which holds the resulting HSV components.
     */
    fun colorToHSV(color: Int, hsv: FloatArray) {
        RGBToHSV(color shr 16 and 0xFF, color shr 8 and 0xFF, color and 0xFF, hsv)
    }

    /**
     * Convert HSV components to an ARGB color. Alpha set to 0xFF.
     * hsv[0] is Hue [0 .. 360)
     * hsv[1] is Saturation [0...1]
     * hsv[2] is Value [0...1]
     * If hsv values are out of range, they are pinned.
     * @param hsv  3 element array which holds the input HSV components.
     * @return the resulting argb color
     */
    fun HSVToColor(hsv: FloatArray): Int {
        return HSVToColor(0xFF, hsv)
    }

    /**
     * Convert HSV components to an ARGB color. The alpha component is passed
     * through unchanged.
     * hsv[0] is Hue [0 .. 360)
     * hsv[1] is Saturation [0...1]
     * hsv[2] is Value [0...1]
     * If hsv values are out of range, they are pinned.
     * @param alpha the alpha component of the returned argb color.
     * @param hsv  3 element array which holds the input HSV components.
     * @return the resulting argb color
     */
    fun HSVToColor(alpha: Int, hsv: FloatArray): Int {
        if (hsv.size < 3) {
            throw RuntimeException("3 components required for hsv")
        }
        return nativeHSVToColor(alpha, hsv)
    }

    private external fun nativeRGBToHSV(red: Int, greed: Int, blue: Int, hsv: FloatArray)
    private external fun nativeHSVToColor(alpha: Int, hsv: FloatArray): Int

    /**
     * Converts an HTML color (named or numeric) to an integer RGB value.
     *
     * @param color Non-null color string.
     *
     * @return A color value, or `-1` if the color string could not be interpreted.
     *
     * @hide
     */
   /* fun getHtmlColor(color: String): Int {
        val i = sColorNameMap!![color.toLowerCase(Locale.ROOT)]
        return i
            ?: try {
                XmlUtils.convertValueToInt(color, -1)
            } catch (nfe: NumberFormatException) {
                -1
            }
    }*/

    private var sColorNameMap: HashMap<String, Int> = HashMap<String, Int>().apply {
        this["black"] = BLACK
        this["darkgray"] = DKGRAY
        this["gray"] = GRAY
        this["lightgray"] = LTGRAY
        this["white"] = WHITE
        this["red"] = RED
        this["green"] = GREEN
        this["blue"] = BLUE
        this["yellow"] = YELLOW
        this["cyan"] = CYAN
        this["magenta"] = MAGENTA
        this["aqua"] = 0x00FFFF
        this["fuchsia"] = 0xFF00FF
        this["darkgrey"] = DKGRAY
        this["grey"] = GRAY
        this["lightgrey"] = LTGRAY
        this["lime"] = 0x00FF00
        this["maroon"] = 0x800000
        this["navy"] = 0x000080
        this["olive"] = 0x808000
        this["purple"] = 0x800080
        this["silver"] = 0xC0C0C0
        this["teal"] = 0x008080
    }

//    init {
//        sColorNameMap["black"] = BLACK
//        sColorNameMap["darkgray"] = DKGRAY
//        sColorNameMap["gray"] = GRAY
//        sColorNameMap["lightgray"] = LTGRAY
//        sColorNameMap["white"] = WHITE
//        sColorNameMap["red"] = RED
//        sColorNameMap["green"] = GREEN
//        sColorNameMap["blue"] = BLUE
//        sColorNameMap["yellow"] = YELLOW
//        sColorNameMap["cyan"] = CYAN
//        sColorNameMap["magenta"] = MAGENTA
//        sColorNameMap["aqua"] = 0x00FFFF
//        sColorNameMap["fuchsia"] = 0xFF00FF
//        sColorNameMap["darkgrey"] = DKGRAY
//        sColorNameMap["grey"] = GRAY
//        sColorNameMap["lightgrey"] = LTGRAY
//        sColorNameMap["lime"] = 0x00FF00
//        sColorNameMap["maroon"] = 0x800000
//        sColorNameMap["navy"] = 0x000080
//        sColorNameMap["olive"] = 0x808000
//        sColorNameMap["purple"] = 0x800080
//        sColorNameMap["silver"] = 0xC0C0C0
//        sColorNameMap["teal"] = 0x008080
//    }
//}

/*fun manipulateColor(color: Int, factor: Float): Int {
    val a: Int = alpha(color)
    val r = (red(color) * factor).roundToInt()
    val g = (green(color) * factor).roundToInt()
    val b = (blue(color) * factor).roundToInt()
    return argb(
        a,
        min(r, 255),
        min(g, 255),
        min(b, 255)
    )
}*/

fun oppositeColor(color: Int): Int {
    val a: Int = alpha(color)
    val r = 255 - red(color)
    val g = 255 - green(color)
    val b = 255 - blue(color)
    return argb(
        a,
        min(r, 255),
        min(g, 255),
        min(b, 255)
    )
}

fun analogousRightColor(color: Int, rotation: Float): Int {
    val red = red(color)
    val green = green(color)
    val blue = blue(color)

    val hsbRightColor = FloatArray(3)
   ColorUtils.RGBToHSL(red, green, blue, hsbRightColor)
    hsbRightColor[0] = (hsbRightColor[0] + rotation) % 360
    val rightColor = ColorUtils.HSLToColor(hsbRightColor)
    return rightColor
}

fun analogousLeftColor(color: Int, rotation: Float): Int {
    val red = red(color)
    val green = green(color)
    val blue = blue(color)

    val hsbLeftColor = FloatArray(3)
    ColorUtils.RGBToHSL(red, green, blue, hsbLeftColor)
    hsbLeftColor[0] = (hsbLeftColor[0] -     rotation) % 360
    val leftColor = ColorUtils.HSLToColor(hsbLeftColor)
    return leftColor
}

fun manipulate(color: Int, h: Float? = null, s: Double? = null, l: Double? = null): Int {
    val hsv = FloatArray(3)
    ColorUtils.colorToHSL(color, hsv)
    h?.let { hsv[0] = hsv[0] * h }
    s?.let { hsv[1] = s.toFloat() }
    l?.let { hsv[2] = l.toFloat() }
    return ColorUtils.HSLToColor(hsv)
}

fun getContrast(r: Int, g: Int, b: Int): Double {
    return 1 - (((0.299 * r) + (0.587 * g) + (0.114 * b)) / 255)
}

/**
 * A set of color-related utility methods, building upon those available in `Color`.
 */
object ColorUtils {
    private val XYZ_WHITE_REFERENCE_X = 95.047
    private val XYZ_WHITE_REFERENCE_Y = 100.0
    private val XYZ_WHITE_REFERENCE_Z = 108.883
    private val XYZ_EPSILON = 0.008856
    private val XYZ_KAPPA = 903.3
    private val MIN_ALPHA_SEARCH_MAX_ITERATIONS = 10
    private val MIN_ALPHA_SEARCH_PRECISION = 1
    private val TEMP_ARRAY = ThreadLocal<DoubleArray>()

    /**
     * Composite two potentially translucent colors over each other and returns the result.
     */
    fun compositeColors(foreground: Int, background: Int): Int {
        val bgAlpha = alpha(background)
        val fgAlpha = alpha(foreground)
        val a = compositeAlpha(fgAlpha, bgAlpha)
        val r = compositeComponent(
            red(foreground), fgAlpha,
            red(background), bgAlpha, a
        )
        val g = compositeComponent(
            green(foreground), fgAlpha,
            green(background), bgAlpha, a
        )
        val b = compositeComponent(
            blue(foreground), fgAlpha,
            blue(background), bgAlpha, a
        )
        return argb(a, r, g, b)
    }

    private fun compositeAlpha(foregroundAlpha: Int, backgroundAlpha: Int): Int {
        return 0xFF - (0xFF - backgroundAlpha) * (0xFF - foregroundAlpha) / 0xFF
    }

    private fun compositeComponent(fgC: Int, fgA: Int, bgC: Int, bgA: Int, a: Int): Int {
        return if (a == 0) 0 else ((0xFF * fgC * fgA) + (bgC * bgA * (0xFF - fgA))) / (a * 0xFF)
    }

    /**
     * Returns the luminance of a color as a float between `0.0` and `1.0`.
     *
     * Defined as the Y component in the XYZ representation of `color`.
     */
    fun calculateLuminance(color: Int): Double {
        val result = tempDouble3Array
        colorToXYZ(color, result)
        // Luminance is the Y component
        return result[1] / 100
    }

    /**
     * Returns the contrast ratio between `foreground` and `background`.
     * `background` must be opaque.
     *
     *
     * Formula defined
     * [here](http://www.w3.org/TR/2008/REC-WCAG20-20081211/#contrast-ratiodef).
     */
    fun calculateContrast(foreground: Int, background: Int): Double {
        var foreground = foreground
        if (alpha(background) != 255) {
            throw IllegalArgumentException(
                "background can not be translucent: #"
                        + Integer.toHexString(background)
            )
        }
        if (alpha(foreground) < 255) {
            // If the foreground is translucent, composite the foreground over the background
            foreground = compositeColors(foreground, background)
        }
        val luminance1 = calculateLuminance(foreground) + 0.05
        val luminance2 = calculateLuminance(background) + 0.05
        // Now return the lighter luminance divided by the darker luminance
        return Math.max(luminance1, luminance2) / Math.min(luminance1, luminance2)
    }

    /**
     * Calculates the minimum alpha value which can be applied to `foreground` so that would
     * have a contrast value of at least `minContrastRatio` when compared to
     * `background`.
     *
     * @param foreground       the foreground color
     * @param background       the opaque background color
     * @param minContrastRatio the minimum contrast ratio
     * @return the alpha value in the range 0-255, or -1 if no value could be calculated
     */
    fun calculateMinimumAlpha(
        foreground: Int, background: Int,
        minContrastRatio: Float
    ): Int {
        if (alpha(background) != 255) {
            throw IllegalArgumentException(
                ("background can not be translucent: #"
                        + Integer.toHexString(background))
            )
        }
        // First lets check that a fully opaque foreground has sufficient contrast
        var testForeground = setAlphaComponent(foreground, 255)
        var testRatio = calculateContrast(testForeground, background)
        if (testRatio < minContrastRatio) {
            // Fully opaque foreground does not have sufficient contrast, return error
            return -1
        }
        // Binary search to find a value with the minimum value which provides sufficient contrast
        var numIterations = 0
        var minAlpha = 0
        var maxAlpha = 255
        while (numIterations <= MIN_ALPHA_SEARCH_MAX_ITERATIONS &&
            (maxAlpha - minAlpha) > MIN_ALPHA_SEARCH_PRECISION
        ) {
            val testAlpha = (minAlpha + maxAlpha) / 2
            testForeground = setAlphaComponent(foreground, testAlpha)
            testRatio = calculateContrast(testForeground, background)
            if (testRatio < minContrastRatio) {
                minAlpha = testAlpha
            } else {
                maxAlpha = testAlpha
            }
            numIterations++
        }
        // Conservatively return the max of the range of possible alphas, which is known to pass.
        return maxAlpha
    }

    /**
     * Convert RGB components to HSL (hue-saturation-lightness).
     *
     *  * outHsl[0] is Hue [0 .. 360)
     *  * outHsl[1] is Saturation [0...1]
     *  * outHsl[2] is Lightness [0...1]
     *
     *
     * @param r      red component value [0..255]
     * @param g      green component value [0..255]
     * @param b      blue component value [0..255]
     * @param outHsl 3-element array which holds the resulting HSL components
     */
    fun RGBToHSL(
         r: Int,
        g: Int,  b: Int,
        outHsl: FloatArray
    ) {
        val rf = r / 255f
        val gf = g / 255f
        val bf = b / 255f
        val max = Math.max(rf, Math.max(gf, bf))
        val min = Math.min(rf, Math.min(gf, bf))
        val deltaMaxMin = max - min
        var h: Float
        val s: Float
        val l = (max + min) / 2f
        if (max == min) {
            // Monochromatic
            s = 0f
            h = s
        } else {
            if (max == rf) {
                h = ((gf - bf) / deltaMaxMin) % 6f
            } else if (max == gf) {
                h = ((bf - rf) / deltaMaxMin) + 2f
            } else {
                h = ((rf - gf) / deltaMaxMin) + 4f
            }
            s = deltaMaxMin / (1f - Math.abs(2f * l - 1f))
        }
        h = (h * 60f) % 360f
        if (h < 0) {
            h += 360f
        }
        outHsl[0] = constrain(h, 0f, 360f)
        outHsl[1] = constrain(s, 0f, 1f)
        outHsl[2] = constrain(l, 0f, 1f)
    }

    /**
     * Convert the ARGB color to its HSL (hue-saturation-lightness) components.
     *
     *  * outHsl[0] is Hue [0 .. 360)
     *  * outHsl[1] is Saturation [0...1]
     *  * outHsl[2] is Lightness [0...1]
     *
     *
     * @param color  the ARGB color to convert. The alpha component is ignored
     * @param outHsl 3-element array which holds the resulting HSL components
     */
    fun colorToHSL( color: Int, outHsl: FloatArray) {
        RGBToHSL(red(color), green(color), blue(color), outHsl)
    }

    /**
     * Convert HSL (hue-saturation-lightness) components to a RGB color.
     *
     *  * hsl[0] is Hue [0 .. 360)
     *  * hsl[1] is Saturation [0...1]
     *  * hsl[2] is Lightness [0...1]
     *
     * If hsv values are out of range, they are pinned.
     *
     * @param hsl 3-element array which holds the input HSL components
     * @return the resulting RGB color
     */
    fun HSLToColor(hsl: FloatArray): Int {
        val h = hsl[0]
        val s = hsl[1]
        val l = hsl[2]
        val c = (1f - Math.abs(2 * l - 1f)) * s
        val m = l - 0.5f * c
        val x = c * (1f - Math.abs((h / 60f % 2f) - 1f))
        val hueSegment = h.toInt() / 60
        var r = 0
        var g = 0
        var b = 0
        when (hueSegment) {
            0 -> {
                r = Math.round(255 * (c + m))
                g = Math.round(255 * (x + m))
                b = Math.round(255 * m)
            }
            1 -> {
                r = Math.round(255 * (x + m))
                g = Math.round(255 * (c + m))
                b = Math.round(255 * m)
            }
            2 -> {
                r = Math.round(255 * m)
                g = Math.round(255 * (c + m))
                b = Math.round(255 * (x + m))
            }
            3 -> {
                r = Math.round(255 * m)
                g = Math.round(255 * (x + m))
                b = Math.round(255 * (c + m))
            }
            4 -> {
                r = Math.round(255 * (x + m))
                g = Math.round(255 * m)
                b = Math.round(255 * (c + m))
            }
            5, 6 -> {
                r = Math.round(255 * (c + m))
                g = Math.round(255 * m)
                b = Math.round(255 * (x + m))
            }
        }
        r = constrain(r, 0, 255)
        g = constrain(g, 0, 255)
        b = constrain(b, 0, 255)
        return rgb(r, g, b)
    }

    /**
     * Set the alpha component of `color` to be `alpha`.
     */
    fun setAlphaComponent(
         color: Int,
        alpha: Int
    ): Int {
        if (alpha < 0 || alpha > 255) {
            throw IllegalArgumentException("alpha must be between 0 and 255.")
        }
        return (color and 0x00ffffff) or (alpha shl 24)
    }

    /**
     * Convert the ARGB color to its CIE Lab representative components.
     *
     * @param color  the ARGB color to convert. The alpha component is ignored
     * @param outLab 3-element array which holds the resulting LAB components
     */
    fun colorToLAB(color: Int, outLab: DoubleArray) {
        RGBToLAB(red(color), green(color), blue(color), outLab)
    }

    /**
     * Convert RGB components to its CIE Lab representative components.
     *
     *
     *  * outLab[0] is L [0 ...1)
     *  * outLab[1] is a [-128...127)
     *  * outLab[2] is b [-128...127)
     *
     *
     * @param r      red component value [0..255]
     * @param g      green component value [0..255]
     * @param b      blue component value [0..255]
     * @param outLab 3-element array which holds the resulting LAB components
     */
    fun RGBToLAB(
         r: Int,
         g: Int, b: Int,
         outLab: DoubleArray
    ) {
        // First we convert RGB to XYZ
        RGBToXYZ(r, g, b, outLab)
        // outLab now contains XYZ
        XYZToLAB(outLab[0], outLab[1], outLab[2], outLab)
        // outLab now contains LAB representation
    }

    /**
     * Convert the ARGB color to it's CIE XYZ representative components.
     *
     *
     * The resulting XYZ representation will use the D65 illuminant and the CIE
     * 2° Standard Observer (1931).
     *
     *
     *  * outXyz[0] is X [0 ...95.047)
     *  * outXyz[1] is Y [0...100)
     *  * outXyz[2] is Z [0...108.883)
     *
     *
     * @param color  the ARGB color to convert. The alpha component is ignored
     * @param outXyz 3-element array which holds the resulting LAB components
     */
    fun colorToXYZ( color: Int,  outXyz: DoubleArray) {
        RGBToXYZ(red(color), green(color), blue(color), outXyz)
    }

    /**
     * Convert RGB components to it's CIE XYZ representative components.
     *
     *
     * The resulting XYZ representation will use the D65 illuminant and the CIE
     * 2° Standard Observer (1931).
     *
     *
     *  * outXyz[0] is X [0 ...95.047)
     *  * outXyz[1] is Y [0...100)
     *  * outXyz[2] is Z [0...108.883)
     *
     *
     * @param r      red component value [0..255]
     * @param g      green component value [0..255]
     * @param b      blue component value [0..255]
     * @param outXyz 3-element array which holds the resulting XYZ components
     */
    fun RGBToXYZ(
         r: Int,
         g: Int,  b: Int,
         outXyz: DoubleArray
    ) {
        if (outXyz.size != 3) {
            throw IllegalArgumentException("outXyz must have a length of 3.")
        }
        var sr = r / 255.0
        sr = if (sr < 0.04045) sr / 12.92 else Math.pow((sr + 0.055) / 1.055, 2.4)
        var sg = g / 255.0
        sg = if (sg < 0.04045) sg / 12.92 else Math.pow((sg + 0.055) / 1.055, 2.4)
        var sb = b / 255.0
        sb = if (sb < 0.04045) sb / 12.92 else Math.pow((sb + 0.055) / 1.055, 2.4)
        outXyz[0] = 100 * ((sr * 0.4124) + (sg * 0.3576) + (sb * 0.1805))
        outXyz[1] = 100 * ((sr * 0.2126) + (sg * 0.7152) + (sb * 0.0722))
        outXyz[2] = 100 * ((sr * 0.0193) + (sg * 0.1192) + (sb * 0.9505))
    }

    /**
     * Converts a color from CIE XYZ to CIE Lab representation.
     *
     *
     * This method expects the XYZ representation to use the D65 illuminant and the CIE
     * 2° Standard Observer (1931).
     *
     *
     *  * outLab[0] is L [0 ...1)
     *  * outLab[1] is a [-128...127)
     *  * outLab[2] is b [-128...127)
     *
     *
     * @param x      X component value [0...95.047)
     * @param y      Y component value [0...100)
     * @param z      Z component value [0...108.883)
     * @param outLab 3-element array which holds the resulting Lab components
     */
    fun XYZToLAB(
         x: Double,
        y: Double,
         z: Double,
         outLab: DoubleArray
    ) {
        var x = x
        var y = y
        var z = z
        if (outLab.size != 3) {
            throw IllegalArgumentException("outLab must have a length of 3.")
        }
        x = pivotXyzComponent(x / XYZ_WHITE_REFERENCE_X)
        y = pivotXyzComponent(y / XYZ_WHITE_REFERENCE_Y)
        z = pivotXyzComponent(z / XYZ_WHITE_REFERENCE_Z)
        outLab[0] = Math.max(0.0, 116 * y - 16)
        outLab[1] = 500 * (x - y)
        outLab[2] = 200 * (y - z)
    }

    /**
     * Converts a color from CIE Lab to CIE XYZ representation.
     *
     *
     * The resulting XYZ representation will use the D65 illuminant and the CIE
     * 2° Standard Observer (1931).
     *
     *
     *  * outXyz[0] is X [0 ...95.047)
     *  * outXyz[1] is Y [0...100)
     *  * outXyz[2] is Z [0...108.883)
     *
     *
     * @param l      L component value [0...100)
     * @param a      A component value [-128...127)
     * @param b      B component value [-128...127)
     * @param outXyz 3-element array which holds the resulting XYZ components
     */
    fun LABToXYZ(
        l: Double,
         a: Double,
         b: Double,
         outXyz: DoubleArray
    ) {
        val fy = (l + 16) / 116
        val fx = a / 500 + fy
        val fz = fy - b / 200
        var tmp = Math.pow(fx, 3.0)
        val xr = if (tmp > XYZ_EPSILON) tmp else (116 * fx - 16) / XYZ_KAPPA
        val yr = if (l > XYZ_KAPPA * XYZ_EPSILON) Math.pow(fy, 3.0) else l / XYZ_KAPPA
        tmp = Math.pow(fz, 3.0)
        val zr = if (tmp > XYZ_EPSILON) tmp else (116 * fz - 16) / XYZ_KAPPA
        outXyz[0] = xr * XYZ_WHITE_REFERENCE_X
        outXyz[1] = yr * XYZ_WHITE_REFERENCE_Y
        outXyz[2] = zr * XYZ_WHITE_REFERENCE_Z
    }

    /**
     * Converts a color from CIE XYZ to its RGB representation.
     *
     *
     * This method expects the XYZ representation to use the D65 illuminant and the CIE
     * 2° Standard Observer (1931).
     *
     * @param x X component value [0...95.047)
     * @param y Y component value [0...100)
     * @param z Z component value [0...108.883)
     * @return int containing the RGB representation
     */
    
    fun XYZToColor(
         x: Double,
         y: Double,
         z: Double
    ): Int {
        var r = ((x * 3.2406) + (y * -1.5372) + (z * -0.4986)) / 100
        var g = ((x * -0.9689) + (y * 1.8758) + (z * 0.0415)) / 100
        var b = ((x * 0.0557) + (y * -0.2040) + (z * 1.0570)) / 100
        r = if (r > 0.0031308) 1.055 * Math.pow(r, 1 / 2.4) - 0.055 else 12.92 * r
        g = if (g > 0.0031308) 1.055 * Math.pow(g, 1 / 2.4) - 0.055 else 12.92 * g
        b = if (b > 0.0031308) 1.055 * Math.pow(b, 1 / 2.4) - 0.055 else 12.92 * b
        return rgb(
            constrain(Math.round(r * 255).toInt(), 0, 255),
            constrain(Math.round(g * 255).toInt(), 0, 255),
            constrain(Math.round(b * 255).toInt(), 0, 255)
        )
    }

    /**
     * Converts a color from CIE Lab to its RGB representation.
     *
     * @param l L component value [0...100]
     * @param a A component value [-128...127]
     * @param b B component value [-128...127]
     * @return int containing the RGB representation
     */
    
    fun LABToColor(
         l: Double,
         a: Double,
         b: Double
    ): Int {
        val result = tempDouble3Array
        LABToXYZ(l, a, b, result)
        return XYZToColor(result[0], result[1], result[2])
    }

    /**
     * Returns the euclidean distance between two LAB colors.
     */
    fun distanceEuclidean( labX: DoubleArray,  labY: DoubleArray): Double {
        return Math.sqrt(
            (Math.pow(labX[0] - labY[0], 2.0)
                    + Math.pow(labX[1] - labY[1], 2.0)
                    + Math.pow(labX[2] - labY[2], 2.0))
        )
    }

    private fun constrain(amount: Float, low: Float, high: Float): Float {
        return if (amount < low) low else (if (amount > high) high else amount)
    }

    private fun constrain(amount: Int, low: Int, high: Int): Int {
        return if (amount < low) low else (if (amount > high) high else amount)
    }

    private fun pivotXyzComponent(component: Double): Double {
        return if (component > XYZ_EPSILON) Math.pow(component, 1 / 3.0) else (XYZ_KAPPA * component + 16) / 116
    }

    /**
     * Blend between two ARGB colors using the given ratio.
     *
     *
     * A blend ratio of 0.0 will result in `color1`, 0.5 will give an even blend,
     * 1.0 will result in `color2`.
     *
     * @param color1 the first ARGB color
     * @param color2 the second ARGB color
     * @param ratio  the blend ratio of `color1` to `color2`
     */
    
    fun blendARGB(
         color1: Int,  color2: Int,
        ratio: Float
    ): Int {
        val inverseRatio = 1 - ratio
        val a = alpha(color1) * inverseRatio + alpha(color2) * ratio
        val r = red(color1) * inverseRatio + red(color2) * ratio
        val g = green(color1) * inverseRatio + green(color2) * ratio
        val b = blue(color1) * inverseRatio + blue(color2) * ratio
        return argb(a.toInt(), r.toInt(), g.toInt(), b.toInt())
    }

    /**
     * Blend between `hsl1` and `hsl2` using the given ratio. This will interpolate
     * the hue using the shortest angle.
     *
     *
     * A blend ratio of 0.0 will result in `hsl1`, 0.5 will give an even blend,
     * 1.0 will result in `hsl2`.
     *
     * @param hsl1      3-element array which holds the first HSL color
     * @param hsl2      3-element array which holds the second HSL color
     * @param ratio     the blend ratio of `hsl1` to `hsl2`
     * @param outResult 3-element array which holds the resulting HSL components
     */
    fun blendHSL(
         hsl1: FloatArray,
         hsl2: FloatArray,
       ratio: Float,
         outResult: FloatArray
    ) {
        if (outResult.size != 3) {
            throw IllegalArgumentException("result must have a length of 3.")
        }
        val inverseRatio = 1 - ratio
        // Since hue is circular we will need to interpolate carefully
        outResult[0] = circularInterpolate(hsl1[0], hsl2[0], ratio)
        outResult[1] = hsl1[1] * inverseRatio + hsl2[1] * ratio
        outResult[2] = hsl1[2] * inverseRatio + hsl2[2] * ratio
    }

    /**
     * Blend between two CIE-LAB colors using the given ratio.
     *
     *
     * A blend ratio of 0.0 will result in `lab1`, 0.5 will give an even blend,
     * 1.0 will result in `lab2`.
     *
     * @param lab1      3-element array which holds the first LAB color
     * @param lab2      3-element array which holds the second LAB color
     * @param ratio     the blend ratio of `lab1` to `lab2`
     * @param outResult 3-element array which holds the resulting LAB components
     */
    fun blendLAB(
         lab1: DoubleArray,
         lab2: DoubleArray,
       ratio: Double,
         outResult: DoubleArray
    ) {
        if (outResult.size != 3) {
            throw IllegalArgumentException("outResult must have a length of 3.")
        }
        val inverseRatio = 1 - ratio
        outResult[0] = lab1[0] * inverseRatio + lab2[0] * ratio
        outResult[1] = lab1[1] * inverseRatio + lab2[1] * ratio
        outResult[2] = lab1[2] * inverseRatio + lab2[2] * ratio
    }

    fun circularInterpolate(a: Float, b: Float, f: Float): Float {
        var a = a
        var b = b
        if (Math.abs(b - a) > 180) {
            if (b > a) {
                a += 360f
            } else {
                b += 360f
            }
        }
        return (a + ((b - a) * f)) % 360
    }

    private val tempDouble3Array: DoubleArray
        private get() {
            var result = TEMP_ARRAY.get()
            if (result == null) {
                result = DoubleArray(3)
                TEMP_ARRAY.set(result)
            }
            return result
        }
}