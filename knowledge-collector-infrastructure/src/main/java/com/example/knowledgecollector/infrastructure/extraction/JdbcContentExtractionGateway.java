package com.example.knowledgecollector.infrastructure.extraction;

import com.example.knowledgecollector.application.exception.ResourceNotFoundException;
import com.example.knowledgecollector.application.extraction.*;
import com.example.knowledgecollector.capability.extraction.ContentExtractionProvider;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.List;

@Repository
public class JdbcContentExtractionGateway implements ContentExtractionGateway {
    private final JdbcClient jdbc;
    public JdbcContentExtractionGateway(JdbcClient jdbc){this.jdbc=jdbc;}
    @Override public long start(ContentExtractionCommand c,String providerId){
        long id=jdbc.sql("select next value for content_extraction_job_seq").query(Long.class).single();
        jdbc.sql("""
            insert into content_extraction_job(id,article_id,requested_url,method,provider_id,status,retry_of_id,created_at)
            values(:id,:article,:url,:method,:provider,'RUNNING',:retry,:now)
            """).param("id",id).param("article",c.articleId()).param("url",c.url()).param("method",c.method())
                .param("provider",providerId).param("retry",c.retryOfId()).param("now",OffsetDateTime.now()).update();
        return id;
    }
    @Override public void succeed(long id,ContentExtractionProvider.ExtractionResult r,long duration){
        jdbc.sql("""
            update content_extraction_job set final_url=:url,status='SUCCESS',page_title=:title,author=:author,
            published_at=:published,content_length=:length,content_html=:html,content_text=:text,raw_html=:raw,
            screenshot=:shot,duration_millis=:duration,error_message=null,finished_at=:now where id=:id
            """).param("url",r.finalUrl()).param("title",r.title()).param("author",r.author()).param("published",r.publishedAt())
                .param("length",r.contentText()==null?0:r.contentText().length()).param("html",r.contentHtml()).param("text",r.contentText())
                .param("raw",r.rawHtml()).param("shot",r.screenshot()).param("duration",duration).param("now",OffsetDateTime.now()).param("id",id).update();
    }
    @Override public void fail(long id,String error,long duration){jdbc.sql("update content_extraction_job set status='FAILED',error_message=:error,duration_millis=:duration,finished_at=:now where id=:id")
            .param("error",trim(error,8000)).param("duration",duration).param("now",OffsetDateTime.now()).param("id",id).update();}
    @Override public void updateArticle(long articleId,ContentExtractionProvider.ExtractionResult r){
        jdbc.sql("""
            update article set title=coalesce(:title,title),author=coalesce(:author,author),publish_time=coalesce(:published,publish_time),
            content_html=:html,content_text=:text,word_count=:words,reading_minutes=:minutes,last_collected_at=:now where id=:id
            """).param("title",blank(r.title())).param("author",blank(r.author())).param("published",r.publishedAt())
                .param("html",r.contentHtml()).param("text",r.contentText()).param("words",words(r.contentText()))
                .param("minutes",Math.max(1,(int)Math.ceil(words(r.contentText())/300.0))).param("now",OffsetDateTime.now()).param("id",articleId).update();
    }
    @Override public ContentExtractionView get(long id){return jdbc.sql("select * from content_extraction_job where id=:id").param("id",id).query(this::map).optional()
            .orElseThrow(()->new ResourceNotFoundException("提取任务不存在："+id));}
    @Override public List<ContentExtractionView> list(Long articleId,int limit){String where=articleId==null?"":" where article_id=:article";var query=jdbc.sql("select * from content_extraction_job"+where+" order by id desc limit :limit").param("limit",limit);if(articleId!=null)query.param("article",articleId);return query.query(this::map).list();}
    private ContentExtractionView map(ResultSet rs,int row)throws SQLException{long retry=rs.getLong("retry_of_id");boolean retryNull=rs.wasNull();return new ContentExtractionView(rs.getLong("id"),(Long)rs.getObject("article_id"),rs.getString("requested_url"),rs.getString("final_url"),rs.getString("method"),rs.getString("provider_id"),rs.getString("status"),rs.getString("page_title"),rs.getString("author"),rs.getObject("published_at",OffsetDateTime.class),rs.getInt("content_length"),rs.getString("content_html"),rs.getString("content_text"),rs.getString("raw_html"),rs.getBytes("screenshot"),rs.getLong("duration_millis"),rs.getString("error_message"),retryNull?null:retry,rs.getObject("created_at",OffsetDateTime.class),rs.getObject("finished_at",OffsetDateTime.class));}
    private int words(String text){return text==null?0:text.trim().split("\\s+|(?<=[\\u4e00-\\u9fa5])(?=[\\u4e00-\\u9fa5])").length;}
    private String blank(String value){return value==null||value.isBlank()?null:value;}
    private String trim(String value,int max){return value==null?null:value.substring(0,Math.min(max,value.length()));}
}
