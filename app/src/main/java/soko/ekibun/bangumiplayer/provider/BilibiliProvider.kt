package soko.ekibun.bangumiplayer.provider

import android.util.Log
import org.jsoup.Jsoup
import retrofit2.Call
import soko.ekibun.bangumi.api.ApiHelper
import soko.ekibun.bangumi.api.bangumi.bean.Episode
import soko.ekibun.bangumi.ui.view.BackgroundWebView
import soko.ekibun.bangumi.util.JsonUtil

class BilibiliProvider: BaseProvider {
    override val siteId: Int = ProviderInfo.BILIBILI
    override val name: String = "哔哩哔哩"
    override val color: Int = 0xf25d8e
    override val hasDanmaku: Boolean = true
    override val supportSearch: Boolean = true
    override val provideVideo: Boolean = false

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

    override fun getDanmaku(video: BaseProvider.VideoInfo, key: String, pos: Int): Call<List<BaseProvider.DanmakuInfo>> {
        return ApiHelper.buildHttpCall("https://comment.bilibili.com/${video.id}.xml", header) {
            val list = ArrayList<BaseProvider.DanmakuInfo>()
            val xml = String(IqiyiProvider.inflate(it.body()?.bytes()?: throw Exception("empty body"), true))
            val doc = Jsoup.parse(xml)
            doc.select("d").forEach {
                val p = it.attr("p").split(",")
                val time = p.getOrNull(0)?.toFloatOrNull()?:0f //出现时间
                val type = p.getOrNull(1)?.toIntOrNull()?:1 // 弹幕类型
                val text = p.getOrNull(2)?.toFloatOrNull()?:25f //字体大小
                val color = (0x00000000ff000000 or (p.getOrNull(3)?.toLongOrNull()?:0L) and 0x00000000ffffffff).toInt() // 颜色
                val date = p.getOrNull(4)?.toLongOrNull()?:0L
                val context =  it.text()
                val danmaku = BaseProvider.DanmakuInfo(time, type, text, color, context, date)
                Log.v("danmaku", danmaku.toString())
                list += danmaku
            }
            return@buildHttpCall list
        }
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