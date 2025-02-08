package com.example.demosignwithusbtoken.Exception;

import org.apache.http.HttpHeaders;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

@Provider
public class GenericExceptionMapper implements ExceptionMapper<Throwable> {

//    private static final String REALM = "SignUSBToken";
//    private static final String AUTHENTICATION_SCHEME = "Basic";

    @Override
    public Response toResponse(Throwable throwable) {
        return Response.status(getStatusType(throwable))
                .entity(throwable.getMessage())
//                .header(HttpHeaders.WWW_AUTHENTICATE, AUTHENTICATION_SCHEME + " realm=\"" + REALM + "\"")
                .type(MediaType.TEXT_PLAIN) // "text/plain"
                .build();
    }

    private Response.StatusType getStatusType(Throwable ex) {
        if (ex instanceof WebApplicationException) {
            return((WebApplicationException)ex).getResponse().getStatusInfo();
        } else {
            // 500, "Internal Server Error"
            return Response.Status.INTERNAL_SERVER_ERROR;
        }
    }
}
