/*
 * Nextcloud Android client application
 *
 * @author Tobias Kaminsky
 * Copyright (C) 2017 Tobias Kaminsky
 * Copyright (C) 2017 Nextcloud GmbH.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */

package com.owncloud.android.ui.activity;

import android.annotation.SuppressLint;
import android.content.pm.ApplicationInfo;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ProgressBar;

import com.owncloud.android.MainApp;
import com.owncloud.android.R;
import com.owncloud.android.lib.common.utils.Log_OC;
import com.owncloud.android.utils.DisplayUtils;
import com.owncloud.android.utils.ThemeUtils;

import java.io.InputStream;

import androidx.appcompat.app.ActionBar;
import androidx.drawerlayout.widget.DrawerLayout;

/**
 * This activity shows an URL as a web view
 */
public class ExternalSiteWebView extends FileActivity {
    public static final String EXTRA_TITLE = "TITLE";
    public static final String EXTRA_URL = "URL";
    public static final String EXTRA_SHOW_SIDEBAR = "SHOW_SIDEBAR";
    public static final String EXTRA_SHOW_TOOLBAR = "SHOW_TOOLBAR";
    public static final String EXTRA_MENU_ITEM_ID = "MENU_ITEM_ID";
    public static final String EXTRA_TEMPLATE = "TEMPLATE";

    private static final String TAG = ExternalSiteWebView.class.getSimpleName();

    protected boolean showToolbar = true;
    protected int webViewLayout = R.layout.externalsite_webview;
    private int menuItemId;
    protected WebView webview;
    private boolean showSidebar;
    String url;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log_OC.v(TAG, "onCreate() start");

        Bundle extras = getIntent().getExtras();
        String title = extras.getString(EXTRA_TITLE);
        url = extras.getString(EXTRA_URL);
        if (extras.containsKey(EXTRA_SHOW_TOOLBAR)) {
            showToolbar = extras.getBoolean(EXTRA_SHOW_TOOLBAR);
        }

        menuItemId = extras.getInt(EXTRA_MENU_ITEM_ID);
        showSidebar = extras.getBoolean(EXTRA_SHOW_SIDEBAR);

        // show progress
        Window window = getWindow();
        if (window != null) {
            window.requestFeature(Window.FEATURE_PROGRESS);
        }

        super.onCreate(savedInstanceState);

        setContentView(webViewLayout);

        webview = findViewById(R.id.webView);
        final WebSettings webSettings = webview.getSettings();

        webview.setFocusable(true);
        webview.setFocusableInTouchMode(true);
        webview.setClickable(true);

        // allow debugging (when building the debug version); see details in
        // https://developers.google.com/web/tools/chrome-devtools/remote-debugging/webviews
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT &&
            (getApplicationInfo().flags & ApplicationInfo.FLAG_DEBUGGABLE) != 0) {
            Log_OC.d(this, "Enable debug for webView");
            WebView.setWebContentsDebuggingEnabled(true);
        }

        // setup toolbar
        if (showToolbar) {
            setupToolbar();
        } else {
            if (findViewById(R.id.appbar) != null) {
                findViewById(R.id.appbar).setVisibility(View.GONE);
            }
        }

        // setup drawer
        setupDrawer(menuItemId);

        if (!showSidebar) {
            setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED);
        }

        if (!TextUtils.isEmpty(title)) {
            setupActionBar(title);
        }
        setupWebSettings(webSettings);

        final ProgressBar progressBar = findViewById(R.id.progressBar);

        if (progressBar != null) {
            webview.setWebChromeClient(new WebChromeClient() {
                public void onProgressChanged(WebView view, int progress) {
                    progressBar.setProgress(progress * 1000);
                }
            });
        }

        webview.setWebViewClient(new WebViewClient() {
            public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
                InputStream resources = getResources().openRawResource(R.raw.custom_error);
                String customError = DisplayUtils.getData(resources);

                if (!customError.isEmpty()) {
                    webview.loadData(customError, "text/html; charset=UTF-8", null);
                }
            }
        });

        webview.loadUrl(url);
    }

    @SuppressLint("SetJavaScriptEnabled")
    private void setupWebSettings(WebSettings webSettings) {
        // enable zoom
        webSettings.setSupportZoom(true);
        webSettings.setBuiltInZoomControls(true);
        webSettings.setDisplayZoomControls(false);

        // Non-responsive webs are zoomed out when loaded
        webSettings.setUseWideViewPort(true);
        webSettings.setLoadWithOverviewMode(true);

        // user agent
        webSettings.setUserAgentString(MainApp.getUserAgent());

        // no private data storing
        webSettings.setSavePassword(false);
        webSettings.setSaveFormData(false);

        // disable local file access
        webSettings.setAllowFileAccess(false);

        // enable javascript
        webSettings.setJavaScriptEnabled(true);
        webSettings.setDomStorageEnabled(true);

        // caching disabled in debug mode
        if ((getApplicationInfo().flags & ApplicationInfo.FLAG_DEBUGGABLE) == 0) {
            webSettings.setAppCacheEnabled(true);
            webSettings.setAppCachePath(getCacheDir().getPath());
        }
    }

    private void setupActionBar(String title) {
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            ThemeUtils.setColoredTitle(actionBar, title, this);

            if (showSidebar) {
                actionBar.setDisplayHomeAsUpEnabled(true);
            } else {
                setDrawerIndicatorEnabled(false);
            }
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        boolean retval;
        switch (item.getItemId()) {
            case android.R.id.home:
                if (showSidebar) {
                    if (isDrawerOpen()) {
                        closeDrawer();
                    } else {
                        openDrawer();
                    }
                } else {
                    finish();
                }
            retval = true;
            break;

            default:
                retval = super.onOptionsItemSelected(item);
                break;
        }
        return retval;
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        setDrawerMenuItemChecked(menuItemId);

    }

    public WebView getWebview() {
        return this.webview;
    }
}
