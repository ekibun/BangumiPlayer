package soko.ekibun.bangumi.ui.video

import android.support.v7.widget.RecyclerView
import android.text.format.Formatter
import android.view.View
import android.widget.TextView
import com.chad.library.adapter.base.BaseSectionQuickAdapter
import com.chad.library.adapter.base.BaseViewHolder
import com.chad.library.adapter.base.entity.SectionEntity
import com.oushangfeng.pinnedsectionitemdecoration.utils.FullSpanUtil
import kotlinx.android.synthetic.main.activity_video.*
import kotlinx.android.synthetic.main.item_episode.view.*
import soko.ekibun.bangumi.api.bangumi.bean.Episode
import soko.ekibun.bangumi.ui.view.BackgroundWebView
import soko.ekibun.bangumi.util.ResourceUtil
import soko.ekibun.bangumi.App
import soko.ekibun.bangumiplayer.R
import soko.ekibun.bangumi.model.VideoCacheModel
import soko.ekibun.bangumi.service.DownloadService

class EpisodeAdapter(val context: VideoActivity, data: MutableList<SectionEntity<Episode>>? = null) :
        BaseSectionQuickAdapter<SectionEntity<Episode>, BaseViewHolder>
        (R.layout.item_episode, R.layout.header_episode, data) {

    override fun convertHead(helper: BaseViewHolder, item: SectionEntity<Episode>) {
        helper.getView<TextView>(R.id.item_header).visibility = if(data.indexOf(item) == 0) View.GONE else View.VISIBLE
        helper.setText(R.id.item_header, item.header)
    }

    override fun convert(helper: BaseViewHolder, item: SectionEntity<Episode>) {
        val index = data.indexOfFirst{ it == item }
        helper.setText(R.id.item_title, item.t.parseSort())
        helper.setText(R.id.item_desc, if(item.t.name_cn.isNullOrEmpty()) item.t.name else item.t.name_cn)
        val color = ResourceUtil.resolveColorAttr(helper.itemView.context,
                when {
                    item.t.progress != null -> R.attr.colorPrimary
                    else -> android.R.attr.textColorSecondary
                })
        val alpha = if((item.t.status?:"") in listOf("Air"))1f else 0.6f
        helper.itemView.item_title.setTextColor(color)
        helper.itemView.item_title.alpha = alpha
        helper.itemView.item_desc.setTextColor(color)
        helper.itemView.item_desc.alpha = alpha
        helper.itemView.item_download.setOnClickListener {
            getViewByPosition(context.episode_detail_list, index, R.id.item_layout)?.let{
                it.item_download_info.text = "获取视频信息"
            }
            context.videoPresenter.videoModel.getVideo(item.t, context.subjectPresenter.subject, BackgroundWebView(helper.itemView.context), {loaded->
                getViewByPosition(context.episode_detail_list, index, R.id.item_layout)?.let{
                    it.item_download_info.text = if(loaded  == true)"解析视频地址" else ""
                }
            }){request, _ ->
                helper.itemView.post {
                    getViewByPosition(context.episode_detail_list, index, R.id.item_layout)?.let{
                        val downloader = App.getVideoCacheModel(helper.itemView.context).getDownloader(data.getOrNull(index)?.t?:return@let, context.subjectPresenter.subject)
                        updateDownload(it, downloader?.downloadPercentage?: Float.NaN, downloader?.downloadedBytes?:0L, downloader != null)
                    }
                }
                if(request == null || request.first.startsWith("/")) return@getVideo
                DownloadService.download(helper.itemView.context, item.t, context.subjectPresenter.subject, request.first, request.second)
            }
        }
        helper.itemView.item_download.setOnLongClickListener {
            val cache = App.getVideoCacheModel(helper.itemView.context).getCache(item.t, context.subjectPresenter.subject)
            if(cache != null)
                DownloadService.remove(helper.itemView.context, item.t, context.subjectPresenter.subject)
            true
        }

        val downloader = App.getVideoCacheModel(helper.itemView.context).getDownloader(item.t, context.subjectPresenter.subject)
        updateDownload(helper.itemView, downloader?.downloadPercentage?: Float.NaN, downloader?.downloadedBytes?:0L, downloader != null)
    }

    fun updateDownload(itemView: View, percent: Float, bytes: Long, hasCache: Boolean, download: Boolean = false){
        if(hasCache && !VideoCacheModel.isFinished(percent)){
            itemView.item_progress.max = 10000
            itemView.item_progress.progress = (percent * 100).toInt()
            itemView.item_download_info.text = DownloadService.parseDownloadInfo(itemView.context, percent, bytes)
            itemView.item_progress.isEnabled = download
            itemView.item_progress.visibility = View.VISIBLE
        }else{
            itemView.item_download_info.text = if(hasCache) Formatter.formatFileSize(itemView.context, bytes) else ""
            itemView.item_progress.visibility = View.INVISIBLE
        }
        itemView.item_download.setImageResource(
                if(VideoCacheModel.isFinished(percent)) R.drawable.ic_cloud_done else if(download) R.drawable.ic_pause else R.drawable.ic_download )
    }

    //val sectionHeader = SECTION_HEADER_VIEW

    override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
        super.onAttachedToRecyclerView(recyclerView)
        FullSpanUtil.onAttachedToRecyclerView(recyclerView, this, SECTION_HEADER_VIEW)
    }

    override fun onViewAttachedToWindow(holder: BaseViewHolder) {
        super.onViewAttachedToWindow(holder)
        FullSpanUtil.onViewAttachedToWindow(holder, this, SECTION_HEADER_VIEW)
    }
}