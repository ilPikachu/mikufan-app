package com.mikufan

import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.webkit.*
import androidx.databinding.DataBindingUtil
import com.mikufan.databinding.ActivityMainBinding
import com.mikufan.util.network.ConnectivityCheck

class MainActivity : AppCompatActivity() {
    companion object {
        private const val TAG = "MainActivity"
        private const val MIKUFAN_URL = "https://www.mikufan.com/"
        private const val MIKUFAN_DOMAIN = "mikufan.com"
        private const val animationDuration: Long = 400
    }

    private lateinit var binding: ActivityMainBinding
    private val connectivityCheck = ConnectivityCheck(this)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_main)

        /*
        if (!connectivityCheck.isConnected) {
            // TODO: Show offline view
        } else {
            setupWebView()
        }
         */

        setupWebView()
        setupSwipeRefresh()
        setupFAB()
    }

    override fun onBackPressed() {
        if (binding.webview.canGoBack()) {
            binding.webview.goBack()
        } else {
            super.onBackPressed()
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
                    // TODO: Show Something went wrong view, variation of offline view
                    binding.progressBar.visibility = View.GONE
                }


                override fun shouldOverrideUrlLoading(view: WebView?, url: String?): Boolean {
                    if (url == null) {
                        Log.e(TAG, "shouldOverrideUrlLoading: URL is null")
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

            settings.javaScriptEnabled = true
            loadUrl(MIKUFAN_URL)
        }
    }

    private fun setupSwipeRefresh() {
        binding.swipeRefresh.setOnRefreshListener {
            binding.webview.reload()
        }
    }

    private fun setupFAB() {
        binding.fab.setOnClickListener {
            ObjectAnimator.ofInt(binding.webview, "ScrollY", binding.webview.scrollY, 0).apply {
                duration = animationDuration
            }.start()
        }
    }

}
