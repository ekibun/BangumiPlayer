package soko.ekibun.bangumiplayer.ui.video.line

import android.app.Activity
import android.app.AlertDialog
import android.content.DialogInterface
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import kotlinx.android.synthetic.main.dialog_add_line.view.*
import soko.ekibun.bangumi.api.bangumi.bean.Subject
import soko.ekibun.bangumiplayer.R
import soko.ekibun.bangumiplayer.model.ProviderModel
import soko.ekibun.bangumiplayer.provider.ProviderInfo

object LineDialog{
    fun showDialog(context: Activity, viewGroup: ViewGroup, subject: Subject, info: ProviderInfo? = null, callback: (ProviderInfo?)-> Unit){
        val view =context.layoutInflater.inflate(R.layout.dialog_add_line, viewGroup, false)
        val dialog = AlertDialog.Builder(context)
                .setView(view).create()

        view.item_delete.visibility = if(info == null) View.GONE else View.VISIBLE
        view.item_delete.setOnClickListener {
            AlertDialog.Builder(context).setMessage("删除这个线路？").setPositiveButton("确定"){ _: DialogInterface, _: Int ->
                callback(null)
                dialog.dismiss()
            }.show()
        }
        val adapter = ArrayAdapter<String>(context, android.R.layout.simple_spinner_item, ProviderModel.providers.map{ it.name })
        adapter.setDropDownViewResource(R.layout.support_simple_spinner_dropdown_item)
        view.item_video_type.adapter = adapter

        if(info != null)
            updateInfo(view, info)
        view.item_search.setOnClickListener {
            SearchDialog.showDialog(context, viewGroup, subject){
                updateInfo(view, it) }
        }
        view.item_video_type.onItemSelectedListener = object: AdapterView.OnItemSelectedListener{
            override fun onNothingSelected(parent: AdapterView<*>?) {}
            override fun onItemSelected(parent: AdapterView<*>?, v: View?, position: Int, id: Long) {
                val provider = ProviderModel.providers[position]
                view.item_load_danmaku.isEnabled = provider.hasDanmaku
            }
        }
        view.item_ok.setOnClickListener {
            val provider = ProviderModel.providers[view.item_video_type.selectedItemPosition]
            callback(ProviderInfo(provider.siteId, view.item_video_id.text.toString(),
                    view.item_video_offset.text.toString().toFloatOrNull()?:0f,
                    view.item_video_title.text.toString(),provider.hasDanmaku && view.item_load_danmaku.isChecked))
            dialog.dismiss()
        }
        dialog.show()
    }

    private fun updateInfo(view: View, info: ProviderInfo){
        val provider = ProviderModel.providers.first { it.siteId == info.siteId }
        view.item_video_type.setSelection(ProviderModel.providers.indexOfFirst { it.siteId == info.siteId})
        view.item_video_id.setText(info.id)
        view.item_video_offset.setText(info.offset.toString())
        view.item_video_title.setText(info.title)
        view.item_load_danmaku.isEnabled = provider.hasDanmaku
        view.item_load_danmaku.isChecked = info.loadDanmaku
    }
}