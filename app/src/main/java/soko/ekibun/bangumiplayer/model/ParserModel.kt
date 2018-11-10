package soko.ekibun.bangumiplayer.model

import retrofit2.Call
import soko.ekibun.bangumi.api.ApiHelper
import soko.ekibun.bangumi.ui.view.BackgroundWebView
import soko.ekibun.bangumiplayer.parser.ParserInfo
import soko.ekibun.bangumiplayer.provider.BaseProvider

object ParserModel {
    fun getVideo(webView: BackgroundWebView, video: BaseProvider.VideoInfo, parser: ParserInfo): Call<Pair<String, Map<String, String>>>{
        var url = parser.api
        if(url.isEmpty())
            url = video.url
        else if(url.endsWith("="))
            url += video.url
        val header = HashMap<String, String>()
        header["referer"] = url
        return ApiHelper.buildWebViewCall(webView, url, header, parser.js)
    }
}