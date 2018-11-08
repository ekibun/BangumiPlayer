package soko.ekibun.bangumiplayer.ui.video

import android.annotation.SuppressLint
import android.support.design.widget.AppBarLayout
import android.support.v4.view.ViewCompat
import android.support.v7.widget.LinearLayoutManager
import android.view.View
import android.view.animation.AnimationUtils
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.chad.library.adapter.base.entity.SectionEntity
import jp.wasabeef.glide.transformations.BlurTransformation
import kotlinx.android.synthetic.main.activity_video.*
import kotlinx.android.synthetic.main.subject_detail.*
import kotlinx.android.synthetic.main.subject_episode.*
import soko.ekibun.bangumi.api.bangumi.bean.Episode
import soko.ekibun.bangumi.api.bangumi.bean.Subject
import soko.ekibun.bangumi.api.bangumi.bean.SubjectProgress
import soko.ekibun.bangumi.ui.subject.EpisodeAdapter
import soko.ekibun.bangumi.ui.subject.SeasonAdapter
import soko.ekibun.bangumi.ui.subject.SmallEpisodeAdapter
import soko.ekibun.bangumi.util.JsonUtil
import soko.ekibun.bangumiplayer.R


class SubjectView(private val context: VideoActivity){
    val episodeAdapter = SmallEpisodeAdapter()
    val episodeDetailAdapter = EpisodeAdapter()
    val seasonAdapter = SeasonAdapter()
    val lineAdapter = LineAdapter()
    val seasonlayoutManager = LinearLayoutManager(context)

    init{
        context.season_list.adapter = seasonAdapter
        seasonlayoutManager.orientation = LinearLayoutManager.HORIZONTAL
        context.season_list.layoutManager = seasonlayoutManager
        context.season_list.isNestedScrollingEnabled = false

        context.episode_list.adapter = episodeAdapter
        val layoutManager = LinearLayoutManager(context)
        layoutManager.orientation = LinearLayoutManager.HORIZONTAL
        context.episode_list.layoutManager = layoutManager//LinearLayoutManager(context)
        context.episode_list.isNestedScrollingEnabled = false

        context.episode_detail_list.adapter = episodeDetailAdapter
        context.episode_detail_list.layoutManager = LinearLayoutManager(context)

        context.item_close.setOnClickListener {
            showEpisodeDetail(false)
        }
        context.episode_detail.setOnClickListener{
            showEpisodeDetail(true)
        }

        context.line_list.adapter = lineAdapter
        context.line_list.layoutManager = LinearLayoutManager(context)
    }

    val weekSmall = listOf("", "周一", "周二", "周三", "周四", "周五", "周六", "周日")
    private fun parseAirWeek(subject: Subject): String{
        var ret = "更新时间："
        subject.air_weekday.toString().forEach {
            ret += weekSmall[it.toString().toInt()] + " "
        }
        return ret
    }

    @SuppressLint("SetTextI18n")
    fun updateSubject(subject: Subject){
        if(context.isDestroyed) return
        context.title = if(subject.name_cn.isNullOrEmpty()) subject.name else subject.name_cn
        context.item_info.text = "总集数：${subject.eps_count}"
        context.item_subject_title.text = subject.name
        context.item_air_time.text = "开播时间：${subject.air_date}"
        context.item_air_week.text = parseAirWeek(subject)

        subject.rating?.let {
            context.item_score.text = it.score.toString()
            context.item_score_count.text = context.getString(R.string.rate_count, it.total)
        }
        Glide.with(context.item_cover)
                .applyDefaultRequestOptions(RequestOptions.placeholderOf(context.item_cover.drawable))
                .load(subject.images?.common)
                .apply(RequestOptions.errorOf(R.drawable.ic_404))
                .into(context.item_cover)

        Glide.with(context.item_cover_blur)
                .applyDefaultRequestOptions(RequestOptions.placeholderOf(context.item_cover_blur.drawable))
                .load(subject.images?.common)
                .apply(RequestOptions.bitmapTransform(BlurTransformation(25, 8)))
                .into(context.item_cover_blur)
        ((subject.eps as? List<*>)?.map{ JsonUtil.toEntity(JsonUtil.toJson(it!!), Episode::class.java)!!})?.let{
            updateEpisode(it)
        }
    }

    private fun updateEpisode(episodes: List<Episode>){
        val eps = episodes.filter { (it.status?:"") in listOf("Air") }.size
        context.episode_detail.text = context.getString(if(eps == episodes.size) R.string.phrase_full else R.string.phrase_updating, eps)

        val maps = HashMap<Int, List<Episode>>()
        episodes.forEach {
            maps[it.type] = (maps[it.type]?:ArrayList()).plus(it)
        }
        episodeAdapter.setNewData(null)
        episodeDetailAdapter.setNewData(null)
        maps.forEach {
            episodeDetailAdapter.addData(object: SectionEntity<Episode>(true, Episode.getTypeName(it.key)){})
            it.value.forEach {
                if((it.status?:"") in listOf("Air"))
                    episodeAdapter.addData(it)
                episodeDetailAdapter.addData(object: SectionEntity<Episode>(it){})
            }
        }
        progress = progress
    }

    private var scrolled = false
    var loadedProgress = false
    var progress: SubjectProgress? = null
        set(value) {
            episodeDetailAdapter.data.forEach { ep ->
                ep.t?.progress = null
                value?.eps?.forEach {
                    if (ep.t?.id == it.id) {
                        ep.t?.progress = it
                    }
                }
            }
            episodeAdapter.notifyDataSetChanged()
            episodeDetailAdapter.notifyDataSetChanged()
            field = value

            if(!scrolled && loadedProgress && episodeAdapter.data.size>0){
                scrolled = true

                var lastView = 0
                episodeAdapter.data.forEachIndexed { index, episode ->
                    if(episode.progress != null)
                        lastView = index
                }
                val layoutManager = (context.episode_list.layoutManager as LinearLayoutManager)
                layoutManager.scrollToPositionWithOffset(lastView, 0)
                layoutManager.stackFromEnd = false
            }
        }

    fun showEpisodeDetail(show: Boolean){
        context.episode_detail_list_header.visibility = if(show) View.VISIBLE else View.INVISIBLE
        context.episode_detail_list_header.animation = AnimationUtils.loadAnimation(context, if(show) R.anim.move_in else R.anim.move_out)
        context.episode_detail_list.visibility = if(show) View.VISIBLE else View.INVISIBLE
        context.episode_detail_list.animation = AnimationUtils.loadAnimation(context, if(show) R.anim.move_in else R.anim.move_out)
    }
}