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
import android.os.Bundle
import android.view.KeyEvent
import android.view.Menu
import android.view.MenuItem
import android.view.View
import kotlinx.android.synthetic.main.activity_video.*
import soko.ekibun.bangumi.api.bangumi.bean.Subject
import soko.ekibun.bangumi.util.JsonUtil
import soko.ekibun.bangumiplayer.R
import android.content.*
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.util.Log
import android.view.animation.AnimationUtils
import android.webkit.CookieManager
import android.webkit.WebView
import android.widget.Toast
import com.oushangfeng.pinnedsectionitemdecoration.PinnedHeaderItemDecoration
import kotlinx.android.synthetic.main.subject_episode.*
import kotlinx.android.synthetic.main.subject_episode.view.*
import soko.ekibun.bangumi.api.bangumi.Bangumi
import soko.ekibun.bangumi.api.bangumi.bean.Episode
import soko.ekibun.bangumi.model.ThemeModel
import soko.ekibun.bangumi.util.AppUtil
import soko.ekibun.bangumi.util.StorageUtil
import soko.ekibun.bangumi.service.DownloadService
import soko.ekibun.bangumi.ui.view.SwipeBackActivity
import soko.ekibun.bangumi.util.Bridge


class VideoActivity : SwipeBackActivity() {
    val videoPresenter: VideoPresenter by lazy { VideoPresenter(this) }
    val systemUIPresenter: SystemUIPresenter by lazy{ SystemUIPresenter(this) }
    //val subjectPresenter: SubjectPresenter by lazy{ SubjectPresenter(this) }

    val cookieManager by lazy { CookieManager.getInstance() }

    val videoPagerAdapter by lazy { VideoPagerAdapter(this, this.supportFragmentManager, item_pager) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_video)

        val cookie = intent?.getStringExtra(EXTRA_COOKIE)?:""
        if(cookie.isNotEmpty()) cookie.split("; ").forEach {
            cookieManager.setCookie(Bangumi.SERVER, it)
        }

        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        systemUIPresenter.init()

        videoPagerAdapter.init(JsonUtil.toEntity(intent.getStringExtra(EXTRA_SUBJECT), Subject::class.java)!!)

        val swipeTouchListener = View.OnTouchListener{ v, _ ->
            if((v as? RecyclerView)?.canScrollHorizontally(1) == true || (v as? RecyclerView)?.canScrollHorizontally(-1) == true)
                shouldCancelActivity = false
            false
        }
        videoPagerAdapter.subjectFragment.detail.episode_list.setOnTouchListener(swipeTouchListener)
        videoPagerAdapter.subjectFragment.detail.season_list.setOnTouchListener(swipeTouchListener)

        episode_detail_list.adapter = videoPagerAdapter.subjectFragment.episodeDetailAdapter
        episode_detail_list.addItemDecoration(PinnedHeaderItemDecoration.Builder(videoPagerAdapter.subjectFragment.episodeDetailAdapter.sectionHeader).create())
        episode_detail_list.layoutManager = LinearLayoutManager(this)

        item_close.setOnClickListener {
            showEpisodeDetail(false)
        }

        registerReceiver(receiver, IntentFilter(ACTION_MEDIA_CONTROL + videoPagerAdapter.subject.id))
        registerReceiver(downloadReceiver, IntentFilter(DownloadService.getBroadcastAction(videoPagerAdapter.subject)))
    }

    fun showEpisodeDetail(show: Boolean){
        episode_detail_list_header.visibility = if(show) View.VISIBLE else View.INVISIBLE
        episode_detail_list_header.animation = AnimationUtils.loadAnimation(this, if(show) R.anim.move_in_bottom else R.anim.move_out_bottom)
        episode_detail_list.visibility = if(show) View.VISIBLE else View.INVISIBLE
        episode_detail_list.animation = AnimationUtils.loadAnimation(this, if(show) R.anim.move_in_bottom else R.anim.move_out_bottom)
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

                val index = videoPagerAdapter.subjectFragment.episodeDetailAdapter.data.indexOfFirst { it.t?.id == episode.id }
                videoPagerAdapter.subjectFragment.episodeDetailAdapter.getViewByPosition(episode_detail_list, index, R.id.item_layout)?.let{
                    videoPagerAdapter.subjectFragment.episodeDetailAdapter.updateDownload(it, percent, bytes, intent.getBooleanExtra(DownloadService.EXTRA_CANCEL, true), !intent.hasExtra(DownloadService.EXTRA_CANCEL))
                }

                val epIndex = videoPagerAdapter.subjectFragment.episodeAdapter.data.indexOfFirst { it.id == episode.id }
                videoPagerAdapter.subjectFragment.episodeAdapter.getViewByPosition(episode_list, epIndex, R.id.item_layout)?.let{
                    videoPagerAdapter.subjectFragment.episodeAdapter.updateDownload(it, percent, bytes, intent.getBooleanExtra(DownloadService.EXTRA_CANCEL, true), !intent.hasExtra(DownloadService.EXTRA_CANCEL))
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
                    PendingIntent.getBroadcast(this, CONTROL_TYPE_PREV, Intent(ACTION_MEDIA_CONTROL + videoPagerAdapter.subject.id).putExtra(EXTRA_CONTROL_TYPE,
                            CONTROL_TYPE_PREV), PendingIntent.FLAG_UPDATE_CURRENT))
            actionPrev.isEnabled = videoPresenter.prevEpisode() != null
            val actionNext = RemoteAction(Icon.createWithResource(this, R.drawable.ic_next), getString(R.string.next_video), getString(R.string.next_video),
                    PendingIntent.getBroadcast(this, CONTROL_TYPE_NEXT, Intent(ACTION_MEDIA_CONTROL + videoPagerAdapter.subject.id).putExtra(EXTRA_CONTROL_TYPE,
                            CONTROL_TYPE_NEXT), PendingIntent.FLAG_UPDATE_CURRENT))
            actionNext.isEnabled = videoPresenter.nextEpisode() != null
            try{
                setPictureInPictureParams(PictureInPictureParams.Builder().setActions(listOf(
                        actionPrev,
                        RemoteAction(Icon.createWithResource(this, if (playPause) R.drawable.ic_play else R.drawable.ic_pause), getString(R.string.play_pause), getString(R.string.play_pause),
                                PendingIntent.getBroadcast(this, CONTROL_TYPE_PLAY, Intent(ACTION_MEDIA_CONTROL + videoPagerAdapter.subject.id).putExtra(EXTRA_CONTROL_TYPE,
                                        if (playPause) CONTROL_TYPE_PLAY else CONTROL_TYPE_PAUSE), PendingIntent.FLAG_UPDATE_CURRENT)),
                        actionNext
                )).build())
            }catch(e: Exception){ }
        }
    }

    val ua by lazy { WebView(this).settings.userAgentString }
    val formhash get()= videoPagerAdapter.subject.formhash?:""
    var pauseOnStop = false
    override fun onStart() {
        super.onStart()
        if(videoPresenter.videoModel.player.duration >0 && pauseOnStop)
            videoPresenter.doPlayPause(true)
        pauseOnStop = false

        videoPagerAdapter.refreshSubject()
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

    override fun processBack(){
        if(systemUIPresenter.isLandscape || videoPresenter.videoModel.player.playWhenReady || episode_detail_list.visibility == View.VISIBLE) return
        super.processBack()
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        if (keyCode == KeyEvent.KEYCODE_BACK){
            when {
                systemUIPresenter.isLandscape -> requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                Build.VERSION.SDK_INT > 23 && isInMultiWindowMode -> Toast.makeText(this, "请先退出多窗口模式", Toast.LENGTH_SHORT).show()
                episode_detail_list.visibility == View.VISIBLE -> showEpisodeDetail(false)
                else -> finish()
            }
            return true
        }
        return super.onKeyDown(keyCode, event)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> when {
                systemUIPresenter.isLandscape -> requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                else -> finish()
            }
            R.id.action_share -> {
                AppUtil.shareString(this, videoPagerAdapter.subject.name + " " + videoPagerAdapter.subject.url)
            }
            R.id.action_refresh ->{
                videoPagerAdapter.refreshSubject()
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
