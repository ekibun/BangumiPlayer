package soko.ekibun.bangumi.ui.video

import android.annotation.SuppressLint
import android.graphics.Color
import android.preference.PreferenceManager
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
    private val danmakus = HashMap<BaseProvider.VideoInfo, HashSet<BaseProvider.DanmakuInfo>>()
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
    }

    @SuppressLint("SetTextI18n")
    fun updateValue(){
        val colorActive = ResourceUtil.resolveColorAttr(context, R.attr.colorPrimary)
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
            3 -> "不重叠"
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
        videoInfoCall = ApiHelper.buildGroupCall(infos.map{ ProviderModel.getVideoInfo(it, video)}.toTypedArray())
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