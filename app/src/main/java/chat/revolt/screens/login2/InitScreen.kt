package chat.revolt.screens.login2

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import chat.revolt.R
import chat.revolt.api.settings.LoadedSettings
import chat.revolt.ui.theme.Theme

@Composable
fun InitScreen(
    navController: NavController
) {
    Box {
        Image(
            painter = painterResource(R.drawable.login_bg),
            modifier = Modifier
                .scale(3f)
                .offset(y = 32.dp)
                .align(Alignment.TopCenter),
            colorFilter = if (LoadedSettings.theme == Theme.M3Dynamic) ColorFilter.tint(
                MaterialTheme.colorScheme.tertiaryContainer
            ) else null,
            contentDescription = null
        )
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceEvenly,
            modifier = Modifier.fillMaxSize()
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier
                    .weight(0.5f)
                    .padding(horizontal = 16.dp)
            ) {
                Spacer(modifier = Modifier.height(64.dp))
                Image(
                    painter = painterResource(R.drawable.revolt_logo_wide),
                    contentDescription = null,
                    colorFilter = ColorFilter.tint(
                        MaterialTheme.colorScheme.onBackground
                    ),
                    modifier = Modifier.fillMaxWidth(0.5f)
                )
                Spacer(modifier = Modifier.height(64.dp))
                Text(
                    "Find your community",
                    style = MaterialTheme.typography.headlineMedium,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    "Revolt is the chat app that’s truly built with you in mind.",
                    style = MaterialTheme.typography.bodyLarge,
                    fontSize = 18.sp,
                    textAlign = TextAlign.Center
                )
            }
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(
                    8.dp,
                    alignment = Alignment.Bottom
                ),
                modifier = Modifier
                    .weight(0.5f)
                    .padding(horizontal = 16.dp)
                    .widthIn(max = 150.dp)
            ) {
                Button(
                    onClick = { navController.navigate("login2/existing/details") },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Log In")
                }
                TextButton(
                    onClick = { navController.navigate("login2/new/details") },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Sign Up")
                }
                Spacer(modifier = Modifier.fillMaxHeight(0.25f))
            }
        }
    }
}