package com.season.ipfs_local.chat;

import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.graphics.Rect;
import android.os.Handler;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.animation.DecelerateInterpolator;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;

import androidx.fragment.app.FragmentActivity;

import com.season.ipfs_local.R;

import io.github.rockerhieu.emojicon.EmojiconEditText;
import io.github.rockerhieu.emojicon.EmojiconsFragment;
import io.github.rockerhieu.emojicon.emoji.Emojicon;


public abstract class PopInputLayout extends FrameLayout implements View.OnClickListener {
    Button btn_finish;
    EmojiconEditText mEtInput;
    FragmentActivity activity;

    FrameLayout layout_emoji;
    LinearLayout et_container;
    View allLayout;
    private String text;

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
        if (mEtInput != null && text != null) {
            mEtInput.setText(text);
        }
    }

    public PopInputLayout(FragmentActivity activity) {
        super(activity);
        this.activity = activity;
        LayoutInflater.from(activity).inflate(R.layout.pop_input, this, true);
        init();
        initView();
    }

    private void init() {
        btn_finish = findViewById(R.id.btn_finish);
        mEtInput = findViewById(R.id.et_input);
        layout_emoji = findViewById(R.id.framelayout_emoji);
        et_container = findViewById(R.id.et_container);
        allLayout = findViewById(R.id.mask_view);


        btn_finish.setOnClickListener(this);
        et_container.setOnClickListener(this);
        allLayout.setOnClickListener(this);
        findViewById(R.id.ll_cop).setOnClickListener(this);
        setEmojiconFragment(false);
    }

    @Override
    public void onClick(View v) {
        int i = v.getId();
        if (i == R.id.ll_cop) {
            InputMethodUtil.hideInput(activity);

        } else if (i == R.id.btn_finish) {
            String content = mEtInput.getText().toString();
            if (TextUtils.isEmpty(content)) {
                //ToastUtil.showToast("????????????????????????");
                return;
            }
            mEtInput.setText("");
            InputMethodUtil.hideInputForced(activity, mEtInput);
            onTextConfirm(content);

        } else if (i == R.id.mask_view) {//??????????????????????????????
            InputMethodUtil.hideInputForced(activity, mEtInput);
            onRemove();
        }
    }

    public abstract void onRemove();

    public abstract void onTextConfirm(String text);


    boolean layout = false;

    private void initView() {

        //?????????????????????
        allLayout.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                final Rect r = new Rect();
                allLayout.getWindowVisibleDisplayFrame(r);
                final int heightDiff = allLayout.getHeight() - (r.bottom - r.top);
                if (!layout && heightDiff > 200) { // if more than 100 pixels??? its probably a keyboard...
                    layout = true;
                    layout_emoji.getLayoutParams().height = heightDiff;
                    layout_emoji.requestLayout();
                }
            }
        });

        et_container.setVisibility(View.INVISIBLE);
        et_container.post(new Runnable() {
            @Override
            public void run() {
                ValueAnimator fadeAnim = ObjectAnimator.ofFloat(et_container, "translationY", et_container.getHeight(), 0F);
                fadeAnim.setInterpolator(new DecelerateInterpolator());
                fadeAnim.setDuration(300);
                fadeAnim.start();
                et_container.setVisibility(View.VISIBLE);
            }
        });

        new Handler().post(new Runnable() {
            @Override
            public void run() {
                InputMethodUtil.showInputForced(activity, mEtInput);
                InputMethodUtil.requestFocus(mEtInput);
                mEtInput.setSelection(mEtInput.getText().toString().length());
            }
        });
    }

    /**
     * ?????????????????????fragment
     *
     * @param useSystemDefault:??????????????????????????????
     */
    private void setEmojiconFragment(boolean useSystemDefault) {
        EmojiconsFragment emojiconsFragment = EmojiconsFragment.newInstance
                (useSystemDefault);
        emojiconsFragment.setmOnEmojiconBackspaceClickedListener(new EmojiconsFragment.OnEmojiconBackspaceClickedListener() {
            @Override
            public void onEmojiconBackspaceClicked(View v) {
                PopInputLayout.this.onEmojiconBackspaceClicked(v);
            }

            @Override
            public void onEmojiconClicked(Emojicon emojicon) {
                PopInputLayout.this.onEmojiconClicked(emojicon);
            }
        });
        activity.getSupportFragmentManager().beginTransaction().replace(R.id.framelayout_emoji, emojiconsFragment).commit();
    }

    public void onEmojiconClicked(Emojicon emojicon) {
        mEtInput.getText().insert(mEtInput.getSelectionStart(), emojicon.getEmoji());
        mEtInput.setSelection(mEtInput.getSelectionStart() + 0);
        //mEtInput.setText();
        //mEtInput.setText(mEtInput.getText().toString() + emojicon.getEmoji());
        //mEtInput.setSelection(mEtInput.getText().toString().length());
    }

    public void onEmojiconBackspaceClicked(View v) {
        mEtInput.dispatchKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DEL));
    }

}
