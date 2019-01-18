package soko.ekibun.bangumi.ui.video.line

import android.content.Context
import com.zhy.adapter.abslistview.CommonAdapter
import com.zhy.adapter.abslistview.ViewHolder
import soko.ekibun.bangumi.parser.ParserInfo

class ParserAdapter(context: Context?, data: List<ParserInfo>?) : CommonAdapter<ParserInfo>(context, android.R.layout.simple_spinner_dropdown_item, data) {

    override fun convert(viewHolder: ViewHolder, item: ParserInfo, position: Int) {
        viewHolder.setText(android.R.id.text1, item.api)
    }
}