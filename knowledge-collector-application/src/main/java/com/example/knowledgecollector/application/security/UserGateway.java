package com.example.knowledgecollector.application.security;
import java.util.List;import java.util.Optional;
public interface UserGateway {
 List<UserView> list(String keyword,int offset,int limit);long count(String keyword);Optional<UserCredentials> findCredentials(String username);UserView get(long id);long create(String username,String displayName,String passwordHash,String role,boolean enabled);UserView update(long id,String displayName,String role,boolean enabled);void updatePassword(long id,String hash);void recordLogin(long id);void audit(String actor,String action,String targetType,String targetId,String summary,String detailJson);
}
