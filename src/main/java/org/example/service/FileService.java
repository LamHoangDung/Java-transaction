package org.example.service;

import org.example.config.AppConfig;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.OAEPParameterSpec;
import javax.crypto.spec.PSource;
import javax.crypto.spec.SecretKeySpec;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;

import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

import java.security.GeneralSecurityException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.spec.MGF1ParameterSpec;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;

import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class FileService {

    private static final String FILE_MARKER = "CSVENC1";

    private static final int NONCE_LENGTH = 12;
    private static final int TAG_LENGTH_BITS = 128;

    private final RsaKeyService rsaKeyService;
    private final AppConfig config;

    public FileService(
            RsaKeyService rsaKeyService,
            AppConfig config
    ) {
        this.rsaKeyService = rsaKeyService;
        this.config = config;
    }

    public Path encrypt(
            Path csvPath,
            Path outputDirectory
    ) throws IOException, GeneralSecurityException {

        // 1. Lấy tên gốc và ngày giao dịch.
        String originalName = csvPath.getFileName().toString();
        LocalDate fileDate = extractCsvDate(originalName);

        // 2. Gom tên gốc và nội dung thành một gói byte.
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();

        try (DataOutputStream output = new DataOutputStream(buffer)) {
            output.writeUTF(originalName);
            output.write(Files.readAllBytes(csvPath));
        }

        byte[] originalData = buffer.toByteArray();

        // 3. Tạo một khóa AES mới cho file này.
        KeyGenerator generator = KeyGenerator.getInstance("AES");
        generator.init(256);

        SecretKey aesKey = generator.generateKey();

        // 4. Tạo nonce ngẫu nhiên cho AES-GCM.
        byte[] nonce = new byte[NONCE_LENGTH];
        new SecureRandom().nextBytes(nonce);

        // 5. Mã hóa gói dữ liệu bằng AES.
        Cipher aesCipher = Cipher.getInstance("AES/GCM/NoPadding");

        aesCipher.init(
                Cipher.ENCRYPT_MODE,
                aesKey,
                new GCMParameterSpec(TAG_LENGTH_BITS, nonce)
        );

        byte[] encryptedData = aesCipher.doFinal(originalData);

        // 6. Lấy public key và mã hóa khóa AES bằng RSA.
        PublicKey publicKey = rsaKeyService.loadPublicKey();

        Cipher rsaCipher = Cipher.getInstance(
                "RSA/ECB/OAEPWithSHA-256AndMGF1Padding"
        );

        rsaCipher.init(
                Cipher.ENCRYPT_MODE,
                publicKey,
                rsaParameters()
        );

        byte[] encryptedAesKey = rsaCipher.doFinal(
                aesKey.getEncoded()
        );

        // 7. Đặt tên file theo ngày + UUID.
        Files.createDirectories(outputDirectory);

        String encryptedName =
                fileDate + "_" + UUID.randomUUID() + ".enc";

        Path encryptedPath = outputDirectory.resolve(encryptedName);

        // 8. Ghi các phần vào file .enc.
        try (DataOutputStream output = new DataOutputStream(
                Files.newOutputStream(
                        encryptedPath,
                        StandardOpenOption.CREATE_NEW,
                        StandardOpenOption.WRITE
                )
        )) {
            output.writeUTF(FILE_MARKER);

            output.writeInt(encryptedAesKey.length);
            output.write(encryptedAesKey);

            output.write(nonce);
            output.write(encryptedData);
        }

        return encryptedPath;
    }


    public Path decrypt(Path encryptedPath) throws IOException, GeneralSecurityException {

        if (encryptedPath == null || !Files.isRegularFile(encryptedPath)) {
            throw new IOException("Không tìm thấy file mã hóa để giải mã.");
        }
        Path outputDirectory = Path.of(
                config.getPartnerLocalDir()
        );
        byte[] encryptedAesKey;
        byte[] nonce;
        byte[] encryptedData;

        // 1. Đọc các phần trong file .enc.
        try (DataInputStream input = new DataInputStream(
                Files.newInputStream(encryptedPath)
        )) {
            String marker = input.readUTF();

            if (!FILE_MARKER.equals(marker)) {
                throw new IOException("Không đúng định dạng file mã hóa.");
            }

            int keyLength = input.readInt();

            if (keyLength <= 0 || keyLength > 4096) {
                throw new IOException("Độ dài khóa mã hóa không hợp lệ.");
            }

            encryptedAesKey = readExactly(input, keyLength);
            nonce = readExactly(input, NONCE_LENGTH);
            encryptedData = input.readAllBytes();
        }

        // 2. Lấy private key và giải mã khóa AES.
        PrivateKey privateKey = rsaKeyService.loadPrivateKey();

        Cipher rsaCipher = Cipher.getInstance(
                "RSA/ECB/OAEPWithSHA-256AndMGF1Padding"
        );

        rsaCipher.init(
                Cipher.DECRYPT_MODE,
                privateKey,
                rsaParameters()
        );

        byte[] aesKeyBytes = rsaCipher.doFinal(encryptedAesKey);

        // 3. Khôi phục object khóa AES.
        SecretKey aesKey = new SecretKeySpec(aesKeyBytes, "AES");

        // 4. sử dụng khóa AES và nonce đọc từ .enc
        Cipher aesCipher = Cipher.getInstance("AES/GCM/NoPadding");
        aesCipher.init(
                Cipher.DECRYPT_MODE,
                aesKey,
                new GCMParameterSpec(TAG_LENGTH_BITS, nonce)
        );

        //giải mã dữ liệu trong file
        byte[] originalData = aesCipher.doFinal(encryptedData);

        // 5. Tách tên gốc với nội dung CSV, roi ghi lại CSV
        String originalName;
        byte[] csvContent;

        try (DataInputStream input = new DataInputStream(
                new ByteArrayInputStream(originalData)
        )) {
            originalName = input.readUTF();
            csvContent = input.readAllBytes();
        }

        // 6. Kiểm tra tên gốc và ngày trên tên .enc.
        LocalDate fileDate = extractCsvDate(originalName);

        String encryptedName = encryptedPath.getFileName().toString();

        if (!encryptedName.startsWith(fileDate + "_")) {
            throw new IOException(
                    "Ngày trên tên file mã hóa không khớp với CSV gốc."
            );
        }

        // 7. Ghi CSV khôi phục, không ghi đè file đang có.
        Files.createDirectories(outputDirectory);

        Path restoredPath = outputDirectory.resolve(originalName);

        Files.write(
                restoredPath,
                csvContent,
                StandardOpenOption.CREATE_NEW,
                StandardOpenOption.WRITE
        );

        return restoredPath;
    }


    public Path encryptLatestCsv(
    ) throws IOException, GeneralSecurityException {

        Path sourceDirectory = Path.of(
                config.getPartnerSourceDir()
        );
        Path uploadDirectory = Path.of(
                config.getPartnerUploadDir()
        );

        // 1. Kiểm tra thư mục nguồn.
        if (!Files.isDirectory(sourceDirectory)) {
            throw new IOException("Không tìm thấy thư mục source.");
        }

        Path latestCsv = null;
        LocalDate latestDate = null;

        // 2. Duyệt các CSV có tên bắt đầu bằng transactions_.
        try (DirectoryStream<Path> files = Files.newDirectoryStream(
                sourceDirectory,
                "transactions_*.csv"
        )) {
            for (Path file : files) {

                String fileName = file.getFileName().toString();

                // Dùng lại hàm kiểm tra tên và lấy ngày đã có.
                LocalDate fileDate = extractCsvDate(fileName);

                // 3. Giữ lại file có ngày lớn nhất.
                if (latestDate == null || fileDate.isAfter(latestDate)) {
                    latestDate = fileDate;
                    latestCsv = file;
                }
            }
        }

        if (latestCsv == null) {
            throw new IOException("Không có CSV phù hợp trong source.");
        }

        // 4. Chuẩn bị thư mục upload.
        Files.createDirectories(uploadDirectory);

        // Mỗi ngày chỉ gửi một file mã hóa trong bài tập này.
        String existingFilePattern = latestDate + "_*.enc";

        try (DirectoryStream<Path> files = Files.newDirectoryStream(
                uploadDirectory,
                existingFilePattern
        )) {
            for (Path file : files) {
                if (Files.isRegularFile(file)) {
                    throw new IOException(
                            "Ngày " + latestDate
                                    + " đã có file mã hóa trong upload."
                    );
                }
            }
        }
        // 5. Mã hóa file được chọn và trả về đường dẫn .enc.
        return encrypt(latestCsv, uploadDirectory);
    }

    public LocalDate extractCsvDate( //hàm kiểm tra cú pháp ngày, sau đó extract ngày
            String fileName
    ) throws IOException {

        Pattern pattern = Pattern.compile(
                "^transactions_(\\d{4}-\\d{2}-\\d{2})\\.csv$"
        );

        Matcher matcher = pattern.matcher(fileName);

        if (!matcher.matches()) {
            throw new IOException(
                    "Tên CSV phải có dạng transactions_yyyy-MM-dd.csv"
            );
        }
            return LocalDate.parse(matcher.group(1));

    }


    private OAEPParameterSpec rsaParameters() {
        return new OAEPParameterSpec(
                "SHA-256",
                "MGF1",
                MGF1ParameterSpec.SHA256,
                PSource.PSpecified.DEFAULT
        );
    }

    private byte[] readExactly(
            DataInputStream input,
            int length
    ) throws IOException {

        byte[] bytes = input.readNBytes(length);

        if (bytes.length != length) {
            throw new EOFException("File mã hóa bị thiếu dữ liệu.");
        }

        return bytes;
    }
}