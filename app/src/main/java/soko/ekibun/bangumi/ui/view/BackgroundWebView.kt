package soko.ekibun.bangumi.ui.view

import android.annotation.SuppressLint
import android.content.Context
import android.os.Handler
import android.util.Log
import android.webkit.*

class BackgroundWebView(context: Context): WebView(context) {
    var onPageFinished = {_:String?->}
    var onInterceptRequest={_: WebResourceRequest ->}

    var uiHandler: Handler = Handler{true}

    override fun loadUrl(url: String?) {
        Log.v("loadUrl", url)
        super.loadUrl(url)
    }

    fun reset(){
        uiHandler.post{
            onPause()
            clearCache(true)
            clearHistory()
            loadUrl("about:blank")
        }
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
            override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                val url = request.url.toString()
                if(!url.startsWith("http")){
                    return true
                }
                return false
            }
            override fun shouldInterceptRequest(view: WebView, request: WebResourceRequest): WebResourceResponse? {
                Log.v("loadres", request.url.toString() +" " + request.requestHeaders.toString())
                onInterceptRequest(request)
                return super.shouldInterceptRequest(view, request)
            }
        }
    }
}
