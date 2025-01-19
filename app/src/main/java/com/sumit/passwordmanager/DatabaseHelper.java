package com.sumit.passwordmanager;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

public class DatabaseHelper extends SQLiteOpenHelper {

    private static final String TAG = "passwords.db";
    private final Context context;

    public DatabaseHelper(Context context) {
        super(context, TAG, null, 1);
        this.context = context;
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String TABLE_NAME = "UserData";
        String QUERY = "CREATE TABLE " + TABLE_NAME + "(" +
                Columns.CREATED + " varchar(15) PRIMARY KEY, " +
                Columns.UPDATED + " varchar(15), " +
                Columns.TITLE + " TEXT, " +
                Columns.USERNAME + " TEXT, " +
                Columns.CONTENT + " TEXT, " +
                Columns.COMMENT + " TEXT)";
        db.execSQL(QUERY);
    }

    @Override
    public void onUpgrade(SQLiteDatabase sqLiteDatabase, int i, int i1) {

    }

    public void fetch(Callback callback) {
        try {
            String QUERY = "SELECT * FROM UserData";
            Cursor cursor = getReadableDatabase().rawQuery(QUERY, null);
            List<Model> list = new ArrayList<>();
            if (cursor.moveToFirst()) {
                do {
                    final String comment = cursor.getString(5);
                    list.add(new Model(cursor.getLong(0), cursor.getLong(1),
                            Utils.decodeBase64(cursor.getString(2)),
                            Utils.decrypt(cursor.getString(4)),
                            comment.isEmpty() ? comment : Utils.decrypt(comment),
                            Utils.decodeBase64(cursor.getString(3))));
                } while (cursor.moveToNext());
            }
            cursor.close();
            callback.Success(list);
        } catch (Exception e) {
            Log.e(TAG, "fetch: ", e);
            Toast.makeText(context, "Something went wrong", Toast.LENGTH_SHORT).show();
        }
    }

    protected void addRow(Model model, Listener listener) {
        try {
            String encryptedContent = Utils.encrypt(model.getContent(), true);
            String encNote = Utils.encrypt(model.getComment(), true);
            if (!encryptedContent.isEmpty()) {
                final ContentValues values = new ContentValues();
                values.put(Columns.CREATED, model.getCreatedTime());
                values.put(Columns.UPDATED, model.getUpdateTime());
                values.put(Columns.TITLE, Utils.encodeBase64(model.getTitle()));
                values.put(Columns.CONTENT, encryptedContent);
                values.put(Columns.USERNAME, Utils.encodeBase64(model.getUsername()));
                values.put(Columns.COMMENT, model.getComment().isEmpty() ? "" : encNote);
                if (isExist(model.getCreatedTime())) {
                    values.remove(Columns.CREATED);
                    getWritableDatabase().update("UserData", values, "Created = ?", new String[]{String.valueOf(model.getCreatedTime())});
                } else {
                    getWritableDatabase().insert("UserData", null, values);
                }
                getWritableDatabase().close();
                listener.onSuccess();
            } else
                listener.onFailed();
        } catch (Exception e) {
            e.printStackTrace();
            listener.onFailed();
        }
    }

    protected boolean isExist(long id) {
        Cursor cursor = getReadableDatabase().rawQuery("SELECT Created from UserData where Created = " + id, null);
        boolean bool = cursor.getCount() > 0;
        cursor.close();
        getReadableDatabase().close();
        return bool;
    }

    public interface Listener {
        void onSuccess();

        void onFailed();
    }

    public interface Callback {
        void Success(List<Model> models);
    }

    static class Columns {
        static final String CREATED = "Created";
        static final String UPDATED = "Updated";
        static final String CATEGORY = "Category";
        static final String TITLE = "Title";
        static final String CONTENT = "Content";
        static final String COMMENT = "Comment";
        static final String USERNAME = "Username";
    }
}