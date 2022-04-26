package com.season.ipfs_local.chat;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.season.ipfs.IPFSRequest;
import com.season.ipfs_local.R;

import java.util.List;

public class ChatAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private Context applicationContext;
    //存放数据
    List<ChatModel> chatModelList;

    Handler handler;

    //通过构造函数传入数据
    public ChatAdapter(Context applicationContext, List<ChatModel> dataList) {
        this.applicationContext = applicationContext;
        this.chatModelList = dataList;
        this.handler = new Handler();
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        //布局加载器
        View view = LayoutInflater.from(parent.getContext()).inflate(viewType == 1 ? R.layout.list_item_right : R.layout.list_item_left, parent, false);
        Log.e("OTC", view.toString());
        return viewType == 1?new ViewHolderRight(view):new ViewHolderLeft(view);
    }

    /**
     * 位置对应的数据与holder进行绑定
     *
     * @param hd
     * @param position
     */
    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder hd, int position) {
        ChatModel chatModel = chatModelList.get(position);
        if (hd instanceof ViewHolderLeft) {
            ViewHolderLeft holder = (ViewHolderLeft) hd;
            holder.leftLayout.setVisibility(View.VISIBLE);
            holder.leftNameTextView.setText(chatModel.getName());
            holder.leftContentTextView.setText(chatModel.getContent());
            holder.leftContentTextView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    ClipboardManager clipboard = (ClipboardManager) applicationContext.getSystemService(Context.CLIPBOARD_SERVICE);
                    ClipData clip = ClipData.newPlainText("simple text",
                            chatModel.getContent());
                    clipboard.setPrimaryClip(clip);
                    Toast.makeText(applicationContext, "复制成功", Toast.LENGTH_SHORT).show();
                }
            });
            loadImage(holder.leftImageView, chatModel.getImgId());
            if (TextUtils.isEmpty(chatModel.getFileName())) {
                holder.leftFileImageView.setVisibility(View.GONE);
                holder.leftContentTextView.setVisibility(View.VISIBLE);
            } else {
                holder.leftFileImageView.setVisibility(View.VISIBLE);
                holder.leftContentTextView.setVisibility(View.GONE);
                holder.leftFileImageView.setImageResource(R.drawable.loading);
                loadImage(holder.leftFileImageView, chatModel.getContent());
            }
        } else {
            ViewHolderRight holder = (ViewHolderRight) hd;
            holder.rightLayout.setVisibility(View.VISIBLE);
            holder.rightNameTextView.setText(chatModel.getName());
            holder.rightContentTextView.setText(chatModel.getContent());
            loadImage(holder.rightImageView, chatModel.getImgId());
            holder.rightNameTextView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    onNameClick();
                }
            });
            holder.rightContentTextView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    ClipboardManager clipboard = (ClipboardManager) applicationContext.getSystemService(Context.CLIPBOARD_SERVICE);
                    ClipData clip = ClipData.newPlainText("simple text",
                            chatModel.getContent());
                    clipboard.setPrimaryClip(clip);
                    Toast.makeText(applicationContext, "复制成功", Toast.LENGTH_SHORT).show();
                }
            });
            holder.rightImageView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    onIconClick();
                }
            });
            if (TextUtils.isEmpty(chatModel.getFileName())) {
                holder.rightFileImageView.setVisibility(View.GONE);
                holder.rightContentTextView.setVisibility(View.VISIBLE);
            } else {
                holder.rightFileImageView.setVisibility(View.VISIBLE);
                holder.rightContentTextView.setVisibility(View.GONE);
                holder.rightFileImageView.setImageResource(R.drawable.loading);
                loadImage(holder.rightFileImageView, chatModel.getContent());
            }
        }

    }

    @Override
    public int getItemViewType(int position) {
        ChatModel chatModel = chatModelList.get(position);
        if (chatModel.getType() == ChatModel.SEND) {
            return 1;
        } else {
            return 2;
        }
    }

    public void onNameClick() {

    }

    public void onIconClick() {

    }

    private void loadImage(ImageView imageView, String cid) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                byte[] bytes = IPFSRequest.catBytes(cid);
                if (bytes == null) {
                    return;
                }
                Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        imageView.setImageBitmap(bitmap);
                    }
                });
            }
        }).start();
    }

    /**
     * 获取数据长度
     *
     * @return
     */
    @Override
    public int getItemCount() {
        return chatModelList.size();
    }

    /**
     * 缓存页面布局，页面快速滚动时不必每次都重新创建View
     */
    static class ViewHolderLeft extends RecyclerView.ViewHolder {
        ImageView leftImageView;
        TextView leftNameTextView;
        TextView leftContentTextView;
        LinearLayout leftLayout;
        ImageView leftFileImageView;

        public ViewHolderLeft(@NonNull View itemView) {
            super(itemView);
            leftImageView = (ImageView) itemView.findViewById(R.id.left_image);
            leftFileImageView = (ImageView) itemView.findViewById(R.id.left_file);
            leftContentTextView = (TextView) itemView.findViewById(R.id.left_content);
            leftNameTextView = (TextView) itemView.findViewById(R.id.left_name);
            leftLayout = (LinearLayout) itemView.findViewById(R.id.left_bubble);

        }
    }
    /**
     * 缓存页面布局，页面快速滚动时不必每次都重新创建View
     */
    static class ViewHolderRight extends RecyclerView.ViewHolder {
        ImageView rightImageView;
        TextView rightNameTextView;
        ImageView rightFileImageView;
        TextView rightContentTextView;
        LinearLayout rightLayout;

        public ViewHolderRight(@NonNull View itemView) {
            super(itemView);
            rightImageView = (ImageView) itemView.findViewById(R.id.right_image);
            rightFileImageView = (ImageView) itemView.findViewById(R.id.right_file);
            rightContentTextView = (TextView) itemView.findViewById(R.id.right_content);
            rightNameTextView = (TextView) itemView.findViewById(R.id.right_name);
            rightLayout = (LinearLayout) itemView.findViewById(R.id.right_bubble);

        }
    }
}