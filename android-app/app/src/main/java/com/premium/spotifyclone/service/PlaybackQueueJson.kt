package com.premium.spotifyclone.service

import com.premium.spotifyclone.data.models.Track
import org.json.JSONArray
import org.json.JSONObject

/** Serialize a play queue into an Intent extra (small lists only; e.g. up to 5 tracks). */
object PlaybackQueueJson {
    const val EXTRA_QUEUE_JSON = "queue_json"
    const val EXTRA_START_INDEX = "queue_start_index"

    fun toJson(tracks: List<Track>): String {
        val arr = JSONArray()
        tracks.forEach { t ->
            val url = t.audioUrl ?: return@forEach
            arr.put(
                JSONObject().apply {
                    put("id", t.id)
                    put("title", t.title)
                    put("artist", t.artist)
                    put("coverUrl", t.coverUrl)
                    put("durationMs", t.durationMs)
                    put("audioUrl", url)
                }
            )
        }
        return arr.toString()
    }

    fun fromJson(json: String?): List<Track> {
        if (json.isNullOrBlank()) return emptyList()
        return runCatching {
            val arr = JSONArray(json)
            buildList {
                for (i in 0 until arr.length()) {
                    val o = arr.getJSONObject(i)
                    add(
                        Track(
                            id = o.getString("id"),
                            title = o.getString("title"),
                            artist = o.getString("artist"),
                            coverUrl = if (o.has("coverUrl")) o.getString("coverUrl") else "",
                            durationMs = o.optLong("durationMs", 0L),
                            audioUrl = o.getString("audioUrl")
                        )
                    )
                }
            }
        }.getOrDefault(emptyList())
    }
}
