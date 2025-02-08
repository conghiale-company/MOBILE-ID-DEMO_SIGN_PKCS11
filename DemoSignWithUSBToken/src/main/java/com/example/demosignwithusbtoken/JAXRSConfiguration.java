package com.example.demosignwithusbtoken;

import com.example.demosignwithusbtoken.Exception.GenericExceptionMapper;
import com.example.demosignwithusbtoken.config.JerseyServletContainerConfig;
import com.example.demosignwithusbtoken.controller.SignFileController;
import com.example.demosignwithusbtoken.controller.SignatureController;
import com.example.demosignwithusbtoken.filter.AuthFilter;
import com.example.demosignwithusbtoken.model.BasicSecurityContext;

import javax.ws.rs.ApplicationPath;
import javax.ws.rs.core.Application;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

// tham khao: https://s.net.vn/OI41
@ApplicationPath("/api")
public class JAXRSConfiguration extends Application {
    @Override
    public Map<String, Object> getProperties() {
        Map<String, Object> props = new HashMap<>();
        props.put("jersey.config.server.provider.classnames",
                "org.glassfish.jersey.media.multipart.MultiPartFeature");
        return props;
    }

    @Override
    public Set<Class<?>> getClasses() {
        Set<java.lang.Class<?>> s = new HashSet<>();
        s.add(JerseyServletContainerConfig.class);
        s.add(SignatureController.class);
        s.add(SignFileController.class);
        s.add(AuthFilter.class);
        s.add(BasicSecurityContext.class);
        s.add(GenericExceptionMapper.class);
        return s;
    }
}