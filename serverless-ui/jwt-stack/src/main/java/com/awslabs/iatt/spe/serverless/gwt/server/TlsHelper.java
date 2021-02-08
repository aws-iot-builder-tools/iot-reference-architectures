package com.awslabs.iatt.spe.serverless.gwt.server;

import io.vavr.Tuple2;
import io.vavr.control.Option;
import io.vertx.core.net.JksOptions;

import javax.servlet.ServletContext;
import java.io.File;
import java.io.IOException;
import java.io.Writer;
import java.security.*;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.security.interfaces.RSAPublicKey;

public interface TlsHelper {
    char[] BLANK_PASSWORD = "".toCharArray();

    Tuple2<KeyStore, File> getRandomKeystore(String name) throws SignatureException, InvalidKeyException, KeyStoreException, CertificateException, NoSuchAlgorithmException, IOException;

    Tuple2<KeyStore, File> getFixedKeystore(Option<ServletContext> servletContextOption, String name) throws SignatureException, InvalidKeyException, KeyStoreException, CertificateException, NoSuchAlgorithmException, IOException;

    Tuple2<KeyStore, File> getKeystoreForKeyPair(KeyPair keyPair, String prefix) throws SignatureException, InvalidKeyException, KeyStoreException, CertificateException, NoSuchAlgorithmException, IOException;

    KeyPair getRandomKeypair();

    void writeKeyPair(String prefix, KeyPair keyPair);

    void writeKey(Key key, Writer writer);

    KeyPair readKeyPair(Option<ServletContext> servletContextOption, String prefix);

    KeyPair decodeKeyPair(byte[] encodedPublicKey, byte[] encodedPrivateKey);

    KeyPair getFixedKeypair(Option<ServletContext> servletContextOption);

    X509Certificate getCertFromKeyPair(KeyPair keyPair, String name) throws SignatureException, InvalidKeyException;

    Void writeCertToFile(X509Certificate x509Certificate, String prefix) throws IOException, CertificateEncodingException;

    JksOptions getRandomJksOptions(String name) throws CertificateException, NoSuchAlgorithmException, KeyStoreException, SignatureException, InvalidKeyException, IOException;

    JksOptions getFixedJksOptions(Option<ServletContext> servletContextOption, String name) throws CertificateException, NoSuchAlgorithmException, KeyStoreException, SignatureException, InvalidKeyException, IOException;

    JksOptions getJksOptionsForKeyPair(KeyPair keyPair, String prefix) throws CertificateException, NoSuchAlgorithmException, KeyStoreException, SignatureException, InvalidKeyException, IOException;

    String getKeyString(RSAPublicKey publicKey);
}
