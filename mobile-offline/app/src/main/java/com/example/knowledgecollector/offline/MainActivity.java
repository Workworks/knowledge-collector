package com.example.knowledgecollector.offline;

import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.text.Html;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ScrollView;
import android.widget.TextView;

import org.json.JSONArray;

import java.util.List;

public class MainActivity extends Activity {
    private OfflineDatabase database;
    private LinearLayout root;

    @Override
    public void onCreate(Bundle state) {
        super.onCreate(state);
        database = new OfflineDatabase(this);
        database.importAssetsIfEmpty(this);
        showList();
    }

    private void showList() {
        root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(28, 28, 28, 28);
        TextView title = text("KC 离线资料库", 28, true);
        TextView subtitle = text("内置 SQLite · 无网络权限 · 仅展示打包时导入的清洗内容", 14, false);
        ListView list = new ListView(this);
        List<OfflineDatabase.ArticleRow> rows = database.list();
        list.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_list_item_2,
                android.R.id.text1, rows));
        list.setOnItemClickListener((parent, view, position, id) -> showDetail(rows.get(position).getId()));
        root.addView(title);
        root.addView(subtitle);
        root.addView(list, new LinearLayout.LayoutParams(-1, 0, 1));
        setContentView(root);
    }

    private void showDetail(long id) {
        OfflineDatabase.ArticleDetail article = database.detail(id);
        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(32, 28, 32, 48);
        TextView back = text("← 返回资料库", 16, true);
        back.setTextColor(Color.rgb(91, 63, 209));
        back.setOnClickListener(view -> showList());
        content.addView(back);
        content.addView(text(article.getTitle(), 28, true));
        content.addView(text(join(article.getSource(), article.getAuthor(), article.getPublishedAt()), 13, false));
        if (article.getAiSummary() != null && !article.getAiSummary().isBlank()) {
            content.addView(text("AI 摘要\n" + article.getAiSummary(), 17, true));
            try {
                JSONArray points = new JSONArray(article.getAiKeyPoints());
                StringBuilder value = new StringBuilder();
                for (int index = 0; index < points.length(); index++) {
                    value.append("• ").append(points.getString(index)).append("\n");
                }
                content.addView(text(value.toString(), 15, false));
            } catch (Exception ignored) {
            }
        }
        content.addView(text(firstNonBlank(article.getContent(), article.getSummary(), "暂无正文"), 17, false));
        ScrollView scroll = new ScrollView(this);
        scroll.addView(content);
        setContentView(scroll);
    }

    private TextView text(String value, int size, boolean bold) {
        TextView view = new TextView(this);
        view.setText(Html.fromHtml(value == null ? "" : value, Html.FROM_HTML_MODE_LEGACY));
        view.setTextSize(size);
        view.setTextColor(Color.rgb(31, 41, 55));
        view.setPadding(0, 8, 0, 18);
        if (bold) view.setTypeface(view.getTypeface(), android.graphics.Typeface.BOLD);
        return view;
    }

    private String join(String... values) {
        StringBuilder result = new StringBuilder();
        for (String value : values) {
            if (value == null || value.isBlank()) continue;
            if (result.length() > 0) result.append(" · ");
            result.append(value);
        }
        return result.toString();
    }

    private String firstNonBlank(String... values) {
        for (String value : values) if (value != null && !value.isBlank()) return value;
        return "";
    }

    @Override
    public void onBackPressed() {
        showList();
    }
}
