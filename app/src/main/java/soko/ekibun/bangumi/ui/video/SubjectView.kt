package soko.ekibun.bangumi.ui.video

import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.support.v7.app.AlertDialog
import android.support.v7.widget.LinearLayoutManager
import android.util.Log
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.widget.PopupWindow
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.chad.library.adapter.base.entity.SectionEntity
import jp.wasabeef.glide.transformations.BlurTransformation
import kotlinx.android.synthetic.main.activity_video.*
import kotlinx.android.synthetic.main.subject_detail.*
import kotlinx.android.synthetic.main.subject_episode.*
import soko.ekibun.bangumi.api.bangumi.bean.Episode
import soko.ekibun.bangumi.api.bangumi.bean.Subject
import soko.ekibun.bangumi.ui.subject.SeasonAdapter
import soko.ekibun.bangumi.util.JsonUtil
import soko.ekibun.bangumi.App
import soko.ekibun.bangumi.ui.view.DragPhotoView
import soko.ekibun.bangumi.util.AppUtil
import soko.ekibun.bangumiplayer.R

class SubjectView(private val context: VideoActivity){
    val episodeAdapter = SmallEpisodeAdapter(context)
    val episodeDetailAdapter = EpisodeAdapter(context)
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

    private val weekSmall = listOf("", "周一", "周二", "周三", "周四", "周五", "周六", "周日")
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

        context.item_detail.setOnClickListener {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("bangumi://subject/${subject.id}"))
            intent.putExtra("extraSubject", JsonUtil.toJson(subject))
            context.startActivity(intent)
        }

        subject.rating?.let {
            context.item_score.text = it.score.toString()
            context.item_score_count.text = context.getString(R.string.rate_count, it.total)
        }
        Glide.with(context.item_cover)
                .applyDefaultRequestOptions(RequestOptions.placeholderOf(context.item_cover.drawable))
                .load(subject.images?.common)
                .apply(RequestOptions.errorOf(R.drawable.ic_404))
                .into(context.item_cover)
        context.item_cover.setOnClickListener {
            val popWindow = PopupWindow(it, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT, true)
            val photoView = DragPhotoView(it.context)
            popWindow.contentView = photoView
            Glide.with(photoView).applyDefaultRequestOptions(RequestOptions.placeholderOf(context.item_cover.drawable))
                    .load(subject.images?.large).into(photoView)
            photoView.mTapListener={
                popWindow.dismiss()
            }
            photoView.mExitListener={
                popWindow.dismiss()
            }
            photoView.mLongClickListener = {
                val systemUiVisibility = popWindow.contentView.systemUiVisibility
                AlertDialog.Builder(context)
                        .setItems(arrayOf("分享"))
                        { _, _ ->
                            AppUtil.shareDrawable(context, photoView.drawable)
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
        Glide.with(context.item_cover_blur)
                .applyDefaultRequestOptions(RequestOptions.placeholderOf(context.item_cover_blur.drawable))
                .load(subject.images?.common)
                .apply(RequestOptions.bitmapTransform(BlurTransformation(25, 8)))
                .into(context.item_cover_blur)
        val eps = ((subject.eps as? List<*>)?.map{ JsonUtil.toEntity(JsonUtil.toJson(it!!), Episode::class.java)!!})
        updateEpisode(eps, subject)
    }

    var bangumiEpisode: List<Episode> = ArrayList()
    @SuppressLint("SetTextI18n")
    fun updateEpisode(episodes: List<Episode>?, subject: Subject){
        Log.v("eps", episodes.toString())
        val cacheEpisode = App.getVideoCacheModel(context).getBangumiVideoCacheList(subject.id)?.videoList?.map{it.value.video}?:ArrayList()
        bangumiEpisode = episodes?: bangumiEpisode
        val eps = bangumiEpisode.filter { (it.status?:"") in listOf("Air") }.size
        context.episode_detail.text = (if(!cacheEpisode.isEmpty()) "已缓存 ${cacheEpisode.size} 话" else "") +
                (if(!cacheEpisode.isEmpty() && !bangumiEpisode.isEmpty()) " / " else "")+
                (if(!bangumiEpisode.isEmpty()) context.getString(if(eps == bangumiEpisode.size) R.string.phrase_full else R.string.phrase_updating, eps) else "")
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
            val layoutManager = (context.episode_list.layoutManager as LinearLayoutManager)
            layoutManager.scrollToPositionWithOffset(lastView, 0)
            layoutManager.stackFromEnd = false
        }
    }

    private var scrolled = false

    fun showEpisodeDetail(show: Boolean){
        context.episode_detail_list_header.visibility = if(show) View.VISIBLE else View.INVISIBLE
        context.episode_detail_list_header.animation = AnimationUtils.loadAnimation(context, if(show) R.anim.move_in_bottom else R.anim.move_out_bottom)
        context.episode_detail_list.visibility = if(show) View.VISIBLE else View.INVISIBLE
        context.episode_detail_list.animation = AnimationUtils.loadAnimation(context, if(show) R.anim.move_in_bottom else R.anim.move_out_bottom)
    }
}