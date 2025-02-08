package com.example.demosignwithusbtoken.controller;

import com.example.demosignwithusbtoken.model.Role;
import com.example.demosignwithusbtoken.service.KeyService;
import org.glassfish.jersey.media.multipart.FormDataContentDisposition;
import org.glassfish.jersey.media.multipart.FormDataParam;
import org.jboss.resteasy.plugins.providers.multipart.MultipartFormDataInput;

import javax.annotation.security.RolesAllowed;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.io.InputStream;

import static java.lang.System.out;

/**
 *
 * @author conghiale
 */

@Path("/sign")
public class SignFileController {

    @POST
    @RolesAllowed({ Role.ROLE_ADMIN, Role.ROLE_CUSTOMER })
    @Consumes(MediaType.MULTIPART_FORM_DATA)
//    @Produces(MediaType.MULTIPART_FORM_DATA)
    public Response signFile(MultipartFormDataInput input) throws IOException {
//            @FormParam("alias") String alias,
//            @FormDataParam("file") InputStream fileInputStream){
//            @FormDataParam("file") FormDataContentDisposition fileDetails) {


        InputStream fileInputStream = input.getFormDataPart("file", InputStream.class,null);
        String alias = input.getFormDataPart("alias", String.class,null);
        String fileName = input.getFormDataPart("fileName", String.class,null);

        if (fileInputStream == null) {
            return Response.status(Response.Status.BAD_REQUEST).entity("Invalid data").build();
        }

        out.println("LINE 41: " + fileName);

        try {
            // Xử lý và ký vào file PDF
            return Response.ok(KeyService.getInstance().signFile(alias, fileName, fileInputStream))
                    .type("application/pdf")
                    .header("Content-Disposition", "attachment; fileName=\"" + fileName + "_signed.pdf\"")
                    .build();
        } catch (Exception e) {
            e.printStackTrace();
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error processing PDF").build();
        }
//        return Response.status(Response.Status.BAD_REQUEST).entity("TEST").build();

    }
}
