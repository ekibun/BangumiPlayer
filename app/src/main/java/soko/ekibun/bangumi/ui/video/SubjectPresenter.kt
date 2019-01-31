package soko.ekibun.bangumi.ui.video

import android.annotation.SuppressLint
import android.app.Dialog
import android.support.v7.widget.PopupMenu
import android.view.*
import kotlinx.android.synthetic.main.activity_video.*
import kotlinx.android.synthetic.main.dialog_episode.view.*
import kotlinx.android.synthetic.main.subject_detail.*
import retrofit2.Call
import soko.ekibun.bangumi.api.ApiHelper
import soko.ekibun.bangumi.api.bangumi.Bangumi
import soko.ekibun.bangumi.api.bangumi.bean.*
import soko.ekibun.bangumi.api.trim21.BgmIpViewer
import soko.ekibun.bangumi.api.trim21.bean.IpView
import soko.ekibun.bangumi.util.JsonUtil
import soko.ekibun.bangumiplayer.R
import soko.ekibun.bangumi.model.ProviderInfoModel
import soko.ekibun.bangumi.provider.ProviderInfoList
import soko.ekibun.bangumi.ui.subject.EditSubjectDialog
import soko.ekibun.bangumi.ui.video.line.LineDialog


class SubjectPresenter(private val context: VideoActivity){
    val api by lazy { Bangumi.createInstance() }
    val subjectView by lazy{ SubjectView(context) }
    private val providerInfoModel by lazy { ProviderInfoModel(context) }

    val subject by lazy{ JsonUtil.toEntity(context.intent.getStringExtra(VideoActivity.EXTRA_SUBJECT), Subject::class.java)!! }
    val token by lazy{ JsonUtil.toEntity(context.intent.getStringExtra(VideoActivity.EXTRA_TOKEN), AccessToken::class.java)!! }

    fun refreshSubject(){
        subjectView.updateSubject(subject)
        refreshLines(subject)
        refreshSubject(subject)
        refreshCollection(subject)
        refreshProgress(subject)
    }

    init{
        refreshSubject()

        context.videoPresenter.doPlay = { position: Int ->
            val episode = subjectView.episodeDetailAdapter.data[position]?.t
            if(episode != null){
                val infos = providerInfoModel.getInfos(subject)
                infos?.getDefaultProvider()?.let{
                    val episodePrev = subjectView.episodeDetailAdapter.data.getOrNull(position-1)?.t
                    val episodeNext = subjectView.episodeDetailAdapter.data.getOrNull(position+1)?.t
                    context.videoPresenter.prev = if(episodePrev == null || (episodePrev.status?:"") !in listOf("Air")) null else position - 1
                    context.videoPresenter.next = if(episodeNext == null || (episodeNext.status?:"") !in listOf("Air")) null else position + 1
                    context.runOnUiThread { context.videoPresenter.play(episode, subject, it, infos.providers) }
                }//TODO ?:episode.url?.let{ WebActivity.launchUrl(context, it) }
            }
        }

        subjectView.episodeAdapter.setOnItemChildClickListener { _, _, position ->
            subjectView.episodeAdapter.data[position]?.let{episode->
                context.videoPresenter.doPlay(subjectView.episodeDetailAdapter.data.indexOfFirst { it.t == episode })
            }
        }

        subjectView.episodeAdapter.setOnItemChildLongClickListener { _, _, position ->
            val eps = subjectView.episodeAdapter.data.subList(0, position + 1)
            subjectView.episodeAdapter.data[position]?.let{ openEpisode(it, subject, eps) }
            true
        }

        subjectView.episodeDetailAdapter.setOnItemClickListener { _, _, position ->
            context.videoPresenter.doPlay(position)
        }

        subjectView.episodeDetailAdapter.setOnItemLongClickListener { _, _, position ->
            val eps = subjectView.episodeDetailAdapter.data.subList(0, position + 1).filter { !it.isHeader }.map { it.t }
            subjectView.episodeDetailAdapter.data[position]?.t?.let{ openEpisode(it, subject, eps) }
            true
        }

        subjectView.seasonAdapter.setOnItemClickListener { _, _, position ->
            val item = subjectView.seasonAdapter.data[position]
            if(item.id != subjectView.seasonAdapter.currentId)
                VideoActivity.startActivity(context, Subject(item.subject_id, "${Bangumi.SERVER}/subject/${item.subject_id}", subject.type, item.name, item.name_cn,
                        images = Images(item.image?.replace("/g/", "/l/"),
                                item.image?.replace("/g/", "/c/"),
                                item.image?.replace("/g/", "/m/"),
                                item.image?.replace("/g/", "/s/"),
                                item.image)), token)
        }
    }

    @SuppressLint("SetTextI18n")
    private fun openEpisode(episode: Episode, subject: Subject, eps: List<Episode>){
        val view = context.layoutInflater.inflate(R.layout.dialog_episode, context.item_detail, false)
        view.item_episode_title.text = if(episode.name_cn.isNullOrEmpty()) episode.name else episode.name_cn
        view.item_episode_desc.text = (if(episode.name_cn.isNullOrEmpty()) "" else episode.name + "\n") +
                (if(episode.airdate.isNullOrEmpty()) "" else "首播：" + episode.airdate + "\n") +
                (if(episode.duration.isNullOrEmpty()) "" else "时长：" + episode.duration + "\n") +
                "讨论 (+" + episode.comment + ")"
        view.item_episode_title.setOnClickListener {
            //WebActivity.launchUrl(context, episode.url)
        }
        when(episode.progress?.status?.id?:0){
            1 -> view.radio_queue.isChecked = true
            2 -> view.radio_watch.isChecked = true
            3 -> view.radio_drop.isChecked = true
            else -> view.radio_remove.isChecked = true
        }
       if(!token.access_token.isNullOrEmpty()) {
           view.item_episode_status.setOnCheckedChangeListener { _, checkedId ->
               val newStatus = when(checkedId){
                   R.id.radio_watch_to ->{
                       val epIds = eps.map{ it.id.toString()}.reduce { acc, s -> "$acc,$s" }
                       api.updateProgress(eps.last().id, SubjectProgress.EpisodeProgress.EpisodeStatus.WATCH, token.access_token ?: "", epIds).enqueue(
                               ApiHelper.buildCallback(context, {
                                   refreshProgress(subject)
                               }, {}))
                       return@setOnCheckedChangeListener }
                   R.id.radio_watch -> SubjectProgress.EpisodeProgress.EpisodeStatus.WATCH
                   R.id.radio_queue -> SubjectProgress.EpisodeProgress.EpisodeStatus.QUEUE
                   R.id.radio_drop -> SubjectProgress.EpisodeProgress.EpisodeStatus.DROP
                   else -> SubjectProgress.EpisodeProgress.EpisodeStatus.REMOVE }
               api.updateProgress(episode.id, newStatus, token.access_token ?: "").enqueue(
                       ApiHelper.buildCallback(context, {
                           refreshProgress(subject)
                       }, {}))
           }
        } else {
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

    private fun refreshProgress(subject: Subject){
        if(!token.access_token.isNullOrEmpty()) {
            api.progress(token.user_id.toString(), subject.id, token.access_token?:"").enqueue(ApiHelper.buildCallback(context, {
                subjectView.progress = it
            }, {
                subjectView.progress = null
                subjectView.loadedProgress = true }))
        }
    }

    private var subjectCall : Call<Subject>? = null
    private fun refreshSubject(subject: Subject){
        subjectCall?.cancel()
        subjectCall = api.subject(subject.id)
        subjectCall?.enqueue(ApiHelper.buildCallback(context, {
            refreshLines(it)
            subjectView.updateSubject(it)
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
                subjectView.seasonAdapter.setNewData(ret.distinct())
                subjectView.seasonAdapter.currentId = bgmIp.id
                subjectView.seasonlayoutManager.scrollToPositionWithOffset(ret.indexOfFirst { it.id == bgmIp.id }, 0)
            }
        }, {}))
    }

    private fun refreshLines(subject: Subject){
        val infos = providerInfoModel.getInfos(subject)
        context.line_list.post { subjectView.lineAdapter.setNewData(infos?.providers) }
        infos?.let{
            //subjectView.lineAdapter.setNewData(it.providers)
            subjectView.lineAdapter.selectIndex = it.defaultProvider

            subjectView.lineAdapter.setOnItemClickListener { _, _, position ->
                it.defaultProvider = position
                providerInfoModel.saveInfos(subject, it)
                refreshLines(subject)
            }
            subjectView.lineAdapter.onSwitchChange = {position: Int, isCheck: Boolean ->
                it.providers[position].loadDanmaku = isCheck
                providerInfoModel.saveInfos(subject, it)
                refreshLines(subject)
            }
            subjectView.lineAdapter.setOnItemLongClickListener { _, _, position ->
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

        context.item_lines.setOnClickListener{
            LineDialog.showDialog(context, context.root_layout, subject){info, _->
                if(info == null) return@showDialog
                val providerInfos = providerInfoModel.getInfos(subject)?: ProviderInfoList()
                providerInfos.providers.add(info)
                providerInfoModel.saveInfos(subject, providerInfos)
                refreshLines(subject)
            }
        }
    }

    private fun refreshCollection(subject: Subject){
        if(!token.access_token.isNullOrEmpty()) {
            //Log.v("token", token.toString())
            api.collectionStatus(subject.id, token.access_token?:"").enqueue(ApiHelper.buildCallback(context, { body->
                val status = body.status
                if(status != null){
                    context.item_collect_image.setImageDrawable(context.resources.getDrawable(
                            if(status.id in listOf(1, 2, 3, 4)) R.drawable.ic_heart else R.drawable.ic_heart_outline, context.theme))
                    context.item_collect_info.text = status.name?:""
                }

                context.item_collect.setOnClickListener{
                    val popupMenu = PopupMenu(context, context.item_collect)
                    val statusList = context.resources.getStringArray(R.array.collection_status)
                    statusList.forEachIndexed { index, s ->
                        popupMenu.menu.add(Menu.NONE, Menu.FIRST + index, index, s) }
                    popupMenu.setOnMenuItemClickListener {menu->
                        val newStatus = CollectionStatusType.status[menu.itemId - Menu.FIRST]
                        val newTags = if(body.tag?.isNotEmpty() == true) body.tag.reduce { acc, s -> "$acc $s" } else ""
                        api.updateCollectionStatus(subject.id, token.access_token?:"",
                                newStatus, newTags, body.comment, body.rating, body.private).enqueue(ApiHelper.buildCallback(context,{},{
                            refreshCollection(subject)
                        }))
                        false
                    }
                    popupMenu.show()
                }

                context.item_collect.setOnLongClickListener {
                    EditSubjectDialog.showDialog(context, subject, body, "", token.access_token?:""){
                        refreshCollection(subject)
                    }
                    true
                }
            }, {}))
        }
    }
}