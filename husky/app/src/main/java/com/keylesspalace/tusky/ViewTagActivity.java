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
import android.view.MenuItem;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import com.keylesspalace.tusky.databinding.ActivityViewTagBinding;
import com.keylesspalace.tusky.fragment.TimelineFragment;
import java.util.Collections;

public class ViewTagActivity extends BottomSheetActivity {

    private static final String HASHTAG = "hashtag";
    private ActivityViewTagBinding binding;

    public static Intent getIntent(Context context, String tag) {
        Intent intent = new Intent(context, ViewTagActivity.class);
        intent.putExtra(HASHTAG, tag);
        return intent;
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityViewTagBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        String hashtag = getIntent().getStringExtra(HASHTAG);

        Toolbar toolbar = binding.includedToolbar.toolbar;
        setSupportActionBar(toolbar);
        ActionBar bar = getSupportActionBar();

        if(bar != null) {
            bar.setTitle(String.format(getString(R.string.title_tag), hashtag));
            bar.setDisplayHomeAsUpEnabled(true);
            bar.setDisplayShowHomeEnabled(true);
        }

        FragmentTransaction fragmentTransaction = getSupportFragmentManager().beginTransaction();
        Fragment fragment = TimelineFragment.newHashtagInstance(Collections.singletonList(hashtag));
        fragmentTransaction.replace(binding.fragmentContainer.getId(), fragment);
        fragmentTransaction.commit();

        applyForcedIntents(binding.getRoot(), null);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId()) {
            case android.R.id.home: {
                getOnBackPressedDispatcher().onBackPressed();

                return true;
            }
        }
        return super.onOptionsItemSelected(item);
    }
}
