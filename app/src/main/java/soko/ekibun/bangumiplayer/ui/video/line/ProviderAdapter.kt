package soko.ekibun.bangumiplayer.ui.video.line

import android.content.Context
import com.zhy.adapter.abslistview.CommonAdapter
import com.zhy.adapter.abslistview.ViewHolder
import soko.ekibun.bangumiplayer.provider.BaseProvider

class ProviderAdapter(context: Context?, data: List<BaseProvider>?) : CommonAdapter<BaseProvider>(context, android.R.layout.simple_spinner_dropdown_item, data) {

    override fun convert(viewHolder: ViewHolder, item: BaseProvider, position: Int) {
        viewHolder.setText(android.R.id.text1, item.name)
    }
}