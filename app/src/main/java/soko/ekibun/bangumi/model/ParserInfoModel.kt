package soko.ekibun.bangumi.model

import android.content.Context
import android.content.SharedPreferences
import android.preference.PreferenceManager
import soko.ekibun.bangumi.util.JsonUtil
import soko.ekibun.bangumi.parser.ParserInfo

class ParserInfoModel(context: Context){
    val sp: SharedPreferences by lazy{ PreferenceManager.getDefaultSharedPreferences(context) }
    fun removeInfo(info: ParserInfo){
        val editor = sp.edit()
        val set = HashSet((sp.getStringSet("parserInfo", null)?: HashSet()))
        val infoString = JsonUtil.toJson(info)
        set.removeAll { it == infoString }
        editor.putStringSet(PREF_PARSER_INFO, set)
        editor.apply()
    }

    fun saveInfo(info: ParserInfo) {
        val editor = sp.edit()
        val set = HashSet((sp.getStringSet("parserInfo", null)?: HashSet()))
        set.add(JsonUtil.toJson(info))
        editor.putStringSet(PREF_PARSER_INFO, set)
        editor.apply()
    }

    fun getInfos(): Set<ParserInfo> {
        return (sp.getStringSet("parserInfo", null)?: HashSet()).map { JsonUtil.toEntity(it, ParserInfo::class.java) }.filterNotNull().toSet()
    }

    companion object {
        const val PREF_PARSER_INFO="parserInfo"
    }
}