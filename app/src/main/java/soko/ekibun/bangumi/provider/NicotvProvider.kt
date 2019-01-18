package soko.ekibun.bangumi.provider

import android.util.Log
import org.jsoup.Jsoup
import retrofit2.Call
import soko.ekibun.bangumi.api.ApiHelper
import soko.ekibun.bangumi.api.bangumi.bean.Episode
import soko.ekibun.bangumi.ui.view.BackgroundWebView
import java.text.DecimalFormat


class NicotvProvider: BaseProvider {
    override val siteId: Int = ProviderInfo.NICOTV
    override val name: String = "nicotv"
    override val color: Int = 0x666666
    override val hasDanmaku: Boolean = false
    override val supportSearch: Boolean = true
    override val provideVideo: Boolean = false

    override fun search(key: String): Call<List<ProviderInfo>> {
        return ApiHelper.buildHttpCall("http://www.nicotv.cc/video/search/${java.net.URLEncoder.encode(key, "utf-8")}", header) {
            val html = it.body()?.string()?: throw Exception("empty body")
            val doc = Jsoup.parse(html)
            val ret = ArrayList<ProviderInfo>()
            doc.select("ul.vod-item-img p.image> a")?.forEach {
                        val title = it.selectFirst("img").attr("alt")
                        val url = it.attr("href")
                        val vid = Regex("""/([0-9]+).html""").find(url)?.groupValues?.get(1) ?: url
                        ret += ProviderInfo(siteId, "$vid-1", 0f, title)
                    }
            return@buildHttpCall ret
        }
    }

    override fun getVideoInfo(info: ProviderInfo, video: Episode): Call<BaseProvider.VideoInfo> {
        val url = "http://www.nicotv.cc/video/play/${info.id}-${DecimalFormat("#.##").format(video.sort + info.offset)}.html"
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
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    companion object {
        private val header: Map<String, String> by lazy {
            val map = HashMap<String, String>()
            //map["User-Agent"] = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/64.0.3282.186 Safari/537.36"
            map
        }
    }
}