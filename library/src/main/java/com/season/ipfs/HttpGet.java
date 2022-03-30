package com.season.ipfs;

import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class HttpGet {
    private static final int TIME_OUT = 60 * 1000;                          //超时时间

    /**
     * 从网络获取json数据,(String byte[})
     *
     * @param requestUrl
     * @return
     */
    public static byte[] getRequest(final String requestUrl) {
        HttpURLConnection conn = null;
        try {
            URL url = new URL(requestUrl);
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setReadTimeout(TIME_OUT);
            conn.setConnectTimeout(TIME_OUT);
            conn.setDoOutput(true);
            conn.setDoInput(true);
            conn.setUseCaches(false);//Post 请求不能使用缓存
            //设置请求头参数
            conn.setRequestProperty("Connection", "Keep-Alive");
            conn.setRequestProperty("Charset", "UTF-8");

            Log.e("TAG", "response: "+conn.getResponseCode());
            //读取服务器返回信息
            if (conn.getResponseCode() == 200) {
                //得到输入流
                InputStream is = conn.getInputStream();
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                byte[] buffer = new byte[1024];
                int len = 0;
                while (-1 != (len = is.read(buffer))) {
                    baos.write(buffer, 0, len);
                    baos.flush();
                }
                return baos.toByteArray();
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
        return null;
    }
}
