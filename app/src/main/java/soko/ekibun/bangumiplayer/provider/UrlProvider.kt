package soko.ekibun.bangumiplayer.provider

import retrofit2.Call
import soko.ekibun.bangumi.api.ApiHelper
import soko.ekibun.bangumi.api.bangumi.bean.Episode
import soko.ekibun.bangumi.ui.view.BackgroundWebView
import java.text.DecimalFormat

class UrlProvider: BaseProvider {
    override val siteId: Int = ProviderInfo.URL
    override val name: String = "链接"
    override val color: Int = 0x888888
    override val hasDanmaku: Boolean = false
    override val supportSearch: Boolean = false

    override fun search(key: String): Call<List<ProviderInfo>> {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getVideoInfo(info: ProviderInfo, video: Episode): Call<BaseProvider.VideoInfo> {
        return ApiHelper.buildCall{BaseProvider.VideoInfo(
                info.id, siteId,
                info.id.replace("{{ep}}", DecimalFormat("#.##").format(video.sort + info.offset)))}
    }

    override fun getDanmakuKey(video: BaseProvider.VideoInfo): Call<String> {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getDanmaku(video: BaseProvider.VideoInfo, key: String, pos: Int): Call<Map<Int, List<BaseProvider.Danmaku>>> {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }
}