package soko.ekibun.bangumi.provider

import retrofit2.Call
import soko.ekibun.bangumi.api.bangumi.bean.Episode
import soko.ekibun.bangumi.ui.view.BackgroundWebView

interface BaseProvider {
    val siteId: Int
    val name: String
    val color: Int
    val hasDanmaku: Boolean
    val supportSearch: Boolean
    val provideVideo: Boolean

    fun search(key: String): Call<List<ProviderInfo>>
    fun getVideoInfo(info: ProviderInfo, video: Episode): Call<VideoInfo>
    fun getDanmakuKey(video: VideoInfo): Call<String>
    fun getDanmaku(video: VideoInfo, key: String, pos: Int): Call<List<DanmakuInfo>>
    fun getVideo(webView: BackgroundWebView, video: VideoInfo): Call<Pair<String, Map<String, String>>>

    data class VideoInfo(
            val id:String,
            val siteId: Int,
            val url:String
    )

    data class DanmakuInfo(
            val time: Float,
            val type: Int,
            val textSize: Float,
            val color: Int,
            val context: String,
            val timeStamp: Long = 0L
    )
}