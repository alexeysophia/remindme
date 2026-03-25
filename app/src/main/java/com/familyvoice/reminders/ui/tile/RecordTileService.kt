package com.familyvoice.reminders.ui.tile

import android.content.Intent
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import com.familyvoice.reminders.MainActivity

/**
 * Quick Settings Tile — "Записать напоминание".
 *
 * Tapping the tile opens MainActivity with a flag to immediately
 * start recording, so the user can go from lock screen → recording in ~1 tap.
 *
 * To add the tile: device Settings → Quick Settings → drag "FamilyVoice" tile.
 */
class RecordTileService : TileService() {

    override fun onStartListening() {
        super.onStartListening()
        qsTile?.apply {
            state = Tile.STATE_INACTIVE
            updateTile()
        }
    }

    override fun onClick() {
        super.onClick()
        qsTile?.apply {
            state = Tile.STATE_ACTIVE
            updateTile()
        }
        // Collapse Quick Settings panel and launch the app
        val intent = Intent(this, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            putExtra(MainActivity.EXTRA_START_RECORDING, true)
        }
        startActivityAndCollapse(intent)
    }
}
