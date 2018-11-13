package soko.ekibun.bangumiplayer.model

import android.content.ContentProvider
import android.content.ContentValues
import android.content.Context
import android.content.UriMatcher
import android.database.Cursor
import android.net.Uri
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class ContentProviderModel: ContentProvider() {
    val uriMatcher = {
        val matcher = UriMatcher(UriMatcher.NO_MATCH)
        matcher.addURI(AUTOHORITY,VIDEO_CACHE, VIDEO_CACHE_CODE)
        matcher
    }()
    lateinit var dbHelper: DatabaseHelper
    lateinit var db: SQLiteDatabase

    fun getTableName(uri: Uri): String{
        return when(uriMatcher.match(uri)){
            VIDEO_CACHE_CODE-> CACHE_TABLE_NAME
            else -> ""
        }
    }

    override fun insert(uri: Uri, values: ContentValues?): Uri {
        val table = getTableName(uri)
        db.insert(table, null, values)
        context?.contentResolver?.notifyChange(uri, null)
        return uri
    }

    override fun query(uri: Uri, projection: Array<out String>?, selection: String?, selectionArgs: Array<out String>?, sortOrder: String?): Cursor {
        val table = getTableName(uri)
        return db.query(table, projection, selection, selectionArgs,null,null,sortOrder,null)
    }

    override fun onCreate(): Boolean {
        dbHelper = DatabaseHelper(context?:return false)
        db = dbHelper.writableDatabase
        return true
    }

    override fun update(uri: Uri, values: ContentValues?, selection: String?, selectionArgs: Array<out String>?): Int {
        val table = getTableName(uri)
        return db.update(table, values, selection, selectionArgs);
    }

    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?): Int {
        val table = getTableName(uri)
        return db.delete(table, selection, selectionArgs)
    }

    override fun getType(uri: Uri?): String {
        return "text/plain"
    }

    companion object {
        const val CACHE_TABLE_NAME = "VideoCache"
        const val AUTOHORITY = "soko.ekibun.bangumiplayer"
        const val VIDEO_CACHE = "cache"
        const val VIDEO_CACHE_CODE = 1
    }
    class DatabaseHelper(context: Context) : SQLiteOpenHelper(context, "playerInfo", null, 1) {
        override fun onCreate(db: SQLiteDatabase) {
            db.execSQL("CREATE TABLE " + CACHE_TABLE_NAME +
                    " (_id INTEGER PRIMARY KEY, " +
                    " data TEXT);")
        }

        override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
            db.execSQL("DROP TABLE IF EXISTS $CACHE_TABLE_NAME")
            onCreate(db)
        }
    }
}