package soko.ekibun.bangumi.provider

import android.util.Log
import org.jsoup.Jsoup
import retrofit2.Call
import soko.ekibun.bangumi.api.ApiHelper
import soko.ekibun.bangumi.api.bangumi.bean.Episode
import soko.ekibun.bangumi.ui.view.BackgroundWebView
import java.text.DecimalFormat

class DililiProvider: BaseProvider {
    override val siteId: Int = ProviderInfo.DILILI
    override val name: String = "5dm"
    override val color: Int = 0xff5050
    override val hasDanmaku: Boolean = true
    override val supportSearch: Boolean = true

    override fun search(key: String): Call<List<ProviderInfo>> {
        return ApiHelper.buildHttpCall("https://www.5dm.tv/search/${java.net.URLEncoder.encode(key, "utf-8")}", header){
            val html = it.body()?.string()?: throw Exception("empty body")
            val doc = Jsoup.parse(html)
            val ret = ArrayList<ProviderInfo>()
            doc.select(".post").forEach {
                val head  = it.selectFirst(".item-head")?.selectFirst("a")?: return@forEach
                val url = head.attr("href")
                if(!url.contains("/bangumi/") && !url.contains("/end/")) return@forEach
                //val vid = head.attr("rel")
                val title = head.attr("title")
                ret += ProviderInfo(siteId, url, 0f, title)
            }
            return@buildHttpCall ret
        }
    }

    override fun getVideoInfo(info: ProviderInfo, video: Episode): Call<BaseProvider.VideoInfo> {
        val url = "${info.id}?link=${DecimalFormat("#.##").format(video.sort + info.offset - 1)}"
        return ApiHelper.buildHttpCall(url, header){
            val doc = Jsoup.parse(it.body()?.string()?:"")
            val cid = Regex("""cid=(.*?)&""")
                    .find(doc.selectFirst("iframe")?.attr("src")?:"")?.groupValues?.get(1)
                    ?:throw Exception("not found")
            return@buildHttpCall BaseProvider.VideoInfo(cid, siteId, url)
        }
    }

    override fun getDanmakuKey(video: BaseProvider.VideoInfo): Call<String> {
        return ApiHelper.buildCall { "OK" }
    }

    override fun getDanmaku(video: BaseProvider.VideoInfo, key: String, pos: Int): Call<List<BaseProvider.DanmakuInfo>> {
        return ApiHelper.buildHttpCall("https://www.5dm.tv/player/xml.php?id=${video.id}", header){
            val list = ArrayList<BaseProvider.DanmakuInfo>()
            val xml = it.body()?.string()?: throw Exception("empty body")
            val doc = Jsoup.parse(xml)
            doc.select("d").forEach {
                val p = it.attr("p").split(",")
                val time = p.getOrNull(0)?.toFloatOrNull()?:0f //出现时间
                val type = p.getOrNull(1)?.toIntOrNull()?:1 // 弹幕类型
                val text = p.getOrNull(2)?.toFloatOrNull()?:25f //字体大小
                val color = (0x00000000ff000000 or (p.getOrNull(3)?.toLongOrNull()?:0L) and 0x00000000ffffffff).toInt() // 颜色
                val context =  it.text()
                val danmaku = BaseProvider.DanmakuInfo(time, type, text, color, context)
                Log.v("danmaku", danmaku.toString())
                list += danmaku
            }
            return@buildHttpCall list
        }
    }

    override val provideVideo: Boolean = false
    override fun getVideo(webView: BackgroundWebView, video: BaseProvider.VideoInfo): Call<Pair<String, Map<String, String>>> {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    companion object {
        private val header: Map<String, String> by lazy {
            val map = HashMap<String, String>()
            map["referer"] = "https://www.5dm.tv/"
            map["cookie"] = "5dm"
            map
        }
    }
}