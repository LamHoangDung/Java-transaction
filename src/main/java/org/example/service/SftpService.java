package org.example.service;

import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.Session;
import org.example.config.AppConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;


public class SftpService {

    private static final Logger log = LoggerFactory.getLogger(SftpService.class);
    private final AppConfig config;

    public SftpService(AppConfig config) {
        this.config = config;
    }

     //Ket noi vao SFTP Docker va tai file CSV cua ngay chi dinh ve thu muc partner

    public File downloadFile() {
        String host = config.get("sftp.host");
        int port = config.getInt("sftp.port");
        String username = config.get("sftp.username");
        String password = config.get("sftp.password");
        String remotePath = config.get("sftp.remote.path");

        // Luu file vao thu muc partner
        Path localDirPath = Paths.get("partner");

        Session session = null; //phien lam viec
        ChannelSftp channelSftp = null; //duong truyen file kenh sftp, duoc su dung de thuc hien cac lenh nhu tai file(get) va day file (put)

        try {
            // Tao thu muc partner neu chua co
            Files.createDirectories(localDirPath);

            //tao ket noi toi sftp
            JSch jsch = new JSch();
            session = jsch.getSession(username, host, port);
            session.setPassword(password);
            session.setConfig("StrictHostKeyChecking", "no");
            session.connect(30_000);
            log.info("Ket noi SFTP thanh cong: {}@{}:{}", username, host, port);

            //mo kenh SFTP de truyen tai file
            channelSftp = (ChannelSftp) session.openChannel("sftp");
            channelSftp.connect();


            List<ChannelSftp.LsEntry> entries = channelSftp.ls(remotePath);  //lay tat ca cac file trong thu muc
            List<String> csvFiles = new ArrayList<>();
            for (ChannelSftp.LsEntry entry : entries) {
                String name = entry.getFilename();
                // Chỉ lấy các file bắt đầu bằng "transactions_" và kết thúc bằng ".csv"
                if (!entry.getAttrs().isDir() && name.startsWith("transactions_") && name.endsWith(".csv")) {
                    csvFiles.add(name);
                }
            }
            if (csvFiles.isEmpty()) {
                log.warn("khong co file csv nao het");
                return null;
            }

            //sort cac file, file moi nhat se nam o cuoi cung
            Collections.sort(csvFiles);
            String latestFileName = csvFiles.get(csvFiles.size() - 1);


            //tai file ve
            String remoteFilePath = remotePath + latestFileName;
            Path localFilePath = localDirPath.resolve(latestFileName);
            channelSftp.get(remoteFilePath, localFilePath.toString()); //hàm channelSftp.get(remote, local) luôn mặc định ở chế độ overwrite, neu tai ve ma co file trung thi overwrite len file do luon
            log.info("tai file thanh cong: {}", localFilePath.toAbsolutePath());
            return localFilePath.toFile();


        } catch (Exception e) {
            log.error("Loi khi tai file tu SFTP: {}", e.getMessage(), e);
            return null;
        } finally {
            // Luon ngat ket noi khi hoan tat
            if (channelSftp != null && channelSftp.isConnected()) {
                channelSftp.disconnect();
            }
            if (session != null && session.isConnected()) {
                session.disconnect();
            }
        }
    }
}
