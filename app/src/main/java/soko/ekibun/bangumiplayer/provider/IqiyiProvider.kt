package soko.ekibun.bangumiplayer.provider

import android.util.Log
import org.jsoup.Jsoup
import retrofit2.Call
import soko.ekibun.bangumi.api.ApiHelper
import soko.ekibun.bangumi.api.bangumi.bean.Episode
import soko.ekibun.bangumi.ui.view.BackgroundWebView
import soko.ekibun.bangumi.util.JsonUtil
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.util.zip.Inflater

class IqiyiProvider: BaseProvider {
    override val siteId: Int = ProviderInfo.IQIYI
    override val name: String = "爱奇艺"
    override val color: Int = 0x00be06
    override val hasDanmaku: Boolean = true
    override val supportSearch: Boolean = true

    override fun search(key: String): Call<List<ProviderInfo>> {
        return ApiHelper.buildHttpCall("http://search.video.iqiyi.com/o?channel_name=动漫&if=html5&key=${java.net.URLEncoder.encode(key, "utf-8")}&pageSize=20&video_allow_3rd=0", header){
            val json = it.body()?.string()?: throw Exception("empty body")
            val ret = ArrayList<ProviderInfo>()
            JsonUtil.toJsonObject(json).getAsJsonObject("data")?.getAsJsonArray("docinfos")
                    ?.map { it.asJsonObject.getAsJsonObject("albumDocInfo") }
                    ?.filter { it.has("score") && "iqiyi" == it.get("siteId")?.asString }?.forEach {
                        ret.add(ProviderInfo(siteId, it.get("albumId").asString, 0f, it.get("albumTitle").asString))
                    }
            return@buildHttpCall ret
        }
    }

    override fun getVideoInfo(info: ProviderInfo, video: Episode): retrofit2.Call<BaseProvider.VideoInfo> {
        return ApiHelper.buildHttpCall("http://mixer.video.iqiyi.com/jp/mixin/videos/avlist?albumId=${info.id}&page=${(video.sort + info.offset).toInt()/100 + 1}&size=100", header){
            var json = it.body()?.string()?:""
            if (json.startsWith("var"))
                json = json.substring(json.indexOf('{'), json.lastIndexOf('}') + 1)
            JsonUtil.toJsonObject(json).getAsJsonArray("mixinVideos").map{it.asJsonObject}.forEach {
                if(it.get("order").asInt.toFloat() == video.sort + info.offset){
                    val videoInfo = BaseProvider.VideoInfo(
                            it.get("tvId").asString,
                            siteId,
                            it.get("url").asString
                    )
                    Log.v("video", videoInfo.toString())
                    return@buildHttpCall videoInfo
                } }
            throw Exception("not found")
        }
    }

    override fun getDanmakuKey(video: BaseProvider.VideoInfo): retrofit2.Call<String> {
        return ApiHelper.buildCall { "OK" }
    }

    override fun getDanmaku(video: BaseProvider.VideoInfo, key: String, pos: Int): retrofit2.Call<Map<Int, List<BaseProvider.Danmaku>>> {
        return ApiHelper.buildGroupCall(arrayOf(
                getDanmakuCall(video, pos / 300 + 1),
                getDanmakuCall(video, pos / 300 + 2)
        ))
    }

    private fun getDanmakuCall(video: BaseProvider.VideoInfo, page: Int): retrofit2.Call<Map<Int, List<BaseProvider.Danmaku>>> {
        return ApiHelper.buildHttpCall("http://cmts.iqiyi.com/bullet/${video.id.substring(video.id.length - 4, video.id.length - 2)}/${video.id.substring(video.id.length - 2, video.id.length)}/${video.id}_300_$page.z", header){
            val map: MutableMap<Int, MutableList<BaseProvider.Danmaku>> = HashMap()
            val xml = String(inflate(it.body()!!.bytes()))
            val doc = Jsoup.parse(xml)
            val infos = doc.select("bulletInfo")
            for (info in infos) {
                val time = Integer.valueOf(info.selectFirst("showTime").text())
                val color = "#" + info.selectFirst("color").text().toUpperCase()
                val context = info.selectFirst("content").text()
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

        fun inflate(data: ByteArray, nowrap: Boolean = false): ByteArray {
            var output: ByteArray

            val inflater = Inflater(nowrap)
            inflater.reset()
            inflater.setInput(data)

            val o = ByteArrayOutputStream(data.size)
            try {
                val buf = ByteArray(1024)
                while (!inflater.finished()) {
                    val i = inflater.inflate(buf)
                    o.write(buf, 0, i)
                }
                output = o.toByteArray()
            } catch (e: java.lang.Exception) {
                output = data
                e.printStackTrace()
            } finally {
                try {
                    o.close()
                } catch (e: IOException) {
                    e.printStackTrace()
                }
            }
            inflater.end()
            return output
        }
    }
}