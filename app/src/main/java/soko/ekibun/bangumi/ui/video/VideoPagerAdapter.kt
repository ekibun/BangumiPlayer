package soko.ekibun.bangumi.ui.video

import android.annotation.SuppressLint
import android.app.Dialog
import android.support.design.widget.Snackbar
import android.support.v4.app.FragmentManager
import android.support.v4.app.FragmentPagerAdapter
import android.support.v4.view.ViewPager
import android.support.v7.app.AlertDialog
import android.support.v7.widget.PopupMenu
import android.view.*
import kotlinx.android.synthetic.main.activity_video.*
import kotlinx.android.synthetic.main.dialog_episode.view.*
import kotlinx.android.synthetic.main.fragment_bangumi.view.*
import kotlinx.android.synthetic.main.subject_detail.view.*
import retrofit2.Call
import soko.ekibun.bangumi.api.ApiHelper
import soko.ekibun.bangumi.api.bangumi.Bangumi
import soko.ekibun.bangumi.api.bangumi.bean.*
import soko.ekibun.bangumi.api.bangumi.bean.Collection
import soko.ekibun.bangumi.api.trim21.BgmIpViewer
import soko.ekibun.bangumi.api.trim21.bean.IpView
import soko.ekibun.bangumi.model.ProviderInfoModel
import soko.ekibun.bangumi.provider.ProviderInfoList
import soko.ekibun.bangumi.ui.subject.EditSubjectDialog
import soko.ekibun.bangumi.ui.video.fragment.VideoFragment
import soko.ekibun.bangumi.ui.video.fragment.subject.SubjectFragment
import soko.ekibun.bangumi.ui.video.line.LineDialog
import soko.ekibun.bangumi.util.Bridge
import soko.ekibun.bangumiplayer.R

class VideoPagerAdapter(private val context: VideoActivity, fragmentManager: FragmentManager, pager: ViewPager): FragmentPagerAdapter(fragmentManager) {
    val subjectFragment = SubjectFragment(context)
    private val providerInfoModel by lazy { ProviderInfoModel(context) }
    val fragments: List<VideoFragment> = listOf(
            subjectFragment
    )

    var subject: Subject = Subject()
        set(value){
            field = value
            fragments.forEach {
                it.onSubjectChange(field)
            }
            refreshLines(field)
            refreshCollection(field)
        }
    fun refreshSubject(){
        refreshSubject(subject)
        refreshProgress(subject)
    }

    fun init(subject: Subject){
        this.subject = subject
        refreshSubject()

        context.videoPresenter.playEpisode  = { ep: Episode ->
            val infos = providerInfoModel.getInfos(subject)
            infos?.getDefaultProvider()?.let{
                context.videoPresenter.prevEpisode = {
                    val position = subjectFragment.episodeDetailAdapter.data.indexOfFirst { it.t?.id == ep.id || (it.t?.type == ep.type && it.t?.sort == ep.sort) }
                    val episode = subjectFragment.episodeDetailAdapter.data.getOrNull(position-1)?.t
                    if((episode?.status?:"") !in listOf("Air")) null else episode
                }
                context.videoPresenter.nextEpisode = {
                    val position = subjectFragment.episodeDetailAdapter.data.indexOfFirst { it.t?.id == ep.id || (it.t?.type == ep.type && it.t?.sort == ep.sort) }
                    val episode = subjectFragment.episodeDetailAdapter.data.getOrNull(position+1)?.t
                    if((episode?.status?:"") !in listOf("Air")) null else episode
                }
                context.videoPresenter.startAt = null
                context.runOnUiThread { context.videoPresenter.play(ep, subject, it, infos.providers) }
            }?: Snackbar.make(context.root_layout, "请先添加播放源", Snackbar.LENGTH_SHORT).show()
        }

        subjectFragment.episodeAdapter.setOnItemChildClickListener { _, _, position ->
            context.videoPresenter.playEpisode(subjectFragment.episodeAdapter.data[position])
        }

        subjectFragment.episodeAdapter.setOnItemChildLongClickListener { _, _, position ->
            val eps = subjectFragment.episodeAdapter.data.subList(0, position + 1)
            subjectFragment.episodeAdapter.data[position]?.let{ openEpisode(it, subject, eps) }
            true
        }

        subjectFragment.episodeDetailAdapter.setOnItemClickListener { _, _, position ->
            context.videoPresenter.playEpisode(subjectFragment.episodeDetailAdapter.data[position].t)
        }

        subjectFragment.episodeDetailAdapter.setOnItemLongClickListener { _, _, position ->
            val eps = subjectFragment.episodeDetailAdapter.data.subList(0, position + 1).filter { !it.isHeader }.map { it.t }
            subjectFragment.episodeDetailAdapter.data[position]?.t?.let{ openEpisode(it, subject, eps) }
            true
        }

        subjectFragment.seasonAdapter.setOnItemClickListener { _, _, position ->
            val item = subjectFragment.seasonAdapter.data[position]
            if(item.id != subjectFragment.seasonAdapter.currentId)
                VideoActivity.startActivity(context, Subject(item.subject_id, "${Bangumi.SERVER}/subject/${item.subject_id}", subject.type, item.name, item.name_cn,
                        images = Images(item.image?.replace("/g/", "/l/"),
                                item.image?.replace("/g/", "/c/"),
                                item.image?.replace("/g/", "/m/"),
                                item.image?.replace("/g/", "/s/"),
                                item.image)))
        }
    }

    private var subjectCall : Call<Subject>? = null
    private fun refreshSubject(subject: Subject){
        subjectCall?.cancel()
        subjectCall = Bangumi.getSubject(subject, context.ua)
        subjectCall?.enqueue(ApiHelper.buildCallback(context, {
            this.subject = it

        }, {}))

        BgmIpViewer.createInstance().subject(subject.id).enqueue(ApiHelper.buildCallback(context, {
            val bgmIp = it.nodes?.firstOrNull { it.subject_id == subject.id }?:return@buildCallback
            val id = it.edges?.firstOrNull{edge-> edge.source == bgmIp.id && edge.relation == "主线故事"}?.target?:bgmIp.id
            val ret = java.util.ArrayList<IpView.Node>()
            it.edges?.filter { edge-> edge.target == id && edge.relation == "主线故事" }?.reversed()?.forEach { edge->
                ret.add(0, it.nodes.firstOrNull{it.id == edge.source}?:return@forEach)
            }
            ret.add(0,it.nodes.firstOrNull { it.id == id }?:return@buildCallback)
            var prevId = id
            while(true){
                prevId = it.edges?.firstOrNull { it.source == prevId && it.relation == "前传"}?.target?:break
                it.edges.filter { edge-> edge.target == prevId && edge.relation == "主线故事" }.reversed().forEach {edge->
                    ret.add(0, it.nodes.firstOrNull{it.id == edge.source}?:return@forEach)
                }
                ret.add(0, it.nodes.firstOrNull{it.id == prevId}?:break)
            }
            var nextId = id
            while(true){
                nextId = it.edges?.firstOrNull { it.source == nextId && it.relation == "续集"}?.target?:break
                ret.add(it.nodes.firstOrNull{it.id == nextId}?:break)
                it.edges.filter { edge-> edge.target == nextId && edge.relation == "主线故事" }.forEach {edge->
                    ret.add(it.nodes.firstOrNull{it.id == edge.source}?:return@forEach)
                }
            }
            if(ret.size > 1){
                subjectFragment.seasonAdapter.setNewData(ret.distinct())
                subjectFragment.seasonAdapter.currentId = bgmIp.id
                subjectFragment.seasonlayoutManager.scrollToPositionWithOffset(subjectFragment.seasonAdapter.data.indexOfFirst { it.id == bgmIp.id }, 0)
            }
        }, {}))
    }

    private fun refreshLines(subject: Subject){
        val infos = providerInfoModel.getInfos(subject)
        context.root_layout.post { subjectFragment.lineAdapter.setNewData(infos?.providers) }
        infos?.let{
            //subjectView.lineAdapter.setNewData(it.providers)
            subjectFragment.lineAdapter.selectIndex = it.defaultProvider

            subjectFragment.lineAdapter.setOnItemClickListener { _, _, position ->
                it.defaultProvider = position
                providerInfoModel.saveInfos(subject, it)
                refreshLines(subject)
            }
            subjectFragment.lineAdapter.onSwitchChange = {position: Int, isCheck: Boolean ->
                it.providers[position].loadDanmaku = isCheck
                providerInfoModel.saveInfos(subject, it)
                refreshLines(subject)
            }
            subjectFragment.lineAdapter.setOnItemLongClickListener { _, _, position ->
                LineDialog.showDialog(context, context.root_layout, subject, it.providers[position]){ info, newLine->
                    when {
                        info == null -> {
                            it.providers.removeAt(position)
                            it.defaultProvider -= if(it.defaultProvider > position) 1 else 0
                            it.defaultProvider = Math.max(0, Math.min(it.providers.size -1, it.defaultProvider))
                        }
                        newLine -> it.providers.add(info)
                        else -> it.providers[position] = info
                    }
                    providerInfoModel.saveInfos(subject, it)
                    refreshLines(subject)
                }
                true
            }
        }

        subjectFragment.detail.item_lines.setOnClickListener{
            LineDialog.showDialog(context, context.root_layout, subject){ info, _->
                if(info == null) return@showDialog
                val providerInfos = providerInfoModel.getInfos(subject)?: ProviderInfoList()
                providerInfos.providers.add(info)
                providerInfoModel.saveInfos(subject, providerInfos)
                refreshLines(subject)
            }
        }
    }

    private fun removeCollection(subject: Subject){
        AlertDialog.Builder(context).setTitle("删除这个条目收藏？")
                .setNegativeButton("取消") { _, _ -> }.setPositiveButton("确定") { _, _ ->
                    ApiHelper.buildHttpCall("${Bangumi.SERVER}/subject/${subject.id}/remove?gh=${context.formhash}", mapOf("User-Agent" to context.ua)){ it.code() == 200 }
                            .enqueue(ApiHelper.buildCallback(context, {
                                if(it) subject.interest = Collection()
                                refreshCollection(subject)
                            }, {}))
                }.show()
    }

    private fun refreshCollection(subject: Subject){
        val body = subject.interest?: Collection()
        val status = body.status
        subjectFragment.detail.item_collect_image.setImageDrawable(context.resources.getDrawable(
                if(status?.id in listOf(1, 2, 3, 4)) R.drawable.ic_heart else R.drawable.ic_heart_outline, context.theme))
        subjectFragment.detail.item_collect_info.text = status?.name?:context.getString(R.string.collect)

        subjectFragment.detail.item_collect.setOnClickListener{

            if(context.formhash.isEmpty()) return@setOnClickListener
            val popupMenu = PopupMenu(context, subjectFragment.detail.item_collect)
            val statusList = context.resources.getStringArray(R.array.collection_status)
            statusList.forEachIndexed { index, s ->
                popupMenu.menu.add(Menu.NONE, Menu.FIRST + index, index, s) }
            if(status != null)
                popupMenu.menu.add(Menu.NONE, Menu.FIRST + statusList.size, statusList.size, "删除")
            popupMenu.setOnMenuItemClickListener {menu->
                if(menu.itemId == Menu.FIRST + statusList.size){
                    removeCollection(subject)
                    return@setOnMenuItemClickListener false
                }
                val newStatus = CollectionStatusType.status[menu.itemId - Menu.FIRST]
                val newTags = if(body.tag?.isNotEmpty() == true) body.tag.reduce { acc, s -> "$acc $s" } else ""
                Bangumi.updateCollectionStatus(subject, context.formhash, context.ua,
                        newStatus, newTags, body.comment?:"", body.rating, body.private).enqueue(ApiHelper.buildCallback(context,{
                    subject.interest = it
                    refreshCollection(subject)
                },{}))
                false
            }
            popupMenu.show()
        }

        subjectFragment.detail.item_collect.setOnLongClickListener {
            EditSubjectDialog.showDialog(context, subject, body, context.formhash, context.ua){
                if(it) removeCollection(subject)
                else refreshCollection(subject)
            }
            true
        }
    }

    @SuppressLint("SetTextI18n")
    private fun openEpisode(episode: Episode, subject: Subject, eps: List<Episode>){
        val view = context.layoutInflater.inflate(R.layout.dialog_episode, context.root_layout, false)
        view.item_episode_title.text = episode.parseSort() + " " + if(episode.name_cn.isNullOrEmpty()) episode.name else episode.name_cn
        view.item_episode_desc.text = (if(episode.name_cn.isNullOrEmpty()) "" else episode.name + "\n") +
                (if(episode.airdate.isNullOrEmpty()) "" else "首播：" + episode.airdate + "\n") +
                (if(episode.duration.isNullOrEmpty()) "" else "时长：" + episode.duration + "\n") +
                "讨论 (+" + episode.comment + ")"
        view.item_episode_title.setOnClickListener {
            Bridge.launchUrl(context, "${Bangumi.SERVER}/m/topic/ep/${episode.id}", "")
        }
        when(episode.progress?.status?.id?:0){
            1 -> view.radio_queue.isChecked = true
            2 -> view.radio_watch.isChecked = true
            3 -> view.radio_drop.isChecked = true
            else -> view.radio_remove.isChecked = true
        }
        //view.item_episode_status.setSelection(intArrayOf(4,2,0,3)[episode.progress?.status?.id?:0])
        if(episode.type != Episode.TYPE_MUSIC) {
            view.item_episode_status.setOnCheckedChangeListener { _, checkedId ->
                updateProgress(subject, if(checkedId == R.id.radio_watch_to)eps else listOf(episode), when(checkedId){
                    R.id.radio_watch_to -> WATCH_TO
                    R.id.radio_watch -> SubjectProgress.EpisodeProgress.EpisodeStatus.WATCH
                    R.id.radio_queue -> SubjectProgress.EpisodeProgress.EpisodeStatus.QUEUE
                    R.id.radio_drop -> SubjectProgress.EpisodeProgress.EpisodeStatus.DROP
                    else -> SubjectProgress.EpisodeProgress.EpisodeStatus.REMOVE
                })
            }
        }else {
            view.item_episode_status.visibility = View.GONE
        }
        val dialog = Dialog(context, R.style.AppTheme_Dialog_Floating)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setContentView(view)
        dialog.window?.setGravity(Gravity.BOTTOM)
        dialog.window?.attributes?.let{
            it.width = ViewGroup.LayoutParams.MATCH_PARENT
            dialog.window?.attributes = it
        }
        dialog.window?.setWindowAnimations(R.style.AnimDialog)
        dialog.setCanceledOnTouchOutside(true)
        dialog.show()
    }

    private fun updateProgress(subject: Subject, eps: List<Episode>, newStatus: String){
        if(newStatus == WATCH_TO){
            val epIds = eps.map{ it.id.toString()}.reduce { acc, s -> "$acc,$s" }
            Bangumi.updateProgress(eps.last().id, SubjectProgress.EpisodeProgress.EpisodeStatus.WATCH, context.formhash, context.ua, epIds).enqueue(
                    ApiHelper.buildCallback(context, {
                        val epStatus = SubjectProgress.EpisodeProgress.EpisodeStatus.getStatus(SubjectProgress.EpisodeProgress.EpisodeStatus.WATCH)
                        eps.forEach { it.progress = if(epStatus != null) SubjectProgress.EpisodeProgress(it.id, epStatus) else null }
                        subjectFragment.episodeAdapter.notifyDataSetChanged()
                        subjectFragment.episodeDetailAdapter.notifyDataSetChanged()
                        refreshProgress(subject)
                    }, {}))
            return
        }
        eps.forEach {episode->
            Bangumi.updateProgress(episode.id, newStatus, context.formhash, context.ua).enqueue(
                    ApiHelper.buildCallback(context, {
                        val epStatus = SubjectProgress.EpisodeProgress.EpisodeStatus.getStatus(newStatus)
                        episode.progress = if(epStatus != null) SubjectProgress.EpisodeProgress(episode.id, epStatus) else null
                        subjectFragment.episodeAdapter.notifyDataSetChanged()
                        subjectFragment.episodeDetailAdapter.notifyDataSetChanged()
                        refreshProgress(subject)
                    }, {}))
        }
    }

    private var epCalls: Call<List<Episode>>? = null
    private fun refreshProgress(subject: Subject){
        epCalls?.cancel()
        epCalls = Bangumi.getSubjectEps(subject.id, context.ua)
        epCalls?.enqueue(ApiHelper.buildCallback(context, {
            subjectFragment.updateEpisode(it, subject)
        }, {}))
    }

    init{
        pager.offscreenPageLimit = 4
        pager.adapter = this
        context.item_tabs.setupWithViewPager(pager)
    }

    override fun getItem(pos: Int): VideoFragment {
        return fragments[pos]
    }

    override fun getCount(): Int {
        return fragments.size
    }

    override fun getPageTitle(pos: Int): CharSequence{
        return context.getString(fragments[pos].titleRes)
    }

    companion object {
        const val WATCH_TO = "watch_to"
    }
}