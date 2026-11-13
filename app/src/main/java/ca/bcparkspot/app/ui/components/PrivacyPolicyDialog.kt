package ca.bcparkspot.app.ui.components

import ca.bcparkspot.app.domain.ReportPolicies
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PrivacyTip
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun PrivacyPolicyDialog(
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.PrivacyTip,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Privacy Policy & Data Usage",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "BC ParkSpot Privacy & Data Commitment",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.primary
                )

                Text(
                    text = "1. Location Data (GPS)\n" +
                            "• Purpose: Device GPS coordinates are used exclusively to calculate trailhead distances and to bookmark where you parked your vehicle.\n" +
                            "• Offline Isolation: Pinned vehicle coordinates are stored on-device in local private Room SQLite storage (unencrypted). Cloud backup is disabled (android:allowBackup=\"false\") to prevent extraction without consent. If signed in, vehicle location is synced exclusively to your private Firestore subcollection.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Text(
                    text = "2. Google Authentication & Account Info\n" +
                            "• Information Used: When you sign in via Google Sign-In (Credential Manager), we store your Firebase User ID (UID), display name, and email address.\n" +
                            "• Zero Tracking: We do not track cross-site activity, sell your information, or inject advertising identifiers.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Text(
                    text = "3. Community Crowdsourced Reports & Retention\n" +
                            "• Public Data: When you submit a parking status report (e.g. 'Lot Full'), your status, stall estimate, note, timestamp, and display name are shared with other hikers.\n" +
                            "• Confirmation and expiry: a lot is shown as confirmed only when at least ${ReportPolicies.MIN_QUORUM_USERS} different hikers report it within ${ReportPolicies.VALIDITY_WINDOW_HOURS} hours. Older reports stop counting.\n" +
                            "• Right to Erasure: Reports are linked to your authenticated UID to prevent spam ballot-stuffing. You retain full right to erasure: deleting your account permanently deletes all reports you have authored.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Text(
                    text = "4. Account Deletion & Right to Erasure\n" +
                            "• You can permanently delete your account and all associated cloud data at any time from the Account dialog.\n" +
                            "• Erasure Scope: Deletion permanently wipes your saved lots, pinned vehicle location, and all community reports authored by your account from Firestore, wipes the local device Room database, and deletes your Google Auth user profile from Firebase.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                modifier = Modifier.testTag("privacy_dialog_close_button")
            ) {
                Text("Understood")
            }
        },
        shape = RoundedCornerShape(16.dp)
    )
}
