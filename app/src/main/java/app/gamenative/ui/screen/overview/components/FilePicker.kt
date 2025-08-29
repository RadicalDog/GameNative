package app.gamenative.ui.screen.overview.components

import android.content.res.Configuration
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Source
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import app.gamenative.PrefManager
import app.gamenative.ui.theme.PluviaTheme
import timber.log.Timber

@Composable
fun FilePickerButton(
    modifier: Modifier = Modifier,
    buttonText: String = "Select File",
    allowedMimeTypes: Array<String> = arrayOf("*/*"),
    onFileSelected: (String?) -> Unit
) {
    val context = LocalContext.current
    var selectedFilePath by remember { mutableStateOf<String?>(null) }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let { selectedUri ->
            val filePath = getFilePathFromUri(selectedUri)
            selectedFilePath = filePath
            onFileSelected(filePath)
        } ?: run {
            selectedFilePath = null
            onFileSelected(null)
        }
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {

        // Display selected file path
        OutlinedTextField(
            value = selectedFilePath ?: "No file selected",
            onValueChange = { },
            readOnly = true,
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Selected File") },
            shape = RoundedCornerShape(16.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = MaterialTheme.colorScheme.outline,
                focusedBorderColor = MaterialTheme.colorScheme.outline,
                focusedLabelColor = MaterialTheme.colorScheme.outline,
            ),
            textStyle = MaterialTheme.typography.bodyMedium
        )

        OutlinedButton(
            modifier = modifier.fillMaxWidth(),
            onClick = {
                launcher.launch(allowedMimeTypes)
            },
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(2.dp, MaterialTheme.colorScheme.primary),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.primary),
            contentPadding = PaddingValues(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Source,
                    contentDescription = "Select file",
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = buttonText,
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold)
                )
            }
        }
    }
}

// Helper function
private fun getFilePathFromUri(uri: Uri): String {
    var uriFix = uri.lastPathSegment?: ""
    if (uriFix.contains("primary:")) {
        Timber.i("Internal path originally $uriFix")
        uriFix = uriFix.replace("primary:", "/storage/emulated/0/")
    } else {
        Timber.i("External path originally $uriFix")
        uriFix = "/storage/"+uriFix.replace(':', '/')
    }

    return uriFix
}


// Example usage
@Composable
fun FilePickerExamples() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // General file picker
        FilePickerButton(
            buttonText = "Select Any File",
            onFileSelected = { filePath ->
                // Handle selected file path
                Timber.tag("FilePicker").d("Selected file: $filePath")
            }
        )

        // Image file picker
        FilePickerButton(
            buttonText = "Select Image",
            allowedMimeTypes = arrayOf("image/*"),
            onFileSelected = { filePath ->
                // Handle selected image path
                Timber.tag("FilePicker").d("Selected image: $filePath")
            }
        )
    }
}

@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES or Configuration.UI_MODE_TYPE_NORMAL)
@Preview(
    device = "spec:width=1920px,height=1080px,dpi=440",
    uiMode = Configuration.UI_MODE_NIGHT_YES or Configuration.UI_MODE_TYPE_NORMAL,
) // Odin2 Mini
@Composable
private fun Preview_DownloadsScreenContent() {
    val context = LocalContext.current
    PrefManager.init(context)
    PluviaTheme {
        Surface {
            FilePickerExamples()
        }
    }
}
