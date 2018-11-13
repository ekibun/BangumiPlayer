package soko.ekibun.bangumiplayer.service

import android.annotation.SuppressLint
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.AsyncTask
import android.os.IBinder
import soko.ekibun.bangumi.api.bangumi.bean.AccessToken
import soko.ekibun.bangumi.api.bangumi.bean.Episode
import soko.ekibun.bangumi.api.bangumi.bean.Subject
import soko.ekibun.bangumi.util.JsonUtil
import soko.ekibun.bangumi.util.NotificationUtil
import soko.ekibun.bangumiplayer.App
import soko.ekibun.bangumiplayer.R
import soko.ekibun.bangumiplayer.model.VideoCacheModel
import soko.ekibun.bangumiplayer.ui.video.VideoActivity
import java.util.*
import android.text.format.Formatter
import com.google.gson.reflect.TypeToken
import java.util.concurrent.Executors
import com.google.android.exoplayer2.offline.Downloader

class DownloadService : Service() {

    override fun onBind(intent: Intent): IBinder? {
        return null
    }

    private val timer = Timer()

    private val cachedThreadPool = Executors.newCachedThreadPool()
    @SuppressLint("UseSparseArrays")
    val taskCollection = HashMap<Int, DownloadTask>()

    val manager by lazy { getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager }



    private fun getGroupSummary(status: Int, intent: PendingIntent): String{
        val groupKey = "download"

        manager.notify(0, NotificationUtil.builder(this, downloadChannelId, "下载")
                .setSmallIcon(when(status) {
                    0 -> R.drawable.offline_pin
                    -1 -> R.drawable.ic_pause
                    else -> R.drawable.stat_sys_download
                })
                .setContentTitle("")
                .setAutoCancel(true)
                .setGroupSummary(true)
                .setGroup(groupKey)
                .setContentIntent(intent)
                .build())
        return groupKey
    }

    override fun onCreate() {
        super.onCreate()
        val downloadWatcher = object: TimerTask(){
            override fun run() {
                try{
                    val status = taskCollection.filter { !VideoCacheModel.isFinished(it.value.downloader.downloadPercentage) }.size
                    taskCollection.forEach {
                        val video = it.value.video
                        val bangumi = it.value.bangumi
                        val token = it.value.token
                        val downloader = it.value.downloader
                        val percent = downloader.downloadPercentage
                        val bytes = downloader.downloadedBytes
                        val isFinished = VideoCacheModel.isFinished(percent)
                        if(isFinished)
                            taskCollection.remove(it.key)

                        sendBroadcast(video, bangumi, percent, bytes)
                        val intent = PendingIntent.getActivity(this@DownloadService, video.id.hashCode(),
                                VideoActivity.parseIntent(this@DownloadService, bangumi, token), PendingIntent.FLAG_UPDATE_CURRENT)
                        manager.notify(video.id.toString(), 0, NotificationUtil.builder(this@DownloadService, downloadChannelId, "下载")
                                .setSmallIcon(if(isFinished) R.drawable.offline_pin else R.drawable.stat_sys_download)
                                .setOngoing(!isFinished)
                                .setAutoCancel(true)
                                .setGroup(this@DownloadService.getGroupSummary(status, intent))
                                .setContentTitle((if(isFinished)"已完成 " else "") + "${bangumi.title} ${video.parseSort()}")
                                .setContentText(if(isFinished)Formatter.formatFileSize(this@DownloadService, bytes) else parseDownloadInfo(this@DownloadService, percent, bytes))
                                .let{ if(!isFinished) it.setProgress(10000, (percent * 100).toInt(), bytes == 0L)
                                    it }
                                .setContentIntent(intent).build())
                    }
                }catch(e: Exception){
                    e.printStackTrace()
                }
            }
        }
        timer.scheduleAtFixedRate(downloadWatcher, 0, 1000)
    }

    override fun onDestroy() {
        timer.cancel()
        super.onDestroy()
    }

    class DownloadTask(val video: Episode, val bangumi: Subject, val token: AccessToken, val downloader: Downloader): AsyncTask<Unit, Unit, Unit>(){
        override fun doInBackground(vararg params: Unit?) {
            while(!Thread.currentThread().isInterrupted && ! VideoCacheModel.isFinished(downloader.downloadPercentage)){
                try {
                    downloader.download{ _, _, _ -> }
                } catch (e: InterruptedException) {
                    break
                }catch(e: Exception){
                    e.printStackTrace()
                    Thread.sleep(1000)
                }
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if(intent != null){
            val video = JsonUtil.toEntity(intent.getStringExtra(EXTRA_EPISODE), Episode::class.java)!!
            val bangumi = JsonUtil.toEntity(intent.getStringExtra(EXTRA_SUBJECT), Subject::class.java)!!

            when(intent.action){
                ACTION_DOWNLOAD -> {
                    val token = JsonUtil.toEntity(intent.getStringExtra(EXTRA_TOKEN), AccessToken::class.java)!!
                    val task = taskCollection[video.id]
                    if(task!= null){
                        taskCollection.remove(video.id)
                        task.cancel(true)
                        sendBroadcast(video, bangumi, task.downloader.downloadPercentage, task.downloader.downloadedBytes, true)
                        val pIntent = PendingIntent.getActivity(this@DownloadService, video.id.hashCode(),
                                VideoActivity.parseIntent(this@DownloadService, bangumi, token), PendingIntent.FLAG_UPDATE_CURRENT)
                        manager.notify(video.id.toString(), 0, NotificationUtil.builder(this@DownloadService, downloadChannelId, "下载")
                                .setSmallIcon(R.drawable.ic_pause)
                                .setOngoing(false)
                                .setAutoCancel(true)
                                .setGroup(this@DownloadService.getGroupSummary(-1, pIntent))
                                .setContentTitle("已暂停 ${bangumi.title} ${video.parseSort()}")
                                .setContentText(parseDownloadInfo(this@DownloadService, task.downloader.downloadPercentage, task.downloader.downloadedBytes))
                                //.setProgress(10000, (task.downloader.downloadPercentage * 100).toInt(), task.downloader.downloadedBytes == 0L)
                                .setContentIntent(pIntent).build())
                    }else{
                        val url = intent.getStringExtra(EXTRA_URL)
                        val map = JsonUtil.toEntity<Map<String, String>>(intent.getStringExtra(EXTRA_HEADER), object : TypeToken<Map<String, String>>() {}.type)?:return super.onStartCommand(intent, flags, startId)
                        val downloader = App.getVideoCacheModel(this).getDownloader(url, map)
                        val newTask = DownloadTask(video, bangumi, token, downloader)
                        taskCollection[video.id] = newTask
                        newTask.executeOnExecutor(cachedThreadPool)
                    }
                }
                ACTION_REMOVE -> {
                    manager.cancel(video.id.toString(), 0)
                    if(taskCollection.containsKey(video.id)){
                        taskCollection[video.id]!!.cancel(true)
                        taskCollection.remove(video.id)
                    }
                    if(taskCollection.isEmpty())
                        manager.cancel(0)
                    val videoCacheModel = App.getVideoCacheModel(this)
                    videoCacheModel.getCache(video, bangumi)?.let {
                        videoCacheModel.getDownloader(it.url, it.header).remove() }
                    videoCacheModel.removeVideoCache(video, bangumi)
                    sendBroadcast(video, bangumi, Float.NaN, 0, false)
                }
            }
        }
        return super.onStartCommand(intent, flags, startId)
    }

    fun sendBroadcast(video: Episode, bangumi: Subject, percent: Float, bytes: Long, hasCache: Boolean? = null){
        val broadcastIntent = Intent(getBroadcastAction(bangumi))
        broadcastIntent.putExtra(EXTRA_EPISODE, JsonUtil.toJson(video))
        broadcastIntent.putExtra(EXTRA_PERCENT, percent)
        broadcastIntent.putExtra(EXTRA_BYTES, bytes)
        hasCache?.let{ broadcastIntent.putExtra(EXTRA_CANCEL, it) }
        sendBroadcast(broadcastIntent)
    }


    companion object {
        const val downloadChannelId = "download"

        const val EXTRA_CANCEL = "extraCancel"
        const val EXTRA_PERCENT = "extraPercent"
        const val EXTRA_BYTES = "extraBytes"
        const val EXTRA_EPISODE = "extraEpisode"
        const val EXTRA_SUBJECT = "extraSubject"
        const val EXTRA_TOKEN = "extraToken"
        const val EXTRA_URL = "extraUrl"
        const val EXTRA_HEADER = "extraHeader"
        const val ACTION_DOWNLOAD = "actionDownload"
        const val ACTION_REMOVE = "actionRemove"

        fun getBroadcastAction(bangumi: Subject): String{
            return "bangumi_download${bangumi.id}"
        }

        fun parseDownloadInfo(context: Context, percent: Float, bytes: Long): String{
            return "${Formatter.formatFileSize(context, bytes)}/${Formatter.formatFileSize(context, (bytes * 100 / percent).toLong())}"
        }

        fun download(context: Context, video: Episode, subject: Subject, token: AccessToken, url: String, header: Map<String, String>){
            App.getVideoCacheModel(context).addVideoCache(video, subject, url, header)
            val intent = Intent(context, DownloadService::class.java)
            intent.action = ACTION_DOWNLOAD
            intent.putExtra(EXTRA_SUBJECT, JsonUtil.toJson(subject))
            intent.putExtra(EXTRA_EPISODE, JsonUtil.toJson(video))
            intent.putExtra(EXTRA_TOKEN, JsonUtil.toJson(token))
            intent.putExtra(EXTRA_URL, url)
            intent.putExtra(EXTRA_HEADER, JsonUtil.toJson(header))
            context.startService(intent)
        }
        fun remove(context: Context, video: Episode, subject: Subject){
            val intent = Intent(context, DownloadService::class.java)
            intent.action = ACTION_REMOVE
            intent.putExtra(EXTRA_SUBJECT, JsonUtil.toJson(subject))
            intent.putExtra(EXTRA_EPISODE, JsonUtil.toJson(video))
            context.startService(intent)
        }
    }
}
