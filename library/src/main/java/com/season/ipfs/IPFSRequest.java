package com.season.ipfs;

import android.app.Activity;
import android.content.Context;
import android.util.Log;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;

public class IPFSRequest {

    public static String ipfsHost = "http://127.0.0.1:5001/api/v0/";

    public static String catString(String cid) {
        byte[] bytes = HttpGet.getRequest(ipfsHost + "cat/" + cid);
        return new String(bytes);
    }

    public static byte[] catBytes(String cid) {
        return HttpGet.getRequest(ipfsHost + "cat/" + cid);
    }

    public static String id() {
        byte[] bytes = HttpGet.getRequest(ipfsHost + "id");
        if(bytes == null){
            return "{}";
        }
        return new String(bytes);
    }

    public static String ls(String cid) {
        byte[] bytes = HttpGet.getRequest(ipfsHost + "ls/"+cid);
        if(bytes == null){
            return "{}";
        }
        return new String(bytes);
    }

    public static String upload(File file) {
        HashMap<String, File> fileHashMap = new HashMap<>();
        fileHashMap.put(file.getName(), file);
        return HttpPost.postRequest(ipfsHost + "add?stream-channels=true&progress=false",
                null, fileHashMap);
    }

    public static String uploadString(Context context, String content) {
        String name = "cacheIpfs";
        try {
            FileOutputStream outputStream = context.openFileOutput(name,
                    Activity.MODE_PRIVATE);
            outputStream.write(content.getBytes());
            outputStream.flush();
            outputStream.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return upload(context.getFileStreamPath(name));
    }

}
