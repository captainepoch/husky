package com.keylesspalace.tusky

import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.internal.runner.junit4.AndroidJUnit4ClassRunner
import androidx.test.platform.app.InstrumentationRegistry
import com.keylesspalace.tusky.db.AppDatabase
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4ClassRunner::class)
class MigrationsTest {

    private val testDB = "migration_test"

    @JvmField
    @Rule
    var helper: MigrationTestHelper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        AppDatabase::class.java.canonicalName,
        FrameworkSQLiteOpenHelperFactory()
    )

    @Test
    fun migrateTo11() {
        val db = helper.createDatabase(testDB, 10)

        val id = 1
        val domain = "domain.site"
        val token = "token"
        val active = true
        val accountId = "accountId"
        val username = "username"
        val values = arrayOf(
            id, domain, token, active, accountId, username, "Display Name",
            "https://picture.url", true, true, true, true, true, true, true,
            true, "1000", "[]", "[{\"shortcode\": \"emoji\", \"url\": \"yes\"}]", 0, false,
            false, true
        )

        db.execSQL(
            "INSERT OR REPLACE INTO `AccountEntity`(`id`,`domain`,`accessToken`,`isActive`," +
                "`accountId`,`username`,`displayName`,`profilePictureUrl`,`notificationsEnabled`," +
                "`notificationsMentioned`,`notificationsFollowed`,`notificationsReblogged`," +
                "`notificationsFavorited`,`notificationSound`,`notificationVibration`," +
                "`notificationLight`,`lastNotificationId`,`activeNotifications`,`emojis`," +
                "`defaultPostPrivacy`,`defaultMediaSensitivity`,`alwaysShowSensitiveMedia`," +
                "`mediaPreviewEnabled`) " +
                "VALUES (nullif(?, 0),?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)",
            values
        )

        db.close()

        val newDb = helper.runMigrationsAndValidate(
            testDB,
            11,
            true,
            AppDatabase.MIGRATION_10_11
        )

        val cursor = newDb.query("SELECT * FROM AccountEntity")
        cursor.moveToFirst()
        assertEquals(id, cursor.getInt(0))
        assertEquals(domain, cursor.getString(1))
        assertEquals(token, cursor.getString(2))
        assertEquals(active, cursor.getInt(3) != 0)
        assertEquals(accountId, cursor.getString(4))
        assertEquals(username, cursor.getString(5))
    }
}
