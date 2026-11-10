package ca.bcparkspot.app.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun BCPassInfoScreen(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("bc_pass_info_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Column {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Text(
                        text = "BC PARKS OFFICIAL GUIDE",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "BC Day-Use Pass System",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "Free passes required at select high-traffic provincial parks to protect alpine trails and manage trailhead parking.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Official Booking Portal Link Card
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Reserve on bcparks.ca",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Passes become available at 7:00 AM two days before your planned visit. Vehicle passes cover all passengers in one car.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.9f)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://bcparks.ca/reservations/day-use-passes/"))
                            context.startActivity(intent)
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.surface,
                            contentColor = MaterialTheme.colorScheme.primary
                        ),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Open BC Parks Pass Portal", fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(imageVector = Icons.Default.OpenInNew, contentDescription = null, modifier = Modifier.size(16.dp))
                    }
                }
            }
        }

        // Key Rules
        item {
            Text(
                text = "Participating Parks & Trailheads",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }

        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                ParkPassRow(
                    parkName = "Joffre Lakes Provincial Park",
                    region = "Sea-to-Sky / Pemberton",
                    rule = "Individual trail pass required for all visitors (both summer & fall season). Strictly no road shoulder parking."
                )
                ParkPassRow(
                    parkName = "Garibaldi Provincial Park",
                    region = "Squamish / Whistler",
                    rule = "Free vehicle pass required for Rubble Creek (Garibaldi Lake/Black Tusk) and Cheakamus Lake trailheads."
                )
                ParkPassRow(
                    parkName = "Golden Ears Provincial Park",
                    region = "Maple Ridge",
                    rule = "Vehicle pass required for South Beach (Alouette Lake), Gold Creek, and West Canyon day-use lots."
                )
                ParkPassRow(
                    parkName = "Buntzen Lake Recreation Area",
                    region = "Anmore / Port Moody",
                    rule = "BC Hydro free parking reservation pass required from mid-May through Labor Day."
                )
                ParkPassRow(
                    parkName = "Mount Seymour Provincial Park",
                    region = "North Vancouver",
                    rule = "Winter vehicle pass required on designated winter dates (Winter 2026-27 pass dates TBA - check bcparks.ca). Free access with no pass during summer."
                )
            }
        }

        // Backcountry Advice
        item {
            Text(
                text = "Crucial Tips Before Leaving Home",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }

        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                TipCard(
                    title = "Screenshot Your Pass Barcode",
                    desc = "Trailheads like Joffre Lakes and Garibaldi Rubble Creek have ZERO cell phone coverage. Download or screenshot your QR code before leaving Squamish or Pemberton."
                )
                TipCard(
                    title = "Strict Towing on Highways",
                    desc = "RCMP and BC Ministry of Transportation strictly tow vehicles parked along Highway 99 (Duffey Lake Rd) and secondary access roads."
                )
                TipCard(
                    title = "Arrive 15 Minutes Early",
                    desc = "Parking gates open as early as 7:00 AM. For parks without passes (e.g. Stawamus Chief, Alice Lake, Deep Cove), lots often fill completely by 8:30 AM on sunny weekends."
                )
            }
        }
    }
}

@Composable
private fun ParkPassRow(parkName: String, region: String, rule: String) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(text = parkName, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Text(text = region, fontSize = 11.sp, color = MaterialTheme.colorScheme.secondary)
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = rule, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun TipCard(title: String, desc: String) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.Top
        ) {
            Icon(
                imageVector = Icons.Default.CheckCircleOutline,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
            Column {
                Text(text = title, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                Spacer(modifier = Modifier.height(2.dp))
                Text(text = desc, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
