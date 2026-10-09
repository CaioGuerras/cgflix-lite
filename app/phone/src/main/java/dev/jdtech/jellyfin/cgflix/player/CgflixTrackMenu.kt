package dev.jdtech.jellyfin.cgflix.player

import android.view.View
import android.widget.PopupMenu
import androidx.media3.common.C
import dev.jdtech.jellyfin.player.local.R
import dev.jdtech.jellyfin.player.local.domain.getTrackNames
import dev.jdtech.jellyfin.player.local.presentation.PlayerViewModel

/**
 * CGFLIX (Etapa 1B): áudio e legenda num menu pequeno preso ao botão, em vez da janela que cobre o
 * vídeo. O vídeo segue tocando enquanto a pessoa escolhe; a troca usa o mesmo caminho do Findroid
 * (`switchToTrack`).
 */
object CgflixTrackMenu {
    fun show(anchor: View, type: @C.TrackType Int, viewModel: PlayerViewModel) {
        val groups =
            viewModel.player.currentTracks.groups.filter { it.type == type && it.isSupported }
        // "Nenhuma" no topo, como no Findroid (índice -1 desliga a faixa)
        val names = arrayOf(anchor.context.getString(R.string.none)) + groups.getTrackNames()
        val selected = groups.indexOfFirst { it.isSelected } + 1
        val popup = PopupMenu(anchor.context, anchor)
        names.forEachIndexed { index, name ->
            popup.menu.add(0, index, index, name).apply {
                isCheckable = true
                isChecked = index == selected
            }
        }
        popup.menu.setGroupCheckable(0, true, true)
        popup.setOnMenuItemClickListener { item ->
            viewModel.switchToTrack(type, item.itemId - 1)
            true
        }
        popup.show()
    }
}
