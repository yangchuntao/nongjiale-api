package com.nongjiale.security;

public record AuthPrincipal(Long id, String subject, String type, String role) {
    public boolean isTourist() { return "TOURIST".equals(type); }
    public boolean isAdmin() { return "ADMIN".equals(type); }
    public boolean isOperator() { return isAdmin() && "OPERATOR".equals(role); }
    public boolean isAgent() { return isAdmin() && "AGENT".equals(role); }
}
