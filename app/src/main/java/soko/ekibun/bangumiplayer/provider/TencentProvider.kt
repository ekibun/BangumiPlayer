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
                            val id = it.get("id").asString
                            val videoInfo = BaseProvider.VideoInfo(
                                    id,
                                    siteId,
                                    it.get("playUrl").asString.split(".html?")[0] + "/$id.html")
                            Log.v("video", videoInfo.toString())
                            return@buildHttpCall videoInfo
                        } }
            throw Exception("not found")
        }
    }

    override fun getDanmakuKey(video: BaseProvider.VideoInfo): Call<String> {
        return ApiHelper.buildHttpCall("http://bullet.video.qq.com/fcgi-bin/target/regist?vid=${video.id}", header){
            val doc = Jsoup.parse(it.body()?.string()?:"")
            return@buildHttpCall doc.selectFirst("targetid").text()
        }
    }

    override fun getDanmaku(video: BaseProvider.VideoInfo, key: String, pos: Int): Call<List<BaseProvider.DanmakuInfo>> {
        val list = ArrayList<retrofit2.Call<List<BaseProvider.DanmakuInfo>>>()
        val pageStart = pos / 300 * 10
        for(i in 0..19)
            list.add(getDanmakuCall(key,pageStart + i))
        return ApiHelper.buildGroupCall(list.toTypedArray())
    }

    private fun getDanmakuCall(key: String, page: Int): retrofit2.Call<List<BaseProvider.DanmakuInfo>> {
        return ApiHelper.buildHttpCall("https://mfm.video.qq.com/danmu?timestamp=${page*30}&target_id=$key", header){
            val list = ArrayList<BaseProvider.DanmakuInfo>()
            val json = it.body()?.string()?: throw Exception("empty body")
            JsonUtil.toJsonObject(json).getAsJsonArray("comments")
                    ?.map { it.asJsonObject }?.forEach {
                        val time = it.get("timepoint")?.asFloat?:0f
                        val contentStyle = try { JsonUtil.toJsonObject(it.get("content_style")?.asString?:"") } catch(e: Exception) { JsonObject() }
                        val type = if(contentStyle.has("contentStyle")){
                            Log.v("position", contentStyle.get("position")?.asInt?.toString())
                            5
                        } else 1
                        val text = 25f //字体大小
                        val color = try{ Color.parseColor(contentStyle.get("color")?.asString?:"") } catch (e: Exception){ Color.WHITE } // 颜色
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