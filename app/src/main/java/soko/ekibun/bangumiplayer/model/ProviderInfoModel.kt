package soko.ekibun.bangumiplayer.model

import android.content.Context
import android.content.SharedPreferences
import android.preference.PreferenceManager
import soko.ekibun.bangumi.api.bangumi.bean.Subject
import soko.ekibun.bangumi.util.JsonUtil
import soko.ekibun.bangumiplayer.provider.ProviderInfoList

class ProviderInfoModel(context: Context){
    val sp: SharedPreferences by lazy{ PreferenceManager.getDefaultSharedPreferences(context) }
    fun prefKey(subject: Subject): String{
        return PREF_PROVIDER_INFO + subject.id
    }

    fun saveInfos(subject: Subject, infos: ProviderInfoList) {
        val editor = sp.edit()
        val key = prefKey(subject)
        if(infos.providers.size == 0){
            editor.remove(key)
        }else{
            editor.putString(key, JsonUtil.toJson(infos))
        }
        editor.apply()
    }

    fun getInfos(subject: Subject): ProviderInfoList? {
        return JsonUtil.toEntity(sp.getString(prefKey(subject), JsonUtil.toJson(ProviderInfoList()))!!, ProviderInfoList::class.java)
    }

    companion object {
        const val PREF_PROVIDER_INFO="providerInfo"
    }
}