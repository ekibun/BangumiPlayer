package soko.ekibun.bangumiplayer.provider

import android.util.Log
import org.jsoup.Jsoup
import retrofit2.Call
import soko.ekibun.bangumi.api.ApiHelper
import soko.ekibun.bangumi.api.bangumi.bean.Episode
import java.text.DecimalFormat
import kotlin.math.roundToInt

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
            doc.select(".post")?.forEach {
                val head  = it.selectFirst(".item-head")?.selectFirst("a")?: return@forEach
                val url = head.attr("href")
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
            return@buildHttpCall  BaseProvider.VideoInfo(cid, siteId, url)
        }
    }

    override fun getDanmakuKey(video: BaseProvider.VideoInfo): Call<String> {
        return ApiHelper.buildCall { "OK" }
    }

    override fun getDanmaku(video: BaseProvider.VideoInfo, key: String, pos: Int): Call<Map<Int, List<BaseProvider.Danmaku>>> {
        return ApiHelper.buildHttpCall("https://www.5dm.tv/player/xml.php?id=${video.id}", header){
            val map: MutableMap<Int, MutableList<BaseProvider.Danmaku>> = HashMap()
            val xml = it.body()!!.string()//String(IqiyiProvider.inflate(it.body()!!.bytes(), true))
            val doc = Jsoup.parse(xml)
            val infos = doc.select("d")
            for (info in infos) {
                val p = info.attr("p").split(",")
                val time = p.getOrNull(0)?.toFloat()?.roundToInt()?:0
                val color = "#" + String.format("%06x",  p.getOrNull(3)?.toLong()).toUpperCase()
                val context = info.text()
                val list: MutableList<BaseProvider.Danmaku> = map[time] ?: ArrayList()
                val danmaku = BaseProvider.Danmaku(context, time, color)
                Log.v("danmaku", danmaku.toString())
                list += danmaku
                map[time] = list
            }
            return@buildHttpCall map
        }
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