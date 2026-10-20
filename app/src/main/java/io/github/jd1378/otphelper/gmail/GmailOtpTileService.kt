package io.github.jd1378.otphelper.gmail

import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService

class GmailOtpTileService : TileService() {
  override fun onClick() {
    super.onClick()
    val intent = Intent(this, GmailFetchActivity::class.java).apply {
      action = GmailFetchActivity.ACTION_FETCH
      addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
    }
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
      val pendingIntent =
          PendingIntent.getActivity(
              this,
              0,
              intent,
              PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
          )
      startActivityAndCollapse(pendingIntent)
    } else {
      @Suppress("DEPRECATION")
      startActivityAndCollapse(intent)
    }
  }

  override fun onStartListening() {
    super.onStartListening()
    qsTile?.state = Tile.STATE_INACTIVE
    qsTile?.updateTile()
  }
}
