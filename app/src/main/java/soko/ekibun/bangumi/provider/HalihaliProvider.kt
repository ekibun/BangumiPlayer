package soko.ekibun.bangumi.provider

import android.util.Log
import org.jsoup.Jsoup
import retrofit2.Call
import soko.ekibun.bangumi.api.ApiHelper
import soko.ekibun.bangumi.api.bangumi.bean.Episode
import soko.ekibun.bangumi.ui.view.BackgroundWebView
import java.text.DecimalFormat


class HalihaliProvider: BaseProvider {
    override val siteId: Int = ProviderInfo.HALIHALI
    override val name: String = "halihali"
    override val color: Int = 0xa82286
    override val hasDanmaku: Boolean = false
    override val supportSearch: Boolean = true
    override val provideVideo: Boolean = true

    override fun search(key: String): Call<List<ProviderInfo>> {
        return ApiHelper.buildHttpCall("https://www.halihali.tv/search/?wd=${java.net.URLEncoder.encode(key, "utf-8")}", header) {
            val html = it.body()?.string()?: throw Exception("empty body")
            val doc = Jsoup.parse(html)
            val ret = ArrayList<ProviderInfo>()
            doc.select("a.list-img")?.forEach {
                val title = it.attr("title")
                val url = it.attr("href")
                val vid = Regex("""/v/([^/]+)/""").find(url)?.groupValues?.get(1) ?: url
                ret += ProviderInfo(siteId, vid, 0f, title)
            }
            return@buildHttpCall ret.toList()
        }
    }

    override fun getVideoInfo(info: ProviderInfo, video: Episode): Call<BaseProvider.VideoInfo> {
        val url = "https://www.halihali.tv/v/${info.id}/0-${DecimalFormat("#.##").format(video.sort + info.offset)}.html"
        val videoInfo = BaseProvider.VideoInfo(info.id, siteId, url)
        Log.v("video", videoInfo.toString())
        return ApiHelper.buildCall{ videoInfo }
    }

    override fun getDanmakuKey(video: BaseProvider.VideoInfo): Call<String> {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }
    override fun getDanmaku(video: BaseProvider.VideoInfo, key: String, pos: Int): Call<List<BaseProvider.DanmakuInfo>> {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }
    override fun getVideo(webView: BackgroundWebView, video: BaseProvider.VideoInfo): Call<Pair<String, Map<String, String>>> {
        return ApiHelper.buildWebViewCall(webView, video.url, header, "") { request, callback ->
            if(!request.isForMainFrame && Regex("""//player.qinmoe.com/play/[^/]+""").find(request.url.toString())!= null){
                ApiHelper.buildWebViewCall(webView, request.url.toString(), request.requestHeaders, "\$('#divBag').trigger(\"click\")").enqueue(callback)
            }
            false
        }
    }

    companion object {
        private val header: Map<String, String> by lazy {
            val map = HashMap<String, String>()
            //map["User-Agent"] = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/64.0.3282.186 Safari/537.36"
            map
        }
    }
}