package soko.ekibun.bangumi.ui.video

import android.content.res.Configuration
import android.graphics.Color
import android.os.Build
import android.support.design.widget.AppBarLayout
import android.view.View
import android.view.WindowManager
import kotlinx.android.synthetic.main.activity_video.*

class SystemUIPresenter(private val context: VideoActivity){
    fun init(){
        setSystemUiVisibility(Visibility.IMMERSIVE)
    }

    init{
        if(Build.VERSION.SDK_INT >= 28)
            context.window.attributes.layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
        context.window.decorView.setOnSystemUiVisibilityChangeListener{
            if(it == 0)
                if(isLandscape) {
                    if(!showMask) setSystemUiVisibility(Visibility.FULLSCREEN)
                    else setSystemUiVisibility(Visibility.FULLSCREEN_IMMERSIVE)
                } else setSystemUiVisibility(Visibility.IMMERSIVE)
        }
    }

    fun appbarCollapsible(enable:Boolean){
        //context.nested_scroll.tag = true
        if(enable){
            //reactive appbar
            val params = context.toolbar_layout.layoutParams as AppBarLayout.LayoutParams
            params.scrollFlags = AppBarLayout.LayoutParams.SCROLL_FLAG_SCROLL or AppBarLayout.LayoutParams.SCROLL_FLAG_EXIT_UNTIL_COLLAPSED
            context.toolbar_layout.layoutParams = params
        }else{
            //expand appbar
            context.app_bar.setExpanded(true)
            (context.toolbar_layout.layoutParams as AppBarLayout.LayoutParams).scrollFlags = 0
            context.toolbar_layout.isTitleEnabled = false
        }
    }

    fun onWindowModeChanged(isInMultiWindowMode: Boolean, isInPictureInPictureMode: Boolean, newConfig: Configuration?) {
        isLandscape = isInPictureInPictureMode || (newConfig?.orientation != Configuration.ORIENTATION_PORTRAIT && !isInMultiWindowMode)
        if (isLandscape) {
            if(!showMask) setSystemUiVisibility(Visibility.FULLSCREEN)
            else setSystemUiVisibility(Visibility.FULLSCREEN_IMMERSIVE)
        }else {
            setSystemUiVisibility(Visibility.IMMERSIVE)
        }
        context.videoPresenter.showDanmakuSetting(false)
        context.videoPresenter.danmakuPresenter.sizeScale = when{
            isInPictureInPictureMode -> 0.7f
            isLandscape -> 1.1f
            else-> 0.8f
        }
    }
    var isLandscape = false
    var showMask = false
    fun setSystemUiVisibility(visibility: Visibility){
        showMask = visibility == Visibility.FULLSCREEN_IMMERSIVE
        when(visibility){
            Visibility.FULLSCREEN -> {
                context.root_layout.fitsSystemWindows=true
                context.app_bar.fitsSystemWindows=true
                context.video_container.fitsSystemWindows=true
                context.toolbar_layout.fitsSystemWindows = true
                context.controller_frame_container.fitsSystemWindows=true
                context.window.statusBarColor = Color.TRANSPARENT
                //context.window.navigationBarColor = Color.TRANSPARENT
                context.window.decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION // hide nav bar
                        or View.SYSTEM_UI_FLAG_FULLSCREEN // hide status bar
                        //or View.SYSTEM_UI_FLAG_IMMERSIVE
                        or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY)
            }
            Visibility.IMMERSIVE -> {
                context.root_layout.fitsSystemWindows=true
                context.app_bar.fitsSystemWindows=true
                context.video_container.fitsSystemWindows=false
                context.controller_frame_container.fitsSystemWindows=false
                context.toolbar_layout.fitsSystemWindows = true
                context.window.setFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS, WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)

                context.window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LAYOUT_STABLE or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
            }
            Visibility.FULLSCREEN_IMMERSIVE -> {
                context.root_layout.fitsSystemWindows=true
                context.app_bar.fitsSystemWindows=true
                context.video_container.fitsSystemWindows=true
                context.toolbar_layout.fitsSystemWindows = true
                context.controller_frame_container.fitsSystemWindows=true

                context.window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LAYOUT_STABLE or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
            }
        }
        //context.controller_frame.layoutParams.height = context.video_container.height
        context.toolbar_layout.post{
            context.toolbar_layout.requestLayout()
        }
    }

    enum class Visibility{
        FULLSCREEN, IMMERSIVE, FULLSCREEN_IMMERSIVE
    }
}