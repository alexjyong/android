package chat.revolt.sheets

import android.widget.Toast
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import chat.revolt.R
import chat.revolt.components.generic.NonIdealState

@Composable
fun WebHookUserSheet(modifier: Modifier = Modifier) {
    val context = LocalContext.current

    NonIdealState(
        icon = {
            Icon(
                painter = painterResource(R.drawable.ic_hook_24dp),
                contentDescription = null,
                modifier = Modifier.size(24.dp)
            )
        },
        title = {
            Text(
                text = stringResource(R.string.user_info_sheet_webhook)
            )
        },
        description = {
            Text(
                text = stringResource(R.string.user_info_sheet_webhook_description),
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }
    )
    Column(Modifier.padding(horizontal = 16.dp)) {
        Text(
            text = stringResource(R.string.user_info_sheet_webhook_description_2),
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(32.dp))
        Button(
            onClick = {
                Toast(context).apply {
                    setText(context.getString(R.string.comingsoon_toast))
                    duration = Toast.LENGTH_SHORT
                    show()
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(text = stringResource(R.string.user_info_sheet_webhook_learn_more))
        }
    }
    Spacer(Modifier.height(48.dp))
}