package zouitel.neighborhood.tile

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Intent
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import zouitel.neighborhood.jotting.JottingActivity
import kotlin.jvm.java

class JottingTileService: TileService() {

    @SuppressLint("StartActivityAndCollapseDeprecated", "NewApi")
    override fun onClick() {
        super.onClick()
        val intent = Intent(applicationContext, JottingActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        intent.action = Intent.ACTION_VIEW
        intent.putExtra("jotting", true)

        val pendingIntent = PendingIntent.getActivity(this,0, intent, PendingIntent.FLAG_IMMUTABLE)
        startActivityAndCollapse(pendingIntent)

        qsTile.state = Tile.STATE_INACTIVE
        qsTile.updateTile()
    }
}