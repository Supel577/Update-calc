package com.example.calcvault.ui.vault.browser

import android.annotation.SuppressLint
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.view.View
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebStorage
import android.webkit.WebView
import android.webkit.WebView.HitTestResult
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.calcvault.data.i18n.VaultStrings
import com.example.calcvault.data.repository.VaultRepository
import com.example.calcvault.data.security.VaultSecurityManager
import kotlinx.coroutines.launch

private data class BrowserMediaAction(
    val isImage: Boolean,
    val url: String,
    val title: String
)

private data class QuickBookmark(
    val title: String,
    val url: String,
    val initial: String,
    val color: Color
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun PrivateBrowserScreen(
    repository: VaultRepository,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val keyboardController = LocalSoftwareKeyboardController.current
    val snackbarHostState = remember { SnackbarHostState() }
    val securityManager = remember { VaultSecurityManager(context) }
    val appLanguage by securityManager.appLanguageFlow.collectAsStateWithLifecycle()

    var urlInput by remember { mutableStateOf("") }
    var currentDisplayUrl by remember { mutableStateOf("") }
    var pageTitle by remember { mutableStateOf("Private Incognito Browser") }
    var loadingProgress by remember { mutableFloatStateOf(0f) }
    var isLoading by remember { mutableStateOf(false) }
    var canGoBack by remember { mutableStateOf(false) }
    var canGoForward by remember { mutableStateOf(false) }
    var lastBackPressTime by remember { mutableLongStateOf(0L) }
    var webViewRef by remember { mutableStateOf<WebView?>(null) }
    var targetMediaAction by remember { mutableStateOf<BrowserMediaAction?>(null) }

    val quickBookmarks = remember {
        listOf(
            QuickBookmark("Google", "https://www.google.com", "G", Color(0xFF4285F4)),
            QuickBookmark("YouTube", "https://www.youtube.com", "▶", Color(0xFFFF0000)),
            QuickBookmark("Wikipedia", "https://www.wikipedia.org", "W", Color(0xFF546E7A)),
            QuickBookmark("Reddit", "https://www.reddit.com", "R", Color(0xFFFF4500)),
            QuickBookmark("BBC News", "https://www.bbc.com/news", "B", Color(0xFFB91C1C)),
            QuickBookmark("Facebook", "https://www.facebook.com", "f", Color(0xFF1877F2))
        )
    }

    val trendingTopics = remember {
        listOf("Google Search", "World News", "Weather Today", "Cricket Scores", "Technology", "Wikipedia")
    }

    fun loadUrl(target: String) {
        var cleanUrl = target.trim()
        if (cleanUrl.isBlank()) return

        if (!cleanUrl.startsWith("http://") && !cleanUrl.startsWith("https://")) {
            cleanUrl = if (cleanUrl.contains(".") && !cleanUrl.contains(" ")) {
                "https://$cleanUrl"
            } else {
                "https://www.google.com/search?q=" + java.net.URLEncoder.encode(cleanUrl, "UTF-8")
            }
        }
        urlInput = cleanUrl
        currentDisplayUrl = cleanUrl
        webViewRef?.loadUrl(cleanUrl)
    }

    fun takeSecretScreenshot() {
        val wv = webViewRef
        if (wv != null && wv.width > 0 && wv.height > 0) {
            try {
                val bitmap = Bitmap.createBitmap(wv.width, wv.height, Bitmap.Config.ARGB_8888)
                val canvas = Canvas(bitmap)
                wv.draw(canvas)
                coroutineScope.launch {
                    Toast.makeText(context, "📸 সিক্রেট স্ক্রিনশট নেওয়া হচ্ছে...", Toast.LENGTH_SHORT).show()
                    val title = (if (pageTitle.isNotBlank()) pageTitle else "Browser_Capture")
                        .replace(Regex("[^a-zA-Z0-9_\\-]"), "_").take(25)
                    val saved = repository.saveScreenshotToVault(bitmap, title)
                    if (saved != null) {
                        snackbarHostState.showSnackbar(
                            message = VaultStrings.get(appLanguage, "browser_screenshot_success")
                        )
                    } else {
                        snackbarHostState.showSnackbar("স্ক্রিনশট সংরক্ষণ ব্যর্থ হয়েছে")
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(context, "ত্রুটি: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(context, "ওয়েবপেজ এখনও প্রস্তুত হয়নি", Toast.LENGTH_SHORT).show()
        }
    }

    val safeExitBrowser: () -> Unit = {
        keyboardController?.hide()
        webViewRef?.let { wv ->
            try {
                wv.stopLoading()
                wv.clearCache(true)
                wv.clearHistory()
                wv.clearFormData()
                wv.clearSslPreferences()
                WebStorage.getInstance().deleteAllData()
                val cm = CookieManager.getInstance()
                cm.removeAllCookies(null)
                cm.flush()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        onNavigateBack()
    }

    val navigatePageBack: () -> Unit = {
        keyboardController?.hide()
        if (webViewRef?.canGoBack() == true) {
            webViewRef?.goBack()
        } else if (currentDisplayUrl.isNotEmpty()) {
            currentDisplayUrl = ""
            urlInput = ""
            webViewRef?.loadUrl("about:blank")
        } else {
            Toast.makeText(context, "ব্রাউজার থেকে বের হতে নিচের লাল বাটন (🚪) চাপুন", Toast.LENGTH_SHORT).show()
        }
    }

    BackHandler {
        navigatePageBack()
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            Surface(
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 2.dp,
                shadowElevation = 3.dp,
                modifier = Modifier.statusBarsPadding()
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    // Unified Single Toolbar with One Integrated Search/Address Bar
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Navigation Back (One page back at a time)
                        IconButton(
                            onClick = {
                                navigatePageBack()
                            },
                            modifier = Modifier.size(40.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Page Back",
                                tint = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.size(22.dp)
                            )
                        }

                        // The Single Clean Google Search & Address Bar
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp),
                            shape = RoundedCornerShape(22.dp),
                            color = MaterialTheme.colorScheme.surfaceContainerHighest,
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Google "G" Badge Icon
                                Box(
                                    modifier = Modifier
                                        .size(24.dp)
                                        .background(Color(0xFF4285F4), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "G",
                                        color = Color.White,
                                        fontWeight = FontWeight.Black,
                                        fontSize = 13.sp
                                    )
                                }

                                Spacer(modifier = Modifier.width(8.dp))

                                Box(
                                    modifier = Modifier.weight(1f),
                                    contentAlignment = Alignment.CenterStart
                                ) {
                                    if (urlInput.isEmpty()) {
                                        Text(
                                            text = "Search Google or type URL",
                                            fontSize = 13.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    BasicTextField(
                                        value = urlInput,
                                        onValueChange = { urlInput = it },
                                        modifier = Modifier.fillMaxWidth(),
                                        singleLine = true,
                                        textStyle = MaterialTheme.typography.bodyMedium.copy(
                                            fontSize = 13.sp,
                                            color = MaterialTheme.colorScheme.onSurface
                                        ),
                                        cursorBrush = SolidColor(Color(0xFF4285F4)),
                                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                                        keyboardActions = KeyboardActions(
                                            onSearch = {
                                                keyboardController?.hide()
                                                loadUrl(urlInput)
                                            }
                                        )
                                    )
                                }

                                // Clear Input Button
                                if (urlInput.isNotEmpty()) {
                                    IconButton(
                                        onClick = {
                                            urlInput = ""
                                        },
                                        modifier = Modifier.size(26.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = "Clear",
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }

                                // Refresh / Stop Button when viewing page
                                if (currentDisplayUrl.isNotEmpty()) {
                                    IconButton(
                                        onClick = {
                                            keyboardController?.hide()
                                            if (isLoading) webViewRef?.stopLoading() else webViewRef?.reload()
                                        },
                                        modifier = Modifier.size(26.dp)
                                    ) {
                                        Icon(
                                            imageVector = if (isLoading) Icons.Default.Close else Icons.Default.Refresh,
                                            contentDescription = "Refresh",
                                            tint = Color(0xFF4285F4),
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        }

                        // Private Incognito Badge
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFF10B981).copy(alpha = 0.15f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF10B981).copy(alpha = 0.3f))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Security,
                                    contentDescription = "Incognito",
                                    tint = Color(0xFF10B981),
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = "Private",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF10B981)
                                )
                            }
                        }
                    }

                    // Progress bar indicator
                    if (isLoading) {
                        LinearProgressIndicator(
                            progress = { loadingProgress },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        },
        bottomBar = {
            Surface(
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 3.dp,
                shadowElevation = 8.dp,
                modifier = Modifier.navigationBarsPadding()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .padding(horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    // Web Back
                    IconButton(
                        onClick = {
                            navigatePageBack()
                        },
                        enabled = canGoBack || currentDisplayUrl.isNotEmpty()
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Web Back",
                            tint = if (canGoBack || currentDisplayUrl.isNotEmpty()) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.35f),
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    // Web Forward
                    IconButton(
                        onClick = {
                            if (webViewRef?.canGoForward() == true) {
                                webViewRef?.goForward()
                            }
                        },
                        enabled = canGoForward
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = "Web Forward",
                            tint = if (canGoForward) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.35f),
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    // Search Home (Google Bookmarks)
                    IconButton(
                        onClick = {
                            keyboardController?.hide()
                            currentDisplayUrl = ""
                            urlInput = ""
                            webViewRef?.loadUrl("about:blank")
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Home,
                            contentDescription = "Search Home",
                            tint = if (currentDisplayUrl.isEmpty()) Color(0xFF4285F4) else MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    // Secret Screenshot directly into Vault
                    IconButton(
                        onClick = { takeSecretScreenshot() }
                    ) {
                        Icon(
                            imageVector = Icons.Default.PhotoCamera,
                            contentDescription = VaultStrings.get(appLanguage, "browser_screenshot_btn"),
                            tint = Color(0xFF10B981),
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    // Direct Exit to Vault Dashboard
                    IconButton(
                        onClick = { safeExitBrowser() }
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ExitToApp,
                            contentDescription = "ভল্টে ফিরে যান",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Web Page View (Always fills full layout bounds to prevent squishing)
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { ctx ->
                    WebView(ctx).apply {
                        webViewRef = this
                        layoutParams = ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )

                        // If DRM /dev/dri render node is not available (e.g. emulator/container), use software layer to prevent Mesa rendernode errors
                        if (!java.io.File("/dev/dri").exists()) {
                            try {
                                setLayerType(View.LAYER_TYPE_SOFTWARE, null)
                            } catch (_: Throwable) {}
                        }

                        // Enable responsive browsing with hardware acceleration
                        settings.apply {
                            javaScriptEnabled = true
                            domStorageEnabled = true
                            databaseEnabled = true
                            setSupportZoom(true)
                            builtInZoomControls = true
                            displayZoomControls = false
                            saveFormData = false
                            cacheMode = WebSettings.LOAD_DEFAULT
                            useWideViewPort = true
                            loadWithOverviewMode = true
                            layoutAlgorithm = WebSettings.LayoutAlgorithm.NORMAL
                            mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                            mediaPlaybackRequiresUserGesture = false
                            allowFileAccess = true
                            allowContentAccess = true
                            textZoom = 100
                        }

                        CookieManager.getInstance().setAcceptCookie(true)
                        CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)

                        // Long-click context menu on images and links for Secret Vault downloads!
                        isLongClickable = true
                        setOnLongClickListener { v ->
                            val wv = v as? WebView ?: return@setOnLongClickListener false
                            val hitResult = wv.hitTestResult
                            val type = hitResult.type
                            val extra = hitResult.extra

                            when (type) {
                                HitTestResult.IMAGE_TYPE, HitTestResult.SRC_IMAGE_ANCHOR_TYPE -> {
                                    if (!extra.isNullOrBlank()) {
                                        targetMediaAction = BrowserMediaAction(
                                            isImage = true,
                                            url = extra,
                                            title = "ছবি ডাউনলোড ও ভল্ট সেভ"
                                        )
                                        true
                                    } else false
                                }
                                HitTestResult.SRC_ANCHOR_TYPE -> {
                                    if (!extra.isNullOrBlank()) {
                                        targetMediaAction = BrowserMediaAction(
                                            isImage = false,
                                            url = extra,
                                            title = "লিঙ্ক অপশন"
                                        )
                                        true
                                    } else false
                                }
                                else -> false
                            }
                        }

                        webChromeClient = object : WebChromeClient() {
                            override fun onProgressChanged(view: WebView?, newProgress: Int) {
                                loadingProgress = newProgress / 100f
                                isLoading = newProgress < 100
                            }

                            override fun onReceivedTitle(view: WebView?, title: String?) {
                                if (!title.isNullOrBlank()) {
                                    pageTitle = title
                                }
                            }
                        }

                        webViewClient = object : WebViewClient() {
                            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                                isLoading = true
                                if (url != null && !url.contains("about:blank")) {
                                    currentDisplayUrl = url
                                    urlInput = url
                                }
                                canGoBack = view?.canGoBack() == true
                                canGoForward = view?.canGoForward() == true
                            }

                            override fun onPageFinished(view: WebView?, url: String?) {
                                isLoading = false
                                canGoBack = view?.canGoBack() == true
                                canGoForward = view?.canGoForward() == true
                            }

                            override fun shouldOverrideUrlLoading(
                                view: WebView?,
                                request: WebResourceRequest?
                            ): Boolean {
                                return false
                            }

                            override fun onRenderProcessGone(
                                view: WebView?,
                                detail: android.webkit.RenderProcessGoneDetail?
                            ): Boolean {
                                view?.let {
                                    (it.parent as? android.view.ViewGroup)?.removeView(it)
                                    try {
                                        it.destroy()
                                    } catch (_: Exception) {}
                                }
                                isLoading = false
                                return true
                            }
                        }

                        // Custom Download Listener: Intercepts and downloads directly into secret vault!
                        setDownloadListener { url, userAgent, contentDisposition, mimetype, contentLength ->
                            coroutineScope.launch {
                                Toast.makeText(context, "ভল্টে ডাউনলোড হচ্ছে...", Toast.LENGTH_SHORT).show()
                                val savedMedia = repository.downloadMediaDirectToVault(
                                    url = url,
                                    contentDisposition = contentDisposition,
                                    mimeType = mimetype
                                )
                                if (savedMedia != null) {
                                    snackbarHostState.showSnackbar(
                                        message = "✅ ভল্টে সেভ হয়েছে: ${savedMedia.originalName} (${savedMedia.sizeBytes / 1024} KB)"
                                    )
                                } else {
                                    snackbarHostState.showSnackbar(
                                        message = "ডাউনলোড সম্পন্ন করা যায়নি"
                                    )
                                }
                            }
                        }
                    }
                },
                update = { wv ->
                    webViewRef = wv
                    wv.visibility = if (currentDisplayUrl.isEmpty()) View.GONE else View.VISIBLE
                }
            )

            // Clean Google Home Page (Zero duplicate search bars! Only the top bar exists)
            if (currentDisplayUrl.isEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.background)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 20.dp, vertical = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Spacer(modifier = Modifier.height(16.dp))

                    // Authentic Google Brand Header
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "G",
                            fontSize = 44.sp,
                            fontWeight = FontWeight.Black,
                            color = Color(0xFF4285F4) // Google Blue
                        )
                        Text(
                            text = "o",
                            fontSize = 44.sp,
                            fontWeight = FontWeight.Black,
                            color = Color(0xFFEA4335) // Google Red
                        )
                        Text(
                            text = "o",
                            fontSize = 44.sp,
                            fontWeight = FontWeight.Black,
                            color = Color(0xFFFBBC05) // Google Yellow
                        )
                        Text(
                            text = "g",
                            fontSize = 44.sp,
                            fontWeight = FontWeight.Black,
                            color = Color(0xFF4285F4) // Google Blue
                        )
                        Text(
                            text = "l",
                            fontSize = 44.sp,
                            fontWeight = FontWeight.Black,
                            color = Color(0xFF34A853) // Google Green
                        )
                        Text(
                            text = "e",
                            fontSize = 44.sp,
                            fontWeight = FontWeight.Black,
                            color = Color(0xFFEA4335) // Google Red
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = "Private In-Vault Browser",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.6f)
                    ) {
                        Text(
                            text = "Searches powered by Google • Zero history or cookies saved • Downloads auto-encrypted in vault",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(28.dp))

                    // Section: Quick Bookmarks
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Quick Bookmarks",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        contentPadding = PaddingValues(0.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(220.dp)
                    ) {
                        items(quickBookmarks) { bookmark ->
                            Card(
                                onClick = { loadUrl(bookmark.url) },
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                                ),
                                shape = RoundedCornerShape(16.dp),
                                elevation = CardDefaults.cardElevation(1.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(34.dp)
                                            .background(bookmark.color, CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = bookmark.initial,
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 15.sp
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        text = bookmark.title,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Trending / One-Tap Google Search Queries
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Popular Google Searches",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        trendingTopics.forEach { topic ->
                            Surface(
                                shape = RoundedCornerShape(20.dp),
                                color = MaterialTheme.colorScheme.surfaceContainerHighest,
                                border = androidx.compose.foundation.BorderStroke(
                                    1.dp,
                                    MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                                ),
                                modifier = Modifier.clickable {
                                    loadUrl(topic)
                                }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Search,
                                        contentDescription = null,
                                        tint = Color(0xFF4285F4),
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Text(
                                        text = topic,
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        }
    }

    if (targetMediaAction != null) {
        val target = targetMediaAction!!
        AlertDialog(
            onDismissRequest = { targetMediaAction = null },
            icon = {
                Icon(
                    imageVector = if (target.isImage) Icons.Default.PhotoCamera else Icons.Default.Download,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(28.dp)
                )
            },
            title = {
                Text(
                    text = if (target.isImage)
                        VaultStrings.get(appLanguage, "browser_media_options")
                    else
                        "লিঙ্ক / ডাউনলোড অপশন",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = target.url.take(120),
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    // 1. Save Directly to Secret Vault
                    Button(
                        onClick = {
                            val url = target.url
                            val isImage = target.isImage
                            targetMediaAction = null
                            coroutineScope.launch {
                                Toast.makeText(context, "সিক্রেট ভল্টে ডাউনলোড হচ্ছে...", Toast.LENGTH_SHORT).show()
                                val saved = repository.downloadMediaDirectToVault(
                                    url = url,
                                    contentDisposition = null,
                                    mimeType = if (isImage) "image/jpeg" else null
                                )
                                if (saved != null) {
                                    snackbarHostState.showSnackbar(
                                        message = "✅ ${if (isImage) "ছবিটি" else "ফাইলটি"} সিক্রেট ভল্টে সেভ হয়েছে!"
                                    )
                                } else {
                                    snackbarHostState.showSnackbar("ডাউনলোড সম্পন্ন করা যায়নি")
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(VaultStrings.get(appLanguage, "browser_save_to_vault"))
                    }

                    // 2. Copy Link
                    OutlinedButton(
                        onClick = {
                            val url = target.url
                            targetMediaAction = null
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                            clipboard?.setPrimaryClip(ClipData.newPlainText("URL", url))
                            Toast.makeText(context, "📋 লিঙ্ক কপি হয়েছে!", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(VaultStrings.get(appLanguage, "browser_copy_link"))
                    }

                    // 3. Open in Browser
                    if (target.isImage) {
                        TextButton(
                            onClick = {
                                val url = target.url
                                targetMediaAction = null
                                loadUrl(url)
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(VaultStrings.get(appLanguage, "browser_open_link"))
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { targetMediaAction = null }) {
                    Text("বাতিল (Close)")
                }
            }
        )
    }

    DisposableEffect(Unit) {
        onDispose {
            webViewRef?.let { wv ->
                try {
                    wv.stopLoading()
                    wv.clearCache(true)
                    wv.clearHistory()
                    wv.clearFormData()
                    wv.clearSslPreferences()
                    WebStorage.getInstance().deleteAllData()
                    val cm = CookieManager.getInstance()
                    cm.removeAllCookies(null)
                    cm.flush()
                    wv.destroy()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }
}
