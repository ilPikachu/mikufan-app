package com.mikufan

import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Typeface
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.util.TypedValue
import android.view.Gravity
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.webkit.*
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.core.view.setPadding
import androidx.databinding.DataBindingUtil
import com.google.android.material.snackbar.Snackbar
import com.mikufan.databinding.ActivityMainBinding
import com.mikufan.util.extension.dpToPx
import com.mikufan.util.network.ConnectivityCheck
import com.shawnlin.numberpicker.NumberPicker

//TODO: Download entire main page html instead of relying on webview cache
class MainActivity : AppCompatActivity() {
    companion object {
        private const val TAG = "MainActivity"
        private const val MIKUFAN_URL = "https://www.mikufan.com/"
        private const val MIKUFAN_DOMAIN = "mikufan.com"
        private const val SCROLL_ANIMATION_DURATION: Long = 400
        private const val FAB_ALPHA = 0.5f
        private const val DIALOG_TITLE_SP = 20f
        private const val BUNDLE_WEBVIEW_URL = "BUNDLE_WEBVIEW_URL"

    }

    private lateinit var binding: ActivityMainBinding
    private val connectivityCheck = ConnectivityCheck(this)

    private var lastPageIndex = 1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_main)

        setupToolbar()
        setupWebView()
        setupSwipeRefresh()
        setupFAB()
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)

        val restoredUrl = savedInstanceState.getString(BUNDLE_WEBVIEW_URL)
        restoredUrl?.let {
            binding.webview.loadUrl(it)
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString(BUNDLE_WEBVIEW_URL, binding.webview.url)
    }

    override fun onBackPressed() {
        if (binding.webview.canGoBack()) {
            binding.webview.goBack()
        } else {
            super.onBackPressed()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_back -> binding.webview.goBack()
            R.id.action_forward -> binding.webview.goForward()
            R.id.action_share -> shareCurrentPage(binding.webview.url)
            R.id.action_goto_page -> showGotoPageDialog()
            R.id.action_search -> showSearchDialog()
        }

        return super.onOptionsItemSelected(item)
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayShowTitleEnabled(false)

        binding.toolbarTitle.setOnClickListener {
            binding.webview.loadUrl(MIKUFAN_URL)
            lastPageIndex = 1
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun setupWebView() {
        binding.webview.apply {
            webChromeClient = WebChromeClient()

            webViewClient = object : WebViewClient() {
                override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                    super.onPageStarted(view, url, favicon)
                    if (!binding.swipeRefresh.isRefreshing) {
                        binding.progressBar.visibility = View.VISIBLE
                    }
                }

                override fun onPageFinished(view: WebView?, url: String?) {
                    super.onPageFinished(view, url)
                    binding.swipeRefresh.isRefreshing = false
                    binding.progressBar.visibility = View.GONE
                }

                override fun onReceivedError(
                    view: WebView?,
                    request: WebResourceRequest?,
                    error: WebResourceError?
                ) {
                    super.onReceivedError(view, request, error)
                    Log.e(TAG, "onReceivedError $error")
                    binding.progressBar.visibility = View.GONE
                    // TODO: Show Something went wrong view, variation of offline view
                    showOfflineSnackBar()
                }


                override fun shouldOverrideUrlLoading(view: WebView?, url: String?): Boolean {
                    if (url == null) {
                        Log.e(TAG, "shouldOverrideUrlLoading: URL is null")
                        return true
                    }

                    if (!connectivityCheck.isConnected) {
                        showOfflineSnackBar()
                        return true
                    }

                    if (url.contains(MIKUFAN_DOMAIN)) {
                        binding.webview.loadUrl(url)
                    } else {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                        startActivity(intent)
                    }

                    return true
                }
            }

            settings.apply {
                javaScriptEnabled = true
                setAppCachePath(applicationContext.cacheDir.absolutePath)
                allowFileAccess = true
                setAppCacheEnabled(true)
                cacheMode = WebSettings.LOAD_DEFAULT
            }

            if (!connectivityCheck.isConnected) {
                settings.cacheMode = WebSettings.LOAD_CACHE_ELSE_NETWORK
            }

            loadUrl(MIKUFAN_URL)
        }
    }

    private fun setupSwipeRefresh() {
        binding.swipeRefresh.apply {
            setColorSchemeColors(
                ContextCompat.getColor(
                    context,
                    R.color.colorPrimary
                )
            )
            setOnRefreshListener {
                binding.webview.reload()
            }
        }
    }

    private fun setupFAB() {
        binding.fab.apply {
            alpha = FAB_ALPHA
            setOnClickListener {
                ObjectAnimator.ofInt(binding.webview, "ScrollY", binding.webview.scrollY, 0).apply {
                    duration = SCROLL_ANIMATION_DURATION
                }.start()
            }
        }
    }

    private fun shareCurrentPage(url: String) {
        val sendIntent: Intent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_TEXT, url)
            type = "text/plain"
        }

        val shareIntent = Intent.createChooser(sendIntent, null)
        startActivity(shareIntent)
    }

    private fun showGotoPageDialog() {
        val numberPicker: NumberPicker = NumberPicker(this).apply {
            minValue = 1
            value = lastPageIndex
            textColor = ContextCompat.getColor(context, R.color.colorPrimary)
            dividerColor = ContextCompat.getColor(context, R.color.colorAccent)
            selectedTextColor = ContextCompat.getColor(context, R.color.colorAccent)
        }

        val alertTitle: TextView = TextView(this).apply {
            text = getString(R.string.goto_page)
            gravity = Gravity.CENTER
            setPadding(dpToPx(8))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, DIALOG_TITLE_SP)
            setTextColor(ContextCompat.getColor(context, R.color.colorAccent))
            setTypeface(null, Typeface.BOLD)
        }

        AlertDialog.Builder(this).apply {
            setCustomTitle(alertTitle)
            setView(numberPicker)
            setNegativeButton(getString(R.string.cancel), null)
            setPositiveButton(getString(R.string.ok)) { _, _ ->
                binding.webview.loadUrl(MIKUFAN_URL + "page/" + numberPicker.value)
                lastPageIndex = numberPicker.value
            }
        }.show()
    }

    private fun showSearchDialog() {
        val searchTitle: TextView = TextView(this).apply {
            text = getString(R.string.search)
            gravity = Gravity.CENTER
            setPadding(dpToPx(8))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, DIALOG_TITLE_SP)
            setTextColor(ContextCompat.getColor(context, R.color.colorAccent))
            setTypeface(null, Typeface.BOLD)
        }

        val editText: EditText = EditText(this).apply {
            gravity = Gravity.CENTER
            setTextSize(TypedValue.COMPLEX_UNIT_SP, DIALOG_TITLE_SP)
            setTextColor(ContextCompat.getColor(context, R.color.colorPrimary))
            setTypeface(null, Typeface.BOLD)
        }

        AlertDialog.Builder(this).apply {
            setCustomTitle(searchTitle)
            setView(editText)
            setNegativeButton(getString(R.string.cancel), null)
            setPositiveButton(getString(R.string.ok)) { _, _ ->
                val query = editText.text.toString().replace(" ", "+")
                binding.webview.loadUrl("https://www.mikufan.com/?s=${query}&searchsubmit=")
            }
        }.show()
    }

    private fun showOfflineSnackBar() {
        Snackbar.make(
            binding.root,
            R.string.offline_message, Snackbar.LENGTH_SHORT
        ).apply {
            setBackgroundTint(ContextCompat.getColor(context, R.color.colorPrimaryDark))
            setAction(R.string.retry) { binding.webview.reload() }
        }.show()
    }

}
