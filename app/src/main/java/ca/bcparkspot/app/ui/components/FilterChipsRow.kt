package ca.bcparkspot.app.ui.components

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ca.bcparkspot.app.model.ParkRegion
import ca.bcparkspot.app.model.RecreationType

@Composable
fun FilterChipsRow(
    selectedRegion: ParkRegion?,
    onSelectRegion: (ParkRegion?) -> Unit,
    selectedType: RecreationType?,
    onSelectType: (RecreationType?) -> Unit,
    filterAvailableOnly: Boolean,
    onToggleAvailableOnly: () -> Unit,
    filterNoPassNeeded: Boolean,
    onToggleNoPassNeeded: () -> Unit,
    filterFreeParking: Boolean,
    onToggleFreeParking: () -> Unit,
    filterEvCharging: Boolean,
    onToggleEvCharging: () -> Unit,
    filterDogFriendly: Boolean,
    onToggleDogFriendly: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        // Regions row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = selectedRegion == null,
                onClick = { onSelectRegion(null) },
                label = { Text("All BC Regions", fontSize = 12.sp) },
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.testTag("filter_region_all")
            )

            ParkRegion.values().forEach { region ->
                FilterChip(
                    selected = selectedRegion == region,
                    onClick = { onSelectRegion(if (selectedRegion == region) null else region) },
                    label = { Text(region.displayName.split("(")[0].trim(), fontSize = 12.sp) },
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier.testTag("filter_region_${region.name}")
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Attribute Toggles Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ToggleFilterChip(
                label = "Available Only",
                isSelected = filterAvailableOnly,
                icon = Icons.Default.CheckCircle,
                onClick = onToggleAvailableOnly,
                testTag = "filter_available"
            )

            ToggleFilterChip(
                label = "No Pass Needed",
                isSelected = filterNoPassNeeded,
                icon = Icons.Default.AssignmentLate,
                onClick = onToggleNoPassNeeded,
                testTag = "filter_no_pass"
            )

            ToggleFilterChip(
                label = "Free Parking",
                isSelected = filterFreeParking,
                icon = Icons.Default.MoneyOff,
                onClick = onToggleFreeParking,
                testTag = "filter_free"
            )

            ToggleFilterChip(
                label = "EV Charging",
                isSelected = filterEvCharging,
                icon = Icons.Default.ElectricCar,
                onClick = onToggleEvCharging,
                testTag = "filter_ev"
            )

            ToggleFilterChip(
                label = "Dog Friendly",
                isSelected = filterDogFriendly,
                icon = Icons.Default.Pets,
                onClick = onToggleDogFriendly,
                testTag = "filter_dog"
            )
        }
    }
}

@Composable
private fun ToggleFilterChip(
    label: String,
    isSelected: Boolean,
    icon: ImageVector,
    onClick: () -> Unit,
    testTag: String
) {
    FilterChip(
        selected = isSelected,
        onClick = onClick,
        leadingIcon = {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(14.dp)
            )
        },
        label = { Text(label, fontSize = 12.sp) },
        shape = RoundedCornerShape(20.dp),
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
            selectedLeadingIconColor = MaterialTheme.colorScheme.onPrimaryContainer
        ),
        modifier = Modifier.testTag(testTag)
    )
}
