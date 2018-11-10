package soko.ekibun.bangumiplayer.provider

import android.util.Log
import com.google.gson.JsonArray
import retrofit2.Call
import soko.ekibun.bangumi.api.ApiHelper
import soko.ekibun.bangumi.api.bangumi.bean.Episode
import soko.ekibun.bangumi.ui.view.BackgroundWebView
import soko.ekibun.bangumi.util.JsonUtil
import kotlin.math.roundToInt

class AcfunProvider: BaseProvider {
    override val siteId: Int = ProviderInfo.ACFUN
    override val name: String = "AcFun"
    override val color: Int = 0xfd4c5b
    override val hasDanmaku: Boolean = true
    override val supportSearch: Boolean = true

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

    override fun getDanmaku(video: BaseProvider.VideoInfo, key: String, pos: Int): Call<Map<Int, List<BaseProvider.Danmaku>>> {
        //http://danmu.aixifan.com/V4/6373781/0/1000/
        return ApiHelper.buildHttpCall("http://danmu.aixifan.com/V4/${video.id}/0/1000/", header) {
            val map: MutableMap<Int, MutableList<BaseProvider.Danmaku>> = HashMap()
            val json = it.body()?.string()?: throw Exception("empty body")
            JsonUtil.toEntity(json, JsonArray::class.java)?.get(2)?.asJsonArray?.map { it.asJsonObject }?.forEach {
                val p = it.get("c").asString.split(",")
                val time = p.getOrNull(0)?.toFloat()?.roundToInt()?:0
                val color = "#" + String.format("%06x",  p.getOrNull(3)?.toLong()).toUpperCase()
                val context =  it.get("m").asString
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
            //map["User-Agent"] = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/64.0.3282.186 Safari/537.36"
            map
        }
    }
}