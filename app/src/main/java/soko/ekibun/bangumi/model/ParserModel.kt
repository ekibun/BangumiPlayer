package soko.ekibun.bangumi.model

import retrofit2.Call
import soko.ekibun.bangumi.api.ApiHelper
import soko.ekibun.bangumi.ui.view.BackgroundWebView
import soko.ekibun.bangumi.parser.ParserInfo
import soko.ekibun.bangumi.provider.BaseProvider

object ParserModel {
    fun getVideo(webView: BackgroundWebView, video: BaseProvider.VideoInfo, parser: ParserInfo): Call<Pair<String, Map<String, String>>>{
        val provider = ProviderModel.providers.firstOrNull { it.siteId == video.siteId }
        if(provider?.provideVideo == true && parser.api.isEmpty() && parser.js.isEmpty())
            return provider.getVideo(webView, video)
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