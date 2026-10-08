package org.example.cronjob;

import org.example.service.SftpService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class ReconciliationJob {

    private static final Logger log = LoggerFactory.getLogger(ReconciliationJob.class);

    private final SftpService sftpService;

    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

    private volatile File downloadedFile;

    public ReconciliationJob(SftpService sftpService) {
        this.sftpService = sftpService;
    }

    public void start() {
        scheduler.scheduleAtFixedRate(
                this::executeJob,
                5,
                120,
                TimeUnit.SECONDS
        );
    }

    public void runNow() {
        executeJob();
    }

    private File executeJob() {
        try {
            log.info("bat dau tai file sftp");

            downloadedFile = sftpService.downloadFile();

            if (downloadedFile == null || !downloadedFile.isFile()) {
                log.warn("khong tai duoc file sftp");
                return null ;
            }
            log.info("da tai file:{}", downloadedFile);

            return downloadedFile;

        } catch (Exception e) {
            log.error("Lỗi trong job tải file", e);
            return null;
        }
    }


    public void shutdown() {
        scheduler.shutdown();
    }

    public File getDownloadedFile(){
        return downloadedFile;
    }
}