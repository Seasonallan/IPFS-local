package com.season.ipfs_local.chat;

import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * 聊天类实体
 */
public class ChatModel {
    //发送类型
    public static final int SEND = 0;
    //接收类型
    public static final int RECEIVE = 1;


    private long id = -1;

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    private String fileName = "";
    private String imgId = "";
    private String name = "";
    private String content = "";

    //收发类型
    private int type;

    public ChatModel(String imgId, String name, String content, int type) {
        this("", imgId, name, content, type);
    }

    public ChatModel(String fileName, String imgId, String name, String content, int type) {
        this.id = System.currentTimeMillis();
        this.fileName = fileName;
        this.imgId = imgId;
        this.name = name;
        this.content = content;
        this.type = type;
    }

    public ChatModel(String json) {
        this.type = ChatModel.RECEIVE;
        try {
            JSONObject jsonObject = new JSONObject(json);
            this.id = jsonObject.getLong("id");
            this.imgId = getString(jsonObject, "icon");
            this.name = getString(jsonObject, "name");
            this.fileName = getString(jsonObject, "fileName");
            this.content = getString(jsonObject, "content");
        } catch (JSONException e) {
            e.printStackTrace();
            this.id = System.currentTimeMillis();
            this.name = "无";
            this.content = json;
        }
    }

    String getString(JSONObject jsonObject, String key){
        try{
            return jsonObject.getString(key);
        }catch (Exception e){
            return null;
        }
    }

    public boolean isSame(ChatModel chatModel){
        if(chatModel == null){
            return false;
        }
        Log.e("TAG", chatModel.id +">>"+ this.id);
        return this.id == chatModel.id;
    }

    public String getImgId() {
        return imgId;
    }

    public String getName() {
        return name;
    }

    public String getContent() {
        return content;
    }

    public int getType() {
        return type;
    }


    public JSONObject toJson() {
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("id", id);
            jsonObject.put("icon", imgId);
            jsonObject.put("name", name);
            jsonObject.put("content", content);
            jsonObject.put("fileName", fileName);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return jsonObject;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }
}