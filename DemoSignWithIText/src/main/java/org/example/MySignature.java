package org.example;

import com.itextpdf.io.image.ImageData;
import com.itextpdf.io.image.ImageDataFactory;
import com.itextpdf.kernel.geom.Rectangle;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfReader;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.kernel.pdf.StampingProperties;
import com.itextpdf.signatures.*;
import org.bouncycastle.jce.provider.BouncyCastleProvider;

import java.io.*;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.*;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.*;

import static java.lang.System.out;

public class MySignature {
    private String configFilePath = "src/main/resources/epass2003.cfg";  // Đường dẫn tới file cấu hình PKCS#11
    private String pin = "12345678";  // Mật khẩu của USB Token
    private String pdfInputPath = "Final_Exam_OOP.pdf";  // File PDF chưa ký
    //    private String pdfInputPath = "LeCongNghia_CV.pdf";  // File PDF chưa ký
    private String pdfOutputPdf = "src/main/resources/signed_output.pdf";  // File PDF sau khi đã ký
    private String imageSignaturePdfPath = "imageSign.jpg";  // File image Sign

    private Certificate[] certificateChain = null;
    private PrivateKey privateKey = null;
    private PublicKey publicKey = null;
    private boolean useFirstCertificateFromToken = false;
    private String issuerCommonName = "Lê Công Nghĩa";
    private String certificateSerialNumber = "1713253906839";
    private String location = "LOCATION";
    private Provider pkcs11Provider = null;

    public MySignature() throws IOException {
        loadPKCS11Provider();
    }

    private void loadPKCS11Provider() throws IOException {
        Path path = Paths.get(configFilePath);
        File file = path.toFile();
        InputStream inputStream = Files.newInputStream(file.toPath());

        pkcs11Provider = new sun.security.pkcs11.SunPKCS11(inputStream);

//        In later Java versions, e.g. Java 11, you instead do it like this:
//        Provider p = Security.getProvider("SunPKCS11");
//        pkcs11Provider = p.configure(inputStream);

        Security.addProvider(pkcs11Provider);
        Security.addProvider(new BouncyCastleProvider());

        if (Security.getProvider(pkcs11Provider.getName()) == null) {
            throw new RuntimeException("Failed to load PKCS#11 provider");
        } else
            out.println("Loaded PKCS#11 provider");
    }

//    get Info in usb token
    public void loadPrimaryKey() throws Exception {
        // Tải PKCS#11 KeyStore
        KeyStore ks = KeyStore.getInstance("PKCS11", pkcs11Provider);
        ks.load(null, pin.toCharArray()); // Pass the PIN required to access the token
        Enumeration<String> aliasesEnum = ks.aliases();

        if (aliasesEnum.hasMoreElements()) {
            while(aliasesEnum.hasMoreElements()) {
//                lấy cái đầu tiên
                String alias = aliasesEnum.nextElement();
                X509Certificate cert = (X509Certificate) ks.getCertificate(alias);

                if (cert != null) {
                    certificateChain = ks.getCertificateChain(alias);
                    privateKey = (PrivateKey) ks.getKey(alias, pin.toCharArray());
                    publicKey = cert.getPublicKey();

                    out.println("LINE 81: PrimaryKey: " + privateKey.getEncoded());
                    out.println("LINE 82: PublicKey: " + publicKey.getEncoded());
                    out.println("LINE 83: SIGNATURE PKCS1: " + createSignature(privateKey));
                    break;
                }
            }
        }
        else {
            throw new KeyStoreException("Keystore is empty");
        }
    }

//    Tạo chữ ký số (signature) từ dữ liệu (payload) (Hash and Sign trong 1 bước)
    private String createSignature(PrivateKey privateKey) throws Exception {
        String header = "{\"alg\":\"RS256\"}";
        String payload = "{\"sub\":\"1234567890\",\"name\":\"John Doe\",\"admin\":true}";

        String dataToSign = base64UrlEncode(header) + "." + base64UrlEncode(payload);
        Signature signature = Signature.getInstance("SHA256withRSA", "BC");
        signature.initSign(privateKey);
        signature.update(dataToSign.getBytes("UTF-8"));
        byte[] signatureBytes = signature.sign();
        return Base64.getUrlEncoder().encodeToString(signatureBytes);
    }

    public String base64UrlEncode(String input) {
        return Base64.getUrlEncoder().encodeToString(input.getBytes())
                .replaceAll("=", "");
    }
//    Sign
    public void signPDF(String alias, String fileName, InputStream fileInputStream) throws Exception {
        // Tải PKCS#11 KeyStore
        KeyStore ks = KeyStore.getInstance("PKCS11");
        ks.load(null, pin.toCharArray()); // Pass the PIN required to access the token
        Enumeration<String> aliasesEnum = ks.aliases();

        if (aliasesEnum.hasMoreElements()) {
            while(aliasesEnum.hasMoreElements()) {
//                lấy cái đầu tiên
                alias = aliasesEnum.nextElement();

                X509Certificate cert = (X509Certificate) ks.getCertificate(alias);

                if (cert != null) {
                    certificateChain = ks.getCertificateChain(alias);
                    privateKey = (PrivateKey) ks.getKey(alias, null);

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

                    sign("REASON", location);
                    break;
                }
            }
        }
        else {
            throw new KeyStoreException("Keystore is empty");
        }
    }

    private String signature(String fileName, InputStream fileInputStream, String digestAlgorithm, String provider, PdfSigner.CryptoStandard signatureType, String reason, String location) throws IOException, GeneralSecurityException {
//        PdfReader reader;
////        Read file in resource
//        InputStream inputStream = this.getClass().getClassLoader().getResourceAsStream(pdfInputPath);
//        if (inputStream == null) {
//            throw new IllegalArgumentException("file not found! LeCongNghia_CV.pdf");
//        } else {
//            reader = new PdfReader(inputStream);
//        }

//        reader = new PdfReader(fileInputStream);

        // Sử dụng ByteArrayOutputStream để ghi dữ liệu PDF đã ký
//        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
//        PdfWriter writer = new PdfWriter(byteArrayOutputStream);
        PdfWriter writer = new PdfWriter(pdfOutputPdf);
        PdfReader reader = new PdfReader(pdfInputPath);
        PdfDocument pdfDoc = new PdfDocument(reader, writer);
        int numberOfPages = pdfDoc.getNumberOfPages(); // index page

        PdfSigner signer = new PdfSigner(reader, Files.newOutputStream(Paths.get("signed_output.pdf")), false);

        // Create the signature appearance
        Rectangle rect = new Rectangle(400, 300, 200, 100);

        ClassLoader loader = Thread.currentThread().getContextClassLoader();
        URL url = loader.getResource(imageSignaturePdfPath);
        assert url != null;
        ImageData imageData = ImageDataFactory.create(url);

//        byte[] data = loadImageDataFromResource(imageSignaturePdfPath);
//
//        ImageData imageData;
//        if (data.length > 0) {
//            imageData = ImageDataFactory.create(data);
//        } else
//            return null;

        PdfSignatureAppearance appearance = signer.getSignatureAppearance();
        appearance
            .setReason(reason)
            .setLocation(location)

            // Specify if the appearance before field is signed will be used
            // as a background for the signed field. The "false" value is the default value.
            .setCertificate(certificateChain[0]) // new
            .setReuseAppearance(false)
            .setPageRect(rect)
            .setPageNumber(numberOfPages)
            .setSignatureGraphic(imageData)
            .setRenderingMode(PdfSignatureAppearance.RenderingMode.GRAPHIC);

        signer.setFieldName("sig");

        IExternalSignature pks = new PrivateKeySignature(privateKey, digestAlgorithm, provider);
        IExternalDigest digest = new BouncyCastleDigest();

//        // Sign the document using the detached mode, CMS or CAdES equivalent.
        signer.signDetached(digest, pks, certificateChain, null, null, null,0, signatureType);
        return "Signature OK";
    }

    private void sign(String reason, String location) throws IOException, GeneralSecurityException {
        IOcspClient ocspClient = new OcspClientBouncyCastle(null);
        ITSAClient tsaClient = null;
        for (Certificate certificate : certificateChain) {
            X509Certificate cert = (X509Certificate) certificate;
            String tsaUrl = CertificateUtil.getTSAURL(cert);
            if (tsaUrl != null) {
                tsaClient = new TSAClientBouncyCastle(tsaUrl);
                break;
            }
        }

        List<ICrlClient> crlList = new ArrayList<>();
        crlList.add(new CrlClientOnline(certificateChain));

        handleSign(pdfInputPath, pdfOutputPdf, certificateChain, privateKey,
            DigestAlgorithms.SHA256, pkcs11Provider.getName(), PdfSigner.CryptoStandard.CMS,
                reason, location, crlList, ocspClient, tsaClient, 0);

    }

    public void handleSign(String src, String dest, Certificate[] chain, PrivateKey pk,
                     String digestAlgorithm, String provider, PdfSigner.CryptoStandard subfilter,
                     String reason, String location, Collection<ICrlClient> crlList,
                     IOcspClient ocspClient, ITSAClient tsaClient, int estimatedSize)
            throws GeneralSecurityException, IOException {

        PdfReader reader = new PdfReader(src);
        PdfWriter writer = new PdfWriter(pdfOutputPdf);
        PdfDocument pdfDoc = new PdfDocument(reader, writer);
        int numberOfPages = pdfDoc.getNumberOfPages(); // index page

        PdfSigner signer = new PdfSigner(reader, Files.newOutputStream(Paths.get(dest)), false);

        // Create the signature appearance
        Rectangle rect = new Rectangle(400, 300, 200, 100);

        ClassLoader loader = Thread.currentThread().getContextClassLoader();
        URL url = loader.getResource(imageSignaturePdfPath);
        assert url != null;
        ImageData imageData = ImageDataFactory.create(url);

        PdfSignatureAppearance appearance = signer.getSignatureAppearance();
        appearance
                .setReason(reason)
                .setLocation(location)

                // Specify if the appearance before field is signed will be used
                // as a background for the signed field. The "false" value is the default value.
//                .setReuseAppearance(false)
//                .setPageRect(rect)
//                .setPageNumber(1)

                .setCertificate(certificateChain[0]) // new
                .setReuseAppearance(false)
                .setPageRect(rect)
                .setPageNumber(numberOfPages)
                .setSignatureGraphic(imageData)
                .setRenderingMode(PdfSignatureAppearance.RenderingMode.GRAPHIC);
        signer.setFieldName("sig");

        IExternalSignature pks = new PrivateKeySignature(pk, digestAlgorithm, provider);
        IExternalDigest digest = new BouncyCastleDigest();

        // Sign the document using the detached mode, CMS or CAdES equivalent.
//        signer.signDetached(digest, pks, chain, crlList, ocspClient, tsaClient, estimatedSize, subfilter);
        signer.signDetached(digest, pks, chain, null, null, null, estimatedSize, subfilter);
    }

}
