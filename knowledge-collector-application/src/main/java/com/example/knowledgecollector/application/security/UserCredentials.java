package com.example.knowledgecollector.application.security;
public record UserCredentials(long id,String username,String displayName,String passwordHash,String role,boolean enabled){}
