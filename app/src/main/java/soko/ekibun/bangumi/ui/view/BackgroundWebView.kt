package soko.ekibun.bangumi.ui.view

import android.annotation.SuppressLint
import android.content.Context
import android.os.Handler
import android.util.Log
import android.webkit.*

class BackgroundWebView(context: Context): WebView(context) {
    var onPageFinished = {_:String?->}
    var onCatchVideo={_: WebResourceRequest ->}

    var uiHandler: Handler = Handler{true}

    override fun loadUrl(url: String?) {
        Log.v("loadUrl", url)
        super.loadUrl(url)
    }

    init{
        @SuppressLint("SetJavaScriptEnabled")
        settings.javaScriptEnabled = true
        settings.useWideViewPort = true
        settings.loadWithOverviewMode = true
        settings.setSupportMultipleWindows(true)
        settings.domStorageEnabled = true
        settings.mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
        settings.blockNetworkImage = true
        webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                onPageFinished(url)
                super.onPageFinished(view, url)
            }
            override fun shouldInterceptRequest(view: WebView, request: WebResourceRequest): WebResourceResponse? {
                Log.v("loadres", request.url.toString() +" " + request.requestHeaders.toString())
                if (request.requestHeaders["Range"] != null || request.url.toString().contains("//player.acfun.cn/route_m3u8")) {
                    onCatchVideo(request)
                    uiHandler.post{
                        view.onPause()
                        view.clearCache(true)
                        view.clearHistory()
                        view.loadUrl("about:blank")
                    }
                    return null
                }
                return super.shouldInterceptRequest(view, request)
            }
        }
    }
}
