package soko.ekibun.bangumi.ui.video.fragment.sakugabooru

import android.annotation.SuppressLint
import android.os.Bundle
import android.support.v7.widget.RecyclerView
import android.support.v7.widget.StaggeredGridLayoutManager
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import org.jsoup.Jsoup
import retrofit2.Call
import soko.ekibun.bangumi.api.ApiHelper
import soko.ekibun.bangumi.api.bangumi.bean.Episode
import soko.ekibun.bangumi.api.bangumi.bean.Subject
import soko.ekibun.bangumi.api.myanimelist.MyAnimeList
import soko.ekibun.bangumi.api.sakugabooru.Sakugabooru
import soko.ekibun.bangumi.model.BooruTagModel
import soko.ekibun.bangumi.provider.ProviderInfo
import soko.ekibun.bangumi.ui.video.VideoActivity
import soko.ekibun.bangumi.ui.video.fragment.VideoFragment
import soko.ekibun.bangumiplayer.R
import java.lang.Exception

@SuppressLint("ValidFragment")
class SakugaFragment(private val videoActivity: VideoActivity): VideoFragment() {
    override val titleRes: Int = R.string.sakugabooru

    val booruTagModel by lazy { BooruTagModel(videoActivity) }

    val adapter by lazy { BooruAdapter() }
    val recyclerView by lazy {
        val recyclerView = RecyclerView(videoActivity)
        recyclerView.layoutManager = StaggeredGridLayoutManager(2, StaggeredGridLayoutManager.VERTICAL)
        recyclerView.adapter = adapter
        adapter.setEnableLoadMore(true)
        adapter.setOnLoadMoreListener({
            loadBooru()
        }, recyclerView)

        adapter.setOnItemClickListener { _, _, position ->
            adapter.data[position].let{
                videoActivity.videoPresenter.play(Episode(0, name = "SakugaBooru ${it.source}"), videoActivity.videoPagerAdapter.subject, ProviderInfo(ProviderInfo.URL, "${it.file_url}\nfile"), listOf())
            }
        }

        recyclerView
    }
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return recyclerView
    }

    var tagsCall: Call<String>? = null
    override fun onSubjectChange(sbj: Subject) {
        if(searchTag.isNotEmpty()) return
        searchTag = booruTagModel.getTag(sbj)
        if(searchTag.isNotEmpty()){
            page = 1
            loadBooru()
            return
        }
        val regexIsAscii = Regex("^[ -~]+$")
        val tag = sbj.infobox?.map{ Pair(it.first, Jsoup.parse(it.second).body().text()) }?.firstOrNull{ it.first.contains("别名") && regexIsAscii.matches(it.second) }?.second?:sbj.name?:return
        if(tag.isEmpty()) return
        tagsCall?.cancel()

        tagsCall = if(regexIsAscii.matches(tag)) ApiHelper.buildCall { tag } else ApiHelper.buildBridgeCall(MyAnimeList.createInstance().searchPrefix(tag)){ result ->
            val item = {
                val air_year = Regex("[0-9]{4}").find(sbj.air_date?:"")?.groupValues?.getOrNull(0)?.toIntOrNull()
                val media_type = (sbj.typeString?:"TV").let { when(it){
                    "剧场版" -> "Movie"
                    else -> it }}
                val items = result.categories?.get(0)?.items?: throw Exception("no items")
                (items.firstOrNull { (air_year == null || it.payload?.start_year == air_year) && (media_type.isEmpty() || it.payload?.media_type == media_type) }?:items.firstOrNull()?: throw Exception("no item"))
            }()
            malId = item.id
            ApiHelper.buildCall { item.name }
        }
        tagsCall?.enqueue(ApiHelper.buildCallback(videoActivity, {tags->
            searchTag = tags
            Log.v("tag", tags)
            page = 1
            loadBooru()
        }, {}))
    }

    var searchTag = ""
    var malId : Int? = null
    var page  = 1
    private fun loadBooru(){
        if(searchTag.isEmpty()) return
        Sakugabooru.createInstance().getPost(searchTag.replace(" ", "*"), 50, page).enqueue(ApiHelper.buildCallback(videoActivity, {
            if(page == 1) {
                adapter.setNewData(null)
                if(it?.size?:0 == 0 && malId != null){
                    ApiHelper.buildHttpCall("${MyAnimeList.SERVER_API}/anime/$malId"){
                        val doc = Jsoup.parse(it.body()?.string()?:throw Exception("empty body"))
                        doc.select(".spaceit_pad")?.firstOrNull { it.selectFirst(".dark_text")?.text() == "English:" }?.ownText()?:""
                    }.enqueue(ApiHelper.buildCallback(null, {
                        if(it.isNotEmpty() && it != searchTag) {
                            page = 1
                            searchTag = it
                            loadBooru()
                        }
                    }))
                    malId = null
                    return@buildCallback
                }else{
                    booruTagModel.saveTag(videoActivity.videoPagerAdapter.subject, searchTag)
                }
            }
            page ++
            adapter.addData(it)
            if(it?.size?:0 < 50)
                adapter.loadMoreEnd()
            else{
                adapter.loadMoreComplete()
            }
        }, { adapter.loadMoreFail() }))
    }

}