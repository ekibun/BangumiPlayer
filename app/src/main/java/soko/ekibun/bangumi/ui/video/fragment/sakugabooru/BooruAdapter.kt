package soko.ekibun.bangumi.ui.video.fragment.sakugabooru

import android.support.constraint.ConstraintLayout
import com.bumptech.glide.Glide
import com.chad.library.adapter.base.BaseQuickAdapter
import com.chad.library.adapter.base.BaseViewHolder
import kotlinx.android.synthetic.main.item_booru.view.*
import soko.ekibun.bangumi.api.sakugabooru.bean.Post
import soko.ekibun.bangumiplayer.R

class BooruAdapter(data: MutableList<Post>? = null) :
        BaseQuickAdapter<Post, BaseViewHolder>(R.layout.item_booru, data) {
    override fun convert(helper: BaseViewHolder, item: Post) {
        helper.itemView.item_source.text = item.source
        val lp = helper.itemView.item_preview.layoutParams as ConstraintLayout.LayoutParams
        lp.dimensionRatio = "h,${item.actual_preview_width}:${item.actual_preview_height}"
        Glide.with(helper.itemView.item_preview)
                .load(item.preview_url)
                .into(helper.itemView.item_preview)
    }
}