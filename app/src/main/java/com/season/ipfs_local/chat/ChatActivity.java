package com.season.ipfs_local.chat;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.season.ipfs_local.IpfsEngine;
import com.season.ipfs_local.R;

import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.List;

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
        findViewById(R.id.send_txt_btn).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                addTextPopInput();
            }
        });
        findViewById(R.id.send_file_btn).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
            }
        });
        IpfsEngine.getInstance(getApplicationContext()).addDataChangeListener(this);
    }

    private void showName() {
        final EditText inputServer = new EditText(this);
        if (name != null) {
            inputServer.setText(name);
            inputServer.setSelection(name.length());
        }
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("请设置你的昵称").setIcon(android.R.drawable.ic_dialog_info).setView(inputServer);
        builder.setPositiveButton("确定", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                name = inputServer.getText().toString();
                sharedPreferences.edit().putString("name", name).commit();
            }
        });
        builder.show();
    }

    private void showIcon() {
        final EditText inputServer = new EditText(this);
        if (icon != null) {
            inputServer.setText(icon);
            inputServer.setSelection(icon.length());
        }
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("请设置你的头像").setIcon(android.R.drawable.ic_dialog_info).setView(inputServer);
        builder.setNegativeButton("上传", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {

            }
        });
        builder.setPositiveButton("确定", new DialogInterface.OnClickListener() {
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
        //给recyclerView创建布局方式
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(layoutManager);
        //创建适配器
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

    //文字编辑弹窗
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

                    IpfsEngine.getInstance(getApplicationContext()).sendChat(chatModelSending);
                } else {
                    Toast.makeText(ChatActivity.this, "Cant be empty！", Toast.LENGTH_SHORT).show();
                }
            }
        };
        frameLayout.addView(popInputLayout);
    }


    @Override
    public void onBackPressed() {
        if(frameLayout.getChildCount() > 0){
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
                ChatModel chatModel = new ChatModel(URLDecoder.decode(data));
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

