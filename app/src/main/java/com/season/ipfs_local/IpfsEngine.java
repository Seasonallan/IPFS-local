package com.season.ipfs_local;

import android.content.Context;
import android.os.Build;
import android.text.TextUtils;
import android.util.Log;

import com.season.ipfs.IPFSRequest;
import com.season.ipfs_local.chat.ChatModel;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

public class IpfsEngine {

    public static final String TOPIC = "topicChat";
    private static IpfsEngine instance;
    private Context context;

    private IpfsEngine(Context context) {
        this.context = context;
        this.dataChangeList = new ArrayList<>();
    }

    public static IpfsEngine getInstance(Context context) {
        if (instance == null) {
            instance = new IpfsEngine(context);
        }
        return instance;
    }

    List<IDataChange> dataChangeList;

    public void addDataChangeListener(IDataChange listener) {
        dataChangeList.remove(listener);
        dataChangeList.add(listener);
    }

    public void onDataChange(String data) {
        for (IDataChange listener : dataChangeList) {
            listener.onDataReceive(data);
        }
    }


    public interface IDataChange {
        void onDataReceive(String data);
    }

    public void setText(String tag, String text) {
        setText(text);
    }

    public void setText(String text) {
        onDataChange(text);
    }

    Process chatProcess;
    Process daemon;

    public void startV2(Runnable runnable) throws Exception {
        downloadFile();
        getBinaryFile().setExecutable(true);
        setText("ipfs init");
        Process exec = runCmd("init");
        if (exec == null) {
            setText("ipfs init error");
            return;
        }
        new Thread(new Runnable() {
            @Override
            public void run() {
                readOutput(exec, 500);

                setText("TT", "ipfs daemon --enable-pubsub-experiment");
                daemon = runCmd("daemon --enable-pubsub-experiment");
                if (daemon == null) {
                    setText("ipfs daemon error");
                    return;
                }
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        readOutput(daemon, 500, "Daemon is ready");

                        setText("ipfs id");
                        String res = IPFSRequest.id();
                        try {
                            setText(new JSONObject(res).toString(2));
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                        runnable.run();
                    }
                }).start();
                try {
                    daemon.waitFor();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }).start();
        exec.waitFor();
    }

    public void start() throws Exception {
        downloadFile();
        getBinaryFile().setExecutable(true);
        setText("ipfs init");
        Process exec = runCmd("init");
        readOutput(exec, 500);

        setText("TT", "ipfs daemon --enable-pubsub-experiment");
        daemon = runCmd("daemon --enable-pubsub-experiment");
        readOutput(daemon, 500, "Daemon is ready");

        setText("ipfs id");
        String res = IPFSRequest.id();
        setText(new JSONObject(res).toString(2));

    }

    public void startChatProcess() {
        setText("TT", "run ipfs pubsub sub " + IpfsEngine.TOPIC);

        chatProcess = runCmd("pubsub sub " + IpfsEngine.TOPIC);
        if (chatProcess == null) {
            setText("TT", "ipfs pubsub error");
            return;
        }
        new Thread(new Runnable() {
            @Override
            public void run() {
                readOutput(chatProcess, 100);
            }
        }).start();
        try {
            chatProcess.waitFor();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public void sendChat(ChatModel chatModel) {
        try {
            runCmd("pubsub pub " + IpfsEngine.TOPIC + " " + URLEncoder.encode(chatModel.toJson().toString()));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void stop() {
        if (daemon != null) {
            daemon.destroy();
        }
    }


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
        //Log.e("TT", "isAlive:" + isAlive);
        return isAlive;
    }

    private File getBinaryFile() {
        return new File(context.getFilesDir(), "ipfs");
    }

    private File getRepoPath() {
        return new File(context.getFilesDir(), ".ipfs_repo");
    }

    File getVersionFile() {
        return new File(context.getFilesDir(), ".ipfs_version");
    }

    boolean isReady() {
        return new File(getRepoPath(), "version").exists();
    }

    private String getBinaryFileByABI(String abi) {
        Log.e("TAG", abi);
        if (abi.toLowerCase().startsWith("x86")) {
            return "ipfs-x86";
        }
        return "ipfs-arm";
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
       // System.out.println("readStream>>" + content);
        return content;
    }

    Process runCmd(String cmd) {
        try {
            String[] env = {"IPFS_PATH=" + getRepoPath().getAbsolutePath()};
            String command = getBinaryFile().getAbsolutePath() + " " + cmd;

            return Runtime.getRuntime().exec(command, env);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    private void downloadFile() throws Exception {
        File file = getBinaryFile();
        if (file.exists()) {
            return;
        }
        InputStream myInput;
        OutputStream myOutput = new FileOutputStream(file.getAbsolutePath());
        myInput = context.getAssets().open(getBinaryFileByABI(Build.CPU_ABI));
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
