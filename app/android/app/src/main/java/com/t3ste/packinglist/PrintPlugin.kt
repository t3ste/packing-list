package com.t3ste.packinglist

import android.content.Context
import android.print.PrintAttributes
import android.print.PrintManager
import android.webkit.WebView
import android.webkit.WebViewClient
import com.getcapacitor.PluginCall
import com.getcapacitor.PluginMethod
import com.getcapacitor.Plugin
import com.getcapacitor.annotation.CapacitorPlugin

// Renders the given HTML off-screen and hands it to Android's own Print
// framework — this is what actually wires up "Print / Save as PDF" on
// Android; window.print() from JS does nothing in a bare embedded WebView
// (only real Chrome implements that), and Chrome Custom Tabs (the earlier
// attempt at this) can't load data: URLs at all — attempting to sent one
// there just silently failed and left the app in a stuck, unresponsive state.
// PrintManager stays entirely within this activity: it's a system dialog,
// not a separate app/tab, so there's nothing to get stuck transitioning to.
@CapacitorPlugin(name = "NativePrint")
class PrintPlugin : Plugin() {
    // Held as a field, not a local variable, so it isn't garbage-collected
    // before the asynchronous page load (onPageFinished) fires.
    private var printWebView: WebView? = null

    @PluginMethod
    fun printHtml(call: PluginCall) {
        val html = call.getString("html")
        val jobName = call.getString("jobName", "Packing List") ?: "Packing List"
        if (html == null) {
            call.reject("Missing html")
            return
        }
        activity.runOnUiThread {
            val webView = WebView(activity)
            printWebView = webView
            webView.webViewClient = object : WebViewClient() {
                override fun onPageFinished(view: WebView, url: String?) {
                    try {
                        val printManager = activity.getSystemService(Context.PRINT_SERVICE) as PrintManager
                        val adapter = view.createPrintDocumentAdapter(jobName)
                        val attrs = PrintAttributes.Builder().build()
                        printManager.print(jobName, adapter, attrs)
                        call.resolve()
                    } catch (e: Exception) {
                        call.reject("Could not start printing: " + e.message)
                    } finally {
                        printWebView = null
                    }
                }
            }
            webView.loadDataWithBaseURL(null, html, "text/html", "UTF-8", null)
        }
    }
}
