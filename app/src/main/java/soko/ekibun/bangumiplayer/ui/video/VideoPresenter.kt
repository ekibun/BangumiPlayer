package soko.ekibun.bangumiplayer.ui.video

import android.annotation.SuppressLint
import android.content.pm.ActivityInfo
import android.graphics.Color
import android.support.constraint.ConstraintLayout
import android.support.design.widget.AppBarLayout
import android.support.design.widget.Snackbar
import android.view.View
import android.view.WindowManager
import com.google.android.exoplayer2.ExoPlaybackException
import kotlinx.android.synthetic.main.activity_video.*
import kotlinx.android.synthetic.main.video_player.*
import soko.ekibun.bangumi.api.bangumi.bean.Episode
import soko.ekibun.bangumi.api.bangumi.bean.Subject
import soko.ekibun.bangumiplayer.model.VideoModel
import soko.ekibun.bangumi.ui.view.BackgroundWebView
import soko.ekibun.bangumi.ui.view.VideoController
import soko.ekibun.bangumi.ui.view.controller.Controller
import soko.ekibun.bangumi.util.AppUtil
import soko.ekibun.bangumiplayer.model.ProviderModel
import soko.ekibun.bangumiplayer.provider.ProviderInfo
import java.util.*

class VideoPresenter(private val context: VideoActivity){

    val danmakuPresenter: DanmakuPresenter by lazy{
        DanmakuPresenter(context.danmaku_flame){
            loadDanmaku = true
        }
    }

    val webView: BackgroundWebView by lazy{ BackgroundWebView(context) }

    val controller: VideoController by lazy{
        VideoController(context.controller_frame, { action: Controller.Action, param: Any ->
            when (action) {
                Controller.Action.PLAY_PAUSE -> doPlayPause(!videoModel.player.playWhenReady)
                Controller.Action.FULLSCREEN ->{
                    context.requestedOrientation = if(context.systemUIPresenter.isLandscape) ActivityInfo.SCREEN_ORIENTATION_PORTRAIT else ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
                }
                Controller.Action.NEXT -> {
                    next?.let{doPlay(it)}
                    //context.viewpager.loadAv(nextAv)
                }
                Controller.Action.DANMAKU -> {
                    if(danmakuPresenter.view.isShown)
                        danmakuPresenter.view.hide() else danmakuPresenter.view.show()
                    controller.updateDanmaku(danmakuPresenter.view.isShown)
                }
                Controller.Action.SEEK_TO -> {
                    videoModel.player.seekTo(param as Long)
                    this.controller.updateProgress(videoModel.player.currentPosition)
                }
                Controller.Action.SHOW -> {
                    context.runOnUiThread{
                        updatePauseResume()
                        updateProgress()
                        context.item_mask.visibility = View.VISIBLE
                        context.toolbar.visibility = View.VISIBLE
                        if(context.systemUIPresenter.isLandscape)
                            context.systemUIPresenter.setSystemUiVisibility(SystemUIPresenter.Visibility.FULLSCREEN_IMMERSIVE)
                    }
                }
                Controller.Action.HIDE -> {
                    context.runOnUiThread{
                        context.item_mask.visibility = View.INVISIBLE
                        if(!context.systemUIPresenter.isLandscape || offset == 0)
                            context.toolbar.visibility = View.INVISIBLE
                        if(context.systemUIPresenter.isLandscape && offset == 0)
                            context.systemUIPresenter.setSystemUiVisibility(SystemUIPresenter.Visibility.FULLSCREEN)
                    }
                }
                Controller.Action.TITLE -> {
                    doPlayPause(false)
                    context.app_bar.setExpanded(false)
                    context.systemUIPresenter.appbarCollapsible(true)
                }
            }
        }, { context.systemUIPresenter.isLandscape })
    }

    val videoModel: VideoModel by lazy{
        VideoModel(context, object : VideoModel.Listener {
            override fun onReady(playWhenReady: Boolean) {
                if (!controller.ctrVisibility) {
                    controller.ctrVisibility = true
                    context.item_logcat.visibility = View.INVISIBLE
                    controller.doShowHide(false)
                }
                if (playWhenReady)
                    doPlayPause(true)
                if (!controller.isShow) {
                    context.item_mask.visibility = View.INVISIBLE
                    context.toolbar.visibility = View.INVISIBLE
                }
                controller.updateLoading(false)
            }

            override fun onBuffering() {
                danmakuPresenter.view.pause()
                controller.updateLoading(true)
            }

            override fun onEnded() {
                doPlayPause(false)
            }

            override fun onVideoSizeChange(width: Int, height: Int, unappliedRotationDegrees: Int, pixelWidthHeightRatio: Float) {
                context.video_surface.scaleX = Math.min(context.video_surface.measuredWidth.toFloat(), (context.video_surface.measuredHeight * width * pixelWidthHeightRatio / height)) / context.video_surface.measuredWidth
                context.video_surface.scaleY = Math.min(context.video_surface.measuredHeight.toFloat(), (context.video_surface.measuredWidth * height * pixelWidthHeightRatio / width)) / context.video_surface.measuredHeight
            }

            override fun onError(error: ExoPlaybackException) {
                exception = error.sourceException
                Snackbar.make(context.root_layout, exception.toString(), Snackbar.LENGTH_SHORT).show()
            }
        })
    }

    var offset = 0
    init{
        val screenSize = AppUtil.getScreenSize(context)
        val lp = context.player_container.layoutParams as ConstraintLayout.LayoutParams
        lp.dimensionRatio = "h,${screenSize.height}:${screenSize.width}"

        val lp_cf = context.controller_frame.layoutParams as ConstraintLayout.LayoutParams
        lp_cf.dimensionRatio = "h,${screenSize.height}:${screenSize.width}"

        context.app_bar.addOnOffsetChangedListener(AppBarLayout.OnOffsetChangedListener{ _, verticalOffset ->
            val visible = if(verticalOffset != 0 || controller.isShow || context.video_surface_container.visibility != View.VISIBLE) View.VISIBLE else View.INVISIBLE
            offset = verticalOffset
            if(context.systemUIPresenter.isLandscape && context.video_surface_container.visibility == View.VISIBLE && visible != context.toolbar.visibility)
                context.systemUIPresenter.setSystemUiVisibility(if(visible == View.VISIBLE) SystemUIPresenter.Visibility.FULLSCREEN_IMMERSIVE else SystemUIPresenter.Visibility.FULLSCREEN)
            if(context.systemUIPresenter.isLandscape && context.video_surface_container.visibility == View.VISIBLE){
                if(offset == 0){
                    context.window.statusBarColor = Color.TRANSPARENT
                    context.window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)
                }else{
                    context.window.setFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS, WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)
                }
            }
            context.toolbar.visibility = visible
        })
    }

    private var loadVideoInfo: Boolean? = null
        set(v) {
            field = v
            parseLogcat()
        }
    private var loadVideo: Boolean? = null
        set(v) {
            field = v
            parseLogcat()
        }
    private var loadDanmaku: Boolean? = null
        set(v) {
            field = v
            parseLogcat()
        }
    private var exception: Exception? = null
        set(v) {
            field = v
            parseLogcat()
        }
    @SuppressLint("SetTextI18n")
    private fun parseLogcat(){
        context.runOnUiThread{
            if(loadVideoInfo == false || loadVideo == false || exception != null)
                controller.updateLoading(false)
            context.item_logcat.text = "获取视频信息…" + if(loadVideoInfo == null) "" else (
                    if(loadVideoInfo != true) "【失败】" else ("【完成】" +
                            "\n解析视频地址…${if(loadVideo == null) "" else if(loadVideo == true) "【完成】" else "【失败】"}" +
                            "\n全舰弹幕装填…${if(loadDanmaku == null) "" else if(loadDanmaku == true) "【完成】" else "【失败】"}" +
                            if(loadVideo == true) "\n开始视频缓冲…" else "")) +
                    if(exception != null) "【失败】\n$exception" else ""
        }
    }

    var doPlay: (Int)->Unit = {}
    var next: Int? = null
    var prev: Int? = null
    //var videoCall: Call<BaseProvider.VideoInfo>? = null
    fun play(episode: Episode, subject: Subject, info: ProviderInfo, infos: List<ProviderInfo>){
        context.systemUIPresenter.appbarCollapsible(false)
        loadVideoInfo = null
        loadVideo = null
        loadDanmaku = null
        exception = null
        controller.updateNext(next != null)
        videoModel.player.playWhenReady = false
        controller.updateLoading(true)
        context.video_surface_container.visibility = View.VISIBLE
        context.video_surface.visibility = View.VISIBLE
        context.controller_frame.visibility = View.VISIBLE
        controller.ctrVisibility = false
        context.item_logcat.visibility = View.VISIBLE
        controller.doShowHide(true)
        controller.setTitle(episode.parseSort() + " - " + if(episode.name_cn.isNullOrEmpty()) episode.name else episode.name_cn)
        playLoopTask?.cancel()
        //context.nested_scroll.tag = true
        danmakuPresenter.view.pause()
        //videoCall?.cancel()
        webView.loadUrl("about:blank")
        videoModel.getVideo(episode, subject, webView, {
            loadVideoInfo = it
            if(loadVideoInfo == true)
            context.runOnUiThread { danmakuPresenter.loadDanmaku(infos.filter { it.loadDanmaku && ProviderModel.getProvider(it.siteId)?.hasDanmaku == true }, episode) }
        },{request, useCache ->
            if(useCache != null) loadVideo = request != null
            if(request != null && useCache != null) videoModel.play(request, context.video_surface, useCache)
        })
       /* videoCall = ProviderModel.getVideoInfo(info, episode)
        videoCall?.enqueue(ApiHelper.buildCallback(context,{video->
            context.runOnUiThread { ParserModel.getVideo(webView, video, info.parser?: ParserInfo("", "")).enqueue(ApiHelper.buildCallback(context, {
                context.runOnUiThread{
                    it?.let{videoModel.play(it, context.video_surface)}
                    loadVideo = true
                } }, {})) }
            context.runOnUiThread { danmakuPresenter.loadDanmaku(infos.filter { it.loadDanmaku && ProviderModel.getProvider(it.siteId)?.hasDanmaku == true }, episode) }
        },{ loadVideoInfo = it == null }))
        */
    }

    private var playLoopTask: TimerTask? = null
    fun doPlayPause(play: Boolean){
        videoModel.player.playWhenReady = play
        updatePauseResume()
        playLoopTask?.cancel()
        if(play){
            playLoopTask = object: TimerTask(){ override fun run() {
                updateProgress()
                danmakuPresenter.add(videoModel.player.currentPosition)
                if(danmakuPresenter.view.isShown && !danmakuPresenter.view.isPaused){
                    danmakuPresenter.view.start(videoModel.player.currentPosition)
                }
            } }
            controller.timer.schedule(playLoopTask, 0, 1000)
            context.video_surface.keepScreenOn = true
            if(!controller.isShow)context.toolbar.visibility = View.INVISIBLE
            danmakuPresenter.view.resume()
        }else{
            context.video_surface.keepScreenOn = false
            danmakuPresenter.view.pause()
        }
        context.systemUIPresenter.appbarCollapsible(!play)
    }

    private fun updateProgress(){
        controller.duration = videoModel.player.duration.toInt() /10
        controller.buffedPosition = videoModel.player.bufferedPosition.toInt() /10
        controller.updateProgress(videoModel.player.currentPosition)
    }

    private fun updatePauseResume() {
        controller.updatePauseResume(videoModel.player.playWhenReady)
        context.setPictureInPictureParams(!videoModel.player.playWhenReady)
    }
}