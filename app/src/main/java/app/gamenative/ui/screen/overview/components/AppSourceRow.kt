package app.gamenative.ui.screen.overview.components

import android.content.res.Configuration
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedIconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import app.gamenative.PrefManager
import app.gamenative.enums.Source
import app.gamenative.service.AppSourceService
import app.gamenative.service.appsource.AppSourceInterface
import app.gamenative.ui.screen.PluviaScreen
import app.gamenative.ui.theme.PluviaTheme
import app.gamenative.ui.util.ListItemImage

@Composable
internal fun AppSourceRow(
    modifier: Modifier = Modifier,
    appSource: AppSourceInterface,
    onNavigateRoute: (String) -> Unit,
) {

    val username = appSource.getUsername()

    // Modern card-style item
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        // Consider adding onClick popup menu in future
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        ),
        border = androidx.compose.foundation.BorderStroke(
            width = 1.dp,
            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Store icon
            Box(
                modifier = Modifier
                    .size(60.dp)
                    .clip(RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                ListItemImage(
                    modifier = Modifier.size(56.dp),
                    imageModifier = Modifier.clip(RoundedCornerShape(10.dp)),
                    image = { appSource.iconUrl }
                )
            }

            // Details
            Column(
                modifier = Modifier.weight(1f)
            ) {
                var topline = appSource.sourceName
                if (username.isNotEmpty()) {
                    topline = username
                }
                Text(
                    text = topline,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.SemiBold
                    ),
                    color = MaterialTheme.colorScheme.onSurface
                )

                // Status indicator: Connected / Disconnected
                var statusColor = MaterialTheme.colorScheme.onSurfaceVariant
                if (appSource.isReadyToSync()) {
                    statusColor = MaterialTheme.colorScheme.tertiary
                }

                // Status text
                var connectedText by appSource.connectedText
                Text(
                    text = connectedText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = statusColor
                )


                // Last sync
                // Ideally, should update when clicked & finished...
                var syncText by appSource.lastSyncTimeHumanReadable
                if (syncText.isNotEmpty()) {
                    Text(
                        text = "Last sync: $syncText",
                        style = MaterialTheme.typography.bodyMedium.copy(fontStyle = FontStyle.Italic),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                var statusText by appSource.sourceMostRecentStatusText
                Text(
                    text = statusText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            if (appSource.isReadyToSync()) {
                // Sync button
                OutlinedIconButton(
                    onClick = { appSource.syncSource() },
                    colors = IconButtonDefaults.iconButtonColors(
                        contentColor = MaterialTheme.colorScheme.primary,
                        containerColor = MaterialTheme.colorScheme.background
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.height(40.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary)
                ) {
                    Icon(imageVector = Icons.Default.Sync, contentDescription = null)
                }
            }
            if (appSource.source == Source.STEAM && !appSource.isReadyToSync()) {
                // Login button
                OutlinedButton(
                    onClick = { onNavigateRoute(PluviaScreen.LoginUser.route) },
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.primary,
                        containerColor = MaterialTheme.colorScheme.background
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.height(40.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary)
                ) {
                    Text(
                        text = "Login",
                    )
                }
            }
        }
    }
}

/***********
 * PREVIEW *
 ***********/

@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES or Configuration.UI_MODE_TYPE_NORMAL)
@Composable
private fun Preview_AppItem() {
    val context = LocalContext.current
    PrefManager.init(context)
    PluviaTheme {
        Surface {
            LazyColumn(
                modifier = Modifier.padding(16.dp)
            ) {
                items(AppSourceService.getAppSources().toList()) { source ->
                    AppSourceRow(appSource = source.second, onNavigateRoute = {})
                }
            }
        }
    }
}
