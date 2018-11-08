package soko.ekibun.bangumiplayer.ui.video

import android.content.res.Configuration
import android.graphics.Color
import android.os.Build
import android.support.design.widget.AppBarLayout
import android.util.Log
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
                }
                else setSystemUiVisibility(Visibility.IMMERSIVE)
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

    fun onWindowModeChanged(isInMultiWindowMode: Boolean, newConfig: Configuration?) {
        if (newConfig?.orientation == Configuration.ORIENTATION_LANDSCAPE && (Build.VERSION.SDK_INT <24 || !isInMultiWindowMode)) {
            isLandscape = true
            if(!showMask)setSystemUiVisibility(SystemUIPresenter.Visibility.FULLSCREEN)
        }else if (newConfig?.orientation == Configuration.ORIENTATION_PORTRAIT){
            isLandscape = false
            setSystemUiVisibility(SystemUIPresenter.Visibility.IMMERSIVE)
        }
    }

    var isLandscape = false
    var showMask = false
    fun setSystemUiVisibility(visibility: Visibility){
        showMask = visibility == Visibility.FULLSCREEN_IMMERSIVE
        Log.v("visibility", visibility.name)
        when(visibility){
            SystemUIPresenter.Visibility.FULLSCREEN -> {
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
            SystemUIPresenter.Visibility.IMMERSIVE -> {
                context.root_layout.fitsSystemWindows=true
                context.app_bar.fitsSystemWindows=true
                context.video_container.fitsSystemWindows=false
                context.controller_frame_container.fitsSystemWindows=false
                context.toolbar_layout.fitsSystemWindows = true
                context.window.setFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS, WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)

                context.window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LAYOUT_STABLE or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
            }
            SystemUIPresenter.Visibility.FULLSCREEN_IMMERSIVE -> {
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