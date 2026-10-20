package io.github.jd1378.otphelper.gmail

import android.service.quicksettings.Tile
import android.service.quicksettings.TileService

class GmailOtpTileService : TileService() {
  override fun onClick() {
    super.onClick()
    GmailFetchWorker.enqueue(this)
    qsTile?.apply {
      state = Tile.STATE_INACTIVE
      updateTile()
    }
  }

  override fun onStartListening() {
    super.onStartListening()
    qsTile?.state = Tile.STATE_INACTIVE
    qsTile?.updateTile()
  }
}
