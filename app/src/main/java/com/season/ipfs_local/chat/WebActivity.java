package com.season.ipfs_local.chat;

import android.graphics.Bitmap;
import android.net.http.SslError;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.webkit.ConsoleMessage;
import android.webkit.JsResult;
import android.webkit.SslErrorHandler;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.season.ipfs.IPFSRequest;
import com.season.ipfs_local.R;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;

public class WebActivity extends AppCompatActivity {

    private void loadIPFSFile(File file, String cid) {
        if (!file.isDirectory()) {
            file.mkdir();
        }
        Log.e("TAG", "ls " + cid);
        String res = IPFSRequest.ls(cid);
        Log.e("TAG", res);
        try {
            JSONObject jsonObject = new JSONObject(res);
            JSONArray jsonArray = jsonObject.getJSONArray("Objects");
            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject itemObject = jsonArray.getJSONObject(i);
                JSONArray itemArray = itemObject.getJSONArray("Links");
                for (int j = 0; j < itemArray.length(); j++) {
                    JSONObject fileObject = itemArray.getJSONObject(j);
                    String Name = fileObject.getString("Name");
                    String Hash = fileObject.getString("Hash");
                    int Type = fileObject.getInt("Type");
                    if (Type == 1) {
                        loadIPFSFile(new File(file, Name), Hash);
                    } else {
                        Log.e("TAG", "cat " + Hash);
                        byte[] fetchedData = IPFSRequest.catBytes(Hash);
                        OutputStream out = new FileOutputStream(new File(file, Name));
                        InputStream is = new ByteArrayInputStream(fetchedData);
                        byte[] buff = new byte[1024];
                        int len = 0;
                        while ((len = is.read(buff)) != -1) {
                            out.write(buff, 0, len);
                        }
                        is.close();
                        out.close();
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    TextView textView;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_web);

        WebView webView = findViewById(R.id.webView);
        textView = findViewById(R.id.title);
        WebSettings webSettings = webView.getSettings();
        webSettings.setCacheMode(WebSettings.LOAD_NO_CACHE);
        // 设置与Js交互的权限 设置允许JS弹窗
        webSettings.setJavaScriptEnabled(true);
        //支持自动适配
        webSettings.setUseWideViewPort(true);
        webSettings.setLoadWithOverviewMode(true);
        webSettings.setSupportZoom(true);  //支持放大缩小
        webSettings.setBuiltInZoomControls(true); //显示缩放按钮
        webSettings.setBlockNetworkImage(true);// 把图片加载放在最后来加载渲染
        webSettings.setAllowFileAccess(true); // 允许访问文件
        webSettings.setSaveFormData(true);
        webSettings.setGeolocationEnabled(true);
        webSettings.setDomStorageEnabled(true);
        webSettings.setJavaScriptCanOpenWindowsAutomatically(true);/// 支持通过JS打开新窗口
        webSettings.setLayoutAlgorithm(WebSettings.LayoutAlgorithm.NARROW_COLUMNS);
        webSettings.setLayoutAlgorithm(WebSettings.LayoutAlgorithm.SINGLE_COLUMN);
        webSettings.setJavaScriptCanOpenWindowsAutomatically(true);
        webSettings.setBlockNetworkImage(false);
        webSettings.setUserAgentString("Mozilla/5.0 (Linux; Android 4.1.1; HTC One X Build/JRO03C) AppleWebKit/537.31 (KHTML, like Gecko) Chrome/26.0.1410.58 Mobile Safari/537.31");
        //不加这个图片显示不出来
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP) {
            webView.getSettings().setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
        }
        webView.getSettings().setBlockNetworkImage(false);
        // 设置允许JS弹窗
        webView.getSettings().setJavaScriptCanOpenWindowsAutomatically(true);
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                Log.d("TAG", "shouldOverrideUrlLoading url:" + url);
                view.loadUrl(url);// 当打开新链接时，使用当前的 WebView，不会使用系统其他浏览器
                return true;
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                Log.d("TAG", "onPageFinished url:" + url);
                textView.setText(view.getTitle());

                tag = true;
                super.onPageFinished(view, url);
            }

            @Override
            public void onPageStarted(WebView view, String url, Bitmap favicon) {
                Log.d("TAG", "onPageStarted url:" + url);
                super.onPageStarted(view, url, favicon);
            }

            @Override
            public void onReceivedSslError(WebView view, SslErrorHandler handler, SslError error) {
                handler.proceed(); //接受所有证书
            }
        });
        webView.setWebChromeClient(new WebChromeClient(){
            @Override
            public boolean onConsoleMessage(ConsoleMessage cm) {
                Log.d("TAG", cm.message() + " -- From line "
                        + cm.lineNumber() + " of "
                        + cm.sourceId() );
                return true;
            }
            @Override
            public boolean onJsAlert(WebView view, String url, String message, JsResult result) {
                Log.d("TAG", "onPageStarted message:" + message);
                return true;
            }
        });

        new Thread(new Runnable() {
            @Override
            public void run() {
                loadIPFSFile(new File(getCacheDir(), "profile"), "QmUe8JiLux4oCDrJ4DDF251JhXWrhjbRYxFcBKCMSFYE9L");

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Log.e("TAG", "file://" + new File(getCacheDir(), "profile") + "/index.html");
                        //访问网页
                        webView.loadUrl("file:" + getCacheDir() + File.separator + "profile/index.html");
                    }
                });
            }
        }).start();
    }

    boolean tag = false;
}
