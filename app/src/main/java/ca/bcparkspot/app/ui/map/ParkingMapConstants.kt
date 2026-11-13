package ca.bcparkspot.app.ui.map

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ca.bcparkspot.app.model.AvailabilityStatus
import ca.bcparkspot.app.ui.theme.MapColors

/**
 * Named constants for Map mode styling, dimensions, camera settings, and pin colors.
 * Hard rule: all numerical dimensions, delays, and colors are strictly named constants.
 */
object ParkingMapConstants {
    // Teardrop pin: a round head carrying the mountain mark, with a point at the lot's exact spot
    const val PIN_WIDTH_DP: Float = 34f
    const val PIN_HEIGHT_DP: Float = 44f
    const val PIN_STROKE_WIDTH_DP: Float = 2.5f
    const val PIN_HALO_WIDTH_DP: Float = 1f
    const val PIN_SHADOW_BLUR_DP: Float = 3f
    const val PIN_SHADOW_OFFSET_DP: Float = 1.5f
    /** Where the head's sides meet the point, as a fraction of the head radius. */
    const val PIN_TAIL_SPREAD: Float = 0.62f
    const val PIN_TAIL_START: Float = 0.78f
    /** The mountain mark inside the head, as fractions of the head radius from its centre. */
    const val MOUNTAIN_HALF_WIDTH: Float = 0.62f
    const val MOUNTAIN_BASE: Float = 0.38f
    const val MOUNTAIN_MAIN_PEAK_X: Float = -0.14f
    const val MOUNTAIN_MAIN_PEAK_Y: Float = -0.5f
    const val MOUNTAIN_VALLEY_X: Float = 0.12f
    const val MOUNTAIN_VALLEY_Y: Float = -0.06f
    const val MOUNTAIN_SIDE_PEAK_X: Float = 0.32f
    const val MOUNTAIN_SIDE_PEAK_Y: Float = -0.26f
    /** The status badge on the head's upper right, sized and placed relative to the head radius. */
    const val BADGE_RADIUS: Float = 0.5f
    const val BADGE_OFFSET: Float = 0.72f
    const val BADGE_GLYPH_SIZE_DP: Float = 10f
    const val PIN_GLYPH_AVAILABLE = "\u2713"
    const val PIN_GLYPH_FILLING_FAST = "!"
    const val PIN_GLYPH_FULL = "\u00D7"
    const val PIN_GLYPH_CLOSED = "\u2212"
    /** A lot nobody has reported on shows the bare mountain mark, no badge. */
    const val PIN_GLYPH_UNKNOWN = ""
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
    val HERO_BANNER_HEIGHT_DP: Dp = 210.dp

    // Attribution styling
    val MAP_ATTRIBUTION_PADDING_START: Dp = 12.dp
    /** Narrow enough to wrap beside the lot-count chip at the right of the map. */
    val MAP_ATTRIBUTION_MAX_WIDTH = 200.dp
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
    const val COLOR_PIN_AVAILABLE: Int = MapColors.PIN_AVAILABLE
    // AvailabilityStatus.FILLING_FAST -> Amber/Orange
    const val COLOR_PIN_FILLING_FAST: Int = MapColors.PIN_FILLING_FAST
    // AvailabilityStatus.FULL -> Red
    const val COLOR_PIN_FULL: Int = MapColors.PIN_FULL
    // AvailabilityStatus.CLOSED -> Dark Slate
    const val COLOR_PIN_CLOSED: Int = MapColors.PIN_CLOSED
    // AvailabilityStatus.UNKNOWN -> Neutral Grey
    const val COLOR_PIN_UNKNOWN: Int = MapColors.PIN_UNKNOWN

    // Marker styling accents
    const val COLOR_PIN_BORDER: Int = MapColors.PIN_BORDER
    const val COLOR_USER_LOCATION_PIN: Int = MapColors.USER_LOCATION // Royal Blue

    // Map initial camera viewport centering across South Coast & Sea-to-Sky BC Parks
    const val INITIAL_CAMERA_LATITUDE: Double = 49.65
    const val INITIAL_CAMERA_LONGITUDE: Double = -122.95
    const val INITIAL_CAMERA_ZOOM: Double = 8.2

    // Offline / Fallback messaging
    const val MAP_UNAVAILABLE_OFFLINE_TITLE: String = "Map unavailable offline"
    const val MAP_UNAVAILABLE_OFFLINE_DESC: String =
        "Map tiles require an internet connection. Your trailheads list, filters, and offline guide are fully functional."

    // Attribution OpenFreeMap asks for, shown directly on the map surface
    const val OSM_ATTRIBUTION_TEXT: String = "OpenFreeMap © OpenMapTiles Data from OpenStreetMap"

    // OpenFreeMap vector styles: free, no API key and no usage limits (openfreemap.org)
    const val MAP_STYLE_URL_LIGHT: String = "https://tiles.openfreemap.org/styles/positron"
    const val MAP_STYLE_URL_DARK: String = "https://tiles.openfreemap.org/styles/dark"
    /** Style layers re-tinted to the app palette; ids from the OpenFreeMap styles, absent ones are skipped. */
    const val MAP_LAYER_LAND: String = "background"
    val MAP_LAYERS_PARK: List<String> = listOf("park", "landuse_park", "landcover_wood")
    const val MAP_LAYER_WATER: String = "water"
}

/**
 * Visual representation data class for a map pin.
 */
data class StatusPinVisual(
    val status: AvailabilityStatus,
    val colorArgb: Int,
    val hexColor: String,
    val isNeutralGrey: Boolean,
    val label: String,
    val glyph: String
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

    val glyph = when (status) {
        AvailabilityStatus.AVAILABLE -> ParkingMapConstants.PIN_GLYPH_AVAILABLE
        AvailabilityStatus.FILLING_FAST -> ParkingMapConstants.PIN_GLYPH_FILLING_FAST
        AvailabilityStatus.FULL -> ParkingMapConstants.PIN_GLYPH_FULL
        AvailabilityStatus.CLOSED -> ParkingMapConstants.PIN_GLYPH_CLOSED
        AvailabilityStatus.UNKNOWN -> ParkingMapConstants.PIN_GLYPH_UNKNOWN
    }

    val hex = String.format("#%06X", (0xFFFFFF and colorArgb))
    val isGrey = (status == AvailabilityStatus.UNKNOWN)

    return StatusPinVisual(
        status = status,
        colorArgb = colorArgb,
        hexColor = hex,
        isNeutralGrey = isGrey,
        label = status.label,
        glyph = glyph
    )
}
