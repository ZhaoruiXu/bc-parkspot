package ca.bcparkspot.app.ui.map

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ca.bcparkspot.app.model.AvailabilityStatus

/**
 * Named constants for Map mode styling, dimensions, camera settings, and pin colors.
 * Hard rule: all numerical dimensions, delays, and colors are strictly named constants.
 */
object ParkingMapConstants {
    // Pin dimensions (in density-independent pixels) - reduced from 36 to 28 to prevent overlaps
    const val PIN_SIZE_DP: Float = 28f
    const val PIN_STROKE_WIDTH_DP: Float = 2.5f
    const val PIN_INNER_ICON_SIZE_DP: Float = 14f
    const val USER_PIN_SIZE_DP: Float = 18f
    const val USER_PIN_STROKE_WIDTH_DP: Float = 3f

    // Camera and layout constants (Hard rule: no magic numbers!)
    const val MAP_BOUNDS_PADDING_TOP_PX: Int = 280
    const val MAP_BOUNDS_PADDING_SIDE_PX: Int = 120
    const val MAP_BOUNDS_PADDING_BOTTOM_PX: Int = 140
    const val CAMERA_ANIMATION_DURATION_MS: Int = 600
    const val STEP_ZOOM_ANIMATION_DURATION_MS: Int = CAMERA_ANIMATION_DURATION_MS / 2 // 300ms quick animation for step zoom
    const val SINGLE_LOT_ZOOM: Double = 11.5
    val MAP_CONTROL_BUTTON_SIZE_DP: Dp = 38.dp
    const val MAP_SURFACE_ALPHA: Float = 0.95f
    val COLOR_MAP_BORDER_NEUTRAL: Color = Color(0xFFCBD5E1)
    val HERO_BANNER_HEIGHT_DP: Dp = 210.dp

    // Attribution styling
    val MAP_ATTRIBUTION_PADDING_START: Dp = 12.dp
    val MAP_ATTRIBUTION_PADDING_TOP: Dp = 12.dp
    val MAP_ATTRIBUTION_CORNER_RADIUS: Dp = 4.dp
    val MAP_ATTRIBUTION_BORDER_WIDTH: Dp = 0.5.dp
    const val MAP_ATTRIBUTION_ALPHA: Float = 0.85f
    val MAP_ATTRIBUTION_FONT_SIZE: TextUnit = 10.sp
    val MAP_ATTRIBUTION_PADDING_HORIZONTAL: Dp = 6.dp
    val MAP_ATTRIBUTION_PADDING_VERTICAL: Dp = 2.dp

    // Offline fallback card
    val MAP_OFFLINE_CARD_PADDING: Dp = 24.dp
    val MAP_OFFLINE_CARD_CORNER_RADIUS: Dp = 16.dp
    val MAP_OFFLINE_BORDER_STROKE_WIDTH: Dp = 1.dp
    const val MAP_OUTLINE_SUBTLE_ALPHA: Float = 0.2f
    val MAP_OFFLINE_ELEVATION: Dp = 4.dp
    val MAP_OFFLINE_CONTENT_SPACING: Dp = 12.dp
    val MAP_OFFLINE_ICON_BG_SIZE: Dp = 54.dp
    val MAP_OFFLINE_ICON_BG_RADIUS: Dp = 27.dp
    val MAP_OFFLINE_ICON_SIZE: Dp = 28.dp
    val MAP_OFFLINE_DESC_FONT_SIZE: TextUnit = 13.sp
    val MAP_OFFLINE_SPACER_HEIGHT: Dp = 4.dp
    val MAP_OFFLINE_BTN_CORNER_RADIUS: Dp = 10.dp
    val MAP_OFFLINE_BTN_ICON_SIZE: Dp = 18.dp
    val MAP_OFFLINE_BTN_SPACING: Dp = 8.dp

    // Map controls stack and lot count pill
    val MAP_CONTROLS_PADDING_TOP: Dp = 10.dp
    val MAP_CONTROLS_PADDING_END: Dp = 12.dp
    val MAP_CONTROLS_SPACING: Dp = 10.dp
    val MAP_LOT_PILL_CORNER_RADIUS: Dp = 20.dp
    val MAP_LOT_PILL_BORDER_WIDTH: Dp = 1.dp
    val MAP_LOT_PILL_SHADOW_ELEVATION: Dp = 3.dp
    val MAP_LOT_PILL_PADDING_HORIZONTAL: Dp = 12.dp
    val MAP_LOT_PILL_PADDING_VERTICAL: Dp = 7.dp
    val MAP_LOT_PILL_FONT_SIZE: TextUnit = 11.sp
    val MAP_CONTROL_BUTTON_CORNER_RADIUS: Dp = 12.dp
    val MAP_CONTROL_BUTTON_BORDER_WIDTH: Dp = 1.dp
    val MAP_CONTROL_BUTTON_SHADOW_ELEVATION: Dp = 3.dp
    val MAP_CONTROL_ICON_SIZE: Dp = 19.dp

    // Selected lot bottom card
    val MAP_BOTTOM_CARD_PADDING_HORIZONTAL: Dp = 16.dp
    val MAP_BOTTOM_CARD_PADDING_VERTICAL: Dp = 12.dp
    val MAP_BOTTOM_CARD_CORNER_RADIUS: Dp = 18.dp
    val MAP_BOTTOM_CARD_ELEVATION: Dp = 8.dp
    val MAP_BOTTOM_CARD_BORDER_WIDTH: Dp = 1.dp
    val MAP_BOTTOM_CARD_CONTENT_PADDING: Dp = 16.dp
    val MAP_BOTTOM_CARD_SPACING: Dp = 10.dp
    val MAP_BOTTOM_CARD_CLOSE_BTN_SIZE: Dp = 28.dp
    val MAP_BOTTOM_CARD_CLOSE_ICON_SIZE: Dp = 18.dp
    val MAP_BOTTOM_CARD_BADGE_SPACING_HORIZONTAL: Dp = 8.dp
    val MAP_BOTTOM_CARD_BADGE_SPACING_VERTICAL: Dp = 6.dp

    // Pass badge on map card
    val MAP_PASS_BADGE_CORNER_RADIUS: Dp = 12.dp
    val MAP_PASS_BADGE_BORDER_WIDTH: Dp = 0.5.dp
    const val MAP_PASS_BADGE_BORDER_ALPHA: Float = 0.25f
    val MAP_PASS_BADGE_PADDING_HORIZONTAL: Dp = 8.dp
    val MAP_PASS_BADGE_PADDING_VERTICAL: Dp = 4.dp
    val MAP_PASS_BADGE_INNER_SPACING: Dp = 4.dp
    val MAP_PASS_BADGE_ICON_SIZE: Dp = 13.dp
    val MAP_PASS_BADGE_FONT_SIZE: TextUnit = 11.sp
    val MAP_PASS_EXPLANATION_FONT_SIZE: TextUnit = 11.sp

    // Bottom card action buttons
    val MAP_ACTION_BUTTON_SPACING: Dp = 8.dp
    val MAP_ACTION_BUTTON_CORNER_RADIUS: Dp = 10.dp
    val MAP_ACTION_BUTTON_FONT_SIZE: TextUnit = 13.sp
    val MAP_NAVIGATE_BUTTON_ICON_SIZE: Dp = 16.dp
    val MAP_NAVIGATE_BUTTON_SPACING: Dp = 4.dp

    // Save Car Sheet UI constants
    val SAVE_CAR_ENABLE_GPS_BTN_HEIGHT: Dp = 28.dp
    val SAVE_CAR_CAPTION_FONT_SIZE: TextUnit = 11.sp

    // Color definitions
    // AvailabilityStatus.AVAILABLE -> Green
    const val COLOR_PIN_AVAILABLE: Int = 0xFF059669.toInt()
    // AvailabilityStatus.FILLING_FAST -> Amber/Orange
    const val COLOR_PIN_FILLING_FAST: Int = 0xFFD97706.toInt()
    // AvailabilityStatus.FULL -> Red
    const val COLOR_PIN_FULL: Int = 0xFFDC2626.toInt()
    // AvailabilityStatus.CLOSED -> Dark Slate
    const val COLOR_PIN_CLOSED: Int = 0xFF334155.toInt()
    // AvailabilityStatus.UNKNOWN -> Neutral Grey
    const val COLOR_PIN_UNKNOWN: Int = 0xFF94A3B8.toInt()

    // Marker styling accents
    const val COLOR_PIN_BORDER: Int = 0xFFFFFFFF.toInt()
    const val COLOR_USER_LOCATION_PIN: Int = 0xFF2563EB.toInt() // Royal Blue

    // Pass badge colors for map card
    const val COLOR_PASS_ACTIVE_CONTAINER: Int = 0xFFFEF3C7.toInt()
    const val COLOR_PASS_ACTIVE_TEXT: Int = 0xFF92400E.toInt()
    const val COLOR_PASS_INACTIVE_CONTAINER: Int = 0xFFF1F5F9.toInt()
    const val COLOR_PASS_INACTIVE_TEXT: Int = 0xFF475569.toInt()

    // Map initial camera viewport centering across South Coast & Sea-to-Sky BC Parks
    const val INITIAL_CAMERA_LATITUDE: Double = 49.65
    const val INITIAL_CAMERA_LONGITUDE: Double = -122.95
    const val INITIAL_CAMERA_ZOOM: Double = 8.2

    // Offline / Fallback messaging
    const val MAP_UNAVAILABLE_OFFLINE_TITLE: String = "Map unavailable offline"
    const val MAP_UNAVAILABLE_OFFLINE_DESC: String =
        "Map tiles require an internet connection. Your trailheads list, filters, and offline guide are fully functional."

    // OpenStreetMap attribution text visible directly on the map surface
    const val OSM_ATTRIBUTION_TEXT: String = "© OpenStreetMap contributors"

    // Free OpenStreetMap Tile Style JSON (no API key required)
    const val OPENSTREETMAP_RASTER_STYLE_JSON: String = """{
  "version": 8,
  "name": "OpenStreetMap",
  "sources": {
    "osm": {
      "type": "raster",
      "tiles": [
        "https://tile.openstreetmap.org/{z}/{x}/{y}.png"
      ],
      "tileSize": 256,
      "attribution": "© OpenStreetMap Contributors",
      "maxzoom": 19
    }
  },
  "layers": [
    {
      "id": "osm-tiles",
      "type": "raster",
      "source": "osm",
      "minzoom": 0,
      "maxzoom": 19
    }
  ]
}"""
}

/**
 * Visual representation data class for a map pin.
 */
data class StatusPinVisual(
    val status: AvailabilityStatus,
    val colorArgb: Int,
    val hexColor: String,
    val isNeutralGrey: Boolean,
    val label: String
)

/**
 * Pure function: maps an AvailabilityStatus to its corresponding pin visual representation.
 * Designed without Android SDK dependencies so it can be cleanly unit-tested on the JVM.
 */
fun mapStatusToPinVisual(status: AvailabilityStatus): StatusPinVisual {
    val colorArgb = when (status) {
        AvailabilityStatus.AVAILABLE -> ParkingMapConstants.COLOR_PIN_AVAILABLE
        AvailabilityStatus.FILLING_FAST -> ParkingMapConstants.COLOR_PIN_FILLING_FAST
        AvailabilityStatus.FULL -> ParkingMapConstants.COLOR_PIN_FULL
        AvailabilityStatus.CLOSED -> ParkingMapConstants.COLOR_PIN_CLOSED
        AvailabilityStatus.UNKNOWN -> ParkingMapConstants.COLOR_PIN_UNKNOWN
    }

    val hex = String.format("#%06X", (0xFFFFFF and colorArgb))
    val isGrey = (status == AvailabilityStatus.UNKNOWN)

    return StatusPinVisual(
        status = status,
        colorArgb = colorArgb,
        hexColor = hex,
        isNeutralGrey = isGrey,
        label = status.label
    )
}
