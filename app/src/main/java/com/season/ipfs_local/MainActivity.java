package com.season.ipfs_local;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.season.ipfs.IpfsEngine;
import com.season.ipfs_local.chat.ChatActivity;
import com.season.ipfs_local.chat.WebActivity;

import java.text.SimpleDateFormat;
import java.util.Date;


public class MainActivity extends AppCompatActivity implements IpfsEngine.IDataChange {

    TextView textView;
    ScrollView scrollView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        textView = findViewById(R.id.text);
        scrollView = findViewById(R.id.scroll);

        IpfsEngine.getInstance(getApplicationContext()).addDataChangeListener(this);
        findViewById(R.id.button_start).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            IpfsEngine.getInstance(getApplicationContext()).startV2(new Runnable() {
                                @Override
                                public void run() {
                                    runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            new Handler().postDelayed(new Runnable() {
                                                @Override
                                                public void run() {
                                                    startActivity(new Intent(MainActivity.this, ChatActivity.class));
                                                }
                                            }, 800);
                                        }
                                    });
                                    IpfsEngine.getInstance(getApplicationContext()).startChatProcess();
                                }
                            });
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }).start();
            }
        });

    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        IpfsEngine.getInstance(getApplicationContext()).stop();
    }

    @Override
    public void onDataReceive(String text) {
        if (TextUtils.isEmpty(text)) {
            return;
        }
        Log.e("TT", text);
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                SimpleDateFormat simpleDateFormat = new SimpleDateFormat("HH:mm:ss #");// HH:mm:ss
                Date date = new Date(System.currentTimeMillis());
                textView.setText(textView.getText().toString() + "\n" + simpleDateFormat.format(date) + " " + text);
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        scrollView.fullScroll(ScrollView.FOCUS_DOWN);
                    }
                }, 300);
            }
        });
    }
}