package soko.ekibun.bangumiplayer.ui.video

import android.util.Log
import retrofit2.Call
import soko.ekibun.bangumi.api.ApiHelper
import soko.ekibun.bangumi.api.bangumi.bean.Episode
import soko.ekibun.bangumi.ui.view.DanmakuView
import soko.ekibun.bangumiplayer.model.ProviderModel
import soko.ekibun.bangumiplayer.provider.BaseProvider
import soko.ekibun.bangumiplayer.provider.ProviderInfo

class DanmakuPresenter(val view: DanmakuView,
                          private val onFinish:(Throwable?)->Unit){
    private val danmakus = HashMap<BaseProvider.VideoInfo, HashMap<Int, List<BaseProvider.Danmaku>>>()

    var videoInfoCall: Call<BaseProvider.VideoInfo>? = null
    val danmakuCalls: ArrayList<Call<String>> = ArrayList()
    val danmakuKeys: HashMap<BaseProvider.VideoInfo, String> = HashMap()
    fun loadDanmaku(infos: List<ProviderInfo>, video: Episode){
        Log.v("infos", infos.toString())
        danmakus.clear()
        view.clear()
        danmakuCalls.forEach { it.cancel() }
        danmakuCalls.clear()
        danmakuKeys.clear()
        videoInfoCall?.cancel()
        videoInfoCall = ApiHelper.buildGroupCall(infos.map{ProviderModel.getVideoInfo(it, video)}.toTypedArray())
        videoInfoCall?.enqueue(ApiHelper.buildCallback(view.context, {videoInfo->
            val call = ProviderModel.getDanmakuKey(videoInfo)
            call.enqueue(ApiHelper.buildCallback(view.context, {
                danmakuKeys[videoInfo] = it
                add(Math.max(lastPos, 0) * 1000L * 300L, videoInfo, it)
            }, {}))
            danmakuCalls.add(call)
        }, {}))
    }

    fun add(pos: Long, videoInfo: BaseProvider.VideoInfo, key: String){
        ProviderModel.getDanmaku(videoInfo, key, (pos / 1000).toInt()).enqueue(ApiHelper.buildCallback(view.context, {
            danmakus.getOrPut(videoInfo){ HashMap() }.putAll(it)
        }, {onFinish(it)}))
    }

    private var lastPos = -1
    fun add(pos:Long){
        val newPos = (pos/1000).toInt() / 300
        if(lastPos == -1 || lastPos != newPos){
            lastPos = newPos
            danmakuKeys.forEach { videoInfo, key ->
                add(pos, videoInfo, key)
            }
        }
        danmakuKeys.forEach { videoInfo, _ ->
            view.add(danmakus[videoInfo]?.get((pos/1000).toInt())?: ArrayList())
        }
    }
}