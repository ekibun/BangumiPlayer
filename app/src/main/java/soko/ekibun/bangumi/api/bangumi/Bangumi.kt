package soko.ekibun.bangumi.api.bangumi

import android.annotation.SuppressLint
import android.util.Log
import android.webkit.CookieManager
import okhttp3.FormBody
import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import org.jsoup.nodes.TextNode
import retrofit2.Call
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.*
import soko.ekibun.bangumi.api.ApiHelper
import soko.ekibun.bangumi.api.bangumi.bean.*
import soko.ekibun.bangumi.api.bangumi.bean.Calendar
import soko.ekibun.bangumi.api.bangumi.bean.Collection
import soko.ekibun.bangumi.util.HttpUtil
import java.net.URI
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.max
import kotlin.math.min

object Bangumi {
    const val SERVER = "https://bgm.tv"

    @SuppressLint("UseSparseArrays")
    fun getSubject(subject: Subject, ua: String): Call<Subject>{
        return ApiHelper.buildHttpCall(subject.url?:"", mapOf("User-Agent" to ua)){ response ->
            val doc = Jsoup.parse(response.body()?.string()?:"")
            val type = when(doc.selectFirst("#navMenuNeue .focus").text()){
                "动画" -> SubjectType.ANIME
                "书籍" -> SubjectType.BOOK
                "音乐" -> SubjectType.MUSIC
                "游戏" -> SubjectType.GAME
                "三次元" -> SubjectType.REAL
                else -> SubjectType.ALL
            }
            //name
            val name = doc.selectFirst(".nameSingle> a")?.text()?:subject.name
            val name_cn = doc.selectFirst(".nameSingle> a")?.attr("title")?:subject.name_cn
            //summary
            val summary = Jsoup.parse(doc.selectFirst("#subject_summary")?.html()?.replace("<br>", "$$$$$")?:"")?.text()?.replace("$$$$$", "\n")?:subject.summary

            val infobox = doc.select("#infobox li")?.map{
                val tip = it.selectFirst("span.tip")?.text()?:""
                Pair(tip.trim(':',' '),
                        it.text().substring(tip.length).trim())
            }
            val eps_count = infobox?.firstOrNull { it.first == "话数" }?.second?.toIntOrNull()?:subject.eps_count
            //air-date
            val air_date = infobox?.firstOrNull { it.first in arrayOf("放送开始", "上映年度", "开始") }?.second?:""
            var air_weekday = "一二三四五六日".map { "星期$it" }.indexOf(infobox?.firstOrNull { it.first == "放送星期" }?.second?:"") + 1
            if(air_weekday == 0)
                air_weekday = "月火水木金土日".map { "${it}曜日" }.indexOf(infobox?.firstOrNull { it.first == "放送星期" }?.second?:"") + 1

            val counts = HashMap<Int, Int>()
            doc.select(".horizontalChart li")?.forEach {
                counts[it.selectFirst(".label")?.text()?.toIntOrNull()?:0] =
                        it.selectFirst(".count").text()?.trim('(',')')?.toIntOrNull()?:0
            }

            val rating = Subject.RatingBean(
                    doc.selectFirst("span[property=\"v:votes\"]")?.text()?.toIntOrNull()?:subject.rating?.total?:0,
                    Subject.RatingBean.CountBean(counts[10]?:0, counts[9]?:0, counts[8]?:0, counts[7]?:0,
                            counts[6]?:0, counts[5]?:0, counts[4]?:0, counts[3]?:0, counts[2]?:0, counts[1]?:0),
                    doc.selectFirst(".global_score .number")?.text()?.toDoubleOrNull()?:subject.rating?.score?:0.0
            )
            val rank = doc.selectFirst(".global_score .alarm")?.text()?.trim('#')?.toIntOrNull()?:subject.rank
            val cover =  doc.selectFirst(".infobox img.cover")
            val img = HttpUtil.getUrl(if(cover?.hasAttr("src") == true) cover.attr("src")?:"" else cover?.attr("data-cfsrc")?:"", URI.create(Bangumi.SERVER))
            val images = Images(
                    img.replace("/c/", "/l/"), img,
                    img.replace("/c/", "/m/"),
                    img.replace("/c/", "/s/"),
                    img.replace("/c/", "/g/"))
            //TODO Collection
            //no eps
            //crt
            val crt = doc.select(".subject_section").filter { it.select(".subtitle")?.text() == "角色介绍" }.getOrNull(0)?.select("li")?.map {
                val a = it.selectFirst("a.avatar")
                val crt_img = HttpUtil.getUrl(Regex("""background-image:url\('([^']*)'\)""").find(a?.html()?:"")?.groupValues?.get(1)
                        ?: "", URI.create(Bangumi.SERVER))
                val stars = it.select("a[rel=\"v:starring\"]").map { psn ->
                    Person(Regex("""/person/([0-9]*)""").find(psn.attr("href")
                            ?: "")?.groupValues?.get(1)?.toIntOrNull() ?: 0,
                            HttpUtil.getUrl(psn.attr("href") ?: "", URI.create(Bangumi.SERVER)),
                            psn.text() ?: "")
                }
                Character(Regex("""/character/([0-9]*)""").find(a?.attr("href")?: "")?.groupValues?.get(1)?.toIntOrNull() ?: 0,
                        HttpUtil.getUrl(a?.attr("href")?: "", URI.create(Bangumi.SERVER)),
                        a?.text() ?: "",
                        it.selectFirst(".info .tip")?.text() ?: "",
                        it.selectFirst(".info .badge_job_tip")?.text() ?: "",
                        Images(crt_img.replace("/s/", "/l/"),
                                crt_img.replace("/s/", "/c/"),
                                crt_img.replace("/s/", "/m/"), crt_img,
                                crt_img.replace("/s/", "/g/")),
                        it.selectFirst("small.fade")?.text()?.trim('(', '+', ')')?.toIntOrNull() ?: 0,
                        actors = stars)
            }
            //TODO staff
            //topic
            val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val topic = doc.select(".topic_list tr")?.map {
                val tds = it.select("td")
                val td0 = tds?.get(0)?.selectFirst("a")
                val topic_id = Regex("""/topic/([0-9]*)""").find(td0?.attr("href")?: "")?.groupValues?.get(1)?.toIntOrNull() ?: 0
                val user = tds?.get(1)?.selectFirst("a")
                val user_id = Regex("""/user/([^/]*)""").find(user?.attr("href")?:"")?.groupValues?.get(1)
                val time  = try{
                    dateFormat.parse(tds?.get(3)?.text()?:"").time /1000
                }catch (e: Exception){ 0L }
                if(td0?.attr("href").isNullOrEmpty()) null else
                    Subject.TopicBean(topic_id,
                            HttpUtil.getUrl(td0?.attr("href")?: "", URI.create(Bangumi.SERVER)),
                            td0?.text() ?: "", topic_id, time, 0,
                            Regex("""([0-9]*)""").find(tds?.get(2)?.text()?: "")?.groupValues?.get(1)?.toIntOrNull() ?: 0,
                            UserInfo(user_id?.toIntOrNull()?:0, HttpUtil.getUrl(user?.attr("href")?:"", URI.create(SERVER)), user_id, user?.text())
                    )
            }?.filterNotNull()
            //blog
            val datetimeFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
            val blog = doc.select(".subject_section").filter { it.select(".subtitle")?.text() == "评论" }.getOrNull(0)?.select("div.item")?.map {
                val a = it.selectFirst(".title a")
                val user = it.selectFirst(".tip_j a")
                val user_id = Regex("""/user/([^/]*)""").find(user?.attr("href")?:"")?.groupValues?.get(1)
                val time  = try{
                    datetimeFormat.parse(it.selectFirst("small.time")?.text()?:"").time /1000
                }catch (e: Exception){
                    e.printStackTrace()
                    0L }
                Subject.BlogBean(Regex("""/blog/([0-9]*)""").find(a?.attr("href")?: "")?.groupValues?.get(1)?.toIntOrNull() ?: 0,
                        HttpUtil.getUrl(a?.attr("href")?: "", URI.create(Bangumi.SERVER)),
                        a?.text() ?: "",
                        it.selectFirst(".content")?.ownText()?:"",
                        HttpUtil.getUrl(it.selectFirst("img")?.attr("src")?:"", URI.create(Bangumi.SERVER)),
                        it.selectFirst("small.orange")?.text()?.trim('(', '+', ')')?.toIntOrNull() ?: 0, time,
                        it.selectFirst("small.time")?.text()?:"",
                        UserInfo(user_id?.toIntOrNull()?:0, HttpUtil.getUrl(user?.attr("href")?:"", URI.create(SERVER)), user_id, user?.text())
                )
            }
            //linked
            val tankobon = doc.select(".subject_section").filter { it.select(".subtitle")?.text() == "单行本" }.getOrNull(0)?.select("li")?.map {
                val avatar = it.selectFirst(".avatar")
                val subjectImg = HttpUtil.getUrl(Regex("""background-image:url\('([^']*)'\)""").find(avatar?.html()?:"")?.groupValues?.get(1)?:"", URI.create(Bangumi.SERVER))
                val title = avatar?.attr("title")?.split("/ ")
                val url = HttpUtil.getUrl(avatar?.attr("href")?:"", URI.create(Bangumi.SERVER))
                val id = Regex("""/subject/([0-9]*)""").find(url)?.groupValues?.get(1)?.toIntOrNull()?:0
                Subject(id, url, 0, title?.getOrNull(0), title?.getOrNull(1), typeString = "单行本",
                        images = Images(subjectImg.replace("/g/", "/l/"),
                                subjectImg.replace("/g/", "/m/"),
                                subjectImg.replace("/g/", "/c/"),
                                subjectImg.replace("/g/", "/s/"), subjectImg))
            }
            var sub = ""
            val linked = doc.select(".subject_section").filter { it.select(".subtitle")?.text() == "关联条目" }.getOrNull(0)?.select("li")?.map {
                val newSub = it.selectFirst(".sub").text()
                if(!newSub.isNullOrEmpty()) sub = newSub
                val avatar = it.selectFirst(".avatar")
                val subjectImg = HttpUtil.getUrl(Regex("""background-image:url\('([^']*)'\)""").find(avatar?.html()?:"")?.groupValues?.get(1)?:"", URI.create(Bangumi.SERVER))
                val title = it.selectFirst(".title")
                val url = HttpUtil.getUrl(title?.attr("href")?:"", URI.create(Bangumi.SERVER))
                val id = Regex("""/subject/([0-9]*)""").find(url)?.groupValues?.get(1)?.toIntOrNull()?:0
                if(tankobon?.firstOrNull { b -> b.id == id } == null)
                    Subject(id, url, 0, title?.text(), avatar.attr("title"), typeString = sub,
                            images = Images(subjectImg.replace("/m/", "/l/"),
                                    subjectImg.replace("/m/", "/c/"), subjectImg,
                                    subjectImg.replace("/m/", "/s/"),
                                    subjectImg.replace("/m/", "/g/")))
                else null
            }?.filterNotNull()?.toMutableList()?:ArrayList()
            linked.addAll(0, tankobon?:ArrayList())
            //commend
            val commend = doc.select(".subject_section").filter { it.select(".subtitle")?.text()?.contains("大概会喜欢") == true }.getOrNull(0)?.select("li")?.map {
                val avatar = it.selectFirst(".avatar")
                val subjectImg = HttpUtil.getUrl(Regex("""background-image:url\('([^']*)'\)""").find(avatar?.html()?:"")?.groupValues?.get(1)?:"", URI.create(Bangumi.SERVER))
                val title = it.selectFirst(".info a")
                val url = HttpUtil.getUrl(title?.attr("href")?:"", URI.create(Bangumi.SERVER))
                val id = Regex("""/subject/([0-9]*)""").find(url)?.groupValues?.get(1)?.toIntOrNull()?:0
                Subject(id, url, 0, title?.text(), avatar.attr("title"),
                        images = Images(subjectImg.replace("/m/", "/l/"),
                                subjectImg.replace("/m/", "/c/"), subjectImg,
                                subjectImg.replace("/m/", "/s/"),
                                subjectImg.replace("/m/", "/g/")))
            }
            //tags
            val tags = doc.select(".subject_tag_section a")?.map {
                Pair(it.selectFirst("span")?.text()?:"", it.selectFirst("small")?.text()?.toIntOrNull()?:0)
            }
            //typeString
            val typeString = doc.selectFirst(".nameSingle small")?.text()?:""
            //collection
            val interest = doc.selectFirst("#collectBoxForm")?.let{
                val collectType = it.selectFirst(".collectType input[checked=checked]")
                val collectStatus = Collection.StatusBean(collectType?.attr("value")?.toIntOrNull()?:return@let null, collectType.id(), collectType.parent()?.text())
                val rate = it.selectFirst(".rating[checked]")?.attr("value")?.toIntOrNull()?:0
                val collectTags = it.selectFirst("#tags")?.attr("value")?.split(" ")?.filter { it.isNotEmpty() }?:ArrayList()
                val collectComment = it.selectFirst("#comment")?.text()
                val private = it.selectFirst("#privacy[checked]")?.attr("value")?.toIntOrNull()?:0
                return@let Collection(collectStatus, rate, collectComment, private, collectTags)
            }
            //formhash
            val formhash = if(doc.selectFirst(".guest") != null) "" else doc.selectFirst("input[name=formhash]")?.attr("value")
            Subject(subject.id, subject.url, type, name, name_cn, summary, eps_count, air_date, air_weekday, rating, rank, images, infobox = infobox,
                    crt=crt, topic = topic, blog = blog, linked = linked, commend = commend, tags = tags, typeString = typeString, formhash = formhash, interest = interest)
        }
    }

    fun updateCollectionStatus(subject: Subject, formhash: String, ua: String, status: String, tags: String, comment: String, rating: Int, privacy: Int = 0): Call<Collection>{
        val index = CollectionStatusType.status.indexOf(status)
        return ApiHelper.buildHttpCall("$SERVER/subject/${subject.id}/interest/update?gh=$formhash", mapOf("User-Agent" to ua), FormBody.Builder()
                .add("referer", "ajax")
                .add("interest", (index + 1).toString())
                .add("rating", rating.toString())
                .add("tags", tags)
                .add("comment", comment)
                .add("privacy", privacy.toString())
                .add("update", "保存").build()){
            Collection(Collection.StatusBean(index + 1, status, when(subject.type){
                SubjectType.BOOK -> listOf("想读", "读过", "在读", "搁置", "抛弃").getOrNull(index)
                SubjectType.MUSIC -> listOf("想听", "听过", "在听", "搁置", "抛弃").getOrNull(index)
                SubjectType.GAME -> listOf("想玩", "玩过", "在玩", "搁置", "抛弃").getOrNull(index)
                else -> listOf("想看", "看过", "在看", "搁置", "抛弃").getOrNull(index)
            }), rating, comment, privacy, tags.split(" ").filter { it.isNotEmpty() })
        }
    }

    fun updateProgress(id: Int,
                       @SubjectProgress.EpisodeProgress.EpisodeStatus.Companion.EpStatusType status: String,
                       formhash: String, ua: String,
                       epIds: String? = null): Call<Boolean>{
        return ApiHelper.buildHttpCall("$SERVER/subject/ep/$id/status/$status?gh=$formhash&ajax=1", mapOf("User-Agent" to ua), FormBody.Builder().add("ep_id", epIds?:id.toString()).build() ){
            return@buildHttpCall it.body()?.string()?.contains("\"status\":\"ok\"") == true
        }
    }

    //eps
    fun getSubjectEps(subject: Int, ua: String): Call<List<Episode>>{
        return ApiHelper.buildHttpCall("$SERVER/subject/$subject/ep", mapOf("User-Agent" to ua)){
            var cat = ""
            val doc = Jsoup.parse(it.body()?.string()?:"")
            val type = when(doc.selectFirst("#navMenuNeue .focus").text()){
                "动画" -> SubjectType.ANIME
                "书籍" -> SubjectType.BOOK
                "音乐" -> SubjectType.MUSIC
                "游戏" -> SubjectType.GAME
                "三次元" -> SubjectType.REAL
                else -> SubjectType.ALL
            }
            doc.select("ul.line_list>li")?.mapNotNull {
                if(it.hasClass("cat")){
                    cat = it.text()
                    null
                }else{
                    val epId = Regex("""/ep/([0-9]*)""").find(it.selectFirst("h6>a")?.attr("href")?: "")?.groupValues?.get(1)?.toIntOrNull() ?: return@mapNotNull null

                    val values = Regex("^\\D*(\\d+\\.?\\d?)\\.(.*)").find(it.selectFirst("h6>a")?.text()?:"")?.groupValues
                    val sort = values?.getOrNull(1)?.toFloatOrNull()?:0f
                    val progress = it.selectFirst(".listEpPrgManager>span")
                    val status = if(type == SubjectType.MUSIC) "Air" else it.selectFirst(".epAirStatus span")?.className()
                    val ep_name = values?.getOrNull(2)?:it.selectFirst("h6>a")?.text()?.substringAfter(".")
                    val ep_name_cn = it.selectFirst("h6>span.tip")?.text()?.substringAfter(" ")
                    val epInfo = it.select("small.grey")?.text()?.split("/")
                    val air_date = epInfo?.firstOrNull { it.trim().startsWith("首播") }?.substringAfter(":")
                    val duration = epInfo?.firstOrNull { it.trim().startsWith("时长") }?.substringAfter(":")
                    val comment = epInfo?.firstOrNull { it.trim().startsWith("讨论") }?.trim()?.substringAfter("+")?.toIntOrNull()?:0

                    Episode(epId, "$SERVER/ep/$epId", if(type == SubjectType.MUSIC) Episode.TYPE_MUSIC else when(cat){
                        "本篇" -> Episode.TYPE_MAIN
                        "特别篇" -> Episode.TYPE_SP
                        "OP" -> Episode.TYPE_OP
                        "ED" -> Episode.TYPE_ED
                        "PV" -> Episode.TYPE_PV
                        "MAD" -> Episode.TYPE_MAD
                        else -> Episode.TYPE_OTHER
                    }, sort, ep_name, ep_name_cn, duration, air_date, comment, status = status, progress =  when{
                        progress?.hasClass("statusWatched") == true -> SubjectProgress.EpisodeProgress(epId, SubjectProgress.EpisodeProgress.EpisodeStatus(SubjectProgress.EpisodeProgress.EpisodeStatus.WATCH_ID, url_name = SubjectProgress.EpisodeProgress.EpisodeStatus.WATCH, cn_name = "看过"))
                        progress?.hasClass("statusQueue") == true -> SubjectProgress.EpisodeProgress(epId, SubjectProgress.EpisodeProgress.EpisodeStatus(SubjectProgress.EpisodeProgress.EpisodeStatus.QUEUE_ID, url_name = SubjectProgress.EpisodeProgress.EpisodeStatus.QUEUE, cn_name = "想看"))
                        progress?.hasClass("statusDrop") == true -> SubjectProgress.EpisodeProgress(epId, SubjectProgress.EpisodeProgress.EpisodeStatus(SubjectProgress.EpisodeProgress.EpisodeStatus.DROP_ID, url_name = SubjectProgress.EpisodeProgress.EpisodeStatus.DROP, cn_name = "抛弃"))
                        else -> null
                    }, cat = cat)
                }
            }?:ArrayList()
        }
    }
}