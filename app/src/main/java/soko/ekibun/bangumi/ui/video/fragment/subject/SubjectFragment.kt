package soko.ekibun.bangumi.ui.video.fragment.subject

import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.support.v7.app.AlertDialog
import android.support.v7.widget.LinearLayoutManager
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.PopupWindow
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.chad.library.adapter.base.entity.SectionEntity
import jp.wasabeef.glide.transformations.BlurTransformation
import kotlinx.android.synthetic.main.activity_video.*
import kotlinx.android.synthetic.main.fragment_bangumi.view.*
import kotlinx.android.synthetic.main.subject_detail.view.*
import kotlinx.android.synthetic.main.subject_episode.view.*
import soko.ekibun.bangumi.App
import soko.ekibun.bangumi.api.bangumi.bean.Episode
import soko.ekibun.bangumi.api.bangumi.bean.Subject
import soko.ekibun.bangumi.model.ProgressModel
import soko.ekibun.bangumi.ui.subject.SeasonAdapter
import soko.ekibun.bangumi.ui.video.EpisodeAdapter
import soko.ekibun.bangumi.ui.video.LineAdapter
import soko.ekibun.bangumi.ui.video.SmallEpisodeAdapter
import soko.ekibun.bangumi.ui.video.VideoActivity
import soko.ekibun.bangumi.ui.video.fragment.VideoFragment
import soko.ekibun.bangumi.ui.view.DragPhotoView
import soko.ekibun.bangumi.ui.view.controller.Controller
import soko.ekibun.bangumi.util.AppUtil
import soko.ekibun.bangumi.util.JsonUtil
import soko.ekibun.bangumiplayer.R

@SuppressLint("ValidFragment")
class SubjectFragment(private val videoActivity: VideoActivity): VideoFragment() {
    override val titleRes: Int = R.string.bangumi

    val episodeAdapter= SmallEpisodeAdapter(videoActivity)
    val episodeDetailAdapter = EpisodeAdapter(videoActivity)
    val seasonAdapter = SeasonAdapter()
    val lineAdapter = LineAdapter()
    val seasonlayoutManager= LinearLayoutManager(videoActivity)

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        onSubjectChange(subject)
        return detail
    }

    var subject: Subject = Subject()
    val detail: View by lazy {
        val detail = videoActivity.layoutInflater.inflate(R.layout.fragment_bangumi, videoActivity.root_layout, false)

        detail.season_list.adapter = seasonAdapter
        seasonlayoutManager.orientation = LinearLayoutManager.HORIZONTAL
        detail.season_list.layoutManager = seasonlayoutManager
        detail.season_list.isNestedScrollingEnabled = false

        detail.episode_list.adapter = episodeAdapter
        val layoutManager = LinearLayoutManager(context)
        layoutManager.orientation = LinearLayoutManager.HORIZONTAL
        detail.episode_list.layoutManager = layoutManager//LinearLayoutManager(context)
        detail.episode_list.isNestedScrollingEnabled = false

        detail.episode_detail.setOnClickListener{
            videoActivity.showEpisodeDetail(true)
        }

        detail.line_list.adapter = lineAdapter
        detail.line_list.layoutManager = LinearLayoutManager(context)

        detail
    }
    @SuppressLint("SetTextI18n")
    override fun onSubjectChange(sbj: Subject) {
        if(videoActivity.isDestroyed) return
        subject = sbj
        videoActivity.title = if(subject.name_cn.isNullOrEmpty()) subject.name else subject.name_cn
        detail.item_info.text = "总集数：${subject.eps_count}"
        detail.item_subject_title.text = subject.name
        detail.item_air_time.text = "开播时间：${subject.air_date}"
        detail.item_air_week.text = parseAirWeek(subject)

        detail.item_detail.setOnClickListener {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("bangumi://subject/${subject.id}"))
            intent.putExtra("extraSubject", JsonUtil.toJson(subject))
            detail.context.startActivity(intent)
        }

        subject.rating?.let {
            detail.item_score.text = it.score.toString()
            detail.item_score_count.text = detail.context.getString(R.string.rate_count, it.total)
        }
        Glide.with(detail.item_cover)
                .load(subject.images?.common)
                .apply(RequestOptions.placeholderOf(detail.item_cover.drawable).error(R.drawable.ic_404))
                .into(detail.item_cover)

        detail.item_cover.setOnClickListener {
            val popWindow = PopupWindow(it, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT, true)
            val photoView = DragPhotoView(it.context)
            popWindow.contentView = photoView
            Glide.with(photoView).load(subject.images?.large)
                    .apply(RequestOptions.placeholderOf(detail.item_cover.drawable)).into(photoView)
            photoView.mTapListener={
                popWindow.dismiss()
            }
            photoView.mExitListener={
                popWindow.dismiss()
            }
            photoView.mLongClickListener = {
                val systemUiVisibility = popWindow.contentView.systemUiVisibility
                AlertDialog.Builder(it.context)
                        .setItems(arrayOf("分享"))
                        { _, _ ->
                            AppUtil.shareDrawable(it.context, photoView.drawable)
                        }.setOnDismissListener {
                            popWindow.contentView.systemUiVisibility = systemUiVisibility
                        }.show()
            }
            popWindow.isClippingEnabled = false
            popWindow.animationStyle = R.style.AppTheme_FadeInOut
            popWindow.showAtLocation(it, Gravity.CENTER, 0, 0)
            popWindow.contentView.systemUiVisibility = (View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                    or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    or View.SYSTEM_UI_FLAG_FULLSCREEN)
        }
        Glide.with(videoActivity.item_cover_blur)
                .load(subject.images?.common)
                .apply(RequestOptions.bitmapTransform(BlurTransformation(25, 8)).placeholder(videoActivity.item_cover_blur.drawable))
                .into(videoActivity.item_cover_blur)

        val eps = ((subject.eps as? List<*>)?.map{ JsonUtil.toEntity(JsonUtil.toJson(it!!), Episode::class.java)!!})
        updateEpisode(eps, subject)

        val info = ProgressModel(detail.context).getProgress(subject)
        if(info == null){
            detail.item_progress_info.text = "尚未观看"
            detail.item_progress_play.text = " 从头开始"
            detail.item_progress.setOnClickListener {
                videoActivity.videoPresenter.playEpisode(episodeDetailAdapter.data.firstOrNull{ it.t != null}?.t?: Episode(sort = 1f))
            }
        }else {
            detail.item_progress_info.text = "上次看到 ${info.episode.parseSort()} ${Controller.stringForTime(info.progress)}"
            detail.item_progress_play.text = " 继续观看"
            detail.item_progress.setOnClickListener {
                videoActivity.videoPresenter.playEpisode(info.episode)
                videoActivity.videoPresenter.startAt = info.progress * 10L
            }
        }
    }

    private var scrolled = false
    var bangumiEpisode: List<Episode> = ArrayList()
    @SuppressLint("SetTextI18n")
    fun updateEpisode(episodes: List<Episode>?, subject: Subject){
        Log.v("eps", episodes.toString())
        val cacheEpisode = App.getVideoCacheModel(detail.context).getBangumiVideoCacheList(subject.id)?.videoList?.map{it.value.video}?:ArrayList()
        bangumiEpisode = episodes?: bangumiEpisode
        val eps = bangumiEpisode.filter { (it.status?:"") in listOf("Air") }.size
        detail.episode_detail.text = (if(!cacheEpisode.isEmpty()) "已缓存 ${cacheEpisode.size} 话" else "") +
                (if(!cacheEpisode.isEmpty() && !bangumiEpisode.isEmpty()) " / " else "")+
                (if(!bangumiEpisode.isEmpty()) detail.context.getString(if(eps == bangumiEpisode.size) R.string.phrase_full else R.string.phrase_updating, eps) else "")
        val maps = LinkedHashMap<String, List<Episode>>()
        bangumiEpisode.plus(cacheEpisode).distinctBy { it.id }.forEach {
            val key = it.cat?:Episode.getTypeName(it.type)
            maps[key] = (maps[key]?:ArrayList()).plus(it)
        }
        episodeAdapter.setNewData(null)
        episodeDetailAdapter.setNewData(null)
        maps.forEach {
            episodeDetailAdapter.addData(object: SectionEntity<Episode>(true, it.key){})
            it.value.forEach {
                if((it.status?:"") in listOf("Air"))
                    episodeAdapter.addData(it)
                episodeDetailAdapter.addData(object: SectionEntity<Episode>(it){})
            }
        }
        if(!scrolled && episodeAdapter.data.size>0){
            scrolled = true

            var lastView = 0
            episodeAdapter.data.forEachIndexed { index, episode ->
                if(episode.progress != null)
                    lastView = index
            }
            val layoutManager = (detail.episode_list.layoutManager as LinearLayoutManager)
            layoutManager.scrollToPositionWithOffset(lastView, 0)
            layoutManager.stackFromEnd = false
        }
    }

    private val weekSmall = listOf("", "周一", "周二", "周三", "周四", "周五", "周六", "周日")
    private fun parseAirWeek(subject: Subject): String{
        var ret = "更新时间："
        subject.air_weekday.toString().forEach {
            ret += weekSmall[it.toString().toInt()] + " "
        }
        return ret
    }
}