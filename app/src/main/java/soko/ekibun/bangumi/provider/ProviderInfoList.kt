package soko.ekibun.bangumi.provider

data class ProviderInfoList(
        val providers: ArrayList<ProviderInfo> = ArrayList(),
        var defaultProvider: Int = 0
){
    fun getDefaultProvider(): ProviderInfo?{
        return providers.getOrNull(defaultProvider)
    }
}