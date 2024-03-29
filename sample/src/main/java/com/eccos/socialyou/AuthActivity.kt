package com.eccos.socialyou


import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View

import androidx.appcompat.app.AppCompatActivity
import com.firebase.client.Firebase

import com.firebase.ui.auth.AuthUI
import com.firebase.ui.auth.ErrorCodes
import com.firebase.ui.auth.IdpResponse
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import java.text.Normalizer


class AuthActivity : AppCompatActivity() {

    private var myFirebaseRef: Firebase? = null
    private val dataRetrieved = 101

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        Firebase.setAndroidContext(this)

        val auth = FirebaseAuth.getInstance()
        if (auth.currentUser != null) {
            // already signed in
            startActivity(this.createIntent())
            // finish my layout
            finish()

        } else {
            // not signed in
            startActivityForResult(
                    AuthUI.getInstance()
                            .createSignInIntentBuilder()
                            .setAvailableProviders(listOf(
                                    AuthUI.IdpConfig.FacebookBuilder().build(),
                                    AuthUI.IdpConfig.EmailBuilder().build(),
                                    AuthUI.IdpConfig.GoogleBuilder().build()))
                            .setTheme(R.style.AppThemeAuth)
                            .setIsSmartLockEnabled(false)
                            .setLogo(R.drawable.logo).build(),
                    RC_SIGN_IN)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        // RC_SIGN_IN is the request code you passed into startActivityForResult(...) when starting the sign in flow.
        if (requestCode == RC_SIGN_IN) {
            val response = IdpResponse.fromResultIntent(data)

            // Successfully signed in
            if (resultCode == Activity.RESULT_OK) {

                var intent= getUserInfo()

                startActivityForResult(intent, dataRetrieved)

                // Finish my layout
                finish()

                // Finish auth layout
                this@AuthActivity.finish()

            } else {
                // Sign in failed
                if (response == null) {
                    // User pressed back button
                    showSnackbar(R.string.sign_in_cancelled)
                    return
                }

                if (response.error!!.errorCode == ErrorCodes.NO_NETWORK) {
                    showSnackbar(R.string.no_internet_connection)
                    return
                }

                showSnackbar(R.string.unknown_error)
                Log.e(TAG, "Sign-in error: ", response.error)
            }
        }

        if (requestCode == dataRetrieved) {
            // Make sure the request was successful
            if (resultCode != Activity.RESULT_OK) {
                finish()
            }
        }
    }

    private fun getUserInfo(): Intent {
        val name = FirebaseAuth.getInstance().currentUser!!.displayName

        //Converting to a standardized string
        var nameUrl= name!!.replace(" ", "").toLowerCase()
        nameUrl= stripAccents(nameUrl)

        //Assuming the user Facebook profile
        val profile = "fb.com/$nameUrl"

        //Making the photo larger
        val photoUrl =  FirebaseAuth.getInstance().currentUser!!.photoUrl
        val url=  "$photoUrl?type=large"

        //Getting reference to Firebase
        myFirebaseRef = Firebase("https://socialyou-be6cf.firebaseio.com/")

        val mInformation = HashMap<String, Any>()

        val userId = FirebaseAuth.getInstance().currentUser!!.uid

        //Saving all data with FireBase
        mInformation.put("name", name.toString())
        mInformation.put("profile", profile)
        mInformation.put("url", url)

        val fb = myFirebaseRef!!.child("users").child(userId)
        fb.setValue(mInformation)

        return Intent(this, MainActivity::class.java)
    }

    private fun stripAccents(s: String): String {
        var s = s
        s = Normalizer.normalize(s, Normalizer.Form.NFD)
        s = s.replace("[\\p{InCombiningDiacriticalMarks}]".toRegex(), "")
        return s
    }

    private fun createIntent(): Intent {
        return Intent(this, MainActivity::class.java)
    }

    private fun showSnackbar(text: Int) {
        val contextView = findViewById<View>(android.R.id.content)

        Snackbar.make(contextView, text, Snackbar.LENGTH_LONG)
                .show()
    }

    companion object {
        private const val TAG = "AuthActivity"

        // Choose an arbitrary request code value
        private const val RC_SIGN_IN = 123
    }
}
