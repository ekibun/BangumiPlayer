package soko.ekibun.bangumi.model

import android.content.Context
import android.net.Uri
import android.os.Environment
import com.google.android.exoplayer2.offline.Downloader
import com.google.android.exoplayer2.offline.DownloaderConstructorHelper
import com.google.android.exoplayer2.offline.ProgressiveDownloader
import com.google.android.exoplayer2.source.hls.offline.HlsDownloader
import com.google.android.exoplayer2.upstream.DataSource
import com.google.android.exoplayer2.upstream.DefaultHttpDataSource
import com.google.android.exoplayer2.upstream.DefaultHttpDataSourceFactory
import com.google.android.exoplayer2.upstream.cache.CacheDataSourceFactory
import com.google.android.exoplayer2.upstream.cache.NoOpCacheEvictor
import com.google.android.exoplayer2.upstream.cache.SimpleCache
import soko.ekibun.bangumi.api.bangumi.bean.Episode
import soko.ekibun.bangumi.api.bangumi.bean.Subject
import soko.ekibun.bangumi.util.JsonUtil
import java.io.File
import android.content.ContentValues

class VideoCacheModel(context: Context){

    //val sp: SharedPreferences by lazy{ PreferenceManager.getDefaultSharedPreferences(context) }
    val resolver by lazy { context.contentResolver }
    val uri = Uri.parse("content://${ContentProviderModel.AUTOHORITY}/${ContentProviderModel.VIDEO_CACHE}")

    fun getVideoCacheList(): Map<Int, VideoCache>{
        val cursor = resolver.query(uri,
                arrayOf("_id", "data"), null, null, null)
        val ret = HashMap<Int, VideoCache>()
        while(cursor?.moveToNext() == true){
            ret[cursor.getInt(0)] = JsonUtil.toEntity(cursor.getString(1), VideoCache::class.java)?:continue
        }
        cursor?.close()
        return  ret
    }

    fun getBangumiVideoCacheList(bangumi: Subject): VideoCache?{
        val cursor = resolver.query(uri,
                arrayOf("data"), "_id=?", arrayOf(bangumi.id.toString()), null)
        if(cursor?.moveToFirst() !=  true) return null
        val infoString = cursor.getString(0)
        cursor.close()
        return  JsonUtil.toEntity(infoString, VideoCache::class.java)
    }

    fun getCache(video: Episode, bangumi: Subject): VideoCache.VideoCacheBean? {
        return getBangumiVideoCacheList(bangumi)?.videoList?.get(video.id)
    }

    fun addVideoCache(video: Episode, bangumi: Subject, url: String, header: Map<String, String>){
        val cacheList = getBangumiVideoCacheList(bangumi)
        val newData = VideoCache(bangumi, (cacheList?.videoList
                ?: HashMap()).plus(
                Pair(video.id, VideoCache.VideoCacheBean(video, url, header))))
        val values = ContentValues()
        values.put("_id", bangumi.id)
        values.put("data", JsonUtil.toJson(newData))
        if(cacheList == null)
            resolver.insert(uri, values)
        else
            resolver.update(uri, values, "_id=?", arrayOf(bangumi.id.toString()))
    }

    fun removeVideoCache(video: Episode, bangumi: Subject){
        val cacheList = getBangumiVideoCacheList(bangumi)?: return
        val newData = VideoCache(bangumi, cacheList.videoList.minus(video.id))
        val values = ContentValues()
        values.put("_id", bangumi.id)
        values.put("data", JsonUtil.toJson(newData))
        if(newData.videoList.isEmpty())
            resolver.delete(uri, "_id=?", arrayOf(bangumi.id.toString()))
        else
            resolver.update(uri, values, "_id=?", arrayOf(bangumi.id.toString()))
    }

    /*
    private val videoCache = "videoCache"
    fun getVideoCacheList(): Map<Int, VideoCache>{
        return JsonUtil.toEntity(sp.getString(videoCache, JsonUtil.toJson(HashMap<Int, VideoCache>()))!!,
                object : TypeToken<Map<Int, VideoCache>>() {}.type)?: HashMap()
    }

    fun getBangumiVideoCacheList(bangumi: Subject): VideoCache?{
        return  getVideoCacheList()[bangumi.id]
    }

    fun getCache(video: Episode, bangumi: Subject): VideoCache.VideoCacheBean? {
        return getBangumiVideoCacheList(bangumi)?.videoList?.get(video.id)
    }

    fun addVideoCache(video: Episode, bangumi: Subject, url: String, header: Map<String, String>){
        val editor = sp.edit()
        val set = HashMap<Int, VideoCache>()
        set += getVideoCacheList()
        set[bangumi.id] = VideoCache(bangumi, (set[bangumi.id]?.videoList?: HashMap()).plus(
                Pair(video.id, VideoCache.VideoCacheBean(video, url, header))
        ))
        editor.putString(videoCache, JsonUtil.toJson(set))
        editor.apply()
    }

    fun removeVideoCache(video: Episode, bangumi: Subject){
        val editor = sp.edit()
        val set = HashMap<Int, VideoCache>()
        set += getVideoCacheList()
        set[bangumi.id] = VideoCache(bangumi, (set[bangumi.id]?.videoList?: HashMap()).minus(video.id))
        set[bangumi.id]?.let{
            if(it.videoList.isEmpty()) set.remove(bangumi.id)
        }
        editor.putString(videoCache, JsonUtil.toJson(set))
        editor.apply()
    }
    */

    fun getDownloader(episode: Episode, bangumi: Subject): Downloader?{
        getCache(episode, bangumi)?.let{ return getDownloader(it.url, it.header) }
        return null
    }

    private val cache by lazy{ SimpleCache(getDiskCacheDir(context, "video"), NoOpCacheEvictor()) }
    fun getDataSourceFactory(header: Map<String, String>, useCache: Boolean): DataSource.Factory{
        val httpSourceFactory= DefaultHttpDataSourceFactory("exoplayer", null, DefaultHttpDataSource.DEFAULT_CONNECT_TIMEOUT_MILLIS, DefaultHttpDataSource.DEFAULT_READ_TIMEOUT_MILLIS, true)
        header.forEach{ httpSourceFactory.defaultRequestProperties.set(it.key, it.value) }
        return if(useCache)  CacheDataSourceFactory(cache, httpSourceFactory) else httpSourceFactory
    }

    fun getDownloader(url: String, header: Map<String, String>): Downloader {
        val dataSourceFactory =getDataSourceFactory(header, true)
        val helper = DownloaderConstructorHelper(cache, dataSourceFactory)
        val downloader = if (url.contains("m3u8")) {
            HlsDownloader(Uri.parse(url), helper)
        } else {
            ProgressiveDownloader(url, url, helper)
        }
        downloader.init()
        return downloader
    }

    data class VideoCache (
            val bangumi: Subject,
            val videoList: Map<Int, VideoCacheBean>
    ){
        data class VideoCacheBean (
                val video: Episode,
                val url: String,
                val header: Map<String, String>
        )
    }

    companion object {
        fun isFinished(downloadPercentage: Float): Boolean{
            return Math.abs(downloadPercentage - 100f) < 0.001f
        }

        fun getDiskCacheDir(context: Context, uniqueName: String): File {
            val cachePath: String = if (Environment.MEDIA_MOUNTED == Environment.getExternalStorageState() || !Environment.isExternalStorageRemovable()) {
                context.externalCacheDir!!.path
            } else {
                context.cacheDir.path
            }
            return File(cachePath + File.separator + uniqueName)
        }
    }
}