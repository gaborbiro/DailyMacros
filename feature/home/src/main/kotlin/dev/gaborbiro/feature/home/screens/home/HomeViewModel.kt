package dev.gaborbiro.feature.home.screens.home

import android.app.Application
import androidx.lifecycle.viewModelScope
import com.google.android.gms.auth.api.identity.AuthorizationRequest
import com.google.android.gms.auth.api.identity.AuthorizationResult
import com.google.android.gms.common.api.Scope
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport
import com.google.api.client.http.HttpRequestInitializer
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.keep.v1.Keep
import com.google.api.services.keep.v1.KeepScopes
import com.google.api.services.keep.v1.model.Note
import com.google.api.services.keep.v1.model.Section
import com.google.api.services.keep.v1.model.TextContent
import com.google.auth.http.HttpCredentialsAdapter
import com.google.auth.oauth2.AccessToken
import com.google.auth.oauth2.GoogleCredentials
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.gaborbiro.feature.home.screens.home.model.HomeUIUpdates
import dev.gaborbiro.feature.home.screens.home.model.HomeViewState
import dev.gaborbiro.nutrition.app_prefs.domain.AppPrefs
import dev.gaborbiro.nutrition.core.clause.asText
import dev.gaborbiro.nutrition.core.viewmodel.BaseViewModel
import dev.gaborbiro.nutrition.data.chatgpt.domain.ChatGPTRepository
import dev.gaborbiro.nutrition.data.chatgpt.domain.model.QueryRequest
import dev.gaborbiro.nutrition.data.common.model.DomainError
import dev.gaborbiro.nutrition.feature.common.errorMessage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject


@HiltViewModel
class HomeViewModel @Inject constructor(
    application: Application,
    private val appPrefs: AppPrefs,
    private val chatGptRepository: ChatGPTRepository,
) : BaseViewModel<HomeViewState, HomeUIUpdates>(application, HomeViewState()) {

    init {
        viewModelScope.launch {
            val lastQuery = appPrefs.lastQuery.get().first()
            state {
                copy(question = lastQuery ?: "")
            }
        }
    }

    fun onTextChanged(text: String) {
        viewModelScope.launch {
            appPrefs.lastQuery.set(text)
        }
    }

    fun onAskChatGPTButtonTapped() {
        viewModelScope.launch {
            state {
                copy(
                    showQueryProgress = true
                )
            }
            val query = appPrefs.lastQuery.get().first()
            query?.let {
                try {
                    val answer = chatGptRepository.query(QueryRequest(it))
                    state {
                        copy(answer = answer.response)
                    }
                } catch (e: DomainError) {
                    handleDomainError(e)
                }
            }
            state {
                copy(
                    showQueryProgress = false
                )
            }
        }
    }

    fun onFetchKeepNotesButtonTapped() {
        viewModelScope.launch {
            state { copy(showFetchKeepNotesProgress = true) }
            appPrefs.googleApiAccessToken.get().first()
                ?.let {
                    fetchData(it)
                }
                ?: run {
//                    val getGoogleIdOption: GetGoogleIdOption = GetGoogleIdOption.Builder()
//                        .setServerClientId(BuildConfig.GOOGLE_CLIENT_ID)
//                        .setAutoSelectEnabled(true)
//                        .setFilterByAuthorizedAccounts(true)
//                        .build()
//                    val request: GetCredentialRequest = GetCredentialRequest.Builder()
//                        .addCredentialOption(getGoogleIdOption)
//                        .build()
                    val authorizationRequest: AuthorizationRequest = AuthorizationRequest.builder()
                        .setRequestedScopes(KeepScopes.all().map { Scope(it) })
                        .build()
                    state { copy(authorizationRequest = authorizationRequest) }
                }
        }
    }

//    suspend fun handleSignIn(result: GetCredentialResponse) {
//        val credential = result.credential
//        when (credential) {
//            is CustomCredential -> {
//                if (credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
//                    try {
//                        // Use googleIdTokenCredential and extract id to validate and
//                        // authenticate on your server.
//                        val googleIdTokenCredential =
//                            GoogleIdTokenCredential.createFrom(credential.data)
//                        trigger(HomeUIUpdates.Toast("Logged in (${googleIdTokenCredential.displayName})".asText()))
//                        appPrefs.googleApiAccessToken.set(googleIdTokenCredential.idToken)
//                        fetchData(googleIdTokenCredential.idToken)
//                    } catch (e: GoogleIdTokenParsingException) {
//                        trigger(HomeUIUpdates.Toast("Received an invalid google id token response".asText()))
//                        Log.e("HomeScreen", "Received an invalid google id token response", e)
//                    }
//                } else {
//                    trigger(HomeUIUpdates.Toast("Unexpected type of credential: ${credential.type}".asText()))
//                    Log.e("HomeScreen", "Unexpected type of credential: ${credential.type}")
//                }
//            }
//
//            else -> {
//                trigger(HomeUIUpdates.Toast("Unexpected type of credential: ${credential.type}".asText()))
//                Log.e("HomeScreen", "Unexpected type of credential: ${credential.type}")
//            }
//        }
//        state {
//            copy(
//                signInRequest = null,
//                showFetchKeepNotesProgress = false,
//            )
//        }
//    }

    fun onAuthorizationResult(authorizationResult: AuthorizationResult) {
        trigger(HomeUIUpdates.Toast(authorizationResult.accessToken!!.asText()))
        fetchData(authorizationResult.accessToken!!)
    }

    private fun fetchData(accessToken: String) {
        val credentials: GoogleCredentials =
            GoogleCredentials.create(AccessToken(accessToken, null))
        val requestInitializer: HttpRequestInitializer = HttpCredentialsAdapter(credentials)
        val keepService = Keep.Builder(
            GoogleNetHttpTransport.newTrustedTransport(),
            GsonFactory.getDefaultInstance(),
            requestInitializer,
        ).build()
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val result = keepService.notes().create(
                    Note().setTitle("Test title")
                        .setBody(Section().setText(TextContent().setText("Test body")))
                        .setName("Test name")
                ).execute()
                println(result)
            } catch (t: Throwable) {
                t.printStackTrace()
                trigger(HomeUIUpdates.Toast("Error fetching Keep notes (${t.message})".asText()))
            }
            state {
                copy(showFetchKeepNotesProgress = false)
            }
        }
    }

    fun handleSignInFailure(error: Throwable?) {
        error?.printStackTrace()
        val domainError = DomainError.DisplayMessageToUser.Message(
            error?.message ?: "Oops! Something went wrong."
        )
        state {
            copy(
                authorizationRequest = null,
                showFetchKeepNotesProgress = false,
            )
        }
        handleDomainError(domainError)
    }

    private fun handleDomainError(error: DomainError) {
        when (error) {
            is DomainError.DisplayMessageToUser -> {
                val message = error.errorMessage()
                trigger(HomeUIUpdates.Toast(message))
            }

            is DomainError.GoToSignInScreen -> {
                trigger(HomeUIUpdates.Toast("Auth error".asText()))
            }
        }
    }
}