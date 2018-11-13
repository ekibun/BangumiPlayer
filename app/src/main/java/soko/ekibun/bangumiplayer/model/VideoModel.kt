package soko.ekibun.bangumiplayer.model

import android.content.Context
import android.net.Uri
import android.view.SurfaceView
import com.google.android.exoplayer2.*
import com.google.android.exoplayer2.source.ExtractorMediaSource
import com.google.android.exoplayer2.source.TrackGroupArray
import com.google.android.exoplayer2.source.hls.HlsMediaSource
import com.google.android.exoplayer2.trackselection.AdaptiveTrackSelection
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector
import com.google.android.exoplayer2.trackselection.TrackSelectionArray
import com.google.android.exoplayer2.upstream.*
import retrofit2.Call
import soko.ekibun.bangumi.api.ApiHelper
import soko.ekibun.bangumi.api.bangumi.bean.Episode
import soko.ekibun.bangumi.api.bangumi.bean.Subject
import soko.ekibun.bangumi.ui.view.BackgroundWebView
import soko.ekibun.bangumiplayer.App
import soko.ekibun.bangumiplayer.parser.ParserInfo
import soko.ekibun.bangumiplayer.provider.BaseProvider


class VideoModel(private val context: Context, private val onAction: Listener) {
    //private val parseModel: ParseModel by lazy{ ParseModel(context) }
    //private val videoCacheModel: VideoCacheModel by lazy{ App.getVideoCacheModel(context) }

    interface Listener{
        fun onReady(playWhenReady: Boolean)
        fun onBuffering()
        fun onEnded()
        fun onVideoSizeChange(width: Int, height: Int, unappliedRotationDegrees: Int, pixelWidthHeightRatio: Float)
        fun onError(error: ExoPlaybackException)
    }

    val player: SimpleExoPlayer by lazy{
        val bandwidthMeter = DefaultBandwidthMeter()
        val videoTackSelectionFactory = AdaptiveTrackSelection.Factory(bandwidthMeter)
        val trackSelector = DefaultTrackSelector(videoTackSelectionFactory)
        val player = ExoPlayerFactory.newSimpleInstance(context, trackSelector)
        player.addListener(object: Player.EventListener{
            override fun onPlaybackParametersChanged(playbackParameters: PlaybackParameters?) {}
            override fun onSeekProcessed() {}
            override fun onTracksChanged(trackGroups: TrackGroupArray?, trackSelections: TrackSelectionArray?) {}
            override fun onPlayerError(error: ExoPlaybackException) { onAction.onError(error) }
            override fun onLoadingChanged(isLoading: Boolean) {}
            override fun onPositionDiscontinuity(reason: Int) {}
            override fun onRepeatModeChanged(repeatMode: Int) {}
            override fun onShuffleModeEnabledChanged(shuffleModeEnabled: Boolean) {}
            override fun onTimelineChanged(timeline: Timeline?, manifest: Any?, reason: Int) {}
            override fun onPlayerStateChanged(playWhenReady: Boolean, playbackState: Int) {
                when (playbackState){
                    Player.STATE_ENDED -> onAction.onEnded()
                    Player.STATE_READY -> onAction.onReady(playWhenReady)
                    Player.STATE_BUFFERING-> onAction.onBuffering()
                }
            }
        })
        player.addVideoListener(object: com.google.android.exoplayer2.video.VideoListener{
            override fun onVideoSizeChanged(width: Int, height: Int, unappliedRotationDegrees: Int, pixelWidthHeightRatio: Float) {
                onAction.onVideoSizeChange(width, height, unappliedRotationDegrees, pixelWidthHeightRatio)
            }
            override fun onRenderedFirstFrame() {
                //onAction.onReady(player.playWhenReady)
            }
        })
        player
    }

    var videoInfoCall: Call<BaseProvider.VideoInfo>? = null
    var videoCall: Call<Pair<String, Map<String,String>>>? = null
    private val videoCacheModel by lazy{App.getVideoCacheModel(context)}
    private val providerInfoModel by lazy{ProviderInfoModel(context)}
    fun getVideo(episode: Episode, subject: Subject, webView: BackgroundWebView, onGetVideoInfo: (Boolean?)->Unit, onGetVideo: (Pair<String, Map<String,String>>?, Boolean?)->Unit) {
        val videoCache = videoCacheModel.getCache(episode, subject)
        if (videoCache != null) {
            onGetVideoInfo(true)
            onGetVideo(Pair(videoCache.url, videoCache.header), true)
        } else {
            val info = providerInfoModel.getInfos(subject)?.getDefaultProvider()?: return
            videoInfoCall?.cancel()
            webView.loadUrl("about:blank")
            videoCall?.cancel()
            videoInfoCall = ProviderModel.getVideoInfo(info, episode)
            videoInfoCall?.enqueue(ApiHelper.buildCallback(context, { video ->
                onGetVideoInfo(true)
                videoCall = ParserModel.getVideo(webView, video, info.parser?: ParserInfo("", ""))
                videoCall?.enqueue(ApiHelper.buildCallback(context, {
                    onGetVideo(it, false)
                }, { if(it != null) onGetVideo(null,if(it.toString().contains("Canceled"))null else false) }))
            }, { if(it != null) onGetVideoInfo(if(it.toString().contains("Canceled"))null else false)}))
        }
    }

    fun play(request: Pair<String,Map<String, String>>, surface: SurfaceView, useCache: Boolean = false){
        val url = request.first
        player.setVideoSurfaceView(surface)
        val httpSourceFactory= DefaultHttpDataSourceFactory("exoplayer", null, DefaultHttpDataSource.DEFAULT_CONNECT_TIMEOUT_MILLIS, DefaultHttpDataSource.DEFAULT_READ_TIMEOUT_MILLIS, true)
        request.second.forEach{
            httpSourceFactory.defaultRequestProperties.set(it.key, it.value)
        }
        val dataSourceFactory = DefaultDataSourceFactory(surface.context, null, videoCacheModel.getDataSourceFactory(request.second, useCache))
        //DefaultHttpDataSourceFactory("exoplayer", null, DefaultHttpDataSource.DEFAULT_CONNECT_TIMEOUT_MILLIS, DefaultHttpDataSource.DEFAULT_READ_TIMEOUT_MILLIS, true)/*if(cache) videoCacheModel.getCacheDataSourceFactory(url) else videoCacheModel.factory*/
        val videoSource = if(url.contains("m3u8"))
            HlsMediaSource.Factory(dataSourceFactory).createMediaSource(Uri.parse(url))
        else ExtractorMediaSource.Factory(dataSourceFactory).createMediaSource(Uri.parse(url))

        player.prepare(videoSource)
        player.playWhenReady = true
    }
}