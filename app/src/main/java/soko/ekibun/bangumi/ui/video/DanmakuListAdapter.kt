package soko.ekibun.bangumi.ui.video

import android.content.res.ColorStateList
import android.view.View
import com.chad.library.adapter.base.BaseQuickAdapter
import com.chad.library.adapter.base.BaseViewHolder
import kotlinx.android.synthetic.main.item_provider.view.*
import soko.ekibun.bangumi.model.ProviderModel
import soko.ekibun.bangumi.provider.BaseProvider
import soko.ekibun.bangumi.provider.ProviderInfo
import soko.ekibun.bangumiplayer.R

class DanmakuListAdapter(data: MutableList<DanmakuInfo>? = null) :
        BaseQuickAdapter<DanmakuListAdapter.DanmakuInfo, BaseViewHolder>(R.layout.item_provider, data) {

    override fun convert(helper: BaseViewHolder, item: DanmakuInfo) {
        helper.itemView.item_title.text = item.provider.id
        val provider = ProviderModel.providers.firstOrNull { it.siteId == item.provider.siteId }?:return
        helper.itemView.item_switch.visibility = View.GONE
        helper.itemView.item_site.backgroundTintList = ColorStateList.valueOf((0xff000000 + provider.color).toInt())
        helper.itemView.item_site.text = provider.name
        helper.itemView.item_id.text = if(item.info.isNotEmpty()) item.info else " ${item.danmakus.size} 条弹幕"
    }

    data class DanmakuInfo(
            val provider: ProviderInfo,
            var danmakus: HashSet<BaseProvider.DanmakuInfo> = HashSet(),
            var info: String = "",
            var videoInfo: BaseProvider.VideoInfo? = null,
            var key: String? = null
    )
}