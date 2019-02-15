package soko.ekibun.bangumi.model

import android.content.Context
import android.content.SharedPreferences
import android.preference.PreferenceManager
import soko.ekibun.bangumi.api.bangumi.bean.Episode
import soko.ekibun.bangumi.api.bangumi.bean.Subject
import soko.ekibun.bangumi.util.JsonUtil

class ProgressModel(context: Context){
    val sp: SharedPreferences by lazy{ PreferenceManager.getDefaultSharedPreferences(context) }
    private fun prefKey(subject: Subject): String{
        return PREF_PROGRESS_INFO + subject.id
    }

    fun saveProgress(subject: Subject, info: Info) {
        val editor = sp.edit()
        val key = prefKey(subject)
        editor.putString(key, JsonUtil.toJson(info))
        editor.apply()
    }

    fun getProgress(subject: Subject): Info? {
        return JsonUtil.toEntity(sp.getString(prefKey(subject), "")!!, Info::class.java)
    }

    data class Info(
            val episode: Episode,
            val progress: Int)

    companion object {
        const val PREF_PROGRESS_INFO="progressInfo"
    }
}