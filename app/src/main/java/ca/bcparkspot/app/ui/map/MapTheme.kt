package ca.bcparkspot.app.ui.map

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import ca.bcparkspot.app.ui.theme.MapColors
import org.maplibre.android.maps.Style
import org.maplibre.android.style.layers.BackgroundLayer
import org.maplibre.android.style.layers.FillLayer
import org.maplibre.android.style.layers.PropertyFactory

object MapTheme {
    /** Below this background luminance the app is in a dark theme, so the map goes dark too. */
    const val DARK_LUMINANCE_THRESHOLD = 0.5f
}

fun mapStyleUrl(dark: Boolean): String =
    if (dark) ParkingMapConstants.MAP_STYLE_URL_DARK else ParkingMapConstants.MAP_STYLE_URL_LIGHT

/** Re-tints the base map's land, parks and water to the app palette; layers a style lacks are skipped. */
fun tintMapStyle(style: Style, dark: Boolean) {
    val land = if (dark) MapColors.darkLand else MapColors.lightLand
    val park = if (dark) MapColors.darkPark else MapColors.lightPark
    val water = if (dark) MapColors.darkWater else MapColors.lightWater
    (style.getLayer(ParkingMapConstants.MAP_LAYER_LAND) as? BackgroundLayer)
        ?.setProperties(PropertyFactory.backgroundColor(land.toArgb()))
    ParkingMapConstants.MAP_LAYERS_PARK.forEach { id -> style.fill(id, park) }
    style.fill(ParkingMapConstants.MAP_LAYER_WATER, water)
}

private fun Style.fill(layerId: String, color: Color) {
    (getLayer(layerId) as? FillLayer)?.setProperties(PropertyFactory.fillColor(color.toArgb()))
}
