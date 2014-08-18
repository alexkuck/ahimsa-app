package io.ahimsa.ahimsa_app.core;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import io.ahimsa.ahimsa_app.AhimsaApplication;

/**
 * Created by askuck on 7/12/14.
 */
public class AhimsaLog
{
    private static final String db_name = "AhimsaLog.db";

    public static final String table_queue = "queue";
    public static final String table_log = "log";

    public static final String id = "_id";
    public static final String time = "time";
    public static final String completed = "completed";
    public static final String details = "details";
    public static final String status = "status";

    public static final String queue = "queue";
    public static final String normal = "normal";
    public static final String error = "error";


    AhimsaApplication application;
    SQLiteDatabase db;

    public AhimsaLog(AhimsaApplication application)
    {
        this.application = application;
        initialize();
    }

    private void initialize()
    {
        // NOTE: insert NULL to first value to have id auto_increment
        final String CREATE_QUEUE = String.format("CREATE TABLE IF NOT EXISTS %s " +
                "(_id INTEGER PRIMARY KEY, time INTEGER, details STRING, completed BOOLEAN, CHECK (completed IN (0, 1)) );", table_queue);

        final String CREATE_LOG = String.format("CREATE TABLE IF NOT EXISTS %s " +
                "(_id INTEGER PRIMARY KEY, time INTEGER, details STRING, status VARCHAR(6), CHECK (status IN ('queue', 'normal', 'error')));", table_log);

        db = application.openOrCreateDatabase(db_name, Context.MODE_PRIVATE, null);
        db.execSQL(CREATE_QUEUE);
        db.execSQL(CREATE_LOG);
    }

    public void pushQueue(String details)
    {
        ContentValues params = new ContentValues();
        params.putNull(this.id);
        params.put(this.time, System.currentTimeMillis());
        params.put(this.completed, false);
        params.put(this.details, details);

        db.insertOrThrow(table_queue, null, params);
    }

    public void confirmTipQueue()
    {
//        final String UPDATE = String.format(
//                "UPDATE %s " +
//                "SET completed = TRUE " +
//                "WHERE (SELECT MIN(id) FROM %s);", table_queue, table_queue);

        ContentValues params = new ContentValues();
        params.put(this.completed, true);
        String where = String.format("(SELECT MIN(_id) FROM %s);", table_queue);

        db.update(table_queue, params, where, null);
    }

    public void confirmAllQueue()
    {
        ContentValues params = new ContentValues();
        params.put(this.completed, true);

        db.update(table_queue, params, null, null);
    }

    public void pushLog(String details, String status)
    {
        ContentValues params = new ContentValues();
        params.putNull(this.id);
        params.put(this.time, System.currentTimeMillis());
        params.put(this.details, details);
        params.put(this.status, status);

        db.insertOrThrow(table_log, null, params);
    }

    // Get Cursor ----------------------------------------------------------------------------------
    public Cursor getQueue()
    {
        String sql = String.format("SELECT * FROM %s WHERE completed == 0 ORDER BY _id DESC", table_queue);
        return db.rawQuery(sql, null);
    }

    public Cursor getLog()
    {
        String sql = String.format("SELECT * FROM %s ORDER BY _id DESC", table_log);
        return db.rawQuery(sql, null);
    }


}
