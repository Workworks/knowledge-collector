package com.example.knowledgecollector.application.externalsystem;
public record ExternalSystemCommand(String code,String name,String type,String description,String icon,String accessUrl,String healthUrl,String openMode,boolean enabled,String allowedRole,int sortOrder){}
