# Android Webview Demo

### 需要配置的权限
在 `AndroidManifest.xml` 文件中添加以下配置：

```xml
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
<uses-permission android:name="android.permission.ACCESS_WIFI_STATE" />
<uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE"/>
<uses-permission android:name="android.permission.MANAGE_EXTERNAL_STORAGE"
        tools:ignore="ScopedStorage" />
```

### 项目说明

webview 基础设置
```kotlin
// 启用JavaScript
 webView.settings.javaScriptEnabled = true

// 启用 localStorage
webView.settings.domStorageEnabled = true
```

图片、文件点击上传没有反应等问题
Android 的 WebView 默认不支持 的文件选择. 所以需要实现WebChromeClient 的 onShowFileChooser 方法

```kotlin
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
  }
```

视频播放无法全屏或退出全屏后出现异常问题
```kotlin
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
```

video 标签视频无法下载
需要这边自行实现DownloadListener
```kotlin
webView.setDownloadListener(new DownloadListener() {
    @Override
    public void onDownloadStart(String url, String userAgent,
                                String contentDisposition, String mimetype,
                                long contentLength) {
        // 在这里自己处理下载逻辑，比如交给系统下载管理器
        DownloadManager.Request request = new DownloadManager.Request(Uri.parse(url));
        request.setMimeType(mimetype);
        request.addRequestHeader("User-Agent", userAgent);
        request.setDescription("正在下载视频");
        request.setTitle(URLUtil.guessFileName(url, contentDisposition, mimetype));
        request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
        request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS,
                URLUtil.guessFileName(url, contentDisposition, mimetype));

        DownloadManager dm = (DownloadManager) getSystemService(DOWNLOAD_SERVICE);
        dm.enqueue(request);
    }
});
```