package org.example.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class AppConfig {
    private static final Logger log = LoggerFactory.getLogger(AppConfig.class);
    private final Properties properties = new Properties();


    public AppConfig() {
        // doc file cua app.properties
        try (InputStream input = getClass().getClassLoader().getResourceAsStream("app.properties")) {
            if (input == null) {
                throw new RuntimeException("khong tim thay file app.properties");
            }
            properties.load(input);
            log.info("load thanh cong file app.properties");
        } catch (IOException e) {
            throw new RuntimeException("khong doc duoc file app.properties " + e.getMessage(), e);
        }
    }
    /**
     * Lấy giá trị dạng chuỗi (String) theo key
     */
    public String get(String key) {
        String value = properties.getProperty(key);
        if (value == null || value.isBlank()) {
            throw new RuntimeException("Thiếu cấu hình cho key: '" + key + "' trong app.properties");
        }
        return value.trim();
    }
    /**
     * Lấy giá trị dạng số nguyên (int) theo key (VD: port, pool size)
     */
    public int getInt(String key) {
        return Integer.parseInt(get(key));
    }
}
