package soko.ekibun.bangumi.ui.video

import android.content.res.ColorStateList
import android.view.View
import com.chad.library.adapter.base.BaseQuickAdapter
import com.chad.library.adapter.base.BaseViewHolder
import kotlinx.android.synthetic.main.item_episode_small.view.*
import soko.ekibun.bangumi.api.bangumi.bean.Episode
import soko.ekibun.bangumi.api.bangumi.bean.Subject
import soko.ekibun.bangumi.util.JsonUtil
import soko.ekibun.bangumi.util.ResourceUtil
import soko.ekibun.bangumi.App
import soko.ekibun.bangumiplayer.R
import soko.ekibun.bangumi.model.VideoCacheModel

class SmallEpisodeAdapter(private val context: VideoActivity, data: MutableList<Episode>? = null) :
        BaseQuickAdapter<Episode, BaseViewHolder>
        (R.layout.item_episode_small, data) {
    val subject by lazy{ JsonUtil.toEntity(context.intent.getStringExtra(VideoActivity.EXTRA_SUBJECT), Subject::class.java)!! }

    override fun convert(helper: BaseViewHolder, item: Episode) {
        helper.setText(R.id.item_title, item.parseSort())
        helper.setText(R.id.item_desc, if(item.name_cn.isNullOrEmpty()) item.name else item.name_cn)
        val color = ResourceUtil.resolveColorAttr(helper.itemView.context,
                when {
                    item.progress != null -> R.attr.colorPrimary
                    else -> android.R.attr.textColorSecondary
                })
        val alpha = if((item.status?:"") in listOf("Air"))1f else 0.6f
        helper.itemView.item_title.setTextColor(color)
        helper.itemView.item_desc.setTextColor(color)
        helper.itemView.item_container.backgroundTintList = ColorStateList.valueOf(color)
        helper.itemView.item_layout.alpha = alpha
        helper.addOnClickListener(R.id.item_container)
        helper.addOnLongClickListener(R.id.item_container)
        val downloader = App.getVideoCacheModel(helper.itemView.context).getDownloader(item, subject)
        updateDownload(helper.itemView, downloader?.downloadPercentage?: Float.NaN, downloader?.downloadedBytes?:0L, downloader != null)
    }

    fun updateDownload(view: View, percent: Float, bytes: Long, hasCache: Boolean, download: Boolean = false){
        view.item_icon.visibility = if(hasCache) View.VISIBLE else View.INVISIBLE
        view.item_icon.setImageResource(when {
            VideoCacheModel.isFinished(percent) -> R.drawable.ic_episode_download_ok
            download -> R.drawable.ic_episode_download
            else -> R.drawable.ic_episode_download_pause
        })
    }
}