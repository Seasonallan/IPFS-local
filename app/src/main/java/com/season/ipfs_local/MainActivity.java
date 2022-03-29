package com.season.ipfs_local;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;


import com.season.ipfs.IPFSRequest;

import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;



public class MainActivity extends AppCompatActivity {

    TextView textView;
    ScrollView scrollView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        textView = findViewById(R.id.text);
        scrollView = findViewById(R.id.scroll);


        findViewById(R.id.button_init).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                download();
            }
        });

        findViewById(R.id.button_start).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        setText("TT", "ipfs daemon --enable-pubsub-experiment");
                        try {
                            daemon = runCmd("daemon --enable-pubsub-experiment");
                            readOutput(daemon, 500, "Daemon is ready");
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }).start();
            }
        });
        findViewById(R.id.button_version).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            setText("ipfs id");
                            String res = IPFSRequest.id();
                            setText(new JSONObject(res).toString(2));
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }).start();
            }
        });
        findViewById(R.id.button_add).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            setText("ipfs add test.txt");
                            String res = IPFSRequest.uploadString(MainActivity.this, "can u see me? no");
                            JSONObject jsonObject = new JSONObject(res);
                            cid = jsonObject.getString("Hash");
                            setText(jsonObject.toString(2));
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }).start();
            }
        });

        findViewById(R.id.button_cat).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            setText("ipfs cat " + cid);
                            String res = IPFSRequest.catString(cid);
                            setText(res);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }).start();
            }
        });

        findViewById(R.id.button_pubsub).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (chatProcess == null) {
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            setText("TT", "run ipfs pubsub sub helloMe");
                            try {
                                chatProcess = runCmd("pubsub sub helloMe");
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        ((Button) findViewById(R.id.button_pubsub)).setText("发送消息");
                                    }
                                });
                                readOutput(chatProcess, 100);
                            } catch (Exception e) {
                                setText("TT", "ERROR");
                                e.printStackTrace();
                            }
                            chatProcess = null;
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    ((Button) findViewById(R.id.button_pubsub)).setText("进入聊天室");
                                }
                            });

                        }
                    }).start();
                } else {
                    final EditText inputServer = new EditText(MainActivity.this);
                    AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                    builder.setTitle("消息").setIcon(android.R.drawable.ic_dialog_info).setView(inputServer)
                            .setNegativeButton("取消", null);
                    builder.setPositiveButton("发送", new DialogInterface.OnClickListener() {

                        public void onClick(DialogInterface dialog, int which) {
                            setText("TT", "run ipfs pubsub pub helloMe " + inputServer.getText().toString());
                            try {
                                chatProcess = runCmd("pubsub pub helloMe " + inputServer.getText().toString());

                            } catch (Exception e) {
                                e.printStackTrace();
                            }

                        }
                    });
                    builder.show();
                }
            }
        });

    }

    String cid = "QmcfkLDz2dsz5NwTLxwL2S8qJHMSFvgRRPwpXpZjxq3KAy";
    Process chatProcess;

    public void readOutput(Process process, long time) {
        readOutput(process, time, null);
    }

    public void readOutput(Process process, long time, String keyBreak) {
        while (isProcessAlive(process)) {
            String result = readStream(process.getInputStream()) + readStream(process.getErrorStream());
            try {
                Thread.sleep(time);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            setText("TT", result);
            if (!TextUtils.isEmpty(keyBreak) && result.contains(keyBreak)) {
                return;
            }
        }
    }

    public boolean isProcessAlive(Process process) {
        boolean isAlive;
        try {
            process.exitValue();
            isAlive = false;
        } catch (IllegalThreadStateException e) {
            isAlive = true;
        }
        Log.e("TT", "isAlive:" + isAlive);
        return isAlive;
    }

    public void setText(String tag, String text) {
        setText(text);
    }

    public void setText(String text) {
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

    String hash;
    Process daemon;

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (daemon != null) {
            daemon.destroy();
        }
    }

    private File getBinaryFile() {
        return new File(getFilesDir(), "ipfs");
    }

    private File getRepoPath() {
        return new File(getFilesDir(), ".ipfs_repo");
    }

    File getVersionFile() {
        return new File(getFilesDir(), ".ipfs_version");
    }

    boolean isReady() {
        return new File(getRepoPath(), "version").exists();
    }

    private String getBinaryFileByABI(String abi) {
        if (abi.toLowerCase().startsWith("x86")) {
            return "ipfs";
        }
        return "ipfs-arm";
    }

    void download() {
        new Thread(new Runnable() {
            @RequiresApi(api = Build.VERSION_CODES.O)
            @Override
            public void run() {
                try {
                    downloadFile();
                    getBinaryFile().setExecutable(true);
//                    try {
//                        String command = "chmod 777 " + getBinaryFile().getAbsolutePath();
//                        Log.i("zyl", "command = " + command);
//                        Runtime.getRuntime().exec(command);
//                    } catch (IOException e) {
//                        Log.i("zyl", "chmod fail!!!!");
//                        e.printStackTrace();
//                    }

                    setText("ipfs init");
                    Process exec = runCmd("init");
                    readOutput(exec, 500);
                } catch (Exception e) {
                    e.printStackTrace();
                    setText(e.getMessage());
                }
            }
        }).start();
    }

    String readStream(InputStream stream) {
        String content = "";
        try {
            if (stream != null) {
                StringBuffer strBuff = new StringBuffer();
                byte[] tmp = new byte[1024];
                while (stream.available() > 0) {
                    int i = stream.read(tmp, 0, 1024);
                    if (i < 0)
                        break;
                    strBuff.append(new String(tmp, 0, i));
                }
                content = strBuff.toString();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        System.out.println("readStream>>" + content);
        return content;
    }

    Process runCmd(String cmd) throws IOException {
        String[] env = {"IPFS_PATH=" + getRepoPath().getAbsolutePath()};
        String command = getBinaryFile().getAbsolutePath() + " " + cmd;

        return Runtime.getRuntime().exec(command, env);
    }

    private void downloadFile() throws Exception {
        File file = getBinaryFile();
        if (file.exists()) {
            return;
        }
        setText("copy ipfs binary");
        InputStream myInput;
        OutputStream myOutput = new FileOutputStream(file.getAbsolutePath());
        myInput = this.getAssets().open(getBinaryFileByABI(Build.CPU_ABI));
        byte[] buffer = new byte[1024];
        int length = myInput.read(buffer);
        while (length > 0) {
            myOutput.write(buffer, 0, length);
            length = myInput.read(buffer);
        }
        myOutput.flush();
        myInput.close();
        myOutput.close();

    }
}