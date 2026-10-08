package org.example.service;
import org.example.config.AppConfig;

import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

public class RsaKeyService {

    private final AppConfig config;

    public RsaKeyService(AppConfig config){
        this.config = config;
    }

    public void generateKey(
    ) throws GeneralSecurityException {

        //chon thuat toan RSA
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");

        //chon do dai khoa
        generator.initialize(2048);

        //tao ra public key va private key theo 1 cặp
        KeyPair keyPair = generator.generateKeyPair();

        //chuyen key thanh chuoi doc duoc - string base64
        String publicKey = Base64.getEncoder().encodeToString(keyPair.getPublic().getEncoded());
        String privatekey = Base64.getEncoder().encodeToString(keyPair.getPrivate().getEncoded());

        System.out.println("public key: " + publicKey);
        System.out.println("private key: " + privatekey);
    }

    public PublicKey loadPublicKey() throws GeneralSecurityException{

        String publicKeyBase64 = config.getRsaPublicKey();

        //decode lai key từ string thành byte
        byte[] publicKeyBytes = Base64.getDecoder().decode(publicKeyBase64);

        //mô tả định dạng của public Key
        X509EncodedKeySpec publicKeySpec = new X509EncodedKeySpec(publicKeyBytes);

        //mô tả định dạng chung object key
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");

        //khôi phục object public key RSA  de su dung
        return keyFactory.generatePublic(publicKeySpec);
    }

    public PrivateKey loadPrivateKey() throws GeneralSecurityException {

        String privateKeyBase64 = config.getRsaPrivateKey();

        //decode key lai từ string -> byte
        byte[] keyBytes = Base64.getDecoder().decode(privateKeyBase64);

        //mo ta định dạng của public key
        PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(keyBytes);

        //mô tả định dạng dinh chung của object key
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");

        //khôi phục object public key RSA  de su dung
        return keyFactory.generatePrivate(keySpec);
    }
}
