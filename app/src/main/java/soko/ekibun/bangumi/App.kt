package soko.ekibun.bangumi

import android.app.Application
import android.content.Context
import soko.ekibun.bangumi.model.ThemeModel
import soko.ekibun.bangumi.util.CrashHandler
import soko.ekibun.bangumi.model.VideoCacheModel
import soko.ekibun.bangumi.util.Bridge

class App: Application(){
    private val videoCacheModel by lazy { VideoCacheModel(this) }

    override fun onCreate() {
        super.onCreate()
        ThemeModel.setTheme(this, ThemeModel(Bridge.getContext(this)).getTheme())
        Thread.setDefaultUncaughtExceptionHandler(CrashHandler(this))
    }

    companion object {
        fun getVideoCacheModel(context: Context): VideoCacheModel {
            val app = context.applicationContext as App
            return app.videoCacheModel
        }
    }
}