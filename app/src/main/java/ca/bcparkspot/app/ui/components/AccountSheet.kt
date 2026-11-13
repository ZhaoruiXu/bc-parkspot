package ca.bcparkspot.app.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.PrivacyTip
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.window.DialogProperties
import ca.bcparkspot.app.R
import ca.bcparkspot.app.domain.ReportPolicy
import ca.bcparkspot.app.ui.account.AccountPrompt
import ca.bcparkspot.app.ui.account.AccountUiState
import ca.bcparkspot.app.ui.account.busyMessage
import ca.bcparkspot.app.ui.account.lastSyncText
import ca.bcparkspot.app.ui.text.formatWindow
import ca.bcparkspot.app.ui.theme.Sizes
import ca.bcparkspot.app.ui.theme.Spacing
import ca.bcparkspot.app.ui.theme.ThemePreference

/** The account sheet (3.3 to 3.6). It only ever opens from the account button. */
@Composable
fun AccountSheet(
    state: AccountUiState,
    message: String?,
    nowMs: Long,
    themePreference: ThemePreference,
    onThemePreferenceChange: (ThemePreference) -> Unit,
    onDismiss: () -> Unit,
    onSignIn: () -> Unit,
    onSignOut: () -> Unit,
    onDeleteConfirmed: () -> Unit,
    onRetryNotSynced: () -> Unit,
    onDiscardRejected: () -> Unit,
    onOpenPrivacyPolicy: () -> Unit
) {
    val res = LocalContext.current.resources
    var confirmDelete by remember { mutableStateOf(false) }
    val busy = state.busy

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text(stringResource(R.string.account_confirm_delete_title)) },
            text = { Text(stringResource(R.string.account_confirm_delete_body)) },
            confirmButton = {
                Button(
                    onClick = {
                        confirmDelete = false
                        onDeleteConfirmed()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    modifier = Modifier.testTag("confirm_delete_account_button")
                ) { Text(stringResource(R.string.account_confirm_delete)) }
            },
            dismissButton = {
                TextButton(onClick = { confirmDelete = false }) { Text(stringResource(R.string.account_cancel)) }
            }
        )
    }

    AlertDialog(
        onDismissRequest = { if (busy == null) onDismiss() },
        properties = DialogProperties(dismissOnBackPress = busy == null, dismissOnClickOutside = busy == null),
        modifier = Modifier.testTag("account_sheet"),
        icon = { Icon(Icons.Default.AccountCircle, contentDescription = null) },
        title = { Text(stringResource(R.string.account_title)) },
        text = {
            if (busy != null) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Spacing.md),
                    modifier = Modifier.fillMaxWidth().testTag("account_busy")
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(Sizes.progressIndicator), strokeWidth = Sizes.progressStroke)
                    Text(busyMessage(res, busy))
                }
                return@AlertDialog
            }
            Column(
                modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(Spacing.md)
            ) {
                Text(
                    text = if (state.isGuest) stringResource(R.string.account_guest) else state.email.orEmpty(),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.testTag("account_identity")
                )
                message?.let {
                    Surface(
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        shape = RoundedCornerShape(Sizes.messageCorner),
                        modifier = Modifier.fillMaxWidth().testTag("account_message")
                    ) {
                        Text(
                            text = it,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.padding(Spacing.md)
                        )
                    }
                }
                if (state.isGuest) {
                    Text(
                        text = stringResource(R.string.account_guest_detail),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                SyncSummary(state, nowMs, onRetryNotSynced, onDiscardRejected)

                if (state.deletionPending) {
                    ActionButton(
                        label = stringResource(R.string.account_retry_delete),
                        offlineLabel = stringResource(R.string.account_offline_delete),
                        online = state.online,
                        destructive = true,
                        tag = "account_retry_delete_button",
                        onClick = onDeleteConfirmed
                    )
                } else {
                    if (state.isGuest) {
                        ActionButton(
                            label = stringResource(R.string.account_sign_in_cta),
                            offlineLabel = stringResource(R.string.account_offline_sign_in),
                            online = state.online,
                            primary = true,
                            tag = "dialog_google_sign_in_button",
                            onClick = onSignIn
                        )
                    } else {
                        ActionButton(
                            label = stringResource(R.string.account_sign_out),
                            offlineLabel = stringResource(R.string.account_offline_sign_out),
                            online = state.online,
                            tag = "auth_sign_out_button",
                            onClick = onSignOut
                        )
                    }
                }

                HorizontalDivider()
                ThemeChoiceRow(selected = themePreference, onSelect = onThemePreferenceChange)

                TextButton(
                    onClick = onOpenPrivacyPolicy,
                    modifier = Modifier.fillMaxWidth().heightIn(min = Sizes.minTouchTarget).testTag("auth_privacy_policy_button")
                ) {
                    Icon(Icons.Default.PrivacyTip, contentDescription = null, modifier = Modifier.size(Sizes.inlineIcon))
                    Spacer(modifier = Modifier.width(Spacing.sm))
                    Text(stringResource(R.string.account_privacy))
                }

                if (!state.deletionPending) {
                    ActionButton(
                        label = stringResource(R.string.account_delete),
                        offlineLabel = stringResource(R.string.account_offline_delete),
                        online = state.online,
                        destructive = true,
                        tag = "auth_delete_account_button",
                        onClick = { confirmDelete = true }
                    )
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            if (busy == null) {
                TextButton(onClick = onDismiss, modifier = Modifier.testTag("account_close_button")) {
                    Text(stringResource(R.string.account_close))
                }
            }
        },
        shape = RoundedCornerShape(Sizes.sheetCorner)
    )
}

@Composable
private fun SyncSummary(state: AccountUiState, nowMs: Long, onRetry: () -> Unit, onDiscard: () -> Unit) {
    val res = LocalContext.current.resources
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs), modifier = Modifier.testTag("account_sync_summary")) {
        Text(
            text = if (state.pendingChanges == 0) stringResource(R.string.account_all_synced)
            else pluralStringResource(R.plurals.account_pending, state.pendingChanges, state.pendingChanges),
            style = MaterialTheme.typography.bodyMedium
        )
        Text(
            text = lastSyncText(res, state.lastSyncAt, nowMs),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        if (state.notSyncedItems > 0) {
            ProblemRow(
                text = pluralStringResource(R.plurals.account_not_synced, state.notSyncedItems, state.notSyncedItems),
                action = stringResource(R.string.account_retry),
                onAction = onRetry
            )
        }
        if (state.rejectedReports > 0) {
            ProblemRow(
                text = pluralStringResource(R.plurals.account_rejected_reports, state.rejectedReports, state.rejectedReports),
                action = stringResource(R.string.account_discard),
                onAction = onDiscard
            )
        }
    }
}

@Composable
private fun ProblemRow(text: String, action: String, onAction: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        Text(text, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error, modifier = Modifier.weight(1f))
        TextButton(onClick = onAction, modifier = Modifier.heightIn(min = Sizes.minTouchTarget)) { Text(action) }
    }
}

@Composable
private fun ActionButton(
    label: String,
    offlineLabel: String,
    online: Boolean,
    tag: String,
    primary: Boolean = false,
    destructive: Boolean = false,
    onClick: () -> Unit
) {
    val text = if (online) label else offlineLabel
    val modifier = Modifier.fillMaxWidth().heightIn(min = Sizes.minTouchTarget).testTag(tag)
    if (primary) {
        Button(onClick = onClick, enabled = online, modifier = modifier) { Text(text) }
    } else {
        OutlinedButton(
            onClick = onClick,
            enabled = online,
            modifier = modifier,
            colors = if (destructive) ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
            else ButtonDefaults.outlinedButtonColors()
        ) { Text(text) }
    }
}

/** Questions an account flow is waiting on. Shown above everything, whether or not the sheet is open. */
@Composable
fun AccountPromptDialog(prompt: AccountPrompt, policy: ReportPolicy, onAnswer: (Boolean) -> Unit) {
    val res = LocalContext.current.resources
    val (body, confirm) = when (prompt) {
        is AccountPrompt.ConfirmMerge ->
            pluralStringResource(R.plurals.account_confirm_merge, prompt.items, prompt.items) to stringResource(R.string.account_confirm_merge_add)
        is AccountPrompt.ConfirmSignOut ->
            pluralStringResource(
                R.plurals.account_confirm_sign_out, prompt.pendingChanges, prompt.pendingChanges, formatWindow(res, policy.validityWindowMs)
            ) to stringResource(R.string.account_sign_out)
    }
    AlertDialog(
        onDismissRequest = { onAnswer(false) },
        modifier = Modifier.testTag("account_prompt"),
        text = { Text(body) },
        confirmButton = {
            Button(onClick = { onAnswer(true) }, modifier = Modifier.testTag("account_prompt_confirm")) { Text(confirm) }
        },
        dismissButton = {
            TextButton(onClick = { onAnswer(false) }, modifier = Modifier.testTag("account_prompt_cancel")) {
                Text(stringResource(R.string.account_cancel))
            }
        }
    )
}

private val THEME_CHOICES = listOf(
    ThemePreference.SYSTEM to R.string.theme_system,
    ThemePreference.LIGHT to R.string.theme_light,
    ThemePreference.DARK to R.string.theme_dark
)

@Composable
private fun ThemeChoiceRow(selected: ThemePreference, onSelect: (ThemePreference) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
        Text(
            text = stringResource(R.string.theme_title),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            THEME_CHOICES.forEach { (preference, label) ->
                FilterChip(
                    selected = preference == selected,
                    onClick = { onSelect(preference) },
                    label = { Text(stringResource(label)) },
                    modifier = Modifier.heightIn(min = Sizes.minTouchTarget).testTag("theme_${preference.name.lowercase()}")
                )
            }
        }
    }
}
