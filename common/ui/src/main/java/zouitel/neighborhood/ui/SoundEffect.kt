package zouitel.neighborhood.ui

import android.content.Context
import android.media.AudioManager

class SoundEffect {

    fun performSoundEffect(context: Context, effectType: Int, isSound: Boolean?) {
        val sysServ = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        if (isSound == true) sysServ.playSoundEffect(effectType, 1.0f)
    }
}
