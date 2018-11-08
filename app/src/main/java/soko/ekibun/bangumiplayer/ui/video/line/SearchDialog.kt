package soko.ekibun.bangumiplayer.ui.video.line

import android.app.Activity
import android.app.AlertDialog
import android.support.v7.widget.LinearLayoutManager
import android.view.ViewGroup
import android.widget.ArrayAdapter
import kotlinx.android.synthetic.main.dialog_search_line.view.*
import retrofit2.Call
import soko.ekibun.bangumi.api.ApiHelper
import soko.ekibun.bangumi.api.bangumi.bean.Subject
import soko.ekibun.bangumiplayer.R
import soko.ekibun.bangumiplayer.model.ProviderModel
import soko.ekibun.bangumiplayer.provider.ProviderInfo

object SearchDialog {
    fun showDialog(context: Activity, viewGroup: ViewGroup, subject: Subject, callback:(ProviderInfo)->Unit){
        val view =context.layoutInflater.inflate(R.layout.dialog_search_line, viewGroup, false)
        val dialog = AlertDialog.Builder(context)
                .setView(view).create()

        val providers = ProviderModel.providers.filter { it.supportSearch }
        val nameList = providers.map{ it.name }.toMutableList()
        nameList.add(0, "所有线路")
        val typeAdapter = ArrayAdapter<String>(context, android.R.layout.simple_spinner_item, nameList)
        typeAdapter.setDropDownViewResource(R.layout.support_simple_spinner_dropdown_item)
        view.item_search_type.adapter = typeAdapter
        view.item_search_key.setText(if(subject.name_cn.isNullOrEmpty()) subject.name else subject.name_cn)
        view.list_search.layoutManager = LinearLayoutManager(context)
        val adapter = SearchLineAdapter()
        adapter.setOnItemClickListener { _, _, position ->
            callback(adapter.data[position])
            dialog.dismiss()
        }
        view.list_search.adapter = adapter
        var searchCall: Call<List<ProviderInfo>>? = null
        view.item_search.setOnClickListener {
            adapter.setNewData(null)
            val pos = view.item_search_type.selectedItemPosition
            val key = view.item_search_key.text.toString()
            searchCall?.cancel()
            searchCall = if(pos == 0) ProviderModel.searchAll(key) else ProviderModel.search(providers[pos-1].siteId, key)
            searchCall?.enqueue(ApiHelper.buildCallback(context,{
                        adapter.addData(it)
                    }, {}))
        }
        dialog.show()
    }
}