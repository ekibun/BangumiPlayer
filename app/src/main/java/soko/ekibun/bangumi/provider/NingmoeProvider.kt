package soko.ekibun.bangumi.provider

import android.util.Log
import okhttp3.MediaType
import okhttp3.RequestBody
import retrofit2.Call
import soko.ekibun.bangumi.api.ApiHelper
import soko.ekibun.bangumi.api.bangumi.bean.Episode
import soko.ekibun.bangumi.ui.view.BackgroundWebView
import soko.ekibun.bangumi.util.JsonUtil
import java.text.DecimalFormat

class NingmoeProvider: BaseProvider {
    override val siteId: Int = ProviderInfo.NINGMOE
    override val name: String = "柠萌"
    override val color: Int = 0xff3e63
    override val hasDanmaku: Boolean = false
    override val supportSearch: Boolean = true
    override val provideVideo: Boolean = false

    override fun search(key: String): Call<List<ProviderInfo>> {
        return ApiHelper.buildHttpCall("https://www.ningmoe.com/api/search", header, RequestBody.create(MediaType.parse("application/json;charset=utf-8"),
                "{\"token\":null,\"keyword\":\"$key\",\"type\":\"anime\",\"bangumi_type\":\"\",\"page\":1,\"limit\":10}")) {
            val json = it.body()?.string()?: throw Exception("empty body")
            val ret = ArrayList<ProviderInfo>()
            JsonUtil.toJsonObject(json).getAsJsonArray("data")?.map{ it.asJsonObject }?.forEach {
                val classification = it.getAsJsonObject("classification")
                val title = classification.get("cn_name")?.asString?.let{ if(it.isEmpty()) null else it }?: { classification.get("en_name")?.asString?:"" }()
                val vid = it.get("bangumi_id").asString
                ret += ProviderInfo(siteId, vid, 0f, title)
            }
            return@buildHttpCall ret
        }
    }

    override fun getVideoInfo(info: ProviderInfo, video: Episode): Call<BaseProvider.VideoInfo> {
        val url = "https://www.ningmoe.com/bangumi/detail/${info.id}/${DecimalFormat("#.##").format(video.sort + info.offset)}/home"
        val videoInfo = BaseProvider.VideoInfo(info.id, siteId, url)
        Log.v("video", videoInfo.toString())
        return ApiHelper.buildCall{ videoInfo }
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