package soko.ekibun.bangumi.provider

import android.util.Log
import retrofit2.Call
import soko.ekibun.bangumi.api.ApiHelper
import soko.ekibun.bangumi.api.bangumi.bean.Episode
import soko.ekibun.bangumi.ui.view.BackgroundWebView
import java.text.DecimalFormat

class UrlProvider: BaseProvider {
    override val siteId: Int = ProviderInfo.URL
    override val name: String = "链接"
    override val color: Int = 0x888888
    override val hasDanmaku: Boolean = false
    override val supportSearch: Boolean = false
    override val provideVideo: Boolean = true

    override fun search(key: String): Call<List<ProviderInfo>> {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getVideoInfo(info: ProviderInfo, video: Episode): Call<BaseProvider.VideoInfo> {
        val ids = info.id.split("\n")
        val id = ids.getOrNull(0)?:""
        val format = (Regex("""\{\{(.*)\}\}""").find(id)?.groupValues?: listOf("{{ep}}", "ep")).toMutableList()
        if(format[0] == "{{ep}}") format[1] = "#.##"
        val url = try{ id.replace(format[0], DecimalFormat(format[1]).format(video.sort + info.offset)) }catch(e: Exception){ info.id }
        val videoInfo = BaseProvider.VideoInfo(info.id, siteId, url)
        Log.v("video", videoInfo.toString())
        return ApiHelper.buildCall{ videoInfo }
    }

    override fun getVideo(webView: BackgroundWebView, video: BaseProvider.VideoInfo): Call<Pair<String, Map<String, String>>> {
        val ids = video.id.split("\n")
        val id = ids.getOrNull(0)?:""
        val isFile = id.startsWith("/") || !(ids.getOrNull(1)?:"").isEmpty()
        val header = HashMap<String, String>()
        header["referer"] = video.url
        if(isFile) return ApiHelper.buildCall { Pair(video.url, HashMap<String, String>()) }
        return ApiHelper.buildWebViewCall(webView, video.url, header)
    }

    override fun getDanmakuKey(video: BaseProvider.VideoInfo): Call<String> {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getDanmaku(video: BaseProvider.VideoInfo, key: String, pos: Int): Call<List<BaseProvider.DanmakuInfo>> {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }
}