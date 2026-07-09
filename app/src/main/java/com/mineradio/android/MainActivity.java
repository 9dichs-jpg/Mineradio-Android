package com.mineradio.android;

import android.annotation.SuppressLint;
import android.net.Uri;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.webkit.JavascriptInterface;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import androidx.appcompat.app.AppCompatActivity;
import androidx.webkit.WebViewAssetLoader;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

public class MainActivity extends AppCompatActivity {

    private WebView webView;
    private NodeService nodeService;

    public class AndroidBridge {
        @JavascriptInterface
        public String getPlatform() { return "android"; }

        @JavascriptInterface
        public boolean isAndroid() { return true; }

        @JavascriptInterface
        public int getServerPort() {
            return (nodeService != null) ? nodeService.getPort() : 0;
        }
    }

    @Override
    @SuppressLint("SetJavaScriptEnabled")
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        nodeService = new NodeService();
        nodeService.start(this, () -> runOnUiThread(this::initWebView));
        initWebView();
    }

    private void initWebView() {
        if (webView != null) return;

        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE |
                View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION |
                View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN |
                View.SYSTEM_UI_FLAG_HIDE_NAVIGATION |
                View.SYSTEM_UI_FLAG_FULLSCREEN |
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
        );

        webView = new WebView(this);
        webView.setLayerType(View.LAYER_TYPE_HARDWARE, null);
        webView.setOverScrollMode(View.OVER_SCROLL_NEVER);
        setContentView(webView);

        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setDatabaseEnabled(true);
        settings.setAllowFileAccess(true);
        settings.setAllowContentAccess(true);
        settings.setCacheMode(WebSettings.LOAD_DEFAULT);
        settings.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
        settings.setMediaPlaybackRequiresUserGesture(false);
        settings.setSupportZoom(false);
        settings.setDisplayZoomControls(false);
        settings.setTextZoom(100);
        settings.setLoadsImagesAutomatically(true);
        settings.setBlockNetworkImage(false);
        if (android.os.Build.VERSION.SDK_INT >= 23) {
            settings.setOffscreenPreRaster(false);
        }

        webView.addJavascriptInterface(new AndroidBridge(), "AndroidBridge");

        final WebViewAssetLoader assetLoader = new WebViewAssetLoader.Builder()
                .addPathHandler("/", new WebViewAssetLoader.AssetsPathHandler(this))
                .addPathHandler("/assets/", new WebViewAssetLoader.AssetsPathHandler(this))
                .addPathHandler("/vendor/", new WebViewAssetLoader.AssetsPathHandler(this))
                .setDomain("mineradio.local")
                .build();

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {
                Uri uri = request.getUrl();
                String path = uri.getPath();
                if (path != null && (path.endsWith("/index.html") || path.equals("/"))) {
                    WebResourceResponse optimized = buildOptimizedIndexResponse();
                    if (optimized != null) return optimized;
                }

                WebResourceResponse response = assetLoader.shouldInterceptRequest(uri);
                if (response != null) return response;
                return super.shouldInterceptRequest(view, request);
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                injectDesktopStubsAndBalancedMode();
            }
        });

        int port = (nodeService != null) ? nodeService.getPort() : 0;
        if (port > 0) {
            webView.loadUrl("http://127.0.0.1:" + port + "/index.html");
        } else {
            webView.loadUrl("https://mineradio.local/index.html");
        }
    }

    private WebResourceResponse buildOptimizedIndexResponse() {
        try {
            InputStream input = getAssets().open("index.html");
            String html = new String(readAllBytesCompat(input), StandardCharsets.UTF_8);
            String inject = buildHeadInject();
            if (html.contains("<head>")) {
                html = html.replace("<head>", "<head>" + inject);
            } else {
                html = inject + html;
            }
            return new WebResourceResponse(
                    "text/html",
                    "UTF-8",
                    new ByteArrayInputStream(html.getBytes(StandardCharsets.UTF_8))
            );
        } catch (Exception e) {
            return null;
        }
    }

    private byte[] readAllBytesCompat(InputStream input) throws Exception {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        byte[] data = new byte[8192];
        int nRead;
        while ((nRead = input.read(data, 0, data.length)) != -1) {
            buffer.write(data, 0, nRead);
        }
        input.close();
        return buffer.toByteArray();
    }

    private String buildHeadInject() {
        return "<script>" +
                "(function(){" +
                "window.__MINERADIO_PROJECTOR_BALANCED__=true;" +
                "try{Object.defineProperty(window,'devicePixelRatio',{get:function(){return 0.75;},configurable:true});}catch(e){}" +
                "var nativeRAF=window.requestAnimationFrame.bind(window);" +
                "var nativeCancel=window.cancelAnimationFrame?window.cancelAnimationFrame.bind(window):function(id){clearTimeout(id)};" +
                "var last=0,min=1000/30;" +
                "window.requestAnimationFrame=function(cb){return nativeRAF(function(t){var wait=min-(t-last);if(wait>0){return setTimeout(function(){nativeRAF(function(t2){last=t2;cb(t2);});},wait);}last=t;cb(t);});};" +
                "window.cancelAnimationFrame=function(id){try{nativeCancel(id);}catch(e){clearTimeout(id);}};" +
                "document.documentElement.classList.add('projector-balanced-root');" +
                "})();" +
                "</script>" +
                "<style id=\"projector-balanced-head-css\">" +
                "html.projector-balanced-root,html.projector-balanced-root body{background:#000;}" +
                "body.projector-balanced #album-bg{filter:blur(42px) brightness(.22) saturate(1.35)!important;transform:scale(1.18)!important;}" +
                "body.projector-balanced #splash::before{filter:blur(.2px)!important;}" +
                "body.projector-balanced #search-box,body.projector-balanced #search-results,body.projector-balanced .home-hero,body.projector-balanced .home-tile,body.projector-balanced .home-card,body.projector-balanced #bottom-bar.visible,body.projector-balanced #play-btn,body.projector-balanced .icon-btn,body.projector-balanced .quality-popover,body.projector-balanced .volume-popover,body.projector-balanced .mini-queue-popover{backdrop-filter:blur(10px) saturate(1.08)!important;-webkit-backdrop-filter:blur(10px) saturate(1.08)!important;}" +
                "body.projector-balanced .home-disc,body.projector-balanced .home-card,body.projector-balanced .home-tile{will-change:transform,opacity;}" +
                "body.projector-balanced canvas{image-rendering:auto;}" +
                "body.projector-balanced *{text-rendering:geometricPrecision;}" +
                "</style>";
    }

    private void injectDesktopStubsAndBalancedMode() {
        String js = "javascript:(function() {" +
            "if (!window.desktopWindow) {" +
            "window.desktopWindow = {" +
            "  isDesktop: false," +
            "  minimize:function(){return Promise.resolve();}," +
            "  toggleMaximize:function(){return Promise.resolve();}," +
            "  toggleFullscreen:function(){return Promise.resolve();}," +
            "  exitFullscreenWindowed:function(){return Promise.resolve();}," +
            "  getState:function(){return Promise.resolve({isMaximized:false,isMinimized:false,isFullscreen:false});}," +
            "  close:function(){return Promise.resolve();}," +
            "  openNeteaseMusicLogin:function(){return Promise.resolve();}," +
            "  clearNeteaseMusicLogin:function(){return Promise.resolve();}," +
            "  openQQMusicLogin:function(){return Promise.resolve();}," +
            "  clearQQMusicLogin:function(){return Promise.resolve();}," +
            "  openUpdateInstaller:function(){return Promise.resolve();}," +
            "  restartApp:function(){return Promise.resolve();}," +
            "  configureGlobalHotkeys:function(){return Promise.resolve();}," +
            "  exportJsonFile:function(){return Promise.resolve();}," +
            "  importJsonFile:function(){return Promise.resolve();}," +
            "  setDesktopLyricsEnabled:function(){return Promise.resolve();}," +
            "  updateDesktopLyrics:function(){return Promise.resolve();}," +
            "  setWallpaperMode:function(){return Promise.resolve();}," +
            "  updateWallpaperMode:function(){return Promise.resolve();}," +
            "  onGlobalHotkey:function(){return function(){};}," +
            "  onDesktopLyricsLockState:function(){return function(){};}," +
            "  onDesktopLyricsEnabledState:function(){return function(){};}," +
            "  onStateChange:function(){return function(){};}" +
            "};}" +
            "document.documentElement.classList.add('projector-balanced-root');" +
            "if(document.body){document.body.classList.add('android-shell','projector-balanced');}" +
        "})();";
        webView.evaluateJavascript(js, null);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK && webView != null && webView.canGoBack()) {
            webView.evaluateJavascript("window.history.back();", null);
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    protected void onDestroy() {
        if (nodeService != null) nodeService.stop();
        if (webView != null) { webView.destroy(); webView = null; }
        super.onDestroy();
    }
}
