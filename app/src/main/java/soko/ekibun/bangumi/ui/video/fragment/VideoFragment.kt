package soko.ekibun.bangumi.ui.video.fragment

import android.support.v4.app.Fragment
import soko.ekibun.bangumi.api.bangumi.bean.Subject

abstract class VideoFragment: Fragment(){
    abstract val titleRes: Int

    abstract fun onSubjectChange(sbj: Subject)
}