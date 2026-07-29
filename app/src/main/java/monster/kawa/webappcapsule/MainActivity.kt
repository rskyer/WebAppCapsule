package monster.kawa.webappcapsule

import android.annotation.SuppressLint
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Base64
import android.view.KeyEvent
import android.webkit.CookieManager
import android.webkit.DownloadListener
import android.webkit.MimeTypeMap
import android.webkit.URLUtil
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.webkit.WebViewAssetLoader
import monster.kawa.webappcapsule.databinding.ActivityMainBinding
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private var filePathCallback: ValueCallback<Array<Uri>>? = null
    private lateinit var fileChooserLauncher: ActivityResultLauncher<Intent>

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        fileChooserLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->
            if (result.resultCode == RESULT_OK) {
                val data = result.data
                var results: Array<Uri>? = null

                if (data != null) {
                    val clipData = data.clipData

                    if (clipData != null) {
                        results = Array(clipData.itemCount) { i ->
                            clipData.getItemAt(i).uri
                        }
                    } else {
                        data.data?.let { uri ->
                            results = arrayOf(uri)
                        }
                    }
                }
                filePathCallback?.onReceiveValue(results)
                filePathCallback = null
            } else {
                filePathCallback?.onReceiveValue(null)
                filePathCallback = null
            }
        }

        WindowInsetsControllerCompat(window, window.decorView).apply {
            hide(WindowInsetsCompat.Type.systemBars())
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }

        val assetLoader = WebViewAssetLoader.Builder()
            .setDomain("appassets.androidplatform.net")
            .addPathHandler("/", WebViewAssetLoader.AssetsPathHandler(this))
            .build()

        binding.webView.apply {
            settings.apply {
                javaScriptEnabled = true
                domStorageEnabled = true
                databasePath = applicationContext.filesDir.path + "/databases/"
                allowFileAccess = true
                allowContentAccess = true
                mediaPlaybackRequiresUserGesture = false
            }

            webChromeClient = object : WebChromeClient() {
                override fun onShowFileChooser(
                    webView: WebView?,
                    filePathCallback: ValueCallback<Array<Uri>>?,
                    fileChooserParams: FileChooserParams?
                ): Boolean {
                    this@MainActivity.filePathCallback?.onReceiveValue(null)
                    this@MainActivity.filePathCallback = filePathCallback

                    val intent = fileChooserParams?.createIntent() ?: Intent(Intent.ACTION_GET_CONTENT).apply {
                        addCategory(Intent.CATEGORY_OPENABLE)
                        type = "*/*"
                    }

                    try {
                        fileChooserLauncher.launch(intent)
                    } catch (e: Exception) {
                        Toast.makeText(this@MainActivity, "نمی‌توان فایل منیجر را باز کرد", Toast.LENGTH_SHORT).show()
                        return false
                    }
                    return true
                }
            }

            webViewClient = object : WebViewClient() {
                override fun shouldInterceptRequest(view: WebView, request: WebResourceRequest): WebResourceResponse? {
                    return if (request.url.host == "appassets.androidplatform.net") {
                        assetLoader.shouldInterceptRequest(request.url)
                    } else {
                        super.shouldInterceptRequest(view, request)
                    }
                }

                override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                    val url = request.url.toString()

                    return when {
                        request.url.host == "appassets.androidplatform.net" -> false

                        else -> {
                            try {
                                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                            } catch (e: Exception) {
                                Toast.makeText(this@MainActivity, "مرورگری یافت نشد", Toast.LENGTH_SHORT).show()
                            }
                            true
                        }
                    }
                }

                override fun onPageFinished(view: WebView, url: String) {
                    view.requestFocus()
                }
            }

            setDownloadListener(DownloadListener { url, userAgent, contentDisposition, mimetype, _ ->
                if (url.startsWith("blob:")) {
                    // نکته مهم: mimetype و contentDisposition که خود WebView برای blob می‌ده
                    // اغلب نادرست/خالی است (مثلا text/plain به‌جای image/png).
                    // برای همین نوع واقعی رو مستقیم از خود blob.type می‌خونیم.
                    val js = """
                        var xhr = new XMLHttpRequest();
                        xhr.open('GET', '$url', true);
                        xhr.responseType = 'blob';
                        xhr.onload = function() {
                            var blob = xhr.response;
                            var realType = blob.type && blob.type.length > 0 ? blob.type : '$mimetype';
                            var reader = new FileReader();
                            reader.readAsDataURL(blob);
                            reader.onloadend = function() {
                                Android.saveBlob(reader.result, realType, '${contentDisposition ?: ""}');
                            };
                        };
                        xhr.send();
                    """.trimIndent()

                    evaluateJavascript(js, null)
                    Toast.makeText(this@MainActivity, "در حال آماده‌سازی فایل...", Toast.LENGTH_SHORT).show()
                } else {
                    downloadFileManually(url, userAgent, contentDisposition, mimetype)
                }
            })

            addJavascriptInterface(object {
                @android.webkit.JavascriptInterface
                fun saveBlob(base64Data: String, mimeType: String, contentDisposition: String) {
                    runOnUiThread {
                        try {
                            val base64 = base64Data.substring(base64Data.indexOf(",") + 1)
                            val bytes = Base64.decode(base64, Base64.DEFAULT)
                            val fileName = guessFileNameFromDisposition(contentDisposition, mimeType)

                            bytes.inputStream().use { input ->
                                saveToDownloads(fileName, mimeType, input)
                            }

                            Toast.makeText(
                                this@MainActivity,
                                "فایل در Downloads ذخیره شد: $fileName",
                                Toast.LENGTH_LONG
                            ).show()
                        } catch (e: Exception) {
                            Toast.makeText(this@MainActivity, "خطا در ذخیره فایل: ${e.message}", Toast.LENGTH_LONG).show()
                        }
                    }
                }
            }, "Android")

            loadUrl("https://appassets.androidplatform.net/index.html")
        }
    }

    /**
     * اسم فایل رو یا از هدر Content-Disposition استخراج می‌کنه،
     * یا در نبود اون، بر اساس mimeType واقعی یک اسم و پسوند درست می‌سازه
     * (به‌جای پیش‌فرض هاردکد شده‌ی .zip که باعث خرابی پسوند می‌شد).
     */
    private fun guessFileNameFromDisposition(contentDisposition: String, mimeType: String): String {
        val regex = Regex("filename\\*?=(?:UTF-8'')?\"?([^\";]+)\"?")
        val match = regex.find(contentDisposition)
        if (match != null) {
            return match.groupValues[1].trim()
        }
        val extension = extensionFromMimeType(mimeType)
        return "file_${System.currentTimeMillis()}$extension"
    }

    private fun extensionFromMimeType(mimeType: String): String {
        if (mimeType.isBlank() || mimeType == "application/octet-stream") return ""
        val guessed = MimeTypeMap.getSingleton().getExtensionFromMimeType(mimeType)
        return if (guessed != null) ".$guessed" else ""
    }

    /**
     * تابع مرکزی و یکسان برای ذخیره‌ی هر نوع فایل در پوشه‌ی Downloads.
     * روی Android 10+ (API 29+) از MediaStore استفاده می‌کنه (سازگار با Scoped Storage).
     * روی نسخه‌های قدیمی‌تر مستقیم توی File می‌نویسه.
     */
    private fun saveToDownloads(fileName: String, mimeType: String?, input: InputStream) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val resolver = contentResolver
            val values = ContentValues().apply {
                put(MediaStore.Downloads.DISPLAY_NAME, fileName)
                put(MediaStore.Downloads.MIME_TYPE, mimeType ?: "application/octet-stream")
                put(MediaStore.Downloads.IS_PENDING, 1)
            }

            val collection = MediaStore.Downloads.EXTERNAL_CONTENT_URI
            val itemUri = resolver.insert(collection, values)
                ?: throw Exception("امکان ساخت فایل در Downloads وجود ندارد")

            resolver.openOutputStream(itemUri)?.use { output ->
                input.copyTo(output, bufferSize = 8 * 1024)
            } ?: throw Exception("امکان نوشتن در فایل وجود ندارد")

            values.clear()
            values.put(MediaStore.Downloads.IS_PENDING, 0)
            resolver.update(itemUri, values, null, null)
        } else {
            val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            val file = File(downloadsDir, fileName)
            FileOutputStream(file).use { output ->
                input.copyTo(output, bufferSize = 8 * 1024)
            }
            MediaScannerConnection.scanFile(this, arrayOf(file.absolutePath), arrayOf(mimeType)) { _, _ -> }
        }
    }

    private fun downloadFileManually(
        url: String,
        userAgent: String,
        contentDisposition: String?,
        mimetype: String?
    ) {
        val fileName = URLUtil.guessFileName(url, contentDisposition, mimetype)
        val uri = Uri.parse(url)

        if (uri.host == "appassets.androidplatform.net") {
            copyFromAssets(uri, fileName, mimetype)
            return
        }

        runOnUiThread {
            Toast.makeText(this, "در حال دانلود: $fileName", Toast.LENGTH_SHORT).show()
        }

        Thread {
            var connection: HttpURLConnection? = null
            try {
                val cookies = CookieManager.getInstance().getCookie(url)
                var currentUrl = url
                var redirects = 0

                while (redirects < 5) {
                    connection = URL(currentUrl).openConnection() as HttpURLConnection
                    connection.instanceFollowRedirects = false
                    connection.requestMethod = "GET"
                    connection.setRequestProperty("User-Agent", userAgent)
                    if (!cookies.isNullOrEmpty()) {
                        connection.setRequestProperty("Cookie", cookies)
                    }
                    connection.connectTimeout = 15000
                    connection.readTimeout = 30000
                    connection.connect()

                    val code = connection.responseCode
                    if (code in 300..399) {
                        val location = connection.getHeaderField("Location") ?: break
                        currentUrl = location
                        connection.disconnect()
                        redirects++
                        continue
                    }
                    break
                }

                val conn = connection ?: throw Exception("اتصال برقرار نشد")
                if (conn.responseCode != HttpURLConnection.HTTP_OK) {
                    throw Exception("HTTP ${conn.responseCode}")
                }

                conn.inputStream.use { input ->
                    saveToDownloads(fileName, mimetype, input)
                }

                runOnUiThread {
                    Toast.makeText(this, "فایل ذخیره شد: $fileName", Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                runOnUiThread {
                    Toast.makeText(this, "دانلود ناموفق: ${e.message}", Toast.LENGTH_LONG).show()
                }
            } finally {
                connection?.disconnect()
            }
        }.start()
    }

    private fun copyFromAssets(uri: Uri, fileName: String, mimetype: String?) {
        runOnUiThread {
            Toast.makeText(this, "در حال آماده‌سازی: $fileName", Toast.LENGTH_SHORT).show()
        }

        Thread {
            try {
                val assetPath = uri.path?.trimStart('/') ?: throw Exception("مسیر فایل نامعتبر است")

                assets.open(assetPath).use { input ->
                    saveToDownloads(fileName, mimetype, input)
                }

                runOnUiThread {
                    Toast.makeText(this, "فایل در Downloads ذخیره شد: $fileName", Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                runOnUiThread {
                    Toast.makeText(this, "خطا در ذخیره‌سازی: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }.start()
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_BACK) return true
        return super.onKeyDown(keyCode, event)
    }

    override fun onBackPressed() {}
}