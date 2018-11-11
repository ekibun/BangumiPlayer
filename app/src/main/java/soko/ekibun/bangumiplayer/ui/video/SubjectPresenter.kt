package soko.ekibun.bangumiplayer.ui.video

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.DialogInterface
import android.view.View
import android.widget.AdapterView
import kotlinx.android.synthetic.main.activity_video.*
import kotlinx.android.synthetic.main.dialog_edit_subject.view.*
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
import soko.ekibun.bangumiplayer.model.ProviderInfoModel
import soko.ekibun.bangumiplayer.provider.ProviderInfoList
import soko.ekibun.bangumiplayer.ui.video.line.LineDialog


class SubjectPresenter(private val context: VideoActivity){
    val api by lazy { Bangumi.createInstance() }
    val subjectView by lazy{ SubjectView(context) }
    private val providerInfoModel by lazy { ProviderInfoModel(context) }

    val subject by lazy{ JsonUtil.toEntity(context.intent.getStringExtra(VideoActivity.EXTRA_SUBJECT), Subject::class.java)!! }
    val token by lazy{ JsonUtil.toEntity(context.intent.getStringExtra(VideoActivity.EXTRA_TOKEN), AccessToken::class.java)!! }

    init{
        subjectView.updateSubject(subject)
        refreshLines(subject)
        refreshSubject(subject)
        refreshCollection(subject)
        refreshProgress(subject)

        context.videoPresenter.doPlay = { position: Int ->
            val episode = subjectView.episodeDetailAdapter.data[position]?.t
            if(episode != null){
                val infos = providerInfoModel.getInfos(subject)
                infos?.getDefaultProvider()?.let{
                    val episodePrev = subjectView.episodeDetailAdapter.data.getOrNull(position-1)?.t
                    val episodeNext = subjectView.episodeDetailAdapter.data.getOrNull(position+1)?.t
                    context.videoPresenter.prev = if(episodePrev == null || (episodePrev.status?:"") !in listOf("Air")) null else position - 1
                    context.videoPresenter.next = if(episodeNext == null || (episodeNext.status?:"") !in listOf("Air")) null else position + 1
                    context.runOnUiThread { context.videoPresenter.play(episode, it, infos.providers) }
                }//TODO ?:episode.url?.let{ WebActivity.launchUrl(context, it) }
            }
        }

        subjectView.episodeAdapter.setOnItemClickListener { _, _, position ->
            subjectView.episodeAdapter.data[position]?.let{episode->
                context.videoPresenter.doPlay(subjectView.episodeDetailAdapter.data.indexOfFirst { it.t == episode })
            }
        }

        subjectView.episodeAdapter.setOnItemLongClickListener { _, _, position ->
            subjectView.episodeAdapter.data[position]?.let{ openEpisode(it, subject) }
            true
        }

        subjectView.episodeDetailAdapter.setOnItemClickListener { _, _, position ->
            context.videoPresenter.doPlay(position)
        }

        subjectView.episodeDetailAdapter.setOnItemLongClickListener { _, _, position ->
            subjectView.episodeDetailAdapter.data[position]?.t?.let{ openEpisode(it, subject) }
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
    private fun openEpisode(episode: Episode, subject: Subject){
        val view = context.layoutInflater.inflate(R.layout.dialog_episode, context.item_detail, false)
        view.item_episode_title.text = if(episode.name_cn.isNullOrEmpty()) episode.name else episode.name_cn
        view.item_episode_desc.text = (if(episode.name_cn.isNullOrEmpty()) "" else episode.name + "\n") +
                (if(episode.airdate.isNullOrEmpty()) "" else "首播：" + episode.airdate + "\n") +
                (if(episode.duration.isNullOrEmpty()) "" else "时长：" + episode.duration + "\n") +
                "讨论 (+" + episode.comment + ")"
        view.item_episode_title.setOnClickListener {
            //WebActivity.launchUrl(context, episode.url)
        }
        view.item_episode_status.setSelection(intArrayOf(3,1,0,2)[episode.progress?.status?.id?:0])
       if(!token.access_token.isNullOrEmpty()) {
            view.item_episode_status.onItemSelectedListener = object: AdapterView.OnItemSelectedListener{
            override fun onNothingSelected(parent: AdapterView<*>?) {}
            override fun onItemSelected(parent: AdapterView<*>?, v: View?, position: Int, id: Long) {
                val newStatus = SubjectProgress.EpisodeProgress.EpisodeStatus.types[position]
                api.updateProgress(episode.id, newStatus, token.access_token ?: "").enqueue(
                        ApiHelper.buildCallback(context, {
                            refreshProgress(subject)
                        }, {}))
            } }
        } else {
            view.item_episode_status.visibility = View.GONE
        }
        AlertDialog.Builder(context)
                .setView(view).show()
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
        //context.data_layout.visibility = View.GONE
        //context.subject_swipe.isRefreshing = true
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
                LineDialog.showDialog(context, context.root_layout, subject, it.providers[position]){info->
                    if(info == null){
                        it.providers.removeAt(position)
                        it.defaultProvider -= if(it.defaultProvider > position) 1 else 0
                        it.defaultProvider = Math.max(0, Math.min(it.providers.size -1, it.defaultProvider))
                    } else
                        it.providers[position] = info
                    providerInfoModel.saveInfos(subject, it)
                    refreshLines(subject)
                }
                true
            }
        }

        context.item_lines.setOnClickListener{
            LineDialog.showDialog(context, context.root_layout, subject){
                if(it == null) return@showDialog
                val providerInfos = providerInfoModel.getInfos(subject)?: ProviderInfoList()
                providerInfos.providers.add(it)
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

                context.item_collect.setOnClickListener {
                    val view = context.layoutInflater.inflate(R.layout.dialog_edit_subject, context.item_collect, false)
                    if(status != null){
                        view.item_status.setSelection(status.id-1)
                        view.item_rating.rating = body.rating.toFloat()
                        view.item_comment.setText(body.comment)
                        view.item_private.isChecked = body.private == 1
                    }
                    AlertDialog.Builder(context)
                            .setView(view)
                            .setPositiveButton("提交"){ _: DialogInterface, _: Int ->
                                val newStatus = CollectionStatusType.status[view.item_status.selectedItemId.toInt()]
                                val newRating = view.item_rating.rating.toInt()
                                val newComment = view.item_comment.text.toString()
                                val newPrivacy = if(view.item_private.isChecked) 1 else 0
                                //Log.v("new", "$new_status,$new_rating,$new_comment")
                                api.updateCollectionStatus(subject.id, token.access_token?:"",
                                        newStatus, newComment, newRating, newPrivacy).enqueue(ApiHelper.buildCallback(context,{},{
                                    refreshCollection(subject)
                                }))
                            }.show()
                }
            }, {}))
        }
    }
}