package com.example.demosignwithusbtoken.filter;

import com.example.demosignwithusbtoken.Exception.GenericExceptionMapper;
import com.example.demosignwithusbtoken.model.BasicSecurityContext;
import com.example.demosignwithusbtoken.model.User;
import com.example.demosignwithusbtoken.service.UserService;
import org.apache.http.HttpHeaders;

import javax.annotation.Priority;
import javax.ws.rs.Priorities;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import javax.ws.rs.ext.Provider;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.security.Principal;
import java.util.Base64;
import java.util.StringTokenizer;

import static java.lang.System.out;

@Provider
@Priority(Priorities.AUTHENTICATION)
public class AuthFilter implements ContainerRequestFilter {
    private static final String REALM = "SignUSBToken";
    private static final String AUTHENTICATION_SCHEME = "Basic";

    @Override
    public void filter(ContainerRequestContext containerRequestContext) throws WebApplicationException, UnsupportedEncodingException {

//        (1) Parsing the Basic Auth Authorization header
//        The structure of authentication header:
//        Authorization: Basic encodedByBase64(username:password)
        String authCredentials = containerRequestContext.getHeaderString(HttpHeaders.AUTHORIZATION); // "Authorization"

        if (authCredentials == null || !authCredentials.startsWith(AUTHENTICATION_SCHEME + " ")) {
            GenericExceptionMapper genericExceptionMapper = new GenericExceptionMapper();
            containerRequestContext.abortWith(genericExceptionMapper.toResponse(new WebApplicationException(Response.status(Response.Status.UNAUTHORIZED)
                    .header(HttpHeaders.WWW_AUTHENTICATE, AUTHENTICATION_SCHEME + " realm=\"" + REALM + "\"")
                    .build())));

            return;
        }

//        if (authCredentials == null) {
//            out.println("LINE 33 - AuthFilter.java");
//            return;
//        } else
//            out.println("LINE 36 - AuthFilter.java");

//        (2) Extract user name and password from Authentication header
        final String encodeUsernamePassword = authCredentials.replaceFirst("Basic" + " ", "");
        byte[] decodedBytes = Base64.getDecoder().decode(encodeUsernamePassword);
        String usernameAndPassword = new String(decodedBytes, StandardCharsets.UTF_8);

        final StringTokenizer tokenizer =  new StringTokenizer(usernameAndPassword, ":");
        final String username = tokenizer.nextToken();
        final String password = tokenizer.nextToken();

//        (3) Getting the User with the username
        User user = UserService.getInstance().getUser(username);

        out.println("LINE 66 - USERNAME: " + username + " PASSWORD: " + password);
        out.println("LINE 67 - #USERNAME: " + (user != null ? user.getUsername() : "NULL") + " #PASSWORD: " + (user != null ? user.getPassword() : "NULL") + " #ROLE: " + (user != null ? user.getRoles().get(0) : "NULL"));

//        (4) Doing authentication
        if (user == null || !user.getPassword().equals(password) || !user.getUsername().equals(username)) {
            GenericExceptionMapper genericExceptionMapper = new GenericExceptionMapper();
            Response response = Response.status(Response.Status.UNAUTHORIZED) // 401 Unauthorized
                    .header(HttpHeaders.WWW_AUTHENTICATE, AUTHENTICATION_SCHEME + " realm=\"" + REALM + "\"")
                    .entity("You cannot access this resource") // the response entity
                    .build();
            containerRequestContext.abortWith(genericExceptionMapper.toResponse(new WebApplicationException(response)));
        }

//        (5) Setting a new SecurityContext
        SecurityContext oldContext = containerRequestContext.getSecurityContext();
        containerRequestContext.setSecurityContext(new BasicSecurityContext(user, oldContext.isSecure()));
//        final Principal principal = new Principal() {
//            @Override
//            public String getName() {
//                return username;
//            }
//        };
//        containerRequestContext.setSecurityContext(new BasicSecurityContext(principal));
    }
}
