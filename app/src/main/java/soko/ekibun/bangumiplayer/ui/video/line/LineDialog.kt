package soko.ekibun.bangumiplayer.ui.video.line

import android.app.Activity
import android.app.AlertDialog
import android.content.DialogInterface
import android.view.View
import android.view.ViewGroup
import android.widget.ListPopupWindow
import kotlinx.android.synthetic.main.dialog_add_line.view.*
import soko.ekibun.bangumi.api.bangumi.bean.Subject
import soko.ekibun.bangumiplayer.R
import soko.ekibun.bangumiplayer.model.ParserInfoModel
import soko.ekibun.bangumiplayer.model.ProviderModel
import soko.ekibun.bangumiplayer.parser.ParserInfo
import soko.ekibun.bangumiplayer.provider.BaseProvider
import soko.ekibun.bangumiplayer.provider.ProviderInfo
import soko.ekibun.bangumiplayer.ui.video.VideoActivity

object LineDialog{

    fun showDialog(context: VideoActivity, viewGroup: ViewGroup, subject: Subject, info: ProviderInfo? = null, callback: (ProviderInfo?)-> Unit){
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
        //provider
        val popList = ListPopupWindow(context)
        popList.anchorView = view.item_video_type
        popList.setAdapter(ProviderAdapter(context, ProviderModel.providers))
        popList.isModal = true
        view.item_video_type.setOnClickListener {
            popList.show()
            popList.listView?.setOnItemClickListener { _, _, position, _ ->
                popList.dismiss()
                val provider = ProviderModel.providers[position]
                view.item_load_danmaku.isEnabled = provider.hasDanmaku
                view.item_video_type.text = provider.name
                view.item_video_type.tag = provider
            }
        }
        ProviderModel.providers.getOrNull(0)?.let{provider->
            view.item_video_type.text = provider.name
            view.item_video_type.tag = provider
        }
        //parser
        updateParser(context, view, viewGroup)

        if(info != null)
            updateInfo(view, info)
        view.item_search.setOnClickListener {
            SearchDialog.showDialog(context, viewGroup, subject){
                updateInfo(view, it) }
        }

        view.item_file.setOnClickListener {
            context.loadFile {file->
                if(file== null) return@loadFile
                updateInfo( view, ProviderInfo(ProviderInfo.URL, file))
            }
        }

        view.item_ok.setOnClickListener {
            val provider = view.item_video_type.tag as? BaseProvider?: return@setOnClickListener
            val parser = view.item_video_api.tag as? ParserInfo
            callback(ProviderInfo(provider.siteId, view.item_video_id.text.toString(),
                    view.item_video_offset.text.toString().toFloatOrNull()?:0f,
                    view.item_video_title.text.toString(),provider.hasDanmaku && view.item_load_danmaku.isChecked, parser))
            dialog.dismiss()
        }
        dialog.show()
    }

    private fun updateParser(context: Activity, view: View, viewGroup: ViewGroup){
        val parserPopList = ListPopupWindow(context)
        parserPopList.anchorView = view.item_video_api
        val parserInfoModel = ParserInfoModel(context)
        val parsers = parserInfoModel.getInfos().toMutableList()
        val nullParser = ParserInfo("", "")
        parsers.add(0, nullParser)
        parsers.add(ParserInfo("添加...", ""))
        val parserAdapter = ParserAdapter(context, parsers)
        parserPopList.setAdapter(parserAdapter)
        parserPopList.isModal = true
        view.item_video_api.setOnClickListener {
            parserPopList.show()
            parserPopList.listView?.setOnItemClickListener { _, _, position, _ ->
                parserPopList.dismiss()
                if(position == parsers.size -1 ){
                    //add
                    ParserDialog.showDialog(context, viewGroup, null){
                        if(it == null) return@showDialog
                        parserInfoModel.saveInfo(it)
                        view.item_video_api.text = it.api
                        view.item_video_api.tag = it
                        updateParser(context, view, viewGroup)
                    }
                } else{
                    val parser = parsers[position]
                    view.item_video_api.text = parser.api
                    view.item_video_api.tag = parser
                }
            }
            parserPopList.listView?.setOnItemLongClickListener { _, _, position, _ ->
                parserPopList.dismiss()
                if(position == parsers.size -1 || position == 0) return@setOnItemLongClickListener true
                //edit
                val info = parsers.getOrNull(position)?: return@setOnItemLongClickListener true
                ParserDialog.showDialog(context, viewGroup, info){
                    if(it == null){
                        parserInfoModel.removeInfo(info)
                    }else{
                        parserInfoModel.removeInfo(info)
                        parserInfoModel.saveInfo(it)
                    }
                    updateParser(context, view, viewGroup)
                }
                true
            }
        }
    }

    private fun updateInfo(view: View, info: ProviderInfo){
        val provider = ProviderModel.providers.first { it.siteId == info.siteId }
        view.item_video_type.text = provider.name
        view.item_video_type.tag = provider
        info.parser?.let{ parser->
            view.item_video_api.text = parser.api
            view.item_video_api.tag = parser
        }
        view.item_video_id.setText(info.id)
        view.item_video_offset.setText(info.offset.toString())
        view.item_video_title.setText(info.title)
        view.item_load_danmaku.isEnabled = provider.hasDanmaku
        view.item_load_danmaku.isChecked = info.loadDanmaku
    }
}