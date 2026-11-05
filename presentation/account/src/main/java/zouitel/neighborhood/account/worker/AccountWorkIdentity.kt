package zouitel.neighborhood.account.worker

import android.content.Context
import androidx.work.Data
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.firebase.auth.FirebaseAuth

/**
 * Binds persisted WorkManager requests to the Firebase identity that created them.
 *
 * Requests without an identity are rejected deliberately: a legacy or manually
 * constructed request must never inherit whichever account happens to be signed in
 * when the worker eventually runs.
 */
internal object AccountWorkIdentity {
    const val FIREBASE_UID_KEY = "account_work_firebase_uid"
    const val GOOGLE_ACCOUNT_EMAIL_KEY = "account_work_google_email"

    fun dataFor(uid: String?, context: Context): Data {
        require(!uid.isNullOrBlank()) {
            "An authenticated Firebase user is required to schedule account work"
        }
        val googleEmail = GoogleSignIn.getLastSignedInAccount(context)?.email
            ?.takeIf(String::isNotBlank)
            ?: throw IllegalStateException("A signed-in Google Drive account is required to schedule account work")
        return Data.Builder()
            .putString(FIREBASE_UID_KEY, uid)
            .putString(GOOGLE_ACCOUNT_EMAIL_KEY, googleEmail)
            .build()
    }

    fun belongsToCurrentUser(inputData: Data, context: Context): Boolean {
        val expectedUid = inputData.getString(FIREBASE_UID_KEY)
            ?.takeIf(String::isNotBlank)
            ?: return false
        val expectedGoogleEmail = inputData.getString(GOOGLE_ACCOUNT_EMAIL_KEY)
            ?.takeIf(String::isNotBlank)
            ?: return false
        val currentGoogleEmail = GoogleSignIn.getLastSignedInAccount(context)?.email
        return FirebaseAuth.getInstance().currentUser?.uid == expectedUid &&
            currentGoogleEmail.equals(expectedGoogleEmail, ignoreCase = true)
    }
}
