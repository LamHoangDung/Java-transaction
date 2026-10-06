package org.example.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;

public class RsaKeyService {

    public void generateAndSave(
            Path publicKeyPath,
            Path privateKeyPath
    ) throws GeneralSecurityException, IOException {

        // Không ghi đè khóa đang có.
        if (Files.exists(publicKeyPath)
                || Files.exists(privateKeyPath)) {

            throw new IllegalStateException(
                    "File khóa đã tồn tại. Không tạo khóa mới ghi đè."
            );
        }

        // Tạo thư mục chứa khóa nếu chưa có.
        Files.createDirectories(
                publicKeyPath.toAbsolutePath().getParent()
        );

        Files.createDirectories(
                privateKeyPath.toAbsolutePath().getParent()
        );

        // Chọn thuật toán tạo cặp khóa.
        KeyPairGenerator generator =
                KeyPairGenerator.getInstance("RSA");

        // Độ dài khóa RSA là 2048 bit.
        generator.initialize(2048);

        // Sinh public key và private key cùng một cặp.
        KeyPair keyPair = generator.generateKeyPair();

        // Chuyển khóa thành byte và lưu vào file.
        Files.write(
                publicKeyPath,
                keyPair.getPublic().getEncoded(),
                StandardOpenOption.CREATE_NEW
        );

        Files.write(
                privateKeyPath,
                keyPair.getPrivate().getEncoded(),
                StandardOpenOption.CREATE_NEW
        );
    }

    public PublicKey loadPublicKey(
            Path publicKeyPath
    ) throws GeneralSecurityException, IOException {

        byte[] keyBytes = Files.readAllBytes(publicKeyPath);

        X509EncodedKeySpec keySpec =
                new X509EncodedKeySpec(keyBytes);

        KeyFactory keyFactory =
                KeyFactory.getInstance("RSA");

        return keyFactory.generatePublic(keySpec);
    }

    public PrivateKey loadPrivateKey(
            Path privateKeyPath
    ) throws GeneralSecurityException, IOException {

        byte[] keyBytes = Files.readAllBytes(privateKeyPath);

        PKCS8EncodedKeySpec keySpec =
                new PKCS8EncodedKeySpec(keyBytes);

        KeyFactory keyFactory =
                KeyFactory.getInstance("RSA");

        return keyFactory.generatePrivate(keySpec);
    }
}