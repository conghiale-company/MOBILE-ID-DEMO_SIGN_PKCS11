package com.example.demosignwithusbtoken.model;

import javax.ws.rs.core.SecurityContext;
import java.security.Principal;

import static java.lang.System.out;

// EX: https://gpcoder.com/5724-rest-web-service-basic-authentication-trong-jersey-2-x/

public class BasicSecurityContext implements SecurityContext {
    private final User user;
    private final boolean secure;

    public BasicSecurityContext(User user, boolean secure) {
        this.user = user;
        this.secure = secure;
    }
    @Override
    public Principal getUserPrincipal() {
        return user::getUsername;
    }

    @Override
    public boolean isUserInRole(String s) {
        out.println("LINE 24 - BasicSecurityContext.java: " + user.getRoles().contains(s));
        return user.getRoles().contains(s);
    }

    @Override
    public boolean isSecure() {
        return secure;
    }

    @Override
    public String getAuthenticationScheme() {
        return SecurityContext.BASIC_AUTH;
    }
//private Principal principal;
//
//    public BasicSecurityContext(Principal principal) {
//        this.principal = principal;
//    }
//
//    @Override
//    public Principal getUserPrincipal() {
//        return principal;
//    }
//
//    @Override
//    public boolean isUserInRole(String role) {
//        return role.equals("USER"); // Định nghĩa vai trò USER
//    }
//
//    @Override
//    public boolean isSecure() {
//        return false;
//    }
//
//    @Override
//    public String getAuthenticationScheme() {
//        return "Basic";
//    }
}
