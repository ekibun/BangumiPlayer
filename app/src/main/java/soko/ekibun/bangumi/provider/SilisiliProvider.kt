package soko.ekibun.bangumi.provider

import android.util.Log
import org.jsoup.Jsoup
import retrofit2.Call
import soko.ekibun.bangumi.api.ApiHelper
import soko.ekibun.bangumi.api.bangumi.bean.Episode
import soko.ekibun.bangumi.api.silisili.Silisili
import soko.ekibun.bangumi.ui.view.BackgroundWebView
import java.text.DecimalFormat

class SilisiliProvider: BaseProvider {
    override val siteId: Int = ProviderInfo.SILISILI
    override val name: String = "silisili"
    override val color: Int = 0xff6688
    override val hasDanmaku: Boolean = true
    override val supportSearch: Boolean = true
    override val provideVideo: Boolean = false

    override fun search(key: String): Call<List<ProviderInfo>> {
        return ApiHelper.buildBridgeCall(Silisili.createInstance().search(key)) {
            return@buildBridgeCall ApiHelper.buildCall {
                val doc = Jsoup.parse(it)
                val ret = ArrayList<ProviderInfo>()
                doc.select("div.anime_list h3>a")?.forEach {
                    val title = it.text()
                    val url = it.attr("href")
                    val vid = Regex("""/([0-9]+).html""").find(url)?.groupValues?.get(1) ?: url
                    ret += ProviderInfo(siteId, vid, 0f, title)
                }
                return@buildCall ret.toList()
            }
        }
    }

    override fun getVideoInfo(info: ProviderInfo, video: Episode): Call<BaseProvider.VideoInfo> {
        val ep = "${info.id}-${DecimalFormat("#.##").format(video.sort + info.offset)}"
        val url = "http://www.silisili.co/play/$ep.html"
        val videoInfo = BaseProvider.VideoInfo("${info.id}/$ep", siteId, url)
        Log.v("video", videoInfo.toString())
        return ApiHelper.buildCall{ videoInfo }
    }

    override fun getDanmakuKey(video: BaseProvider.VideoInfo): Call<String> {
        return ApiHelper.buildCall { "OK" }
    }
    override fun getDanmaku(video: BaseProvider.VideoInfo, key: String, pos: Int): Call<List<BaseProvider.DanmakuInfo>> {
        return ApiHelper.buildHttpCall("http://47.100.0.249/danmu/dm/${video.id}.php", header) {
            val list = ArrayList<BaseProvider.DanmakuInfo>()
            val xml = it.body()?.string()?: throw Exception("empty body")//String(IqiyiProvider.inflate(it.body()?.bytes()?: throw Exception("empty body"), true))
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