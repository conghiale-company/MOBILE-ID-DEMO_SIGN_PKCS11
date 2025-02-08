package org.example;

import java.io.IOException;
import java.security.GeneralSecurityException;

/**
 * Hello world!
 *
 */
public class App 
{
    public static void main( String[] args ) throws Exception {
        MySignature signature = new MySignature();
        signature.signPDF("ConghialeKey", "", null);
//        signature.loadPrimaryKey();
    }
}
