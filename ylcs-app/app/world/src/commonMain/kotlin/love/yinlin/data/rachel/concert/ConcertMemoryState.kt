package love.yinlin.data.rachel.concert

import androidx.compose.runtime.Stable
import love.yinlin.cs.APIRes
import love.yinlin.cs.ServerRes

@Stable
internal data class ConcertItem(
    val concert: Concert,
    val songs: List<ConcertSong>,
    val attended: Boolean = false,
) {
    val posterRes: APIRes? = concert.poster?.let { poster ->
        val parts = poster.split('/', limit = 2)
        val classification = parts[0].takeIf { it.isNotBlank() && it != "." && it != ".." } ?: return@let null
        val index = parts.getOrNull(1)?.toIntOrNull()?.takeIf { it in 0 .. Int.MAX_VALUE } ?: return@let null
        ServerRes.Photo.pic(classification = classification, index = index, thumb = true)
    }
}

@Stable
internal data class ConcertSongItem(
    val song: ConcertSong,
    val concertCount: Int,
    val heardCount: Int = 0,
)

@Stable
internal class ConcertMemoryState private constructor(
    val concerts: List<ConcertItem>,
    val songs: List<ConcertSongItem>,
) {
    val uniqueSongs: List<ConcertSongItem> = songs.filter { it.concertCount == 1 }
    val attendedConcerts: List<ConcertItem> = concerts.filter { it.attended }
    val heardSongs: List<ConcertSongItem> = songs.filter { it.heardCount > 0 }
    val unheardSongs: List<ConcertSongItem> = songs.filter { it.heardCount == 0 }

    fun withAttended(attended: List<Int>): ConcertMemoryState {
        val attendedIds = attended.toSet()
        val updatedConcerts = concerts.map { it.copy(attended = it.concert.cid in attendedIds) }
        val heardCounts = mutableMapOf<Int, Int>()

        for (item in updatedConcerts) {
            if (item.attended) {
                for (song in item.songs.distinctBy { it.sid }) {
                    heardCounts[song.sid] = (heardCounts[song.sid] ?: 0) + 1
                }
            }
        }

        return ConcertMemoryState(
            concerts = updatedConcerts,
            songs = songs.map { it.copy(heardCount = heardCounts[it.song.sid] ?: 0) },
        )
    }

    companion object {
        val Empty = ConcertMemoryState([], [])

        fun create(concerts: List<Concert>, songs: List<ConcertSong>, attended: List<Int>): ConcertMemoryState {
            val songCatalog = songs.distinctBy { it.sid }.sortedBy { it.sid }
            val songById = songCatalog.associateBy { it.sid }
            val concertItems = concerts.distinctBy { it.cid }
                .sortedWith(compareByDescending<Concert> { it.date }.thenByDescending { it.cid })
                .map { ConcertItem(concert = it, songs = it.songs.mapNotNull(songById::get)) }

            val concertCounts = mutableMapOf<Int, Int>()
            for (item in concertItems) {
                for (song in item.songs.distinctBy { it.sid }) {
                    concertCounts[song.sid] = (concertCounts[song.sid] ?: 0) + 1
                }
            }

            val songItems = songCatalog.map { ConcertSongItem(it, concertCounts[it.sid] ?: 0) }

            return ConcertMemoryState(concertItems, songItems).withAttended(attended)
        }
    }
}
