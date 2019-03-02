package soko.ekibun.bangumi.model

import android.content.Context
import android.content.SharedPreferences
import android.preference.PreferenceManager
import soko.ekibun.bangumi.api.bangumi.bean.Subject

class BooruTagModel(context: Context){
    val sp: SharedPreferences by lazy{ PreferenceManager.getDefaultSharedPreferences(context) }
    private fun prefKey(subject: Subject): String{
        return PREF_PROVIDER_INFO + subject.id
    }

    fun saveTag(subject: Subject, tag: String) {
        val editor = sp.edit()
        val key = prefKey(subject)
        if(tag.isEmpty()){
            editor.remove(key)
        }else{
            editor.putString(key, tag)
        }
        editor.apply()
    }

    fun getTag(subject: Subject): String {
        return sp.getString(prefKey(subject), "")?:""
    }

    companion object {
        const val PREF_PROVIDER_INFO="booruTag"
    }
}