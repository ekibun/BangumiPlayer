package soko.ekibun.bangumiplayer.provider

import android.graphics.Color
import android.util.Log
import com.google.gson.JsonObject
import org.jsoup.Jsoup
import retrofit2.Call
import soko.ekibun.bangumi.api.ApiHelper
import soko.ekibun.bangumi.api.bangumi.bean.Episode
import soko.ekibun.bangumi.ui.view.BackgroundWebView
import soko.ekibun.bangumi.util.JsonUtil

class YoukuProvider: BaseProvider {
    override val siteId: Int = ProviderInfo.YOUKU
    override val name: String = "优酷"
    override val color: Int = 0x1ebeff
    override val hasDanmaku: Boolean = true
    override val supportSearch: Boolean = true

    override fun search(key: String): Call<List<ProviderInfo>> {
        return ApiHelper.buildHttpCall("http://www.soku.com/search_video/q_${java.net.URLEncoder.encode(key, "utf-8")}", header){
            var html = it.body()?.string()?: throw Exception("empty body")
            html = html.substring(html.indexOf("<div class=\\\"sk-result-list\\\""))
            html = html.substring(0, html.indexOf("</script>")).replace("\\n", "\n").replace("\\\"", "\"").replace("\\t", "\t")
            val doc = Jsoup.parse(html)
            val ret = ArrayList<ProviderInfo>()
            doc.select(".sk-mod")?.filter { it.selectFirst("a[data-spm=ddetail]") != null && it.selectFirst(".source")?.text()?.contains("优酷") == true }
                    ?.forEach {
                        val title = it.selectFirst("a[data-spm=dtitle]")?.attr("title")?:return@forEach
                        val url = it.selectFirst("a[data-spm=ddetail]").attr("href")
                        val vid = Regex("""/show/id_z([^/.]*).html""").find(url)?.groupValues?.get(1) ?: url
                        ret += ProviderInfo(siteId, vid, 0f, title)
                    }
            return@buildHttpCall ret
        }
    }

    override fun getVideoInfo(info: ProviderInfo, video: Episode): Call<BaseProvider.VideoInfo> {
        return ApiHelper.buildHttpCall("http://list.youku.com/show/episode?id=${info.id}&stage=reload_${((video.sort + info.offset).toInt()-1) / 10 * 10 + 1}&callback=jQuery", header){
            var json = it.body()?.string()?:""
            json = json.substring(json.indexOf('{'), json.lastIndexOf('}') + 1)
            val element = JsonUtil.toJsonObject(json)
            val li = Jsoup.parse(element.get("html").asString)
            li.select(".c555").forEach {
                if(it.parent().text().substringBefore(it.text()).toFloatOrNull() == video.sort + info.offset){
                    val videoId = Regex("""id_([^.=]+)""").find(it.attr("href"))?.groupValues?.get(1)?:"http:" + it.attr("href")
                    val videoInfo = BaseProvider.VideoInfo(videoId,
                            siteId,
                            "http:" + it.attr("href")
                    )
                    Log.v("video", videoInfo.toString())
                    return@buildHttpCall videoInfo
                } }
            throw Exception("not found")
        }
    }

    override fun getDanmakuKey(video: BaseProvider.VideoInfo): Call<String> {
        return ApiHelper.buildHttpCall(video.url, header){
            val doc = it.body()?.string()?:""
            return@buildHttpCall Regex("""videoId: '([0-9]+)'""").find(doc)!!.groupValues[1]
        }
    }

    override fun getDanmaku(video: BaseProvider.VideoInfo, key: String, pos: Int): Call<List<BaseProvider.DanmakuInfo>> {
        val list = ArrayList<retrofit2.Call<List<BaseProvider.DanmakuInfo>>>()
        val pageStart = pos / 300 * 5
        for(i in 0..9)
            list.add(getDanmakuCall(key,pageStart + i))
        return ApiHelper.buildGroupCall(list.toTypedArray())
    }

    private fun getDanmakuCall(key: String, page: Int): retrofit2.Call<List<BaseProvider.DanmakuInfo>> {
        return ApiHelper.buildHttpCall("http://service.danmu.youku.com/list?jsoncallback=&mat=$page&mcount=1&ct=1001&iid=$key", header){
            val list = ArrayList<BaseProvider.DanmakuInfo>()
            val json = it.body()?.string()?: throw Exception("empty body")
            JsonUtil.toJsonObject(json).getAsJsonArray("result")
                    ?.map { it.asJsonObject }?.forEach {
                        val time = (it.get("playat")?.asFloat?:0f) / 1000
                        val propertis = try { JsonUtil.toJsonObject(it.get("propertis")?.asString?:"") } catch(e: Exception) { JsonObject() }
                        val type = if(propertis.get("pos")?.asInt?:3 != 3){
                            Log.v("position", propertis.get("pos")?.asInt?.toString())
                            5
                        } else 1
                        val text = 25f //字体大小
                        val color = propertis.get("color")?.asInt?: Color.WHITE // 颜色
                        val context =  it.get("content")?.asString?:""
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
            map["User-Agent"] = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/64.0.3282.186 Safari/537.36"
            map
        }
    }
}