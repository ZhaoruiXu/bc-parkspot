package ca.bcparkspot.app.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import ca.bcparkspot.app.R
import ca.bcparkspot.app.ui.theme.Sizes
import ca.bcparkspot.app.ui.theme.Spacing

/** The app icon and name, used wherever the app introduces itself. */
@Composable
fun BrandMark(textColor: Color, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.testTag("brand_mark"),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
    ) {
        Image(
            painter = painterResource(R.drawable.ic_bc_parking_icon),
            contentDescription = null,
            modifier = Modifier.size(Sizes.brandMark).clip(RoundedCornerShape(Sizes.brandMarkCorner))
        )
        Text(
            text = stringResource(R.string.app_name),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.ExtraBold,
            color = textColor
        )
    }
}
