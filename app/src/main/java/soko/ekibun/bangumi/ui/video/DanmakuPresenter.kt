package soko.ekibun.bangumi.ui.video

import android.annotation.SuppressLint
import android.graphics.Color
import android.preference.PreferenceManager
import android.support.v7.widget.LinearLayoutManager
import android.view.View
import android.widget.SeekBar
import master.flame.danmaku.danmaku.model.BaseDanmaku
import master.flame.danmaku.danmaku.model.IDisplayer
import master.flame.danmaku.danmaku.model.android.DanmakuContext
import master.flame.danmaku.ui.widget.DanmakuView
import retrofit2.Call
import soko.ekibun.bangumi.api.ApiHelper
import soko.ekibun.bangumi.api.bangumi.bean.Episode
import soko.ekibun.bangumi.model.ProviderModel
import soko.ekibun.bangumi.provider.BaseProvider
import soko.ekibun.bangumi.provider.ProviderInfo
import master.flame.danmaku.danmaku.model.android.Danmakus
import master.flame.danmaku.danmaku.parser.BaseDanmakuParser
import kotlinx.android.synthetic.main.danmaku_setting.*
import soko.ekibun.bangumi.util.ResourceUtil
import soko.ekibun.bangumiplayer.R

class DanmakuPresenter(val view: DanmakuView, val context: VideoActivity,
                       private val onFinish:(Throwable?)->Unit){
    private val sp by lazy { PreferenceManager.getDefaultSharedPreferences(context) }
    private val danmakuContext by lazy { DanmakuContext.create() }
    private val parser by lazy { object : BaseDanmakuParser() {
        override fun parse(): Danmakus {
            return Danmakus()
        } } }
    var sizeScale = 0.8f
        set(value) {
            field = value
            updateValue()
        }
    private val adapter =  DanmakuListAdapter()

    init{
        val overlappingEnablePair = HashMap<Int, Boolean>()
        overlappingEnablePair[BaseDanmaku.TYPE_SCROLL_LR] = true
        overlappingEnablePair[BaseDanmaku.TYPE_FIX_BOTTOM] = true
        BaseDanmaku.TYPE_MOVEABLE_XXX
        danmakuContext.setDanmakuStyle(IDisplayer.DANMAKU_STYLE_STROKEN, 3f)
                .setDuplicateMergingEnabled(true)
                .preventOverlapping(overlappingEnablePair)
        view.prepare(parser, danmakuContext)
        view.enableDanmakuDrawingCache(true)

        updateValue()
        val seekBarChange = object: SeekBar.OnSeekBarChangeListener{
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                if(!fromUser) return
                when(seekBar){
                    context.danmaku_opac_seek -> sp.edit().putInt(DANMAKU_OPACITY, progress).apply()
                    context.danmaku_size_seek -> sp.edit().putInt(DANMAKU_SIZE, progress + 50).apply()
                    context.danmaku_loc_seek -> sp.edit().putInt(DANMAKU_LOCATION, progress).apply()
                    context.danmaku_speed_seek -> sp.edit().putInt(DANMAKU_SPEED, progress).apply()
                }
                updateValue()
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        }
        context.danmaku_opac_seek.setOnSeekBarChangeListener(seekBarChange)
        context.danmaku_size_seek.setOnSeekBarChangeListener(seekBarChange)
        context.danmaku_loc_seek.setOnSeekBarChangeListener(seekBarChange)
        context.danmaku_speed_seek.setOnSeekBarChangeListener(seekBarChange)

        val onClick = View.OnClickListener{ view: View ->
            val key = when(view){
                context.danmaku_top -> DANMAKU_ENABLE_TOP
                context.danmaku_scroll -> DANMAKU_ENABLE_SCROLL
                context.danmaku_bottom -> DANMAKU_ENABLE_BOTTOM
                context.danmaku_special -> DANMAKU_ENABLE_SPECIAL
                else -> return@OnClickListener
            }
            sp.edit().putBoolean(key, !sp.getBoolean(key, true)).apply()
            updateValue()
        }
        context.danmaku_top.setOnClickListener(onClick)
        context.danmaku_scroll.setOnClickListener(onClick)
        context.danmaku_bottom.setOnClickListener(onClick)
        context.danmaku_special.setOnClickListener(onClick)

        val onClickVideoFrame = View.OnClickListener{ view: View ->
            val key = when(view){
                context.video_frame_auto -> VIDEO_FRAME_AUTO
                context.video_frame_stretch -> VIDEO_FRAME_STRENTCH
                context.video_frame_fill -> VIDEO_FRAME_FILL
                context.video_frame_16_9 -> VIDEO_FRAME_16_9
                context.video_frame_4_3 -> VIDEO_FRAME_4_3
                else -> return@OnClickListener
            }
            sp.edit().putInt(VIDEO_FRAME, key).apply()
            context.videoPresenter.resizeVideoSurface()
            updateValue()
        }
        context.video_frame_auto.setOnClickListener(onClickVideoFrame)
        context.video_frame_stretch.setOnClickListener(onClickVideoFrame)
        context.video_frame_fill.setOnClickListener(onClickVideoFrame)
        context.video_frame_16_9.setOnClickListener(onClickVideoFrame)
        context.video_frame_4_3.setOnClickListener(onClickVideoFrame)

        context.item_danmaku_list.isNestedScrollingEnabled = false
        context.item_danmaku_list.layoutManager = LinearLayoutManager(context)
        context.item_danmaku_list.adapter = adapter
    }

    @SuppressLint("SetTextI18n")
    fun updateValue(){
        val colorActive = ResourceUtil.resolveColorAttr(context, R.attr.colorPrimary)
        //videoFrame
        val videoFrame = sp.getInt(VIDEO_FRAME, VIDEO_FRAME_AUTO)
        context.video_frame_auto.setTextColor(if(videoFrame == VIDEO_FRAME_AUTO) colorActive else Color.WHITE)
        context.video_frame_stretch.setTextColor(if(videoFrame == VIDEO_FRAME_STRENTCH) colorActive else Color.WHITE)
        context.video_frame_fill.setTextColor(if(videoFrame == VIDEO_FRAME_FILL) colorActive else Color.WHITE)
        context.video_frame_16_9.setTextColor(if(videoFrame == VIDEO_FRAME_16_9) colorActive else Color.WHITE)
        context.video_frame_4_3.setTextColor(if(videoFrame == VIDEO_FRAME_4_3) colorActive else Color.WHITE)
        //block
        danmakuContext.ftDanmakuVisibility = sp.getBoolean(DANMAKU_ENABLE_TOP, true)
        danmakuContext.r2LDanmakuVisibility = sp.getBoolean(DANMAKU_ENABLE_SCROLL, true)
        danmakuContext.l2RDanmakuVisibility = sp.getBoolean(DANMAKU_ENABLE_SCROLL, true)
        danmakuContext.fbDanmakuVisibility = sp.getBoolean(DANMAKU_ENABLE_BOTTOM, true)
        danmakuContext.SpecialDanmakuVisibility = sp.getBoolean(DANMAKU_ENABLE_SPECIAL, true)
        context.danmaku_top.setTextColor(if(danmakuContext.ftDanmakuVisibility) colorActive else Color.WHITE)
        context.danmaku_scroll.setTextColor(if(danmakuContext.r2LDanmakuVisibility) colorActive else Color.WHITE)
        context.danmaku_bottom.setTextColor(if(danmakuContext.fbDanmakuVisibility) colorActive else Color.WHITE)
        context.danmaku_special.setTextColor(if(danmakuContext.SpecialDanmakuVisibility) colorActive else Color.WHITE)
        //opacity
        context.danmaku_opac_seek.progress = sp.getInt(DANMAKU_OPACITY, 100)
        context.danmaku_opac_value.text = "${context.danmaku_opac_seek.progress}%"
        danmakuContext.setDanmakuTransparency(context.danmaku_opac_seek.progress / 100f)
        //size
        context.danmaku_size_seek.progress = sp.getInt(DANMAKU_SIZE, 100) - 50
        context.danmaku_size_value.text = "${context.danmaku_size_seek.progress + 50}%"
        danmakuContext.setScaleTextSize(sizeScale * (context.danmaku_size_seek.progress + 50) / 100f)
        //location
        val maxLinesPair = HashMap<Int, Int>()
        context.danmaku_loc_seek.progress = sp.getInt(DANMAKU_LOCATION, 4)
        context.danmaku_loc_value.text = when(context.danmaku_loc_seek.progress) {
            0 -> "1/4屏"
            1 -> "半屏"
            2 -> "3/4屏"
            3 -> "满屏"
            else -> "无限"
        }
        maxLinesPair[BaseDanmaku.TYPE_SCROLL_RL] = Math.ceil(view.height / (50 * sizeScale * (context.danmaku_size_seek.progress + 50) / 100.0) * when(context.danmaku_loc_seek.progress) {
            0 -> 0.25
            1 -> 0.5
            2 -> 0.75
            3 -> 1.0
            else -> 1000.0
        }).toInt()
        danmakuContext.setMaximumLines(maxLinesPair)
        //speed
        context.danmaku_speed_seek.progress = sp.getInt(DANMAKU_SPEED, 2)
        context.danmaku_speed_value.text = when(context.danmaku_speed_seek.progress) {
            0 -> "极慢"
            1 -> "较慢"
            2 -> "适中"
            3 -> "较快"
            else -> "极快"
        }
        danmakuContext.setScrollSpeedFactor(1.2f * when(context.danmaku_speed_seek.progress) {
            0 -> 2f
            1 -> 1.5f
            2 -> 1f
            3 -> 0.75f
            else -> 0.5f
        })
    }
    companion object {
        const val DANMAKU_OPACITY = "danmakuOpacity"
        const val DANMAKU_SIZE = "danmakuSize"
        const val DANMAKU_SPEED = "danmakuSpeed"
        const val DANMAKU_LOCATION = "danmakuLocation"

        const val DANMAKU_ENABLE_TOP = "danmakuEnableTop"
        const val DANMAKU_ENABLE_SCROLL = "danmakuEnableScroll"
        const val DANMAKU_ENABLE_BOTTOM = "danmakuEnableBottom"
        const val DANMAKU_ENABLE_SPECIAL = "danmakuEnableSpecial"

        const val VIDEO_FRAME = "videoFrame"
        const val VIDEO_FRAME_AUTO = 0
        const val VIDEO_FRAME_STRENTCH = 1
        const val VIDEO_FRAME_FILL = 2
        const val VIDEO_FRAME_16_9 = 3
        const val VIDEO_FRAME_4_3 = 4
    }

    private val videoInfoCalls = ArrayList<Call<BaseProvider.VideoInfo>>()
    private val danmakuCalls: ArrayList<Call<String>> = ArrayList()
    private val danmakuKeys: HashMap<DanmakuListAdapter.DanmakuInfo, String> = HashMap()
    fun loadDanmaku(infos: List<ProviderInfo>, video: Episode){
        view.removeAllDanmakus(true)
        danmakuCalls.forEach { it.cancel() }
        danmakuCalls.clear()
        danmakuKeys.clear()

        videoInfoCalls.forEach { it.cancel() }
        videoInfoCalls.clear()

        adapter.setNewData(infos.map{ DanmakuListAdapter.DanmakuInfo(it) })
        adapter.data.forEach {
            loadDanmaku(it, video)
        }
        adapter.setOnItemClickListener { _, _, position ->
            loadDanmaku(adapter.data[position], video)
        }
    }

    private fun loadDanmaku(danmakuInfo: DanmakuListAdapter.DanmakuInfo, video: Episode){
        when {
            danmakuInfo.videoInfo == null -> {
                danmakuInfo.info = " 获取视频信息..."
                context.runOnUiThread {  adapter.notifyItemChanged(adapter.data.indexOf(danmakuInfo)) }
                val videoCall = ProviderModel.getVideoInfo(danmakuInfo.provider, video)
                videoInfoCalls.add(videoCall)
                videoCall.enqueue(ApiHelper.buildCallback(view.context, {videoInfo->
                    danmakuInfo.videoInfo = videoInfo
                    loadDanmaku(danmakuInfo, video)
                }, {
                    danmakuInfo.info = it?.message?.let{" 获取视频信息出错: $it"}?:""
                    context.runOnUiThread {  adapter.notifyItemChanged(adapter.data.indexOf(danmakuInfo)) }
                }))
            }
            danmakuInfo.key == null -> {
                danmakuInfo.info = " 获取弹幕信息..."
                context.runOnUiThread {  adapter.notifyItemChanged(adapter.data.indexOf(danmakuInfo)) }
                val call = ProviderModel.getDanmakuKey(danmakuInfo.videoInfo?:return)
                danmakuCalls.add(call)
                call.enqueue(ApiHelper.buildCallback(view.context, {
                    danmakuInfo.key = it
                    doAdd(Math.max(lastPos, 0) * 1000L * 300L, danmakuInfo)
                }, {
                    danmakuInfo.info = it?.message?.let{" 获取弹幕信息出错: $it"}?:""
                    context.runOnUiThread { adapter.notifyItemChanged(adapter.data.indexOf(danmakuInfo)) }
                }))
            }
            else -> doAdd(Math.max(lastPos, 0) * 1000L * 300L, danmakuInfo)
        }
    }

    private fun doAdd(pos: Long, danmakuInfo: DanmakuListAdapter.DanmakuInfo){
        val call = ProviderModel.getDanmaku(danmakuInfo.videoInfo?:return, danmakuInfo.key?:return, (pos / 1000).toInt())
        danmakuInfo.info = " 加载弹幕..."
        context.runOnUiThread {  adapter.notifyItemChanged(adapter.data.indexOf(danmakuInfo)) }
        call.enqueue(ApiHelper.buildCallback(view.context, {
            val oldSet = danmakuInfo.danmakus.toList()
            danmakuInfo.danmakus.addAll(it)
            danmakuInfo.danmakus.minus(oldSet).forEach {
                val danmaku = danmakuContext.mDanmakuFactory.createDanmaku(it.type, danmakuContext)?: return@forEach
                danmaku.time = (it.time * 1000).toLong()
                danmaku.textSize = it.textSize * (parser.displayer.density - 0.6f)
                danmaku.textColor = it.color
                danmaku.textShadowColor = if (it.color <= Color.BLACK) Color.WHITE else Color.BLACK
                danmaku.text = it.context
                view.addDanmaku(danmaku)
            }
            context.runOnUiThread {  adapter.notifyItemChanged(adapter.data.indexOf(danmakuInfo)) }
        }, {
            danmakuInfo.info = it?.message?.let{" 加载弹幕出错: $it"}?:""
            context.runOnUiThread {  adapter.notifyItemChanged(adapter.data.indexOf(danmakuInfo)) }
            onFinish(it)}))
    }

    private var lastPos = -1
    fun add(pos:Long){
        val newPos = (pos/1000).toInt() / 300
        if(lastPos == -1 || lastPos != newPos){
            lastPos = newPos
            adapter.data.forEach { doAdd(pos, it) }
        }
    }
}