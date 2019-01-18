package soko.ekibun.bangumi.provider

import android.util.Log
import org.jsoup.Jsoup
import retrofit2.Call
import soko.ekibun.bangumi.api.ApiHelper
import soko.ekibun.bangumi.api.bangumi.bean.Episode
import soko.ekibun.bangumi.ui.view.BackgroundWebView
import java.text.DecimalFormat

class Anime1Provider: BaseProvider {
    override val siteId: Int = ProviderInfo.ANIME1
    override val name: String = "Anime1"
    override val color: Int = 0xe4017f
    override val hasDanmaku: Boolean = false
    override val supportSearch: Boolean = true
    override val provideVideo: Boolean = true

    override fun search(key: String): Call<List<ProviderInfo>> {
        return ApiHelper.buildHttpCall("https://anime1.me/?s=${java.net.URLEncoder.encode(key, "utf-8")}", header){
            val html = it.body()?.string()?: throw Exception("empty body")
            val doc = Jsoup.parse(html)
            val ret = HashSet<ProviderInfo>()
            doc.select(".cat-links a")?.forEach {
                ret.add(ProviderInfo(siteId, it.text(), 0f, it.text()))
            }
            return@buildHttpCall ret.toList()
        }
    }

    override fun getVideoInfo(info: ProviderInfo, video: Episode): Call<BaseProvider.VideoInfo> {
        val title = "${info.id} [${DecimalFormat("00.##").format(video.sort + info.offset)}]"
        return ApiHelper.buildBridgeCall(ApiHelper.buildHttpCall("https://anime1.me/?s=${java.net.URLEncoder.encode(title, "utf-8")}", header){
            val html = it.body()?.string()?: throw Exception("empty body")
            val doc = Jsoup.parse(html)
            doc.select(".post")?.mapNotNull { it.selectFirst(".entry-title a") }?.firstOrNull {it.text() == title }?.let{
                return@buildHttpCall it.attr("href")
            }
            throw Exception("not found")
        }){post->
            Log.v("post", post)
            return@buildBridgeCall ApiHelper.buildHttpCall(post, header){
                val html = it.body()?.string()?: throw Exception("empty body")
                val src = Jsoup.parse(html).selectFirst("iframe")?.attr("src")?:throw Exception("not found")
                val videoInfo = BaseProvider.VideoInfo(post, siteId, src)
                Log.v("video", videoInfo.toString())
                return@buildHttpCall videoInfo
            }
        }
    }

    override fun getVideo(webView: BackgroundWebView, video: BaseProvider.VideoInfo): Call<Pair<String, Map<String, String>>> {
        return ApiHelper.buildWebViewCall(webView, video.url, header, "(function() { try{ return player.getCache().src; }catch(e){ let sources = jwplayer().getPlaylist()[0].sources; return sources[sources.length-1].file}})()")
    }

    override fun getDanmakuKey(video: BaseProvider.VideoInfo): Call<String> {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getDanmaku(video: BaseProvider.VideoInfo, key: String, pos: Int): Call<List<BaseProvider.DanmakuInfo>> {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    companion object {
        private val header: Map<String, String> by lazy {
            val map = HashMap<String, String>()
            map["referer"] = "https://anime1.me/"
            map
        }
    }
}