package soko.ekibun.bangumi.ui.video.line

import android.content.res.ColorStateList
import android.view.View
import com.chad.library.adapter.base.BaseQuickAdapter
import com.chad.library.adapter.base.BaseViewHolder
import kotlinx.android.synthetic.main.item_provider.view.*
import soko.ekibun.bangumiplayer.R
import soko.ekibun.bangumi.model.ProviderModel
import soko.ekibun.bangumi.provider.ProviderInfo

class SearchLineAdapter(data: MutableList<ProviderInfo>? = null) :
        BaseQuickAdapter<ProviderInfo, BaseViewHolder>(R.layout.item_provider, data) {

    override fun convert(helper: BaseViewHolder, item: ProviderInfo) {
        helper.itemView.item_switch.visibility = View.GONE
        helper.itemView.item_title.text = if(item.title.isEmpty()) item.id else item.title
        val provider = ProviderModel.providers.firstOrNull { it.siteId == item.siteId }?:return
        helper.itemView.item_site.backgroundTintList = ColorStateList.valueOf((0xff000000 + provider.color).toInt())
        helper.itemView.item_site.text = provider.name
        helper.itemView.item_id.text = item.id
    }
}