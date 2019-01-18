package soko.ekibun.bangumi.provider

import android.graphics.Color
import android.util.Log
import org.jsoup.Jsoup
import retrofit2.Call
import soko.ekibun.bangumi.api.ApiHelper
import soko.ekibun.bangumi.api.bangumi.bean.Episode
import soko.ekibun.bangumi.ui.view.BackgroundWebView
import soko.ekibun.bangumi.util.JsonUtil

class PptvProvider: BaseProvider {
    override val siteId: Int = ProviderInfo.PPTV
    override val name: String = "PPTV"
    override val color: Int = 0x00a0e9
    override val hasDanmaku: Boolean = true
    override val supportSearch: Boolean = true

    override fun search(key: String): Call<List<ProviderInfo>> {
        return ApiHelper.buildHttpCall("http://search.pptv.com/result?search_query=${java.net.URLEncoder.encode(key, "utf-8")}&result_type=3", header){
            val html = it.body()?.string()?: throw Exception("empty body")
            val doc = Jsoup.parse(html)
            val ret = ArrayList<ProviderInfo>()
            doc.select(".video-info")?.forEach {
                val title = it.selectFirst(".video-title")
                val vid = Regex("""/([^/.]+).html""").find(title?.attr("href")?:"")?.groupValues?.get(1) ?: return@forEach
                ret += ProviderInfo(siteId, vid, 0f, title?.text() ?: "")
            }
            return@buildHttpCall ret
        }
    }

    override fun getVideoInfo(info: ProviderInfo, video: Episode): Call<BaseProvider.VideoInfo> {
        return ApiHelper.buildBridgeCall(ApiHelper.buildHttpCall("http://v.pptv.com/page/${info.id}.html"){
            return@buildHttpCall Regex(""""id":([0-9]*),""").find(it.body()?.string()?:"")?.groupValues?.get(1) ?: throw Exception("cannot get id")
        }) {
            return@buildBridgeCall ApiHelper.buildHttpCall("http://apis.web.pptv.com/show/videoList?format=jsonp&pid=$it", header){
                val src = JsonUtil.toJsonObject(it.body()?.string()?:"")
                src.get("data").asJsonObject.get("list").asJsonArray?.map{it.asJsonObject}?.forEach {
                    if(it.get("title").asString.toFloatOrNull() == video.sort + info.offset){
                        val videoInfo = BaseProvider.VideoInfo(
                                it.get("id").asString,
                                siteId,
                                it.get("url").asString
                        )
                        Log.v("video", videoInfo.toString())
                        return@buildHttpCall videoInfo
                    } }
                throw Exception("not found")
            }
        }
    }

    override fun getDanmakuKey(video: BaseProvider.VideoInfo): Call<String> {
        return ApiHelper.buildCall { "OK" }
    }

    override fun getDanmaku(video: BaseProvider.VideoInfo, key: String, pos: Int): Call<List<BaseProvider.DanmakuInfo>> {
        val list = ArrayList<retrofit2.Call<List<BaseProvider.DanmakuInfo>>>()
        val pageStart = pos / 300 * 3
        for(i in 0..5)
            list.add(getDanmakuCall(video, pageStart + i))
        return ApiHelper.buildGroupCall(list.toTypedArray())
    }

    private fun getDanmakuCall(video: BaseProvider.VideoInfo, page: Int): retrofit2.Call<List<BaseProvider.DanmakuInfo>> {
        return ApiHelper.buildHttpCall("http://apicdn.danmu.pptv.com/danmu/v4/pplive/ref/vod_${video.id}/danmu?pos=${page* 1000}", header){
            val list = ArrayList<BaseProvider.DanmakuInfo>()
            val json = it.body()?.string()?: throw Exception("empty body")
            JsonUtil.toJsonObject(json).getAsJsonObject("data")?.getAsJsonArray("infos")
                    ?.map { it.asJsonObject }?.filter { it.get("id").asLong != 0L }?.forEach {
                        val time = (it.get("play_point")?.asFloat?:0f) / 10
                        val type = when {// 弹幕类型
                            it.get("motion")?.asInt?:0 == 0 -> 1
                            it.get("font_position")?.asInt?:0 == 300 -> 4
                            else -> 5 }
                        val text = 25f //字体大小
                        val color = try{ Color.parseColor(it.get("font_color")?.asString?:"") } catch (e: Exception){ Color.WHITE } // 颜色
                        val context =  Jsoup.parse(it.get("content")?.asString?:"").text()
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
            map["Cookie"] = "PUID=616193bbc9804a7dde16-12845cf9388b; __crt=1474886744633; ppi=302c3638; Hm_lvt_7adaa440f53512a144c13de93f4c22db=1475285458,1475556666,1475752293,1475913662"
            map
        }
    }
}