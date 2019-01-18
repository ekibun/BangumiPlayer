package soko.ekibun.bangumi.provider

import android.util.Log
import com.google.gson.JsonArray
import retrofit2.Call
import soko.ekibun.bangumi.api.ApiHelper
import soko.ekibun.bangumi.api.bangumi.bean.Episode
import soko.ekibun.bangumi.ui.view.BackgroundWebView
import soko.ekibun.bangumi.util.JsonUtil

class AcfunProvider: BaseProvider {
    override val siteId: Int = ProviderInfo.ACFUN
    override val name: String = "AcFun"
    override val color: Int = 0xfd4c5b
    override val hasDanmaku: Boolean = true
    override val supportSearch: Boolean = true
    override val provideVideo: Boolean = true

    override fun search(key: String): Call<List<ProviderInfo>> {
        return ApiHelper.buildHttpCall("http://search.aixifan.com/search?q=${java.net.URLEncoder.encode(key, "utf-8")}", header){
            val json = it.body()?.string()?: throw Exception("empty body")
            val ret = ArrayList<ProviderInfo>()
            JsonUtil.toJsonObject(json).getAsJsonObject("data")?.getAsJsonObject("page")?.getAsJsonArray("ai")
                    ?.map { it.asJsonObject }?.forEach {
                        ret.add(ProviderInfo(siteId, it.get("contentId").asString, 0f, it.get("title").asString))
                    }
            return@buildHttpCall ret
        }
    }

    override fun getVideoInfo(info: ProviderInfo, video: Episode): Call<BaseProvider.VideoInfo> {
        //http://www.acfun.cn/album/abm/bangumis/video?albumId=
        return ApiHelper.buildHttpCall("http://www.acfun.cn/album/abm/bangumis/video?albumId=${info.id}&num=${(video.sort + info.offset).toInt()/20 + 1}&size=20", header){
            val json = it.body()?.string()?: throw Exception("empty body")
            JsonUtil.toJsonObject(json).getAsJsonObject("data")?.getAsJsonArray("content")?.map{it.asJsonObject}?.forEach {
                if(it.get("sort").asInt.toFloat() / 10 == video.sort + info.offset){
                    val video0 = it.getAsJsonArray("videos")[0].asJsonObject
                    val videoInfo = BaseProvider.VideoInfo(
                            video0.get("danmakuId").asString,
                            siteId,
                            "http://m.acfun.cn/v/?ab=${video0.get("albumId").asString}#${video0.get("groupId").asString}-${video0.get("videoId").asString}"
                    )
                    Log.v("video", videoInfo.toString())
                    return@buildHttpCall videoInfo
                } }
            throw Exception("not found")
        }
    }

    override fun getDanmakuKey(video: BaseProvider.VideoInfo): Call<String> {
        return ApiHelper.buildCall { "OK" }
    }

    override fun getDanmaku(video: BaseProvider.VideoInfo, key: String, pos: Int): Call<List<BaseProvider.DanmakuInfo>> {
        return getDanmakuCall(video, ArrayList(), 0)
    }

    private fun getDanmakuCall(video: BaseProvider.VideoInfo, list: ArrayList<BaseProvider.DanmakuInfo>, pos: Long): Call<List<BaseProvider.DanmakuInfo>> {
        return ApiHelper.buildBridgeCall<List<BaseProvider.DanmakuInfo>, List<BaseProvider.DanmakuInfo>>(ApiHelper.buildHttpCall("http://danmu.aixifan.com/V4/${video.id}/$pos/1000/", header) {
            val sublist = ArrayList<BaseProvider.DanmakuInfo>()
            val json = it.body()?.string()?: throw Exception("empty body")
            JsonUtil.toEntity(json, JsonArray::class.java)?.get(2)?.asJsonArray?.map { it.asJsonObject }?.forEach {
                val p = it.get("c").asString.split(",")
                val time = p.getOrNull(0)?.toFloatOrNull()?:0f //出现时间
                val type = p.getOrNull(2)?.toIntOrNull()?:1 // 弹幕类型
                val text = p.getOrNull(3)?.toFloatOrNull()?:25f //字体大小
                val color = (0x00000000ff000000 or (p.getOrNull(1)?.toLongOrNull()?:0L) and 0x00000000ffffffff).toInt() // 颜色
                val date = p.getOrNull(5)?.toLongOrNull()?:0L //创建时间
                val context =  it.get("m").asString
                val danmaku = BaseProvider.DanmakuInfo(time, type, text, color, context, date)
                Log.v("danmaku", danmaku.toString())
                sublist += danmaku
            }
            return@buildHttpCall sublist
        }){
            list.addAll(it)
            val newPos = it.lastOrNull()?.timeStamp?: return@buildBridgeCall ApiHelper.buildCall { list }
            if(newPos == pos) return@buildBridgeCall ApiHelper.buildCall { list }
            return@buildBridgeCall getDanmakuCall(video, list, newPos)
        }
    }

    override fun getVideo(webView: BackgroundWebView, video: BaseProvider.VideoInfo): Call<Pair<String, Map<String, String>>> {
        return ApiHelper.buildWebViewCall(webView, video.url, header, "") { request, _ ->
            request.url.toString().contains("//player.acfun.cn/route_m3u8")
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