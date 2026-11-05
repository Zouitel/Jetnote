package zouitel.neighborhood.account.worker

import androidx.work.Data
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

    fun dataFor(uid: String?): Data {
        require(!uid.isNullOrBlank()) {
            "An authenticated Firebase user is required to schedule account work"
        }
        return Data.Builder()
            .putString(FIREBASE_UID_KEY, uid)
            .build()
    }

    fun belongsToCurrentUser(inputData: Data): Boolean {
        val expectedUid = inputData.getString(FIREBASE_UID_KEY)
            ?.takeIf(String::isNotBlank)
            ?: return false
        return FirebaseAuth.getInstance().currentUser?.uid == expectedUid
    }
}
