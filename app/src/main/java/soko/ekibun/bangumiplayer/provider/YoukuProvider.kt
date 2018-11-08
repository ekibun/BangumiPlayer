package soko.ekibun.bangumiplayer.provider

import android.util.Log
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

    override fun getVideo(webView: BackgroundWebView, api: String, video: BaseProvider.VideoInfo): Call<Pair<String,Map<String, String>>> {
        val apis = api.split(" ")
        var url = apis.getOrNull(0)?:""
        val js = apis.getOrNull(1)?:""
        if(url.isEmpty())
            url = video.url
        else if(url.endsWith("="))
            url += video.url
        return ApiHelper.buildWebViewCall(webView, url, header, js)
    }

    override fun getDanmakuKey(video: BaseProvider.VideoInfo): Call<String> {
        return ApiHelper.buildHttpCall(video.url, header){
            val doc = it.body()?.string()?:""
            return@buildHttpCall Regex("""videoId: '([0-9]+)'""").find(doc)!!.groupValues[1]
        }
    }

    override fun getDanmaku(video: BaseProvider.VideoInfo, key: String, pos: Int): Call<Map<Int, List<BaseProvider.Danmaku>>> {
        val list = ArrayList<retrofit2.Call<Map<Int, List<BaseProvider.Danmaku>>>>()
        val pageStart = pos / 300 * 5
        for(i in 0..9)
            list.add(getDanmakuCall(key,pageStart + i))
        return ApiHelper.buildGroupCall(list.toTypedArray())
    }

    private fun getDanmakuCall(key: String, page: Int): retrofit2.Call<Map<Int, List<BaseProvider.Danmaku>>> {
        return ApiHelper.buildHttpCall("http://service.danmu.youku.com/list?jsoncallback=&mat=$page&mcount=1&ct=1001&iid=$key", header){
            val map = HashMap<Int, MutableList<BaseProvider.Danmaku>>()
            val result = JsonUtil.toJsonObject(it.body()?.string()?:"").getAsJsonArray("result")
            result.map{ it.asJsonObject}
                    .forEach {
                        val time = it.get("playat").asInt / 1000 //Integer.valueOf(info.selectFirst("showTime").text())
                        val property = it.get("propertis").asString

                        val color = "#" + if(property.contains("color")) String.format("%06x", JsonUtil.toJsonObject(property).get("color").asInt).toUpperCase() else "FFFFFF"//info.selectFirst("color").text().toUpperCase()
                        val context = it.get("content").asString
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
            map["User-Agent"] = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/64.0.3282.186 Safari/537.36"
            map
        }
    }
}