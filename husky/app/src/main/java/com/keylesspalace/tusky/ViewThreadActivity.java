/*
 * Husky -- A Pleroma client for Android
 *
 * Copyright (C) 2022  The Husky Developers
 * Copyright (C) 2017  Andrew Dawson
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.keylesspalace.tusky;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.FragmentTransaction;
import com.keylesspalace.tusky.databinding.ActivityViewThreadBinding;
import com.keylesspalace.tusky.fragment.ViewThreadFragment;

public class ViewThreadActivity extends BottomSheetActivity {

    public static final int REVEAL_BUTTON_HIDDEN = 1;
    public static final int REVEAL_BUTTON_REVEAL = 2;
    public static final int REVEAL_BUTTON_HIDE = 3;

    public static Intent startIntent(Context context, String id, String url) {
        Intent intent = new Intent(context, ViewThreadActivity.class);
        intent.putExtra(ID_EXTRA, id);
        intent.putExtra(URL_EXTRA, url);
        return intent;
    }

    private static final String ID_EXTRA = "id";
    private static final String URL_EXTRA = "url";
    private static final String FRAGMENT_TAG = "ViewThreadFragment_";

    private int revealButtonState = REVEAL_BUTTON_HIDDEN;
    private ActivityViewThreadBinding binding;
    private ViewThreadFragment fragment;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityViewThreadBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        Toolbar toolbar = binding.includedToolbar.toolbar;
        setSupportActionBar(toolbar);
        ActionBar actionBar = getSupportActionBar();
        if(actionBar != null) {
            actionBar.setTitle(R.string.title_view_thread);
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setDisplayShowHomeEnabled(true);
        }

        String id = getIntent().getStringExtra(ID_EXTRA);

        fragment =
            (ViewThreadFragment) getSupportFragmentManager().findFragmentByTag(FRAGMENT_TAG + id);
        if(fragment == null) {
            fragment = ViewThreadFragment.newInstance(id);
        }

        FragmentTransaction fragmentTransaction = getSupportFragmentManager().beginTransaction();
        fragmentTransaction.replace(binding.fragmentContainer.getId(), fragment, FRAGMENT_TAG + id);
        fragmentTransaction.commit();

        applyForcedIntents(binding.getRoot(), null);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.view_thread_toolbar, menu);
        MenuItem menuItem = menu.findItem(R.id.action_reveal);
        menuItem.setVisible(revealButtonState != REVEAL_BUTTON_HIDDEN);
        menuItem.setIcon(revealButtonState == REVEAL_BUTTON_REVEAL ? R.drawable.ic_eye_24dp :
            R.drawable.ic_hide_media_24dp);
        return super.onCreateOptionsMenu(menu);
    }

    public void setRevealButtonState(int state) {
        switch(state) {
            case REVEAL_BUTTON_HIDDEN:
            case REVEAL_BUTTON_REVEAL:
            case REVEAL_BUTTON_HIDE:
                this.revealButtonState = state;
                invalidateOptionsMenu();
                break;
            default:
                throw new IllegalArgumentException("Invalid reveal button state: " + state);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId()) {
            case android.R.id.home: {
                getOnBackPressedDispatcher().onBackPressed();

                return true;
            }
            case R.id.action_reveal: {
                fragment.onRevealPressed();
                return true;
            }
        }
        return super.onOptionsItemSelected(item);
    }
}
