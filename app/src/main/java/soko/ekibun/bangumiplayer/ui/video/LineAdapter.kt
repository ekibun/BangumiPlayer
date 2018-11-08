package soko.ekibun.bangumiplayer.ui.video

import android.content.res.ColorStateList
import com.chad.library.adapter.base.BaseQuickAdapter
import com.chad.library.adapter.base.BaseViewHolder
import kotlinx.android.synthetic.main.item_provider.view.*
import soko.ekibun.bangumi.util.ResourceUtil
import soko.ekibun.bangumiplayer.R
import soko.ekibun.bangumiplayer.model.ProviderModel
import soko.ekibun.bangumiplayer.provider.ProviderInfo

class LineAdapter(data: MutableList<ProviderInfo>? = null) :
        BaseQuickAdapter<ProviderInfo, BaseViewHolder>(R.layout.item_provider, data) {
    var selectIndex = 0
    var onSwitchChange =  {_: Int, _: Boolean->}

    override fun convert(helper: BaseViewHolder, item: ProviderInfo) {
        val index = data.indexOfFirst{ it === item }
        val color = ResourceUtil.resolveColorAttr(helper.itemView.context,
                if (index == selectIndex) R.attr.colorPrimary else android.R.attr.textColorSecondary)
        helper.itemView.item_title.setTextColor(color)
        helper.itemView.item_title.text = if(item.title.isEmpty()) item.id else item.title
        val provider = ProviderModel.providers.firstOrNull { it.siteId == item.siteId }?:return
        helper.itemView.item_switch.isEnabled = provider.hasDanmaku
        helper.itemView.item_switch.isChecked = provider.hasDanmaku && item.loadDanmaku
        helper.itemView.item_switch.setOnCheckedChangeListener { _, isChecked ->
            onSwitchChange(index, isChecked)
        }
        helper.itemView.item_site.backgroundTintList = ColorStateList.valueOf((0xff000000 + provider.color).toInt())
        helper.itemView.item_site.text = provider.name
        helper.itemView.item_id.text = item.id
    }
}