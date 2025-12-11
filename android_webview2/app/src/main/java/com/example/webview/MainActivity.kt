package com.example.webview

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog

class MainActivity : ComponentActivity() {

    private lateinit var webView: WebView
    private lateinit var customViewContainer: FrameLayout
    private var customView: View? = null
    private var customViewCallback: WebChromeClient.CustomViewCallback? = null

    private var mUploadMessageAboveL: ValueCallback<Array<Uri>>? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        webView = findViewById(R.id.webView)
        webView.webViewClient = WebViewClient()  // 保证在应用内加载网页，而不是调用系统浏览器

        // 启用JavaScript
        webView.settings.javaScriptEnabled = true

        // 启用 localStorage
        webView.settings.domStorageEnabled = true
        webView.settings.mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
        // 允许 file:/// 加载 https 外部资源（核心）
        webView.settings.allowFileAccess = true
        webView.settings.allowFileAccessFromFileURLs = true
        webView.settings.allowUniversalAccessFromFileURLs = true

        // 允许混合内容（Android 5.0+ 必须）
        webView.settings.mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
        //
        val register = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            println(it)
            val resultCode = it.resultCode
            val data = it.data
            if (resultCode == Activity.RESULT_OK && data != null) {
                val clipData = data.clipData
                println(clipData)
                val contentUri = if (clipData != null) {
                    val uris = ArrayList<Uri>()
                    for (i in 0 until clipData.itemCount) {
                        val uri = clipData.getItemAt(i).uri
                        uris.add(uri)
                    }
                    uris.toTypedArray()
                } else {
                    val uris = ArrayList<Uri>()
                    data.data?.let { it1 -> uris.add(it1) }
                    uris.toTypedArray()
                }
                println(contentUri)
                println(mUploadMessageAboveL)
                mUploadMessageAboveL?.onReceiveValue(contentUri)
            } else {
                mUploadMessageAboveL?.onReceiveValue(null)
            }
            mUploadMessageAboveL = null
        }


        webView.webViewClient = WebViewClient()
        customViewContainer = findViewById(R.id.customViewContainer)

        // 设置 WebChromeClient 处理全屏视频
        webView.webChromeClient = object : WebChromeClient() {
            override fun onShowFileChooser(
                webView: WebView,
                filePathCallback: ValueCallback<Array<Uri>>,
                fileChooserParams: FileChooserParams
            ): Boolean {
                println("onShowFileChooser")
                mUploadMessageAboveL = filePathCallback
                val intent = Intent(fileChooserParams.createIntent())
                intent.addCategory(Intent.CATEGORY_OPENABLE)
                intent.type = "*/*"
                intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true) // 允许多选
                register.launch(Intent.createChooser(intent, "Select File"))

                return true
            }

            override fun onShowCustomView(view: View, callback: CustomViewCallback) {
                // 当视频请求全屏时，显示自定义视图
                if (customView != null) {
                    callback.onCustomViewHidden()
                    return
                }

                customView = view
                customViewContainer.visibility = View.VISIBLE
                customViewContainer.addView(customView)
                customViewCallback = callback
                webView.visibility = View.GONE
                customViewContainer.bringToFront()
            }

            override fun onHideCustomView() {
                // 当退出全屏时，恢复默认视图
                customView?.let {
                    customViewContainer.removeView(it)
                    customView = null
                    customViewContainer.visibility = View.GONE
                    customViewCallback?.onCustomViewHidden()
                    webView.visibility = View.VISIBLE
                }
            }

            override fun getDefaultVideoPoster(): Bitmap {
                return Bitmap.createBitmap(50, 50, Bitmap.Config.ARGB_8888)
            }

        }

        // 监听长按点击，保存图片
        webView.setOnLongClickListener {
            val hitTestResult = webView.hitTestResult
            if (hitTestResult.type == WebView.HitTestResult.IMAGE_TYPE || hitTestResult.type == WebView.HitTestResult.SRC_IMAGE_ANCHOR_TYPE) {
                // 弹出保存图片的对话框
                AlertDialog.Builder(this)
                    .setTitle("保存图片")
                    .setMessage("是否保存图片？")
                    .setPositiveButton("保存") { _, _ ->
                        // 保存图片的代码
                        val imageUrl = hitTestResult.extra
                        println(imageUrl)
                    }
                    .setNegativeButton("取消", null)
                    .show()

                return@setOnLongClickListener true
            }
            false
        }

        webView.loadUrl("file:///android_asset/index.html")
    }
}
