package com.example.knowledgecollector.infrastructure.evidence;

import com.example.knowledgecollector.application.evidence.*;
import com.example.knowledgecollector.application.exception.ResourceNotFoundException;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;
import java.sql.*;
import java.time.OffsetDateTime;
import java.util.List;

@Repository
public class JdbcEvidenceFileGateway implements EvidenceFileGateway {
    private final JdbcClient jdbc; public JdbcEvidenceFileGateway(JdbcClient jdbc){this.jdbc=jdbc;}
    @Override public EvidenceFileView add(String ownerType,long ownerId,String kind,String name,String key,String type,long length,String checksum,String provider){
        int version=jdbc.sql("select coalesce(max(version_no),0)+1 from evidence_file where owner_type=:type and owner_id=:id and file_kind=:kind")
                .param("type",ownerType).param("id",ownerId).param("kind",kind).query(Integer.class).single();
        long id=jdbc.sql("select next value for evidence_file_seq").query(Long.class).single();
        jdbc.sql("insert into evidence_file(id,owner_type,owner_id,file_kind,file_name,object_key,content_type,content_length,checksum,version_no,provider_id,created_at) values(:id,:ownerType,:ownerId,:kind,:name,:key,:contentType,:length,:checksum,:version,:provider,:now)")
                .param("id",id).param("ownerType",ownerType).param("ownerId",ownerId).param("kind",kind).param("name",name).param("key",key).param("contentType",type).param("length",length).param("checksum",checksum).param("version",version).param("provider",provider).param("now",OffsetDateTime.now()).update();
        return get(id);
    }
    @Override public List<EvidenceFileView> list(String ownerType,Long ownerId){StringBuilder sql=new StringBuilder("select * from evidence_file where 1=1");if(ownerType!=null&&!ownerType.isBlank())sql.append(" and owner_type=:type");if(ownerId!=null)sql.append(" and owner_id=:id");sql.append(" order by created_at desc,id desc");var q=jdbc.sql(sql.toString());if(ownerType!=null&&!ownerType.isBlank())q.param("type",ownerType);if(ownerId!=null)q.param("id",ownerId);return q.query(this::map).list();}
    @Override public EvidenceFileView get(long id){return jdbc.sql("select * from evidence_file where id=:id").param("id",id).query(this::map).optional().orElseThrow(()->new ResourceNotFoundException("证据文件不存在："+id));}
    private EvidenceFileView map(ResultSet rs,int row)throws SQLException{return new EvidenceFileView(rs.getLong("id"),rs.getString("owner_type"),rs.getLong("owner_id"),rs.getString("file_kind"),rs.getString("file_name"),rs.getString("object_key"),rs.getString("content_type"),rs.getLong("content_length"),rs.getString("checksum"),rs.getInt("version_no"),rs.getString("provider_id"),rs.getObject("created_at",OffsetDateTime.class));}
}
