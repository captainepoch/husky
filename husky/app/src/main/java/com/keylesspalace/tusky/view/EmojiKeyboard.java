package com.keylesspalace.tusky.view;

import android.view.*;
import android.content.*;
import android.util.*;
import android.widget.*;
import android.app.*;
import android.text.*;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

import androidx.annotation.NonNull;
import androidx.viewpager2.widget.ViewPager2;
import androidx.recyclerview.widget.RecyclerView;
import androidx.preference.PreferenceManager;
import com.keylesspalace.tusky.R;
import com.keylesspalace.tusky.adapter.StickerAdapter;
import com.keylesspalace.tusky.adapter.UnicodeEmojiAdapter;
import com.keylesspalace.tusky.entity.StickerPack;

import java.util.*;

public class EmojiKeyboard extends LinearLayout {
    private TabLayout tabs;
    private ViewPager2 pager;
    private TabLayoutMediator currentMediator;
    private String preferenceKey;
    private SharedPreferences pref;
    private Set<String> recents;
    private String RECENTS_DELIM = "; ";
    private int MAX_RECENTS_ITEMS = 50;
    private RecyclerView.Adapter adapter;
    public boolean isSticky = false; // TODO

    public EmojiKeyboard(Context context) {
        super(context);
        init(context);
    }

    public EmojiKeyboard(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public EmojiKeyboard(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    void init(Context context) {
        inflate(context, R.layout.item_emoji_picker, this);
        
        pref = PreferenceManager.getDefaultSharedPreferences(context);
        tabs = findViewById(R.id.picker_tabs);
        pager = findViewById(R.id.picker_pager);
    }
    
    public static final int UNICODE_MODE = 0;
    public static final int CUSTOM_MODE  = 1;
    public static final int STICKER_MODE = 2;

    private void setupKeyboardWithAdapter(RecyclerView.Adapter adapter, String preferenceKey) {
        this.preferenceKey = preferenceKey;
        this.adapter = adapter;

        List<String> list = Arrays.asList(pref.getString(preferenceKey, "").split(RECENTS_DELIM));
        recents = new LinkedHashSet<String>(list);
        ((EmojiKeyboardAdapter)adapter).onRecentsUpdate(recents);

        pager.setAdapter(adapter);

        if(currentMediator != null)
            currentMediator.detach();

        currentMediator = new TabLayoutMediator(tabs, pager, (TabLayoutMediator.TabConfigurationStrategy)adapter);
        currentMediator.attach();
    }

    public void setupStickerKeyboard(OnEmojiSelectedListener listener, StickerPack packs[]) {
        MAX_RECENTS_ITEMS = 20;
        setupKeyboardWithAdapter(new StickerAdapter(packs, (_id, _emoji) -> {
            this.appendToRecents(_emoji);
            listener.onEmojiSelected(_id, _emoji);
        }), "STICKER_RECENTS");
    }

    public void setupKeyboard(String id, int mode, OnEmojiSelectedListener listener) {
        switch(mode) {
            // WOOOPS, I forgot that I need to pass data to adapter
            // For stickers, use SetupStickerKeyboard instead
            // For custom emoji, use TODO
            case CUSTOM_MODE:
            case STICKER_MODE:
                throw new IllegalArgumentException();
            default:
            case UNICODE_MODE:
                setupKeyboardWithAdapter(new UnicodeEmojiAdapter(id, (_id, _emoji) -> {
                    this.appendToRecents(_emoji);
                    listener.onEmojiSelected(_id, _emoji);
                }), "UNICODE_RECENTS");
        }
    }
    
    private void appendToRecents(String id) {
        recents.remove(id);
        recents.add(id);
        int size = recents.size();
        String joined;
        final SharedPreferences.Editor editor = pref.edit();
        if(size > MAX_RECENTS_ITEMS) {
            List<String> list = new ArrayList<String>(recents);
            list = list.subList(size - MAX_RECENTS_ITEMS, size);
            joined = TextUtils.join(RECENTS_DELIM, list);
            if(isSticky) {
                recents = new LinkedHashSet<String>(list);
            }
        } else {
            joined = TextUtils.join(RECENTS_DELIM, recents);
        }
        
        editor.putString(preferenceKey, joined);
        editor.apply();
        
        // no point on updating view if we are will be closed
        if(isSticky) {
            ((EmojiKeyboardAdapter)adapter).onRecentsUpdate(recents);
        }
    }

    public interface OnEmojiSelectedListener {
        void onEmojiSelected(@NonNull String id, @NonNull String emoji);
    }
    
    public interface EmojiKeyboardAdapter {
        void onRecentsUpdate(@NonNull Set<String> set);
    }
    
    public static void show(Context ctx, String id, int mode, OnEmojiSelectedListener listener) {
        final Dialog dialog = new Dialog(ctx);
        
        dialog.setTitle(null);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_emoji_keyboard);
        EmojiKeyboard kbd = (EmojiKeyboard)dialog.findViewById(R.id.dialog_emoji_keyboard);
        kbd.setupKeyboard(id, mode, (_id, _emoji) -> {
            listener.onEmojiSelected(_id, _emoji);
            if(!kbd.isSticky)
                dialog.dismiss();
        });
        
        dialog.show();
    }
}
