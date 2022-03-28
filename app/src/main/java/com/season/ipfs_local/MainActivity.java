package com.season.ipfs_local;

import androidx.appcompat.app.AppCompatActivity;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.concurrent.TimeUnit;

import io.ipfs.kotlin.IPFS;
import io.ipfs.kotlin.model.NamedHash;
import io.ipfs.kotlin.model.VersionInfo;
import okhttp3.OkHttpClient;

public class MainActivity extends AppCompatActivity {

    IPFS ipfs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        OkHttpClient okHttpClient = new OkHttpClient.Builder()
                .connectTimeout(1000, TimeUnit.SECONDS)
                .readTimeout(1000, TimeUnit.SECONDS)
                .build();

        ipfs = new IPFS("http://127.0.0.1:5001/api/v0/", okHttpClient);


        findViewById(R.id.button_init).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (isReady()) {
                    Log.e("TT", "already exists");
                    return;
                }
                download();
            }
        });

        findViewById(R.id.button_start).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        Log.e("TT", "run ipfs daemon");
                        try {
                            daemon = runCmd("daemon --enable-pubsub-experiment");
                            daemon.waitFor();
                            String result = readStream(daemon.getInputStream()) + readStream(daemon.getErrorStream());
                            Log.e("TT", result);
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
                            VersionInfo versionInfo = ipfs.getInfo().version();
                            Log.e("TT", versionInfo.getVersion());
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
                            NamedHash res = ipfs.getAdd().string("hello world! 2022");
                            hash = res.getHash();
                            Log.e("TT", res.getHash() + ">> " + res.getName());
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
                            String res = ipfs.getGet().cat(hash);
                            Log.e("TT", res);
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
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        Log.e("TT", "run ipfs pubsub sub helloMe");
                        try {
                            Process process = runCmd("pubsub sub helloMe");

                            Log.e("TT", "waitFor");
                            new Thread() {
                                public void run() {
                                    while (true) {
                                        String result = readStream(process.getInputStream()) + readStream(process.getErrorStream());
                                        try {
                                            Thread.sleep(5000);
                                        } catch (InterruptedException e) {
                                            e.printStackTrace();
                                        }
                                        Log.e("TT", result);
                                    }
                                }
                            }.start();
                            process.waitFor();
                            String result = readStream(process.getInputStream()) + readStream(process.getErrorStream());
                            Log.e("TT", result);

                            runCmd("pubsub pub helloMe \"hello, IPFS指南，飞向未来!\"");
                        } catch (Exception e) {
                            Log.e("TT", "ERROR");
                            e.printStackTrace();
                        }
                    }
                }).start();
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
        return new File(getFilesDir(), "ipfsbin");
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
            return "x86";
        }
        return "arm";
    }

    void download() {

        ProgressDialog progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Copy IPFS binary");
        progressDialog.setCancelable(false);
        progressDialog.show();

        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    downloadFile();
                    getBinaryFile().setExecutable(true);

                    Process exec = runCmd("init");
                    exec.waitFor();

                    String result = readStream(exec.getInputStream()) + readStream(exec.getErrorStream());
                    Log.e("TT", result);
                } catch (Exception e) {
                }
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        progressDialog.dismiss();
                    }
                });
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