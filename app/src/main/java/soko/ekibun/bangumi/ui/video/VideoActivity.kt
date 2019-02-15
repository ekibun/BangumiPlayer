package soko.ekibun.bangumi.ui.video

import android.Manifest
import android.app.PendingIntent
import android.app.PictureInPictureParams
import android.app.RemoteAction
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.graphics.drawable.Icon
import android.os.Build
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.view.KeyEvent
import android.view.Menu
import android.view.MenuItem
import android.view.View
import kotlinx.android.synthetic.main.activity_video.*
import soko.ekibun.bangumi.api.bangumi.bean.AccessToken
import soko.ekibun.bangumi.api.bangumi.bean.Subject
import soko.ekibun.bangumi.util.JsonUtil
import soko.ekibun.bangumiplayer.R
import android.content.*
import android.util.Log
import android.webkit.CookieManager
import android.webkit.WebView
import android.widget.Toast
import kotlinx.android.synthetic.main.subject_episode.*
import org.jsoup.Jsoup
import soko.ekibun.bangumi.api.ApiHelper
import soko.ekibun.bangumi.api.bangumi.Bangumi
import soko.ekibun.bangumi.api.bangumi.bean.Episode
import soko.ekibun.bangumi.model.ThemeModel
import soko.ekibun.bangumi.util.AppUtil
import soko.ekibun.bangumi.util.StorageUtil
import soko.ekibun.bangumi.service.DownloadService
import soko.ekibun.bangumi.util.Bridge


class VideoActivity : AppCompatActivity() {
    val videoPresenter: VideoPresenter by lazy { VideoPresenter(this) }
    val systemUIPresenter: SystemUIPresenter by lazy{ SystemUIPresenter(this) }
    val subjectPresenter: SubjectPresenter by lazy{ SubjectPresenter(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_video)

        ThemeModel.setTheme(this, ThemeModel(Bridge.getContext(this)).getTheme())
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        val cookieManager = CookieManager.getInstance()
        val cookie  = intent.getStringExtra(VideoActivity.EXTRA_COOKIE)?:""
        var needReload = false
        cookie.split("; ").filterNot { it.isEmpty() }.forEach {
            cookieManager.setCookie(Bangumi.SERVER, it) }
        ApiHelper.buildHttpCall(Bangumi.SERVER, mapOf("User-Agent" to ua)){
            val doc = Jsoup.parse(it.body()?.string()?:"")
            if(doc.selectFirst(".guest") != null) return@buildHttpCall null
            it.headers("set-cookie").forEach {
                cookieManager.setCookie(Bangumi.SERVER, it)
                needReload = true }
            doc.selectFirst("input[name=formhash]")?.attr("value")
        }.enqueue(ApiHelper.buildCallback(this, { hash->
            if(hash.isNullOrEmpty()) return@buildCallback
            formhash = hash?:formhash
            if(needReload) subjectPresenter.refreshSubject()
        }))

        systemUIPresenter.init()

        registerReceiver(receiver, IntentFilter(ACTION_MEDIA_CONTROL + subjectPresenter.subject.id))
        registerReceiver(downloadReceiver, IntentFilter(DownloadService.getBroadcastAction(subjectPresenter.subject)))
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.action_subject, menu)
        return true
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        onMultiWindowModeChanged((Build.VERSION.SDK_INT >=24 && isInMultiWindowMode), newConfig)
        super.onConfigurationChanged(newConfig)
    }

    override fun onMultiWindowModeChanged(isInMultiWindowMode: Boolean, newConfig: Configuration?) {
        systemUIPresenter.onWindowModeChanged(isInMultiWindowMode, (Build.VERSION.SDK_INT >=24 && isInPictureInPictureMode), newConfig)
        if(video_surface_container.visibility == View.VISIBLE)
            videoPresenter.controller.doShowHide(false)
        super.onMultiWindowModeChanged(isInMultiWindowMode, newConfig)
    }

    private val downloadReceiver = object: BroadcastReceiver(){
        override fun onReceive(context: Context, intent: Intent) {
            try{
                val episode = JsonUtil.toEntity(intent.getStringExtra(DownloadService.EXTRA_EPISODE), Episode::class.java)!!
                val percent = intent.getFloatExtra(DownloadService.EXTRA_PERCENT, Float.NaN)
                val bytes = intent.getLongExtra(DownloadService.EXTRA_BYTES, 0L)

                val index = subjectPresenter.subjectView.episodeDetailAdapter.data.indexOfFirst { it.t?.id == episode.id }
                subjectPresenter.subjectView.episodeDetailAdapter.getViewByPosition(episode_detail_list, index, R.id.item_layout)?.let{
                    subjectPresenter.subjectView.episodeDetailAdapter.updateDownload(it, percent, bytes, intent.getBooleanExtra(DownloadService.EXTRA_CANCEL, true), !intent.hasExtra(DownloadService.EXTRA_CANCEL))
                }

                val epIndex = subjectPresenter.subjectView.episodeAdapter.data.indexOfFirst { it.id == episode.id }
                subjectPresenter.subjectView.episodeAdapter.getViewByPosition(episode_list, epIndex, R.id.item_layout)?.let{
                    subjectPresenter.subjectView.episodeAdapter.updateDownload(it, percent, bytes, intent.getBooleanExtra(DownloadService.EXTRA_CANCEL, true), !intent.hasExtra(DownloadService.EXTRA_CANCEL))
                }
            }catch (e: Exception){
                e.printStackTrace()
            }
        }
    }

    private val receiver = object: BroadcastReceiver(){
        override fun onReceive(context: Context, intent: Intent) {
            when(intent.getIntExtra(EXTRA_CONTROL_TYPE,0)){
                CONTROL_TYPE_PAUSE ->{
                    videoPresenter.doPlayPause(false)
                }
                CONTROL_TYPE_PLAY ->{
                    videoPresenter.doPlayPause(true)
                }
                CONTROL_TYPE_NEXT ->
                    videoPresenter.nextEpisode()?.let{videoPresenter.playEpisode(it)}
                CONTROL_TYPE_PREV ->
                    videoPresenter.prevEpisode()?.let{videoPresenter.playEpisode(it)}
            }
        }
    }

    public override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        if(systemUIPresenter.isLandscape && videoPresenter.videoModel.player.playWhenReady && Build.VERSION.SDK_INT >= 24) {
            @Suppress("DEPRECATION") enterPictureInPictureMode()
            setPictureInPictureParams(false)
        }
    }

    fun setPictureInPictureParams(playPause: Boolean){
        if(Build.VERSION.SDK_INT >= 26) {
            val actionPrev = RemoteAction(Icon.createWithResource(this, R.drawable.ic_prev), getString(R.string.next_video), getString(R.string.next_video),
                    PendingIntent.getBroadcast(this, CONTROL_TYPE_PREV, Intent(ACTION_MEDIA_CONTROL + subjectPresenter.subject.id).putExtra(EXTRA_CONTROL_TYPE,
                            CONTROL_TYPE_PREV), PendingIntent.FLAG_UPDATE_CURRENT))
            actionPrev.isEnabled = videoPresenter.prevEpisode() != null
            val actionNext = RemoteAction(Icon.createWithResource(this, R.drawable.ic_next), getString(R.string.next_video), getString(R.string.next_video),
                    PendingIntent.getBroadcast(this, CONTROL_TYPE_NEXT, Intent(ACTION_MEDIA_CONTROL + subjectPresenter.subject.id).putExtra(EXTRA_CONTROL_TYPE,
                            CONTROL_TYPE_NEXT), PendingIntent.FLAG_UPDATE_CURRENT))
            actionNext.isEnabled = videoPresenter.nextEpisode() != null
            try{
                setPictureInPictureParams(PictureInPictureParams.Builder().setActions(listOf(
                        actionPrev,
                        RemoteAction(Icon.createWithResource(this, if (playPause) R.drawable.ic_play else R.drawable.ic_pause), getString(R.string.play_pause), getString(R.string.play_pause),
                                PendingIntent.getBroadcast(this, CONTROL_TYPE_PLAY, Intent(ACTION_MEDIA_CONTROL + subjectPresenter.subject.id).putExtra(EXTRA_CONTROL_TYPE,
                                        if (playPause) CONTROL_TYPE_PLAY else CONTROL_TYPE_PAUSE), PendingIntent.FLAG_UPDATE_CURRENT)),
                        actionNext
                )).build())
            }catch(e: Exception){ }
        }
    }

    val ua by lazy { WebView(this).settings.userAgentString }
    var formhash = ""
    var pauseOnStop = false
    override fun onStart() {
        super.onStart()
        if(videoPresenter.videoModel.player.duration >0 && pauseOnStop)
            videoPresenter.doPlayPause(true)
        pauseOnStop = false
    }

    override fun onStop() {
        super.onStop()
        if(videoPresenter.videoModel.player.playWhenReady)
            pauseOnStop = true
        videoPresenter.doPlayPause(false)
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(receiver)
        unregisterReceiver(downloadReceiver)
    }

    //back
    private fun processBack(){
        when {
            systemUIPresenter.isLandscape -> requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
            Build.VERSION.SDK_INT > 23 && isInMultiWindowMode -> Toast.makeText(this, "请先退出多窗口模式", Toast.LENGTH_SHORT).show()
            episode_detail_list.visibility == View.VISIBLE -> subjectPresenter.subjectView.showEpisodeDetail(false)
            else -> finish()
        }
    }
    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        if (keyCode == KeyEvent.KEYCODE_BACK){
            processBack()
            return true
        }
        return super.onKeyDown(keyCode, event)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> processBack()
            R.id.action_share -> {
                AppUtil.shareString(this, subjectPresenter.subject.name + " " + subjectPresenter.subject.url)
            }
            R.id.action_refresh ->{
                subjectPresenter.refreshSubject()
            }
        }
        return super.onOptionsItemSelected(item)
    }

    private fun checkStorage(): Boolean{
        if (Build.VERSION.SDK_INT >= 23 && checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE), REQUEST_STORAGE_CODE)
            return false
        }
        return true
    }

    private var loadFileCallback:((String?)-> Unit)? = null
    fun loadFile(callback:(String?)-> Unit){
        loadFileCallback = callback
        if (!checkStorage()) return
        val intent = Intent()
        intent.type = "video/*"
        intent.action = Intent.ACTION_GET_CONTENT
        startActivityForResult(intent, REQUEST_FILE_CODE)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_STORAGE_CODE) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED && loadFileCallback != null) {
                loadFile(loadFileCallback!!)
            } else {
                loadFileCallback?.invoke(null)
                loadFileCallback = null
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_FILE_CODE && resultCode == RESULT_OK) {//文件
            val uri = data?.data?: return
            val path = StorageUtil.getRealPathFromUri(this, uri)
            Log.v("path", path.toString())
            loadFileCallback?.invoke(path)
        }
    }

    companion object {
        const val EXTRA_SUBJECT = "extraSubject"
        const val EXTRA_COOKIE = "extraCookie"
        const val ACTION_MEDIA_CONTROL = "bangumiActionMediaControl"
        const val EXTRA_CONTROL_TYPE = "extraControlType"
        const val CONTROL_TYPE_PAUSE = 1
        const val CONTROL_TYPE_PLAY = 2
        const val CONTROL_TYPE_NEXT = 3
        const val CONTROL_TYPE_PREV = 4

        private const val REQUEST_STORAGE_CODE = 1
        private const val REQUEST_FILE_CODE = 2

        fun startActivity(context: Context, subject: Subject, newTask:Boolean = false) {
            context.startActivity(parseIntent(context, subject, newTask))
        }

        fun parseIntent(context: Context, subject: Subject, newTask:Boolean = true): Intent {
            val intent = Intent(context, VideoActivity::class.java)
            if(newTask) intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_MULTIPLE_TASK
            intent.putExtra(EXTRA_SUBJECT, JsonUtil.toJson(subject))
            return intent
        }
    }
}
