package com.example.knowledgecollector.offline;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

final class OfflineDatabase extends SQLiteOpenHelper {
    OfflineDatabase(Context context) {
        super(context, "knowledge-collector-offline.db", null, 1);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE article("
                + "id INTEGER PRIMARY KEY,"
                + "title TEXT NOT NULL,"
                + "source_name TEXT,"
                + "author TEXT,"
                + "summary TEXT,"
                + "content_text TEXT,"
                + "publish_time TEXT,"
                + "ai_summary TEXT,"
                + "ai_key_points TEXT"
                + ")");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
    }

    void importAssetsIfEmpty(Context context) {
        try (Cursor cursor = getReadableDatabase().rawQuery("SELECT COUNT(*) FROM article", null)) {
            cursor.moveToFirst();
            if (cursor.getLong(0) > 0) return;
        }
        try (InputStream input = context.getAssets().open("articles.json");
             ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[8192];
            int length;
            while ((length = input.read(buffer)) >= 0) {
                output.write(buffer, 0, length);
            }
            JSONArray articles = new JSONArray(new String(output.toByteArray(), StandardCharsets.UTF_8));
            SQLiteDatabase db = getWritableDatabase();
            db.beginTransaction();
            try {
                for (int index = 0; index < articles.length(); index++) {
                    JSONObject article = articles.getJSONObject(index);
                    ContentValues values = new ContentValues();
                    values.put("id", article.optLong("id", index + 1));
                    values.put("title", article.optString("title", "无标题"));
                    values.put("source_name", article.optString("sourceName"));
                    values.put("author", article.optString("author"));
                    values.put("summary", article.optString("summary"));
                    values.put("content_text", article.optString("contentText"));
                    values.put("publish_time", article.optString("publishTime"));
                    JSONObject ai = article.optJSONObject("ai");
                    values.put("ai_summary", ai == null ? "" : ai.optString("oneSentenceSummary"));
                    JSONArray keyPoints = ai == null ? null : ai.optJSONArray("keyPoints");
                    values.put("ai_key_points", keyPoints == null ? "[]" : keyPoints.toString());
                    db.insert("article", null, values);
                }
                db.setTransactionSuccessful();
            } finally {
                db.endTransaction();
            }
        } catch (Exception exception) {
            throw new IllegalStateException("离线资料导入失败", exception);
        }
    }

    List<ArticleRow> list() {
        List<ArticleRow> result = new ArrayList<>();
        try (Cursor cursor = getReadableDatabase().rawQuery(
                "SELECT id,title,source_name,summary FROM article ORDER BY publish_time DESC,id DESC", null)) {
            while (cursor.moveToNext()) {
                result.add(new ArticleRow(cursor.getLong(0), cursor.getString(1),
                        cursor.getString(2), cursor.getString(3)));
            }
        }
        return result;
    }

    ArticleDetail detail(long id) {
        try (Cursor cursor = getReadableDatabase().rawQuery(
                "SELECT title,source_name,author,summary,content_text,publish_time,ai_summary,ai_key_points "
                        + "FROM article WHERE id=?",
                new String[]{Long.toString(id)})) {
            if (!cursor.moveToFirst()) throw new IllegalArgumentException("文章不存在");
            return new ArticleDetail(cursor.getString(0), cursor.getString(1), cursor.getString(2),
                    cursor.getString(3), cursor.getString(4), cursor.getString(5),
                    cursor.getString(6), cursor.getString(7));
        }
    }

    static final class ArticleRow {
        private final long id;
        private final String title;
        private final String source;
        private final String summary;

        ArticleRow(long id, String title, String source, String summary) {
            this.id = id;
            this.title = title;
            this.source = source;
            this.summary = summary;
        }

        long getId() {
            return id;
        }

        @Override public String toString() {
            return title + "\n" + (source == null ? "" : source);
        }
    }

    static final class ArticleDetail {
        private final String title;
        private final String source;
        private final String author;
        private final String summary;
        private final String content;
        private final String publishedAt;
        private final String aiSummary;
        private final String aiKeyPoints;

        ArticleDetail(String title, String source, String author, String summary,
                      String content, String publishedAt, String aiSummary, String aiKeyPoints) {
            this.title = title;
            this.source = source;
            this.author = author;
            this.summary = summary;
            this.content = content;
            this.publishedAt = publishedAt;
            this.aiSummary = aiSummary;
            this.aiKeyPoints = aiKeyPoints;
        }

        String getTitle() {
            return title;
        }

        String getSource() {
            return source;
        }

        String getAuthor() {
            return author;
        }

        String getSummary() {
            return summary;
        }

        String getContent() {
            return content;
        }

        String getPublishedAt() {
            return publishedAt;
        }

        String getAiSummary() {
            return aiSummary;
        }

        String getAiKeyPoints() {
            return aiKeyPoints;
        }
    }
}
