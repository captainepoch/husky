package com.keylesspalace.tusky.util;

import android.content.Context;
import android.net.Uri;
import android.util.Log;
import androidx.annotation.NonNull;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.keylesspalace.tusky.db.AppDatabase;
import com.keylesspalace.tusky.db.TootDao;
import com.keylesspalace.tusky.db.TootEntity;
import java.util.ArrayList;
import kotlin.Lazy;
import static org.koin.java.KoinJavaComponent.inject;

public final class SaveTootHelper {

    private static final String TAG = "SaveTootHelper";

    private final TootDao tootDao;
    private final Context context;
    private final Lazy<Gson> gson = inject(com.google.gson.Gson.class);

    public SaveTootHelper(@NonNull AppDatabase appDatabase, @NonNull Context context) {
        this.tootDao = appDatabase.tootDao();
        this.context = context;
    }

    public void deleteDraft(int tootId) {
        TootEntity item = tootDao.find(tootId);
        if(item != null) {
            deleteDraft(item);
        }
    }

    public void deleteDraft(@NonNull TootEntity item) {
        // Delete any media files associated with the status.
        ArrayList<String> uris =
            gson.getValue().fromJson(item.getUrls(), new TypeToken<ArrayList<String>>() {
            }.getType());
        if(uris != null) {
            for(String uriString : uris) {
                Uri uri = Uri.parse(uriString);
                if(context.getContentResolver().delete(uri, null, null) == 0) {
                    Log.e(TAG, String.format("Did not delete file %s.", uriString));
                }
            }
        }
        // Update DB
        tootDao.delete(item.getUid());
    }
}
