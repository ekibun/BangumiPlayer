package soko.ekibun.bangumi.model

import retrofit2.Call
import soko.ekibun.bangumi.api.ApiHelper
import soko.ekibun.bangumi.api.bangumi.bean.Episode
import soko.ekibun.bangumi.provider.*

object ProviderModel{
    val providers: List<BaseProvider> = listOf(
            IqiyiProvider(),
            BilibiliProvider(),
            PptvProvider(),
            YoukuProvider(),
            TencentProvider(),
            AcfunProvider(),
            DilidiliProvider(),
            DililiProvider(),
            Anime1Provider(),
            UrlProvider(),
            NicotvProvider(),
            SilisiliProvider(),
            HalihaliProvider(),
            NingmoeProvider()
    )

    fun getProvider(siteId: Int): BaseProvider?{
        return providers.firstOrNull { it.siteId == siteId }
    }

    fun searchAll(key: String): Call<List<ProviderInfo>>{
        return ApiHelper.buildGroupCall(providers.filter { it.supportSearch }.map{ it.search(key) }.toTypedArray())
    }

    fun search(siteId: Int, key: String): Call<List<ProviderInfo>>{
        providers.forEach {
            if(it.siteId == siteId) return it.search(key)
        }
        throw Exception("no such parser")
    }

    fun getVideoInfo(info: ProviderInfo, video: Episode): Call<BaseProvider.VideoInfo>{
        providers.forEach {
            if(it.siteId == info.siteId) return it.getVideoInfo(info, video)
        }
        throw Exception("no such parser")
    }

    fun getDanmakuKey(video: BaseProvider.VideoInfo): Call<String>{
        providers.forEach {
            if(it.siteId == video.siteId) return it.getDanmakuKey(video)
        }
        throw Exception("no such parser")
    }

    fun getDanmaku(video: BaseProvider.VideoInfo, key: String, pos: Int): Call<List<BaseProvider.DanmakuInfo>>{
        providers.forEach {
            if(it.siteId == video.siteId) return it.getDanmaku(video, key, pos)
        }
        throw Exception("no such parser")
    }
}