package soko.ekibun.bangumi.provider

import soko.ekibun.bangumi.parser.ParserInfo

data class ProviderInfo(
        var siteId: Int = 0,
        var id: String = "",
        var offset: Float = 0f,
        var title: String = "",
        var loadDanmaku: Boolean = true,
        var parser: ParserInfo? = null
){

    companion object {
        const val IQIYI = 1
        const val YOUKU = 2
        const val PPTV = 3
        const val TENCENT = 4
        const val DILIDLILI = 5
        const val URL = 6
        const val BILIBILI = 7
        const val DILILI = 8
        const val ACFUN = 9
        const val ANIME1 = 10
        const val NICOTV = 11
        const val SILISILI = 12
        const val HALIHALI = 13
    }
}