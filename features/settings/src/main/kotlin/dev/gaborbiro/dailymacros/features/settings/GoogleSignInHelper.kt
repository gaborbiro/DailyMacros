package dev.gaborbiro.dailymacros.features.settings

import android.content.Context
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.Scope

const val DRIVE_SCOPE_TOKEN = "oauth2:https://www.googleapis.com/auth/drive.appdata"
private const val DRIVE_SCOPE = "https://www.googleapis.com/auth/drive.appdata"

fun buildGoogleSignInOptions(): GoogleSignInOptions =
    GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
        .requestEmail()
        .requestScopes(Scope(DRIVE_SCOPE))
        .build()

fun launchGoogleSignIn(context: Context, launcher: ActivityResultLauncher<android.content.Intent>) {
    launcher.launch(GoogleSignIn.getClient(context, buildGoogleSignInOptions()).signInIntent)
}

@Composable
fun rememberGoogleSignInLauncher(
    onSuccess: (email: String) -> Unit,
    onFailure: (message: String) -> Unit,
): ActivityResultLauncher<android.content.Intent> =
    rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val data = result.data ?: return@rememberLauncherForActivityResult
        val task = GoogleSignIn.getSignedInAccountFromIntent(data)
        try {
            val email = task.getResult(ApiException::class.java)?.email
            if (email != null) onSuccess(email) else onFailure("No email returned")
        } catch (e: ApiException) {
            onFailure(e.message ?: e.statusCode.toString())
        }
    }
