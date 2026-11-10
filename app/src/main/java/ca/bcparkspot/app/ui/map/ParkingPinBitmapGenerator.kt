package ca.bcparkspot.app.ui.map

import android.content.Context
import android.graphics.*
import ca.bcparkspot.app.model.AvailabilityStatus
import org.maplibre.android.annotations.Icon
import org.maplibre.android.annotations.IconFactory

object ParkingPinBitmapGenerator {

    private val iconCache = mutableMapOf<AvailabilityStatus, Icon>()
    private var userLocationIcon: Icon? = null

    /**
     * Get or create a MapLibre Icon for a given parking availability status.
     */
    fun getPinIcon(context: Context, status: AvailabilityStatus): Icon {
        iconCache[status]?.let { return it }

        val visual = mapStatusToPinVisual(status)
        val density = context.resources.displayMetrics.density
        val sizePx = (ParkingMapConstants.PIN_SIZE_DP * density).toInt()
        val strokePx = ParkingMapConstants.PIN_STROKE_WIDTH_DP * density

        val bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        val radius = (sizePx - strokePx * 2) / 2f
        val centerX = sizePx / 2f
        val centerY = sizePx / 2f

        // Shadow Paint
        val shadowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.argb(80, 0, 0, 0)
            maskFilter = BlurMaskFilter(4f * density, BlurMaskFilter.Blur.NORMAL)
        }
        canvas.drawCircle(centerX, centerY + 2f * density, radius, shadowPaint)

        // Fill Paint (Status Color)
        val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = visual.colorArgb
            style = Paint.Style.FILL
        }
        canvas.drawCircle(centerX, centerY, radius, fillPaint)

        // White Stroke Paint
        val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = ParkingMapConstants.COLOR_PIN_BORDER
            style = Paint.Style.STROKE
            strokeWidth = strokePx
        }
        canvas.drawCircle(centerX, centerY, radius, strokePaint)

        // Inner "P" icon text paint
        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = ParkingMapConstants.PIN_INNER_ICON_SIZE_DP * density
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        // Center text vertically
        val textBounds = Rect()
        textPaint.getTextBounds("P", 0, 1, textBounds)
        val textY = centerY + (textBounds.height() / 2f)
        canvas.drawText("P", centerX, textY, textPaint)

        val icon = IconFactory.getInstance(context).fromBitmap(bitmap)
        iconCache[status] = icon
        return icon
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
