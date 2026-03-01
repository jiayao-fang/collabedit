package com.example.collabedit.security;

public class JwtUserPrincipal {
    private final Long id;
    public JwtUserPrincipal(Long id) {
        this.id = id;
    }

    public Long getId() {
        return id;
    }

    @Override
    public String toString() {
        return id != null ? id.toString() : null;
    }
}
