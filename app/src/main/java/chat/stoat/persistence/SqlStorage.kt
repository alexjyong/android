package chat.stoat.persistence

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import chat.stoat.StoatApplication

object SqlStorage {
    val driver: SqlDriver = AndroidSqliteDriver(
        Database.Schema,
        StoatApplication.instance.applicationContext,
        "revolt.db"
    )
}