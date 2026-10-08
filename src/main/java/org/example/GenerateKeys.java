package org.example;

import org.example.config.AppConfig;
import org.example.service.RsaKeyService;

import java.security.GeneralSecurityException;

public class GenerateKeys {
    public static void main(String[] args)
            throws GeneralSecurityException {
        AppConfig config = new AppConfig();

        RsaKeyService keyService = new RsaKeyService(config);

        keyService.generateKey();
    }
}
