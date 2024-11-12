package meloplayer.app.db


import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class MusicMetadataDBSchemaManager(context: Context) : SQLiteOpenHelper(
    context,
    DATABASE_NAME,
    null,
    DATABASE_VERSION
) {
    companion object {
        const val DATABASE_VERSION = 1
        const val DATABASE_NAME = "MusicMetadata.db"
    }

    override fun onCreate(db: SQLiteDatabase) {
        for (sqlCreateEntry in MusicMetadataDBContract.SQL_CREATE_ENTRIES) {
            db.execSQL(sqlCreateEntry)
        }
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {

    }

    override fun onDowngrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        onUpgrade(db, oldVersion, newVersion)
    }
}
