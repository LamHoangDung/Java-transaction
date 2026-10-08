package org.example;

import org.example.config.AppConfig;
import org.example.service.FileService;
import org.example.service.RsaKeyService;

import java.nio.file.Path;

public class PartnerMain {

    public static void main(String[] args) {

        try {
            AppConfig config = new AppConfig();
            RsaKeyService rsaKeyService = new RsaKeyService(config);
            FileService fileService = new FileService(rsaKeyService, config);
            Path encryptedFile = fileService.encryptLatestCsv();
            System.out.println(
                    "tao file ma hoa thanh cong" + encryptedFile
            );
        } catch (Exception e) {
            System.err.println(
                    "tao file ma hoa that bai: " + e.getMessage()
            );

            System.exit(1);
        }
    }
}