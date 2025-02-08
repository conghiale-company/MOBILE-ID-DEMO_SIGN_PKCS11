package org.example;

import com.itextpdf.kernel.pdf.PdfDictionary;
import com.itextpdf.signatures.IExternalSignatureContainer;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.security.GeneralSecurityException;
import java.security.PrivateKey;
import java.security.Signature;

public class MyExternalSignatureContainer implements IExternalSignatureContainer {
    private PrivateKey privateKey;

    public MyExternalSignatureContainer(PrivateKey privateKey) {
        this.privateKey = privateKey;
    }

    @Override
    public byte[] sign(InputStream data) throws GeneralSecurityException {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            byte[] buffer = new byte[1024];
            int bytesRead;
            while ((bytesRead = data.read(buffer)) != -1) {
                baos.write(buffer, 0, bytesRead);
            }
            byte[] bytes = baos.toByteArray();

            return performSigning(bytes, privateKey);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public void modifySigningDictionary(PdfDictionary pdfDictionary) {

    }

    static byte[] performSigning(byte[] data, PrivateKey privateKey) throws Exception {
         Signature signature = Signature.getInstance("SHA256withRSA");
         signature.initSign(privateKey);
         signature.update(data);
         return signature.sign();
    }
}
