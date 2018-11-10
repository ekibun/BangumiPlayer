package soko.ekibun.bangumiplayer.provider

import android.util.Log
import org.jsoup.Jsoup
import retrofit2.Call
import soko.ekibun.bangumi.api.ApiHelper
import soko.ekibun.bangumi.api.bangumi.bean.Episode
import soko.ekibun.bangumi.ui.view.BackgroundWebView
import soko.ekibun.bangumi.util.JsonUtil
import kotlin.math.roundToInt

class BilibiliProvider: BaseProvider {
    override val siteId: Int = ProviderInfo.BILIBILI
    override val name: String = "哔哩哔哩"
    override val color: Int = 0xf25d8e
    override val hasDanmaku: Boolean = true
    override val supportSearch: Boolean = true

    override fun search(key: String): Call<List<ProviderInfo>> {
        return ApiHelper.buildHttpCall("http://api.bilibili.com/x/web-interface/search/type?search_type=media_bangumi&keyword=${java.net.URLEncoder.encode(key, "utf-8")}", header){
            val json = it.body()?.string()?: throw Exception("empty body")
            val ret = ArrayList<ProviderInfo>()
            JsonUtil.toJsonObject(json).getAsJsonObject("data")?.getAsJsonArray("result")
                    ?.map { it.asJsonObject }?.forEach {
                        ret.add(ProviderInfo(siteId, it.get("season_id").asString, 0f, Jsoup.parse(it.get("title").asString).text()))
                    }
            return@buildHttpCall ret
        }
    }

    override fun getVideoInfo(info: ProviderInfo, video: Episode): Call<BaseProvider.VideoInfo> {
        return ApiHelper.buildHttpCall("https://bangumi.bilibili.com/view/web_api/season?season_id=${info.id}", header){
            val json = it.body()?.string()?: throw Exception("empty body")
            val ep = JsonUtil.toJsonObject(json).getAsJsonObject("result")?.getAsJsonArray("episodes")
                    ?.map { it.asJsonObject }?.first {
                        it.get("index")?.asString?.toFloatOrNull() == video.sort + info.offset
                    }?: throw Exception("no such episode")
            return@buildHttpCall BaseProvider.VideoInfo(ep.get("cid").toString(), siteId,
                    "https://www.bilibili.com/bangumi/play/ep${ep.get("ep_id").asString}")
        }
    }

    override fun getDanmakuKey(video: BaseProvider.VideoInfo): Call<String> {
        return ApiHelper.buildCall { "OK" }
    }

    override fun getDanmaku(video: BaseProvider.VideoInfo, key: String, pos: Int): Call<Map<Int, List<BaseProvider.Danmaku>>> {
        return ApiHelper.buildHttpCall("https://comment.bilibili.com/${video.id}.xml", header) {
            val map: MutableMap<Int, MutableList<BaseProvider.Danmaku>> = HashMap()
            val xml = String(IqiyiProvider.inflate(it.body()!!.bytes(), true))
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
            //map["User-Agent"] = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/64.0.3282.186 Safari/537.36"
            map
        }
    }
}