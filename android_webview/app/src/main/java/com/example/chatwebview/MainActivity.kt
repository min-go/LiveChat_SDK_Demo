package com.example.chatwebview

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

class MainActivity : ComponentActivity() {
    private lateinit var webView: WebView
    private lateinit var customViewContainer: FrameLayout
    private var customView: View? = null
    private var customViewCallback: WebChromeClient.CustomViewCallback? = null

    private var mUploadMessageAboveL: ValueCallback<Array<Uri>>? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        webView = findViewById(R.id.webview)
        webView.webViewClient = WebViewClient()  // 保证在应用内加载网页，而不是调用系统浏览器

        // 启用JavaScript
        webView.settings.javaScriptEnabled = true

        // 启用 localStorage
        webView.settings.domStorageEnabled = true

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


        webView.loadUrl(buildUrl())
    }

    @Throws(Exception::class)
    fun buildUrl(): String {
        val pluginId = "Your Plugin ID"

        val encryptedUserInfo = encrypt(
            """
        {"name":"Zhangsan","email":"test1@example.org","phone":"13800138000","visitorId":"34e535ea-67cd-49d8-a643-3ea35cd40378"}
        """.trimIndent()
        )

        val encodedEnc = URLEncoder.encode(encryptedUserInfo, StandardCharsets.UTF_8.name())

        val url = "https://chat-plugin.hiifans.com/native.html?pluginId=$pluginId&enc=$encodedEnc"

        println("loadUrl: $url")

        return url
    }

    @Throws(Exception::class)
    fun encrypt(plainText: String): String {
        // 联系我们获取加密密钥和IV向量
        val encryptionKey = "您的加密密钥"
        val encryptionIv = "您的IV向量"

        require(encryptionKey.toByteArray().size == 16) { "密钥必须为16字节" }
        require(encryptionIv.toByteArray().size == 16) { "IV 必须为16字节" }

        val keySpec = SecretKeySpec(encryptionKey.toByteArray(StandardCharsets.UTF_8), "AES")
        val ivSpec = IvParameterSpec(encryptionIv.toByteArray(StandardCharsets.UTF_8))

        val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
        cipher.init(Cipher.ENCRYPT_MODE, keySpec, ivSpec)

        val encrypted = cipher.doFinal(plainText.toByteArray(StandardCharsets.UTF_8))

        return Base64.getEncoder().encodeToString(encrypted)
    }
}