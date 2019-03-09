package soko.ekibun.bangumi.provider

import android.util.Log
import org.jsoup.Jsoup
import retrofit2.Call
import soko.ekibun.bangumi.api.ApiHelper
import soko.ekibun.bangumi.api.bangumi.bean.Episode
import soko.ekibun.bangumi.api.dilidili.Dilidili
import soko.ekibun.bangumi.ui.view.BackgroundWebView

class DilidiliProvider: BaseProvider {
    override val siteId: Int = ProviderInfo.DILIDLILI
    override val name: String = "嘀哩嘀哩"
    override val color: Int = 0xf3a47d
    override val hasDanmaku: Boolean = false
    override val supportSearch: Boolean = true
    override val provideVideo: Boolean = false

    override fun search(key: String): Call<List<ProviderInfo>> {
        return ApiHelper.buildBridgeCall(Dilidili.createInstance().search(key)) {
            return@buildBridgeCall ApiHelper.buildCall {
                val ret = ArrayList<ProviderInfo>()
                try{
                    it.getAsJsonArray("result").map{ it.asJsonObject }.filter { it.get("typedir")?.asString?.startsWith("/anime/") == true }.forEach {
                        ret.add(ProviderInfo(siteId, it.get("typedir").asString, 0f, it.get("typename").asString))
                    }
                }catch (e: Exception){
                    e.printStackTrace()
                }
                return@buildCall ret.toList()
            }
        }
    }

    override fun getVideoInfo(info: ProviderInfo, video: Episode): Call<BaseProvider.VideoInfo> {
        val ids = info.id.split(" ")
        val vid = ids[0]
        val num = ids.getOrNull(1)?.toIntOrNull()?:0
        return ApiHelper.buildHttpCall("http://m.dilidili.name$vid", header){
            val d = Jsoup.parse(it.body()?.string()?:"")
            d.selectFirst(".episode").select("a").filter { it.text().toFloatOrNull() == video.sort + info.offset }.getOrNull(num)?.let {
                val url = it.attr("href")
                val videoInfo = BaseProvider.VideoInfo(
                        Regex("""dilidili.name/watch[0-9]?/([^/]*)/""").find(url)?.groupValues?.get(1) ?: "",
                        siteId,
                        url
                )
                Log.v("video", videoInfo.toString())
                return@buildHttpCall videoInfo
            }
            throw Exception("not found")
        }
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