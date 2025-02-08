package com.example.demosignwithusbtoken.service;

import com.example.demosignwithusbtoken.model.Key;
import com.example.demosignwithusbtoken.utils.MySignature;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.security.GeneralSecurityException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.util.List;

public class KeyService {
    private volatile static KeyService keyService;

    private KeyService() {}

    public static KeyService getInstance() {
        KeyService keyService = KeyService.keyService;
        if (keyService == null) {
            synchronized (KeyService.class) {
                keyService = KeyService.keyService;
                if (keyService == null) {
                    KeyService.keyService = keyService = new KeyService();
                }
            }
        }

        return keyService;
    }

    public List<Key> getKeys() throws URISyntaxException, IOException, KeyStoreException, NoSuchAlgorithmException, CertificateException {
        return MySignature.getInstance().getKeys();
    }

    public boolean createKey(String alias, String information, String extension) throws Exception {
        return MySignature.getInstance().createSignature(alias, information, extension);
    }

    public boolean removeKey(String alias) throws URISyntaxException, IOException, CertificateException, KeyStoreException, NoSuchAlgorithmException {
        return MySignature.getInstance().removeSignature(alias);
    }

    public ByteArrayInputStream signFile(String alias, String fileName, InputStream fileInputStream) throws URISyntaxException, IOException, GeneralSecurityException {
        return MySignature.getInstance().signPDF(alias, fileName, fileInputStream);
    }
}
