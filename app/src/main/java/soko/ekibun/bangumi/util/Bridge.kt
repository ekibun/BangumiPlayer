package soko.ekibun.bangumi.util

import android.content.Context

object Bridge {
    private const val PACKAGE_NAME = "soko.ekibun.bangumi"

    fun getContext(context: Context): Context{
        return context.createPackageContext(PACKAGE_NAME, Context.CONTEXT_IGNORE_SECURITY or Context.CONTEXT_INCLUDE_CODE)
    }

    fun launchUrl(context: Context, url: String?, openUrl: String){
        val bridgeContext = getContext(context)
        val classWebActivity = bridgeContext.classLoader.loadClass("$PACKAGE_NAME.ui.web.WebActivity")
        val classWebActivityCompanion = bridgeContext.classLoader.loadClass("$PACKAGE_NAME.ui.web.WebActivity\$Companion")
        val instance = classWebActivity.getDeclaredField("Companion").get(null)
        classWebActivityCompanion.getDeclaredMethod("launchUrl", Context::class.java, String::class.java, String::class.java).invoke(instance, bridgeContext, url, openUrl)
    }
}