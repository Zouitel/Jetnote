package zouitel.neighborhood.feedback

/**
 * Public, non-secret configuration for the feedback feature.
 *
 * GitHub write credentials must never be embedded in the Android application.
 * Issue creation is intentionally disabled until a trusted backend endpoint
 * is configured to authenticate users, validate payloads, and rate-limit writes.
 */
object Contents {
    const val API_URL = "https://api.github.com/repos/Zouitel/Jetnote/issues"
}
