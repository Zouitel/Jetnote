package zouitel.neighborhood.workspace.ui.features

import android.content.Context
import android.telephony.TelephonyManager
import java.util.Locale

internal fun country(context: Context): Result<String?> {
    return runCatching {
        val telephonyManager =
            context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
        val countryIso = telephonyManager.networkCountryIso

        val country = Locale("", countryIso).displayCountry.takeIf { it.isNotEmpty() }
        val countryFlag = countryIso.uppercase().flagEmoji.takeIf { it.isNotEmpty() }
        "$country $countryFlag"
    }.onFailure {
        it.printStackTrace()
    }
}

val String.flagEmoji: String
    get() {
        if (length != 2) return ""
        val firstLetter = Character.codePointAt(this.uppercase(), 0) - 0x41 + 0x1F1E6
        val secondLetter = Character.codePointAt(this.uppercase(), 1) - 0x41 + 0x1F1E6
        return String(Character.toChars(firstLetter)) + String(Character.toChars(secondLetter))
    }

const val SINGLE_DAY = 86_400_000