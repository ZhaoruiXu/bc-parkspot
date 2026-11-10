package ca.bcparkspot.app.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ca.bcparkspot.app.model.AvailabilityStatus
import ca.bcparkspot.app.model.ParkingLot

@Composable
fun CommunityReportDialog(
    lot: ParkingLot,
    onDismiss: () -> Unit,
    onSubmit: (status: AvailabilityStatus, comment: String, estimatedSpots: Int?) -> Unit
) {
    var selectedStatus by remember { mutableStateOf<AvailabilityStatus>(lot.effectiveStatus) }
    var reporterComment by remember { mutableStateOf("") }
    var spotsText by remember { mutableStateOf("") }
    var commentError by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text(
                    text = "Report Parking Status",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = lot.lotName,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "What is the current parking status?",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold
                )

                // Status Radios / Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf(
                        AvailabilityStatus.AVAILABLE,
                        AvailabilityStatus.FILLING_FAST,
                        AvailabilityStatus.FULL
                    ).forEach { status ->
                        FilterChip(
                            selected = selectedStatus == status,
                            onClick = { selectedStatus = status },
                            label = { Text(status.label, fontSize = 11.sp) },
                            modifier = Modifier.weight(1f).testTag("report_status_${status.name}")
                        )
                    }
                }

                // Estimated Spots
                OutlinedTextField(
                    value = spotsText,
                    onValueChange = { spotsText = it.filter { char -> char.isDigit() } },
                    label = { Text("Estimated Empty Spots") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
                    modifier = Modifier.fillMaxWidth().testTag("report_spots_input"),
                    singleLine = true
                )

                // Comment (Enforced 3 to 500 characters after trim with counter)
                OutlinedTextField(
                    value = reporterComment,
                    onValueChange = {
                        if (it.length <= 500) {
                            reporterComment = it
                            if (it.trim().length >= 3) commentError = false
                        }
                    },
                    label = { Text("Notes for other hikers *") },
                    placeholder = { Text("e.g. Park staff directing to overflow lot, passes checked") },
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    modifier = Modifier.fillMaxWidth().testTag("report_comment_input"),
                    maxLines = 3,
                    isError = commentError,
                    supportingText = {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            if (commentError) {
                                Text("A note of at least 3 characters is required.", color = MaterialTheme.colorScheme.error, fontSize = 11.sp)
                            } else {
                                Text("Min 3 chars (notes for fellow hikers).", fontSize = 11.sp)
                            }
                            Text("${reporterComment.length}/500", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val trimmed = reporterComment.trim()
                    if (trimmed.length < 3 || trimmed.length > 500) {
                        commentError = true
                        return@Button
                    }
                    val spots = spotsText.toIntOrNull()
                    onSubmit(selectedStatus, trimmed, spots)
                },
                enabled = reporterComment.trim().length >= 3 && reporterComment.length <= 500,
                modifier = Modifier.testTag("report_submit_button")
            ) {
                Text("Submit Report")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("report_cancel_button")
            ) {
                Text("Cancel")
            }
        },
        shape = RoundedCornerShape(16.dp)
    )
}
