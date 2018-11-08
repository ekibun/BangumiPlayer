package soko.ekibun.bangumiplayer.provider

import retrofit2.Call
import soko.ekibun.bangumi.api.bangumi.bean.Episode
import soko.ekibun.bangumi.ui.view.BackgroundWebView

interface BaseProvider {
    val siteId: Int
    val name: String
    val color: Int
    val hasDanmaku: Boolean
    val supportSearch: Boolean

    fun search(key: String): Call<List<ProviderInfo>>
    fun getVideoInfo(info: ProviderInfo, video: Episode): Call<VideoInfo>
    fun getVideo(webView: BackgroundWebView, api: String, video: VideoInfo): Call<Pair<String,Map<String, String>>>
    fun getDanmakuKey(video: VideoInfo): Call<String>
    fun getDanmaku(video: VideoInfo, key: String, pos: Int): Call<Map<Int, List<Danmaku>>>

    data class VideoInfo(
            val id:String,
            val siteId: Int,
            val url:String
    )

    data class Danmaku(
            val context:String,
            val time: Int,
            val color: String
    )
}