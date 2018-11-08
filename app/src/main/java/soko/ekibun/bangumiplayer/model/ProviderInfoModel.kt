package soko.ekibun.bangumiplayer.model

import android.content.Context
import android.content.SharedPreferences
import android.preference.PreferenceManager
import com.google.gson.reflect.TypeToken
import soko.ekibun.bangumi.api.bangumi.bean.Subject
import soko.ekibun.bangumi.util.JsonUtil
import soko.ekibun.bangumiplayer.provider.ProviderInfoList

class ProviderInfoModel(context: Context){
    val sp: SharedPreferences by lazy{ PreferenceManager.getDefaultSharedPreferences(context) }
    fun saveInfos(subject: Subject, infos: ProviderInfoList) {
        val editor = sp.edit()
        val list = JsonUtil.toEntity<Map<Int, ProviderInfoList>>(sp.getString(PREF_PROVIDER_INFO, JsonUtil.toJson(HashMap<Int, ProviderInfoList>()))!!, object: TypeToken<Map<Int, ProviderInfoList>>(){}.type)?.toMutableMap()?:HashMap()
        if(infos.providers.size == 0){
            list.remove(subject.id)
        }else{
            list[subject.id] = infos
        }
        editor.putString(PREF_PROVIDER_INFO, JsonUtil.toJson(list))
        editor.apply()
    }

    fun getInfos(subject: Subject): ProviderInfoList? {
        return JsonUtil.toEntity<Map<Int, ProviderInfoList>>(sp.getString(PREF_PROVIDER_INFO, JsonUtil.toJson(HashMap<Int, ProviderInfoList>()))!!, object: TypeToken<Map<Int, ProviderInfoList>>(){}.type)?.get(subject.id)
    }

    companion object {
        const val PREF_PROVIDER_INFO="providerInfo"
    }
}