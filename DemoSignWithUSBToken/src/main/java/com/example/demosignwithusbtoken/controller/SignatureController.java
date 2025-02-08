package com.example.demosignwithusbtoken.controller;

import com.example.demosignwithusbtoken.config.JerseyServletContainerConfig;
import com.example.demosignwithusbtoken.model.Key;
import com.example.demosignwithusbtoken.model.Role;
import com.example.demosignwithusbtoken.service.KeyService;
import org.glassfish.jersey.media.multipart.FormDataContentDisposition;
import org.glassfish.jersey.media.multipart.FormDataParam;

import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.security.GeneralSecurityException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.util.List;
import javax.annotation.security.PermitAll;
import javax.annotation.security.RolesAllowed;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

/**
 *
 * @author conghiale
 */

@Path("/signature")
@PermitAll
public class SignatureController {
//    private final JerseyServletContainerConfig jerseyServletContainerConfig = new JerseyServletContainerConfig();

    @GET
    @RolesAllowed("ADMIN")
    @Produces(MediaType.APPLICATION_JSON)
    public List<Key> getKeys() throws URISyntaxException, IOException, KeyStoreException, NoSuchAlgorithmException, CertificateException {
        return KeyService.getInstance().getKeys();
    }

    @DELETE
    @Path("/{alias}")
    @RolesAllowed(Role.ROLE_ADMIN)
    @Produces(MediaType.APPLICATION_JSON)
    public boolean removeKey(@PathParam("alias") String alias) throws CertificateException, URISyntaxException, IOException, KeyStoreException, NoSuchAlgorithmException {
        return KeyService.getInstance().removeKey(alias);
    }

    @POST
    @RolesAllowed(Role.ROLE_CUSTOMER)
    @Produces(MediaType.APPLICATION_JSON)
    public boolean createKey(Key key) throws Exception {
        if (key == null) {
            return false;
        }
        return KeyService.getInstance().createKey(key.getAlias(), key.getInformation(), key.getExtension());
    }
}
