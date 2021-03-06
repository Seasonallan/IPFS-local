package com.season.ipfs_local.chat;

import android.Manifest;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.content.ContentUris;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.season.ipfs.IPFSRequest;
import com.season.ipfs.IpfsEngine;
import com.season.ipfs_local.R;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class ChatActivity extends AppCompatActivity implements IpfsEngine.IDataChange {

    List<ChatModel> chatModelList = new ArrayList<ChatModel>();
    RecyclerView recyclerView;
    FrameLayout frameLayout;
    ChatAdapter chatAdapter;
    SharedPreferences sharedPreferences;
    String name;
    String icon;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);
        initRecycler();

        sharedPreferences = getSharedPreferences("user", Context.MODE_PRIVATE);
        name = sharedPreferences.getString("name", null);
        icon = sharedPreferences.getString("icon", null);

        findViewById(R.id.set_icon_btn).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showIcon();
            }
        });
        findViewById(R.id.set_name_btn).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showName();
            }
        });
        findViewById(R.id.set_icon_web).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(ChatActivity.this, WebActivity.class));
            }
        });
        findViewById(R.id.send_txt_btn).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(true){
                    chatModelSending = new ChatModel(icon, name, new Random().nextLong() +"", new Random().nextBoolean()?ChatModel.SEND:ChatModel.RECEIVE);
                    chatModelList.add(chatModelSending);
                    chatAdapter.notifyItemInserted(chatModelList.size() - 1);
                    recyclerView.scrollToPosition(chatModelList.size() - 1);

                    sendChat(chatModelSending);
                    return;
                }
                addTextPopInput();
            }
        });
        findViewById(R.id.send_file_btn).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                selectIcon = false;
                if (ContextCompat.checkSelfPermission(ChatActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                        != PackageManager.PERMISSION_GRANTED) {
                    //??????WRITE_EXTERNAL_STORAGE??????
                    ActivityCompat.requestPermissions(ChatActivity.this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                            1);
                } else {
                    goSelectFile();
                }
            }
        });
        IpfsEngine.getInstance(getApplicationContext()).addDataChangeListener(this);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] strPerm,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, strPerm, grantResults);

        if (grantResults.length > 0
                && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            goSelectFile();
        } else {
            Toast.makeText(this, "????????????",
                    Toast.LENGTH_LONG).show();
        }
    }

    private void goSelectFile() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("*/*");
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        try {
            startActivityForResult(Intent.createChooser(intent, "????????????"), 10086);
        } catch (android.content.ActivityNotFoundException ex) {
            Toast.makeText(ChatActivity.this, "??????????????????????????????????????????????????????????????????", Toast.LENGTH_SHORT).show();
        }
    }

    private void showName() {
        final EditText inputServer = new EditText(this);
        if (name != null) {
            inputServer.setText(name);
            inputServer.setSelection(name.length());
        }
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("?????????????????????").setIcon(android.R.drawable.ic_dialog_info).setView(inputServer);
        builder.setPositiveButton("??????", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                name = inputServer.getText().toString();
                sharedPreferences.edit().putString("name", name).commit();
            }
        });
        builder.show();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Log.e("TAG", ">>" + requestCode);
        switch (requestCode) {
            case 10086:
                try {
                    String path = "";
                    if (Build.VERSION.SDK_INT >= 19) {
                        path = handleImageOnKitKat(ChatActivity.this, data);
                    } else {
                        path = handleImageBeforeKitKat(ChatActivity.this, data);
                    }
                    Log.e("TAG", ">>>" + path);
                    if(!TextUtils.isEmpty(path)){
                        final File uploadFile = new File(path);
                        new Thread(new Runnable() {
                            @Override
                            public void run() {
                                String res = IPFSRequest.upload(uploadFile);
                                if(res == null){
                                    Toast.makeText(ChatActivity.this, "????????????", Toast.LENGTH_SHORT).show();
                                    return;
                                }
                                try {
                                    JSONObject jsonObject = new JSONObject(res);
                                    String cid = jsonObject.getString("Hash");
                                    if(selectIcon){
                                        icon = cid;
                                        sharedPreferences.edit().putString("icon", icon).commit();
                                        return;
                                    }
                                    String fileName = jsonObject.getString("Name");
                                    chatModelSending = new ChatModel(fileName, icon, name, cid, ChatModel.SEND);
                                    chatModelList.add(chatModelSending);
                                    runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            chatAdapter.notifyItemInserted(chatModelList.size() - 1);
                                            recyclerView.scrollToPosition(chatModelList.size() - 1);
                                        }
                                    });

                                    //IpfsEngine.getInstance(getApplicationContext()).sendChat(chatModelSending);
                                } catch (JSONException e) {
                                    e.printStackTrace();
                                }
                                Log.e("TAG", "res>>>" + res);
                            }
                        }).start();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
                break;
        }
    }

    public static String handleImageOnKitKat(Context context, Intent data) {
        Log.e("TAG", "handleImageOnKitKat" + data.toString());
        String imagePath = null;
        Uri uri = data.getData();
        if (DocumentsContract.isDocumentUri(context, uri)) {
            //?????????document?????????Uri,?????????document id??????
            String docId = DocumentsContract.getDocumentId(uri);
            Log.e("TAG", "docId" + docId);
            Log.e("TAG", "Authority" + uri.getAuthority());
            if ("com.android.providers.media.documents".equals(uri.getAuthority())) {
                String id = docId.split(":")[1];  //????????????????????????id
                String selection = MediaStore.Images.Media._ID + "=" + id;
                imagePath = getImagePath(context, MediaStore.Images.Media.EXTERNAL_CONTENT_URI, selection);
            } else if ("com.android.providers.downloads.documents".equals(uri.getAuthority())) {
                if (docId != null && docId.startsWith("raw:")) {
                    return docId.substring(4);
                }
                Uri contentUri = ContentUris.withAppendedId(
                        Uri.parse("content://downloads/public_downloads"), Long.valueOf(docId));
                imagePath = getImagePath(context, contentUri, null);
            }
        } else if ("content".equalsIgnoreCase(uri.getScheme())) {
            //????????????document?????????Uri,???????????????????????????
            imagePath = getImagePath(context, uri, null);
        }
        return imagePath;
    }

    public static String handleImageBeforeKitKat(Context context, Intent data) {
        Uri uri = data.getData();
        return getImagePath(context, uri, null);
    }

    private static String getImagePath(Context context, Uri uri, String selection) {
        String path = null;
        Cursor cursor = context.getContentResolver().query(uri, null, selection, null, null);
        if (cursor != null) {
            if (cursor.moveToFirst()) {
                path = cursor.getString(cursor.getColumnIndex(MediaStore.Images.Media.DATA));
            }
            cursor.close();
        }
        return path;
    }

    boolean selectIcon = false;
    private void showIcon() {
        final EditText inputServer = new EditText(this);
        if (icon != null) {
            inputServer.setText(icon);
            inputServer.setSelection(icon.length());
        }
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("?????????????????????").setIcon(android.R.drawable.ic_dialog_info).setView(inputServer);
        builder.setNegativeButton("??????", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                selectIcon = true;
                if (ContextCompat.checkSelfPermission(ChatActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                        != PackageManager.PERMISSION_GRANTED) {
                    //??????WRITE_EXTERNAL_STORAGE??????
                    ActivityCompat.requestPermissions(ChatActivity.this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                            1);
                } else {
                    goSelectFile();
                }
            }
        });
        builder.setPositiveButton("??????", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                icon = inputServer.getText().toString();
                sharedPreferences.edit().putString("icon", icon).commit();
            }
        });
        builder.show();
    }

    ChatModel chatModelSending;

    private void initRecycler() {
        chatModelList.clear();
        frameLayout = findViewById(R.id.layout_container);
        recyclerView = findViewById(R.id.my_recyclerView);
        //???recyclerView??????????????????
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(layoutManager);
        //???????????????
        chatAdapter = new ChatAdapter(getApplicationContext(), chatModelList) {
            @Override
            public void onNameClick() {
                showName();
            }

            @Override
            public void onIconClick() {
                showIcon();
            }
        };
        recyclerView.setAdapter(chatAdapter);
    }

    PopInputLayout popInputLayout;

    //??????????????????
    private void addTextPopInput() {
        frameLayout.removeAllViews();
        popInputLayout = new PopInputLayout(ChatActivity.this) {
            @Override
            public void onRemove() {
                if (frameLayout != null) {
                    frameLayout.removeAllViews();
                }
            }

            @Override
            public void onTextConfirm(String text) {
                if (frameLayout != null) {
                    frameLayout.removeAllViews();
                }
                String msg = text;
                if (!msg.isEmpty()) {
                    chatModelSending = new ChatModel(icon, name, msg, ChatModel.SEND);
                    chatModelList.add(chatModelSending);
                    chatAdapter.notifyItemInserted(chatModelList.size() - 1);
                    recyclerView.scrollToPosition(chatModelList.size() - 1);

                    sendChat(chatModelSending);
                } else {
                    Toast.makeText(ChatActivity.this, "Cant be empty???", Toast.LENGTH_SHORT).show();
                }
            }
        };
        frameLayout.addView(popInputLayout);
    }



    public void sendChat(ChatModel chatModel) {
        try {
            IpfsEngine.getInstance(getApplicationContext()).runCmd("pubsub pub helloMe " + URLEncoder.encode(chatModel.toJson().toString()));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onBackPressed() {
        if (frameLayout.getChildCount() > 0) {
            InputMethodUtil.hideInput(this);
            frameLayout.removeAllViews();
            return;
        }
        super.onBackPressed();
    }

    @Override
    public void onDataReceive(String data) {
        if (TextUtils.isEmpty(data)) {
            return;
        }
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                ChatModel chatModel;
                try {
                    new JSONObject(data);
                    chatModel  = new ChatModel(data);
                } catch (JSONException e) {
                    //e.printStackTrace();
                    chatModel = new ChatModel(URLDecoder.decode(data));
                }
                if (chatModel.isSame(chatModelSending)) {
                    return;
                }
                chatModelList.add(chatModel);
                chatAdapter.notifyItemInserted(chatModelList.size() - 1);
                recyclerView.scrollToPosition(chatModelList.size() - 1);
            }
        });
    }
}

