package com.example.demosignwithusbtoken.utils;

import java.io.*;

import static java.lang.System.out;

import java.math.BigInteger;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.*;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.*;

import com.example.demosignwithusbtoken.model.Key;
import com.itextpdf.io.image.ImageData;
import com.itextpdf.io.image.ImageDataFactory;
import com.itextpdf.kernel.geom.Rectangle;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfReader;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.signatures.*;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x509.*;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cert.jcajce.JcaX509ExtensionUtils;
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;

import javax.ws.rs.core.Context;
import javax.servlet.ServletContext;

public class MySignature {

    private String configFilePath = "C:/WINDOWS/System32/eps2003csp11.dll";  // Đường dẫn tới file cấu hình PKCS#11
    private String pin = "12345678";  // Mật khẩu của USB Token
    private String pdfInputPath = "Final_Exam_OOP.pdf";  // File PDF chưa ký
//    private String pdfInputPath = "LeCongNghia_CV.pdf";  // File PDF chưa ký
    private String pdfOutputPath = "src/main/resources/signed_output.pdf";  // File PDF sau khi đã ký
    private String imageSignaturePdfPath = "imageSign.jpg";  // File image Sign

    private Certificate[] certificateChain = null;
    private PrivateKey privateKey = null;
    private PublicKey publicKey = null;
    private boolean useFirstCertificateFromToken = false;
    private String issuerCommonName = "Lê Công Nghĩa";
    private String certificateSerialNumber = "1713253906839";
    private String location = "LOCATION";
    private Provider pkcs11Provider = null;

    @Context
    private ServletContext servletContext;

    private volatile static MySignature mySignature;


    private MySignature() throws URISyntaxException, IOException {
        loadPKCS11Provider();
    }

    public static MySignature getInstance() throws URISyntaxException, IOException {
        if (mySignature == null) {
            synchronized (MySignature.class) {
                if (mySignature == null) {
                    mySignature = new MySignature();
                }
            }
        }

        return mySignature;
    }

    public MySignature(String configFilePath, String pin, String pdfInputPath, String pdfOutputPath, String imageSignaturePdfPath) throws URISyntaxException, IOException {
        this.configFilePath = configFilePath;
        this.pin = pin;
        this.pdfInputPath = pdfInputPath;
        this.pdfOutputPath = pdfOutputPath;
        this.imageSignaturePdfPath = imageSignaturePdfPath;

        loadPKCS11Provider();
    }

    //    Sign EX: https://kb.itextpdf.com/itext/digital-signatures-chapter-4
    public ByteArrayInputStream signPDF(String alias, String fileName, InputStream fileInputStream) throws GeneralSecurityException, IOException {
        // Tải PKCS#11 KeyStore
        KeyStore ks = KeyStore.getInstance("PKCS11");
        ks.load(null, pin.toCharArray()); // Pass the PIN required to access the token
        Enumeration<String> aliasesEnum = ks.aliases();

        if (aliasesEnum.hasMoreElements()) {
            while(aliasesEnum.hasMoreElements()) {
//                lấy cái đ tiên
//                alias = aliasesEnum.nextElement();

                X509Certificate cert = (X509Certificate) ks.getCertificate(alias);
                if (cert != null) {
                    certificateChain = ks.getCertificateChain(alias);
                    privateKey = (PrivateKey) ks.getKey(alias, null);
//                    out.println("LINE - 111: " + certificateChain[0].toString());

//                    Get location
                    String subject = cert.getSubjectX500Principal().toString();
                    // Regex pattern to find the 'ST' attribute in the subject DN
                    String regex = "ST=([^,]+)";

                    // Use Pattern and Matcher for regex operations
                    java.util.regex.Pattern pattern = java.util.regex.Pattern.compile(regex);
                    java.util.regex.Matcher matcher = pattern.matcher(subject);

                    // Check if the pattern finds a match
                    if (matcher.find()) {
                        location = matcher.group(1);
                    } else {
                        System.out.println("ST parameter not found.");
                    }

                    return signature(fileName, fileInputStream, "REASON", location);
                }
            }
        }
        else {
            throw new KeyStoreException("Keystore is empty");
        }

        return null;
    }

    //    get keys (All)
    public List<Key> getKeys() throws KeyStoreException, IOException, NoSuchAlgorithmException, CertificateException {
        List<Key> keys = new ArrayList<>();

        if (Security.getProvider(pkcs11Provider.getName()) == null) {
            throw new RuntimeException("Failed to load PKCS#11 provider");
        }

        KeyStore ks = KeyStore.getInstance("PKCS11");
        ks.load(null, pin.toCharArray()); // Pass the PIN required to access the token
        Enumeration<String> aliasesEnum = ks.aliases();

        if (aliasesEnum.hasMoreElements()) {
            while(aliasesEnum.hasMoreElements()) {
                // choose the required certificate using the alias
                String alias  = aliasesEnum.nextElement();

                X509Certificate cert = (X509Certificate) ks.getCertificate(alias);

                String serialNumber = cert.getSerialNumber().toString();
                String information = cert.getSubjectX500Principal().toString();

                keys.add(new Key(alias, serialNumber, information));
            }
        }
        else {
            throw new KeyStoreException("Keystore is empty");
        }

        return keys;
    }

    //    Create Signature
    public boolean createSignature(String alias, String information, String extension) throws Exception {
        generationKeyPair_SelfSignedCertificate(alias, information, extension);
        return true;
    }

    //    Delete Signature
    public boolean removeSignature(String alias) throws KeyStoreException, CertificateException, IOException, NoSuchAlgorithmException {
        KeyStore keyStore = KeyStore.getInstance("PKCS11", pkcs11Provider);
        keyStore.load(null, pin.toCharArray());

        X509Certificate certificate = (X509Certificate) keyStore.getCertificate(alias);

        if (certificate == null) {
            out.println("No key found to delete.");
            return false;
        } else {
            // Check if the alias exists
            if (keyStore.containsAlias(alias)) {
                // Remove the entry
                keyStore.deleteEntry(alias);
                System.out.println("Entry removed successfully.");
                return true;
            } else {
                System.out.println("No such entry exists.");
                return false;
            }
        }
    }

    private void loadPKCS11Provider() {

        InputStream inputStream = this.getClass().getClassLoader().getResourceAsStream("epass2003.cfg");
        if (inputStream == null) {
            throw new IllegalArgumentException("file not found! example.txt");
        } else {
            pkcs11Provider = new sun.security.pkcs11.SunPKCS11(inputStream);
        }

        Security.addProvider(pkcs11Provider);
        Security.addProvider(new BouncyCastleProvider());

        if (Security.getProvider(pkcs11Provider.getName()) == null) {
            throw new RuntimeException("Failed to load PKCS#11 provider");
        }
    }

    private void generationKeyPair_SelfSignedCertificate(String alias, String information, String extension) throws Exception {
        KeyStore keyStore = KeyStore.getInstance("PKCS11", pkcs11Provider);
        keyStore.load(null, pin.toCharArray());  // Sử dụng PIN để đăng nhập vào token

        SecureRandom sr = new SecureRandom();
        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA", pkcs11Provider);
        keyPairGenerator.initialize(2048, sr);

        KeyPair keyPair = keyPairGenerator.generateKeyPair();
        privateKey = keyPair.getPrivate();
        publicKey = keyPair.getPublic();

//        Xác Định Thời Gian Hiệu Lực của Chứng Chỉ:
//        Date notBefore = new Date();
//        Date notAfter = new Date(notBefore.getTime() + 365L * 24 * 3600 * 1000); // Valid for 1 year
//
//        X509V3CertificateGenerator certGen = new X509V3CertificateGenerator();
//        certGen.setSerialNumber(BigInteger.valueOf(System.currentTimeMillis()));
//        certGen.setIssuerDN(new X500Principal(information));
//        certGen.setNotBefore(notBefore);
//        certGen.setNotAfter(notAfter);
//        certGen.setSubjectDN(new X500Principal(information));
//        certGen.setPublicKey(publicKey);
//        certGen.setSignatureAlgorithm("SHA256WithRSA");
//        certGen.addExtension(X509Extensions.BasicConstraints, true,
//                new BasicConstraints(false));
//
//        certGen.addExtension(X509Extensions.KeyUsage, true, new KeyUsage(
//                KeyUsage.digitalSignature | KeyUsage.keyEncipherment));
//
//        certGen.addExtension(X509Extensions.ExtendedKeyUsage, true,
//                new ExtendedKeyUsage(KeyPurposeId.id_kp_serverAuth));
////
//        certGen.addExtension(X509Extensions.SubjectAlternativeName, false,
//                new GeneralNames(new GeneralName(GeneralName.rfc822Name, extension)));
////
//        X509Certificate certificate = certGen.generate(privateKey, pkcs11Provider.getName());
        X509Certificate certificate = generateCertificate(keyPair, information, extension);
        certificateChain = new Certificate[]{certificate};

        keyStore.setKeyEntry(alias, privateKey, pin.toCharArray(), certificateChain);
        keyStore.store(null, pin.toCharArray()); // Persist changes
    }

    private ByteArrayInputStream signature(String fileName, InputStream fileInputStream, String reason, String location) throws IOException, GeneralSecurityException {
        PdfReader reader;
//        Read file in resource
//        InputStream inputStream = this.getClass().getClassLoader().getResourceAsStream(pdfInputPath);
//        if (inputStream == null) {
//            throw new IllegalArgumentException("file not found! LeCongNghia_CV.pdf");
//        } else {
//            reader = new PdfReader(inputStream);
//        }

//        writeToFile(fileInputStream, "root.pdf");
        reader = new PdfReader(fileInputStream);

        // Sử dụng ByteArrayOutputStream để ghi dữ liệu PDF đã ký
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
//        PdfWriter writer = new PdfWriter(pdfOutputPath); // pdfOutputPath -> byteArrayOutputStream sẽ bị lỗi
//        PdfDocument pdfDoc = new PdfDocument(reader, writer);
        PdfDocument pdfDoc = new PdfDocument(reader);
        int numberOfPages = pdfDoc.getNumberOfPages(); // index page

        PdfSigner signer = new PdfSigner(reader, byteArrayOutputStream, false);

        // Create the signature appearance
        Rectangle rect = new Rectangle(400, 300, 200, 100);

//        ClassLoader loader = Thread.currentThread().getContextClassLoader();
//        URL url = loader.getResource(imageSignaturePdfPath);
//        assert url != null;
//        ImageData imageData = ImageDataFactory.create(url);

        byte[] data = loadImageDataFromResource(imageSignaturePdfPath);

        ImageData imageData;
        if (data.length > 0) {
            imageData = ImageDataFactory.create(data);
        } else
            return null;

        PdfSignatureAppearance appearance = signer.getSignatureAppearance();
        appearance
                .setReason(reason)
                .setLocation(location)

                // Specify if the appearance before field is signed will be used
                // as a background for the signed field. The "false" value is the default value.
                .setReuseAppearance(false)
                .setPageRect(rect)
                .setPageNumber(numberOfPages)
                .setSignatureGraphic(imageData)
                .setRenderingMode(PdfSignatureAppearance.RenderingMode.GRAPHIC);
        signer.setFieldName("sig");

        IExternalSignature pks = new PrivateKeySignature(privateKey, DigestAlgorithms.SHA256, pkcs11Provider.getName());
        IExternalDigest digest = new BouncyCastleDigest();

        // Sign the document using the detached mode, CMS or CAdES equivalent.
        signer.signDetached(digest, pks, certificateChain, null, null, null, 0, PdfSigner.CryptoStandard.CMS);

        byte[] output = byteArrayOutputStream.toByteArray();

//        pdfDoc.close();
//        reader.close();
        byteArrayOutputStream.close();

//        Write file to resource
        saveToResourcesFolder(output, fileName + "_signed.pdf");
//        Trả về ByteArrayInputStream chứa dữ liệu PDF đã ký
        return new ByteArrayInputStream(output);
    }

    private  X509Certificate generateCertificate(KeyPair keyPair,String information, String extension) throws Exception {
        Security.addProvider(new BouncyCastleProvider());

        // Tạo một chứng chỉ tự ký với Bouncy Castle
        ContentSigner contentSigner = new JcaContentSignerBuilder("SHA256WithRSA").build(keyPair.getPrivate());
        X500Name issuerName = new X500Name(information);
        BigInteger serialNumber = BigInteger.valueOf(System.currentTimeMillis());
        Date startDate = new Date(System.currentTimeMillis() - 24 * 60 * 60 * 1000); // Yesterday
        Date endDate = new Date(System.currentTimeMillis() + 365 * 24 * 60 * 60 * 1000); // 1 year from now

        JcaX509v3CertificateBuilder certificateBuilder = new JcaX509v3CertificateBuilder(
                issuerName,
                serialNumber,
                startDate,
                endDate,
                issuerName,
                keyPair.getPublic()
        );

        // Add extensions
        JcaX509ExtensionUtils extensionUtils = new JcaX509ExtensionUtils();
        certificateBuilder.addExtension(
                Extension.subjectKeyIdentifier,
                false,
                extensionUtils.createSubjectKeyIdentifier(keyPair.getPublic())
        );
        certificateBuilder.addExtension(
                Extension.authorityKeyIdentifier,
                false,
                extensionUtils.createAuthorityKeyIdentifier(keyPair.getPublic())
        );
        certificateBuilder.addExtension(
                Extension.basicConstraints,
                true,
                new BasicConstraints(true)
        );
        certificateBuilder.addExtension(
                Extension.keyUsage,
                true,
                new KeyUsage(KeyUsage.digitalSignature | KeyUsage.keyEncipherment)
        );
        certificateBuilder.addExtension(
                Extension.extendedKeyUsage,
                true,
                new ExtendedKeyUsage(KeyPurposeId.id_kp_serverAuth)
        );
        certificateBuilder.addExtension(
                Extension.subjectAlternativeName,
                false,
                new GeneralNames(new GeneralName(GeneralName.rfc822Name, extension))
        );

        // Xây dựng chứng chỉ
        X509CertificateHolder certificateHolder = certificateBuilder.build(contentSigner);

        // Chuyển đổi chứng chỉ holder thành X509Certificate
        JcaX509CertificateConverter certificateConverter = new JcaX509CertificateConverter();
        return certificateConverter.getCertificate(certificateHolder);
    }

    public byte[] loadImageDataFromResource(String imagePath) throws IOException {
        // Sử dụng ClassLoader để truy cập resource
        ClassLoader classLoader = getClass().getClassLoader();
        // Đọc file từ resource
        InputStream inputStream = classLoader.getResourceAsStream(imagePath);
        if (inputStream == null) {
            throw new FileNotFoundException("File not found in resources: " + imagePath);
        }
        // Đọc dữ liệu từ InputStream vào một mảng byte
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024];
        int bytesRead;
        while ((bytesRead = inputStream.read(buffer)) != -1) {
            byteArrayOutputStream.write(buffer, 0, bytesRead);
        }
        byte[] imageData = byteArrayOutputStream.toByteArray();
        // Đóng luồng
        inputStream.close();
        byteArrayOutputStream.close();
        return imageData;
    }

    // save uploaded file with ByteArrayOutputStream
    public void saveToResourcesFolder(ByteArrayOutputStream outputStream, String filename) throws IOException {
        // Identify the resources folder
//        Path resourcesPath = Paths.get("src", "data");
        Path resourcesPath = Paths.get("D:\\Data\\MOBILE_ID\\BE_DemoSignWithUSBToken\\DemoSignWithUSBToken\\src\\data");

        // Construct the path within the resources folder
        Path filePath = resourcesPath.resolve(filename);

        // Create missing directories if they don't exist
        Files.createDirectories(filePath.getParent());

//        File file = new File(filePath.toUri());
//        if (file.exists()) {
//            out.println("LINE 351: " + file.getName() + " -- " + file.getAbsolutePath());
//
//            try (FileOutputStream fos = new FileOutputStream(file)) {
//                fos.write(fileOutputStream.to);
//            }
//        }

        // Save the file to the resources folder
        try (OutputStream fileOutputStream = Files.newOutputStream(filePath.toFile().toPath())) {
            fileOutputStream.write(outputStream.toByteArray());
            out.println("LINE 370: " + "success");
        }
    }

    public void saveToResourcesFolder(byte[] outputStream, String filename) throws IOException {
        // Identify the resources folder
//        Path resourcesPath = Paths.get("src", "data");
        Path resourcesPath = Paths.get("D:\\Data\\MOBILE_ID\\BE_DemoSignWithUSBToken\\DemoSignWithUSBToken\\src\\data");

        // Construct the path within the resources folder
        Path filePath = resourcesPath.resolve(filename);

        // Create missing directories if they don't exist
        Files.createDirectories(filePath.getParent());

//        File file = new File(filePath.toUri());
//        if (file.exists()) {
//            out.println("LINE 351: " + file.getName() + " -- " + file.getAbsolutePath());
//
//            try (FileOutputStream fos = new FileOutputStream(file)) {
//                fos.write(fileOutputStream.to);
//            }
//        }

        // Save the file to the resources folder
        try (OutputStream fileOutputStream = Files.newOutputStream(filePath.toFile().toPath())) {
            fileOutputStream.write(outputStream);
            out.println("LINE 370: " + "success");
        }
    }

    // save uploaded file with InputStream
    private void writeToFile(InputStream inputStream,
                             String filename) throws IOException {
        Path resourcesPath = Paths.get("D:\\Data\\MOBILE_ID\\BE_DemoSignWithUSBToken\\DemoSignWithUSBToken\\src\\data");
        Path filePath = resourcesPath.resolve(filename);

        OutputStream out;
        int read;
        byte[] bytes = new byte[1024];

        out = Files.newOutputStream(new File(filePath.toUri()).toPath());
        while ((read = inputStream.read(bytes)) != -1) {
            out.write(bytes, 0, read);
        }
        out.flush();
        out.close();
    }
}
