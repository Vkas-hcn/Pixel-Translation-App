package com.vkas.translationapp.widget;

import android.content.Context;
import android.text.Editable;
import android.text.InputType;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;

import com.vkas.translationapp.R;
import com.vkas.translationapp.utils.KLog;

public class EditSearchView extends LinearLayout {

    private ConstraintLayout conViewEditSearch;
    private ImageView imgIcSearch;

    public interface EditSearchListener {

        void onTextChanged(String et);
    }

    public interface EditSearchCancelListener {
        void onCancelListener();
    }

    private String search;
    private EditText et_txt;
    private Context context;
    private EditSearchListener listener = null;
    public EditSearchCancelListener listenerCancel = null;

    public EditSearchView(Context context) {
        super(context);
        this.init(context);
    }

    public EditSearchView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        this.init(context);
    }

    public EditSearchView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        this.init(context);
    }

    private void init(Context context) {
        this.context = context;
        View  view = LayoutInflater.from(context).inflate(R.layout.view_edit_search, this);
        conViewEditSearch = view.findViewById(R.id.con_view_edit_search);
        imgIcSearch = view.findViewById(R.id.img_ic_search);
        this.setBackgroundResource(R.color.white);
        et_txt = view.findViewById(R.id.view_edit);
        et_txt.setOnEditorActionListener(new SearchClickEvent());
        et_txt.addTextChangedListener(textWatcher);
    }

    public void setEditHintTxt(String msg) {
        this.et_txt.setHint(msg);
    }


    public void setEditSearchListener(EditSearchListener listener) {
        this.listener = listener;
    }

    public void setSearchCancelListener(EditSearchCancelListener listener) {
        this.listenerCancel = listener;
    }

    public void setEtClear() {
        this.et_txt.setHint("");
    }


    final class SearchClickEvent implements TextView.OnEditorActionListener {
        @Override
        public boolean onEditorAction(TextView textView, int i, KeyEvent keyEvent) {
            if (i == EditorInfo.IME_ACTION_SEARCH) {
                search = et_txt.getText().toString().trim();
                InputMethodManager inputMethodManager = (InputMethodManager) getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
                inputMethodManager.hideSoftInputFromWindow(et_txt.getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
                return true;
            }
            return false;
        }
    }

    final TextWatcher textWatcher = new TextWatcher() {
        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {

        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {

        }

        @Override
        public void afterTextChanged(Editable s) {
            listener.onTextChanged(s.toString().trim());
        }
    };


    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
    }

}

