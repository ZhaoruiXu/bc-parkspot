package ca.bcparkspot.app.ui.map

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BlurMaskFilter
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Rect
import android.graphics.Typeface
import ca.bcparkspot.app.ui.theme.MapColors
import ca.bcparkspot.app.model.AvailabilityStatus
import org.maplibre.android.annotations.Icon
import org.maplibre.android.annotations.IconFactory

object ParkingPinBitmapGenerator {

    private val iconCache = mutableMapOf<AvailabilityStatus, Icon>()
    private var userLocationIcon: Icon? = null

    /** A teardrop pin in the status colour with the app's mountain mark, plus a status badge when hikers reported. */
    fun getPinIcon(context: Context, status: AvailabilityStatus): Icon {
        iconCache[status]?.let { return it }
        val icon = IconFactory.getInstance(context).fromBitmap(drawPin(context.resources.displayMetrics.density, mapStatusToPinVisual(status)))
        iconCache[status] = icon
        return icon
    }

    private fun drawPin(density: Float, visual: StatusPinVisual): Bitmap {
        val c = ParkingMapConstants
        val baseWidth = c.PIN_WIDTH_DP * density
        val stroke = c.PIN_STROKE_WIDTH_DP * density
        val halo = c.PIN_HALO_WIDTH_DP * density
        val shadowPad = (c.PIN_SHADOW_BLUR_DP + c.PIN_SHADOW_OFFSET_DP) * density
        val radius = baseWidth * HALF - stroke - halo - shadowPad * HALF
        // Room for the badge past the head's edge, added on both sides so the point stays centred.
        val badgeOverflow = (radius * (c.BADGE_OFFSET + c.BADGE_RADIUS) - radius).coerceAtLeast(0f)
        val width = (baseWidth + badgeOverflow * BOTH_SIDES).toInt()
        val height = (c.PIN_HEIGHT_DP * density + badgeOverflow).toInt()
        val cx = width * HALF
        val cy = badgeOverflow + radius + stroke + halo
        val tipY = height - shadowPad

        val outline = Path().apply {
            addCircle(cx, cy, radius, Path.Direction.CW)
            val tail = Path().apply {
                moveTo(cx - radius * c.PIN_TAIL_SPREAD, cy + radius * c.PIN_TAIL_START)
                lineTo(cx, tipY)
                lineTo(cx + radius * c.PIN_TAIL_SPREAD, cy + radius * c.PIN_TAIL_START)
                close()
            }
            op(tail, Path.Op.UNION)
        }

        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.save()
        canvas.translate(0f, c.PIN_SHADOW_OFFSET_DP * density)
        canvas.drawPath(outline, Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = MapColors.PIN_SHADOW
            maskFilter = BlurMaskFilter(c.PIN_SHADOW_BLUR_DP * density, BlurMaskFilter.Blur.NORMAL)
        })
        canvas.restore()
        canvas.drawPath(outline, Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = MapColors.PIN_HALO; style = Paint.Style.STROKE; strokeWidth = stroke + halo * BOTH_SIDES
        })
        canvas.drawPath(outline, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = visual.colorArgb; style = Paint.Style.FILL })
        canvas.drawPath(outline, Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = MapColors.PIN_BORDER; style = Paint.Style.STROKE; strokeWidth = stroke
        })

        val mountain = Path().apply {
            moveTo(cx - radius * c.MOUNTAIN_HALF_WIDTH, cy + radius * c.MOUNTAIN_BASE)
            lineTo(cx + radius * c.MOUNTAIN_MAIN_PEAK_X, cy + radius * c.MOUNTAIN_MAIN_PEAK_Y)
            lineTo(cx + radius * c.MOUNTAIN_VALLEY_X, cy + radius * c.MOUNTAIN_VALLEY_Y)
            lineTo(cx + radius * c.MOUNTAIN_SIDE_PEAK_X, cy + radius * c.MOUNTAIN_SIDE_PEAK_Y)
            lineTo(cx + radius * c.MOUNTAIN_HALF_WIDTH, cy + radius * c.MOUNTAIN_BASE)
            close()
        }
        canvas.drawPath(mountain, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = MapColors.PIN_GLYPH; style = Paint.Style.FILL })

        if (visual.glyph.isNotEmpty()) {
            val bx = cx + radius * c.BADGE_OFFSET
            val by = cy - radius * c.BADGE_OFFSET
            val br = radius * c.BADGE_RADIUS
            canvas.drawCircle(bx, by, br, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = MapColors.PIN_BORDER })
            canvas.drawCircle(bx, by, br, Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = visual.colorArgb; style = Paint.Style.STROKE; strokeWidth = halo
            })
            val text = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = visual.colorArgb
                textSize = c.BADGE_GLYPH_SIZE_DP * density
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                textAlign = Paint.Align.CENTER
            }
            val bounds = Rect()
            text.getTextBounds(visual.glyph, 0, visual.glyph.length, bounds)
            canvas.drawText(visual.glyph, bx, by - bounds.exactCenterY(), text)
        }
        return bitmap
    }

    /**
     * Get or create a MapLibre Icon for the user's location (distinct blue pulsing pin).
     */
    fun getUserLocationIcon(context: Context): Icon {
        userLocationIcon?.let { return it }

        val density = context.resources.displayMetrics.density
        val sizePx = (ParkingMapConstants.USER_PIN_SIZE_DP * density).toInt()
        val strokePx = ParkingMapConstants.USER_PIN_STROKE_WIDTH_DP * density

        val bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        val centerX = sizePx / 2f
        val centerY = sizePx / 2f
        val radius = (sizePx - strokePx * 2) / 2f

        // Blue Fill
        val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = ParkingMapConstants.COLOR_USER_LOCATION_PIN
            style = Paint.Style.FILL
        }
        canvas.drawCircle(centerX, centerY, radius, fillPaint)

        // White Outer Ring
        val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = ParkingMapConstants.COLOR_PIN_BORDER
            style = Paint.Style.STROKE
            strokeWidth = strokePx
        }
        canvas.drawCircle(centerX, centerY, radius, strokePaint)

        val icon = IconFactory.getInstance(context).fromBitmap(bitmap)
        userLocationIcon = icon
        return icon
    }
}

/** Halving and doubling for centring and for padding applied on both sides. */
private const val HALF = 0.5f
private const val BOTH_SIDES = 2
