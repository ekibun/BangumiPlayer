package soko.ekibun.bangumiplayer.provider

import android.util.Log
import org.jsoup.Jsoup
import retrofit2.Call
import soko.ekibun.bangumi.api.ApiHelper
import soko.ekibun.bangumi.api.bangumi.bean.Episode
import soko.ekibun.bangumi.ui.view.BackgroundWebView
import soko.ekibun.bangumi.util.JsonUtil

class TencentProvider: BaseProvider {
    override val siteId: Int = ProviderInfo.TENCENT
    override val name: String = "腾讯视频"
    override val color: Int = 0xff820f
    override val hasDanmaku: Boolean = true
    override val supportSearch: Boolean = true

    override fun search(key: String): Call<List<ProviderInfo>> {
        return ApiHelper.buildHttpCall("http://m.v.qq.com/search.html?keyWord=${java.net.URLEncoder.encode(key, "utf-8")}", header){
            val html = it.body()?.string()?: throw Exception("empty body")
            val doc = Jsoup.parse(html)
            val ret = ArrayList<ProviderInfo>()
            doc.select(".search_item")?.filter { it.selectFirst(".mask_scroe") != null && it.selectFirst(".figure_source")?.text()?.contains("腾讯") ?: false }
                    ?.forEach {
                        val genre = it.selectFirst(".figure_genre").text()
                        var title = it.selectFirst(".figure_title").text()
                        val region = Regex("""\[([^]]+)] """).find(title)?.groupValues?.get(1) ?: ""
                        if (!genre.contains("动画") && !region.contains("动漫"))
                            return@forEach
                        val url = it.selectFirst(".figure").attr("href")
                        val vid = Regex("""/([^/.]+).html""").find(url)?.groupValues?.get(1) ?: url
                        title = title.substringAfter(" ")
                        ret += ProviderInfo(siteId, vid, 0f, title)
                    }
            return@buildHttpCall ret
        }
    }

    override fun getVideoInfo(info: ProviderInfo, video: Episode): Call<BaseProvider.VideoInfo> {
        return ApiHelper.buildHttpCall("https://s.video.qq.com/get_playsource?id=${info.id}&type=4&range=${(video.sort + info.offset).toInt()}-${(video.sort + info.offset).toInt()+1}&otype=json", header){
            var json = it.body()?.string()?:""
            json = json.substring(json.indexOf('{'), json.lastIndexOf('}') + 1)
            JsonUtil.toJsonObject(json).getAsJsonObject("PlaylistItem")
                    .getAsJsonArray("videoPlayList").map{it.asJsonObject}.forEach {
                        if(it.get("episode_number").asString.toFloatOrNull() == video.sort + info.offset && it.get("type").asString == "1"){
                            val videoInfo = BaseProvider.VideoInfo(
                                    it.get("id").asString,
                                    siteId,
                                    it.get("playUrl").asString)
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
            url = video.url.split(".html?")[0] + "/${video.id}.html"
        else if(url.endsWith("="))
            url += video.url.split(".html?")[0] + "/${video.id}.html"
        return ApiHelper.buildWebViewCall(webView, url, header, js)
    }

    override fun getDanmakuKey(video: BaseProvider.VideoInfo): Call<String> {
        return ApiHelper.buildHttpCall("http://bullet.video.qq.com/fcgi-bin/target/regist?vid=${video.id}", header){
            val doc = Jsoup.parse(it.body()?.string()?:"")
            return@buildHttpCall doc.selectFirst("targetid").text()
        }
    }

    override fun getDanmaku(video: BaseProvider.VideoInfo, key: String, pos: Int): Call<Map<Int, List<BaseProvider.Danmaku>>> {
        val list = ArrayList<retrofit2.Call<Map<Int, List<BaseProvider.Danmaku>>>>()
        val pageStart = pos / 300 * 10
        for(i in 0..19)
            list.add(getDanmakuCall(key,pageStart + i))
        return ApiHelper.buildGroupCall(list.toTypedArray())
    }

    private fun getDanmakuCall(key: String, page: Int): retrofit2.Call<Map<Int, List<BaseProvider.Danmaku>>> {
        return ApiHelper.buildHttpCall("https://mfm.video.qq.com/danmu?timestamp=${page*30}&target_id=$key", header){
            val map: MutableMap<Int, MutableList<BaseProvider.Danmaku>> = HashMap()
            val result = JsonUtil.toJsonObject(it.body()?.string()?:"").getAsJsonArray("comments")
            result.map{ it.asJsonObject}
                    .forEach {
                        val time = it.get("timepoint").asInt
                        val color = "#" + (it.get("bb_bcolor")?.asString?.replace("0x","")?:"FFFFFF")
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