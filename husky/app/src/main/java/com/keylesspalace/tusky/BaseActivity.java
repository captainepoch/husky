/*
 * Husky -- A Pleroma client for Android
 *
 * Copyright (C) 2023  The Husky Developers
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

import static org.koin.java.KoinJavaComponent.inject;
import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build.VERSION;
import android.os.Build.VERSION_CODES;
import android.os.Bundle;
import android.view.View;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.preference.PreferenceManager;
import com.google.android.material.snackbar.Snackbar;
import com.keylesspalace.tusky.adapter.AccountSelectionAdapter;
import com.keylesspalace.tusky.db.AccountEntity;
import com.keylesspalace.tusky.db.AccountManager;
import com.keylesspalace.tusky.interfaces.AccountSelectionListener;
import com.keylesspalace.tusky.interfaces.PermissionRequester;
import com.keylesspalace.tusky.util.ThemeUtils;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import kotlin.Lazy;
import timber.log.Timber;

public abstract class BaseActivity extends AppCompatActivity {

    public Lazy<AccountManager> accountManager = inject(AccountManager.class);
    private static final int REQUESTER_NONE = Integer.MAX_VALUE;
    private HashMap<Integer, PermissionRequester> requesters;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);

        /* There isn't presently a way to globally change the theme of a whole application at
         * runtime, just individual activities. So, each activity has to set its theme before any
         * views are created. */
        String theme = preferences.getString("appTheme", ThemeUtils.APP_THEME_DEFAULT);
        Timber.d("Active Theme [" + theme + "]");
        if(theme.equals("black")) {
            setTheme(R.style.TuskyBlackTheme);
        }

        // Set the taskdescription programmatically, the theme would turn it blue
        String appName = getString(R.string.app_name);
        Bitmap appIcon = BitmapFactory.decodeResource(getResources(), R.mipmap.ic_launcher);
        int recentsBackgroundColor = ThemeUtils.getColor(this, R.attr.colorSurface);

        setTaskDescription(
            new ActivityManager.TaskDescription(appName, appIcon, recentsBackgroundColor));

        int style = textStyle(preferences.getString("statusTextSize", "medium"));
        getTheme().applyStyle(style, false);

        if(requiresLogin()) {
            redirectIfNotLoggedIn();
        }

        requesters = new HashMap<>();
    }

    @Override
    public void setContentView(View view) {
        super.setContentView(view);

        view.setFitsSystemWindows(true);
    }

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(HuskyApplication.getLocaleManager().setLocale(base));
    }

    protected boolean requiresLogin() {
        return true;
    }

    private static int textStyle(String name) {
        return switch(name) {
            case "smallest" -> R.style.TextSizeSmallest;
            case "small" -> R.style.TextSizeSmall;
            default -> R.style.TextSizeMedium;
            case "large" -> R.style.TextSizeLarge;
            case "largest" -> R.style.TextSizeLargest;
        };
    }

    public void startActivityWithSlideInAnimation(Intent intent) {
        super.startActivity(intent);
        if(VERSION.SDK_INT < VERSION_CODES.TIRAMISU) {
            Timber.d("Execute enter transition");

            overridePendingTransition(R.anim.slide_from_right, R.anim.slide_to_left);
        }
    }

    @Override
    public void finish() {
        super.finish();
        if(VERSION.SDK_INT < VERSION_CODES.TIRAMISU) {
            Timber.d("Execute exit transition");

            overridePendingTransition(R.anim.slide_from_left, R.anim.slide_to_right);
        }
    }

    public void finishWithoutSlideOutAnimation() {
        super.finish();
    }

    protected void redirectIfNotLoggedIn() {
        AccountEntity account = accountManager.getValue().getActiveAccount();
        if(account == null) {
            Intent intent = new Intent(this, LoginActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivityWithSlideInAnimation(intent);
            finish();
        }
    }

    protected void showErrorDialog(View anyView,
        @StringRes int descriptionId,
        @StringRes int actionId,
        View.OnClickListener listener)
    {
        if(anyView != null) {
            Snackbar bar = Snackbar.make(anyView, getString(descriptionId), Snackbar.LENGTH_SHORT);
            bar.setAction(actionId, listener);
            bar.show();
        }
    }

    public void showAccountChooserDialog(CharSequence dialogTitle,
        boolean showActiveAccount,
        AccountSelectionListener listener)
    {
        List<AccountEntity> accounts = accountManager.getValue().getAllAccountsOrderedByActive();
        AccountEntity activeAccount = accountManager.getValue().getActiveAccount();

        switch(accounts.size()) {
            case 1 -> {
                listener.onAccountSelected(activeAccount);
                return;
            }
            case 2 -> {
                if(!showActiveAccount) {
                    for(AccountEntity account : accounts) {
                        if(activeAccount != account) {
                            listener.onAccountSelected(account);
                            return;
                        }
                    }
                }
            }
        }

        if(!showActiveAccount && activeAccount != null) {
            accounts.remove(activeAccount);
        }
        AccountSelectionAdapter adapter = new AccountSelectionAdapter(this);
        adapter.addAll(accounts);

        new AlertDialog.Builder(this).setTitle(dialogTitle).setAdapter(adapter,
            (dialogInterface, index) -> listener.onAccountSelected(accounts.get(index))).show();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
        @NonNull String[] permissions,
        @NonNull int[] grantResults)
    {
        if(requesters.containsKey(requestCode)) {
            PermissionRequester requester = requesters.remove(requestCode);
            requester.onRequestPermissionsResult(permissions, grantResults);
        }
    }

    public void requestPermissions(String[] permissions, PermissionRequester requester) {
        ArrayList<String> permissionsToRequest = new ArrayList<>();
        for(String permission : permissions) {
            if(ContextCompat.checkSelfPermission(this, permission) !=
               PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(permission);
            }
        }
        if(permissionsToRequest.isEmpty()) {
            int[] permissionsAlreadyGranted = new int[permissions.length];
            for(int i = 0; i < permissionsAlreadyGranted.length; ++i) {
                permissionsAlreadyGranted[i] = PackageManager.PERMISSION_GRANTED;
            }
            requester.onRequestPermissionsResult(permissions, permissionsAlreadyGranted);
            return;
        }

        int newKey = requester == null ? REQUESTER_NONE : requesters.size();
        if(newKey != REQUESTER_NONE) {
            requesters.put(newKey, requester);
        }
        String[] permissionsCopy = new String[permissionsToRequest.size()];
        permissionsToRequest.toArray(permissionsCopy);
        ActivityCompat.requestPermissions(this, permissionsCopy, newKey);

    }
}
