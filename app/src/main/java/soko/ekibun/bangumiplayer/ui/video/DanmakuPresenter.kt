package soko.ekibun.bangumiplayer.ui.video

import android.graphics.Color
import master.flame.danmaku.danmaku.model.BaseDanmaku
import master.flame.danmaku.danmaku.model.IDisplayer
import master.flame.danmaku.danmaku.model.android.DanmakuContext
import master.flame.danmaku.ui.widget.DanmakuView
import retrofit2.Call
import soko.ekibun.bangumi.api.ApiHelper
import soko.ekibun.bangumi.api.bangumi.bean.Episode
import soko.ekibun.bangumiplayer.model.ProviderModel
import soko.ekibun.bangumiplayer.provider.BaseProvider
import soko.ekibun.bangumiplayer.provider.ProviderInfo
import master.flame.danmaku.danmaku.model.android.Danmakus
import master.flame.danmaku.danmaku.parser.BaseDanmakuParser

class DanmakuPresenter(val view: DanmakuView,
                       private val onFinish:(Throwable?)->Unit){
    private val danmakus = HashMap<BaseProvider.VideoInfo, HashSet<BaseProvider.DanmakuInfo>>()
    private val danmakuContext by lazy { DanmakuContext.create() }
    private val parser by lazy { object : BaseDanmakuParser() {
        override fun parse(): Danmakus {
            return Danmakus()
        } } }

    init{
        val maxLinesPair = HashMap<Int, Int>()
        val overlappingEnablePair = HashMap<Int, Boolean>()
        maxLinesPair[BaseDanmaku.TYPE_SCROLL_RL] = 20
        overlappingEnablePair[BaseDanmaku.TYPE_SCROLL_LR] = true
        overlappingEnablePair[BaseDanmaku.TYPE_FIX_BOTTOM] = true

        danmakuContext.setDanmakuStyle(IDisplayer.DANMAKU_STYLE_STROKEN, 3f)
                .setDuplicateMergingEnabled(true)
                .setScrollSpeedFactor(1.2f)
                .setScaleTextSize(0.8f)
                .setMaximumLines(maxLinesPair)
                .preventOverlapping(overlappingEnablePair)
        view.prepare(parser, danmakuContext)
        view.enableDanmakuDrawingCache(true)
    }

    private var videoInfoCall: Call<BaseProvider.VideoInfo>? = null
    private val danmakuCalls: ArrayList<Call<String>> = ArrayList()
    private val danmakuKeys: HashMap<BaseProvider.VideoInfo, String> = HashMap()
    fun loadDanmaku(infos: List<ProviderInfo>, video: Episode){
        danmakus.clear()
        view.removeAllDanmakus(true)
        danmakuCalls.forEach { it.cancel() }
        danmakuCalls.clear()
        danmakuKeys.clear()
        videoInfoCall?.cancel()
        videoInfoCall = ApiHelper.buildGroupCall(infos.map{ProviderModel.getVideoInfo(it, video)}.toTypedArray())
        videoInfoCall?.enqueue(ApiHelper.buildCallback(view.context, {videoInfo->
            val call = ProviderModel.getDanmakuKey(videoInfo)
            call.enqueue(ApiHelper.buildCallback(view.context, {
                danmakuKeys[videoInfo] = it
                doAdd(Math.max(lastPos, 0) * 1000L * 300L, videoInfo, it)
            }, {}))
            danmakuCalls.add(call)
        }, {}))
    }

    fun doAdd(pos: Long, videoInfo: BaseProvider.VideoInfo, key: String){
        ProviderModel.getDanmaku(videoInfo, key, (pos / 1000).toInt()).enqueue(ApiHelper.buildCallback(view.context, {
            val set = danmakus.getOrPut(videoInfo){ HashSet() }
            val oldSet = set.toList()
            set.addAll(it)
            set.minus(oldSet).forEach {
                val danmaku = danmakuContext.mDanmakuFactory.createDanmaku(it.type, danmakuContext)?: return@forEach
                danmaku.time = (it.time * 1000).toLong()
                danmaku.textSize = it.textSize * (parser.displayer.density - 0.6f)
                danmaku.textColor = it.color
                danmaku.textShadowColor = if (it.color <= Color.BLACK) Color.WHITE else Color.BLACK
                danmaku.text = it.context
                view.addDanmaku(danmaku)
            }
        }, {onFinish(it)}))
    }

    private var lastPos = -1
    fun add(pos:Long){
        val newPos = (pos/1000).toInt() / 300
        if(lastPos == -1 || lastPos != newPos){
            lastPos = newPos
            danmakuKeys.forEach { videoInfo, key ->
                doAdd(pos, videoInfo, key)
            }
        }
    }
}