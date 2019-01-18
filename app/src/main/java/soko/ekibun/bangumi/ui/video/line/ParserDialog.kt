package soko.ekibun.bangumi.ui.video.line

import android.app.Activity
import android.app.AlertDialog
import android.content.DialogInterface
import android.view.View
import android.view.ViewGroup
import kotlinx.android.synthetic.main.dialog_add_parser.view.*
import soko.ekibun.bangumiplayer.R
import soko.ekibun.bangumi.parser.ParserInfo

object ParserDialog {
    fun showDialog(context: Activity, viewGroup: ViewGroup, info: ParserInfo?, callback:(ParserInfo?)->Unit){
        val view =context.layoutInflater.inflate(R.layout.dialog_add_parser, viewGroup, false)
        val dialog = AlertDialog.Builder(context)
                .setView(view).create()
        view.item_delete.visibility = if(info == null || (info.api.isEmpty() && info.js.isEmpty())) View.GONE else View.VISIBLE
        view.item_delete.setOnClickListener {
            AlertDialog.Builder(context).setMessage("删除这个接口？").setPositiveButton("确定"){ _: DialogInterface, _: Int ->
                callback(null)
                dialog.dismiss()
            }.show()
        }
        info?.let{
            view.item_api.setText(it.api)
            view.item_js.setText(it.js)
        }
        view.item_ok.setOnClickListener {
            callback(ParserInfo(view.item_api.text.toString(), view.item_js.text.toString()))
            dialog.dismiss()
        }
        dialog.show()
    }
}