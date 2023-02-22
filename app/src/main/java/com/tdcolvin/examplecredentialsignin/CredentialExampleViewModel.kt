package com.tdcolvin.examplecredentialsignin

import android.app.Activity
import android.app.Application
import android.util.Log
import androidx.credentials.*
import androidx.credentials.exceptions.*
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

class CredentialExampleViewModel(application: Application) : AndroidViewModel(application) {
    private val credentialManager by lazy {
        CredentialManager.create(application)
    }

    val signedInPasswordCredential = MutableStateFlow<PasswordCredential?>(null)

    fun signInOrSignUpWithEnteredCredential(activity: Activity, username: String, password: String) {
        viewModelScope.launch {
            val signInSuccess = true
            //do some sign in or sign up logic here
            // signInSuccess = doSomeSignInOrSignUpWork(username, password)

            //then if successful...
            if (signInSuccess) {
                //Set signedInPasswordCredential - this is a flag to indicate to the UI that we're now
                //signed in.
                signedInPasswordCredential.value = PasswordCredential(username, password)

                //...And offer to the user to save the credential to the store.
                saveCredential(activity, username, password)
            }
        }
    }

    fun signInWithSavedCredential(activity: Activity) {
        viewModelScope.launch {
            try {
                val passwordCredential = getCredential(activity) ?: return@launch

                val signInSuccess = true
                //Run your app's sign in logic using the returned password credential
                // signInSuccess = doSomeSignInWork(username, password)

                //then if successful...
                if (signInSuccess) {
                    //Indicate to the UI that we're now signed in.
                    signedInPasswordCredential.value = passwordCredential
                }
            }
            catch (e: Exception) {
                Log.e("CredentialTest", "Error getting credential", e)
            }
        }
    }

    private suspend fun getCredential(activity: Activity): PasswordCredential? {
        try {
            //Tell the credential library that we're only interested in password credentials
            val getCredRequest = GetCredentialRequest(
                listOf(GetPasswordOption())
            )

            //Show the user a dialog allowing them to pick a saved credential
            val credentialResponse = credentialManager.getCredential(
                request = getCredRequest,
                activity = activity,
            )

            //Return the selected credential (as long as it's a username/password)
            return credentialResponse.credential as? PasswordCredential
        }
        catch (e: GetCredentialCancellationException) {
            //User cancelled the request. Return nothing
            return null
        }
        catch (e: NoCredentialException) {
            //We don't have a matching credential
            return null
        }
        catch (e: GetCredentialException) {
            Log.e("CredentialTest", "Error getting credential", e)
            throw e
        }
    }

    //Typically you would run this function only after a successful sign-in. No point in saving
    //credentials that aren't correct.
    private suspend fun saveCredential(activity: Activity, username: String, password: String) {
        try {
            //Ask the user for permission to add the credentials to their store
            credentialManager.createCredential(
                request = CreatePasswordRequest(username, password),
                activity = activity,
            )
            Log.v("CredentialTest", "Credentials successfully added")

            //Note the new credentials
            signedInPasswordCredential.value = PasswordCredential(username, password)
        }
        catch (e: CreateCredentialCancellationException) {
            //do nothing, the user chose not to save the credential
            Log.v("CredentialTest", "User cancelled the save")
        }
        catch (e: CreateCredentialException) {
            Log.v("CredentialTest", "Credential save error", e)
        }
    }

    fun simulateLogOut() {
        signedInPasswordCredential.value = null
    }
}