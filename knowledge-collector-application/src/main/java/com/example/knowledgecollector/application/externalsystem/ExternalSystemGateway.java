package com.example.knowledgecollector.application.externalsystem;
import java.time.OffsetDateTime;import java.util.List;
public interface ExternalSystemGateway {List<ExternalSystemView> list();ExternalSystemView get(long id);long create(ExternalSystemCommand c);ExternalSystemView update(long id,ExternalSystemCommand c);ExternalSystemView setEnabled(long id,boolean enabled);void updateHealth(long id,String status,OffsetDateTime checked,OffsetDateTime success,String error);void audit(String actor,String action,long id,String summary);}
