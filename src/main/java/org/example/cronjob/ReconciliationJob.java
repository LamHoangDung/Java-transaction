package org.example.cronjob;

import org.example.entity.ReconciliationResultEntity;
import org.example.service.ConvertService;
import org.example.service.EmailService;
import org.example.service.ReconciliationService;
import org.example.service.SftpService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.time.LocalDate;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class ReconciliationJob {
    private static final Logger log = LoggerFactory.getLogger(ReconciliationJob.class);
    private final SftpService sftpService;
    private final ConvertService convertService;
    private final ReconciliationService reconService;
    private final EmailService emailService;



    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    public ReconciliationJob(SftpService sftpService,
                                  ConvertService convertService,
                                  ReconciliationService reconService,
                                  EmailService emailService) {
        this.sftpService = sftpService;
        this.convertService = convertService;
        this.reconService = reconService;
        this.emailService = emailService;
    }

    public void runNow() {
        executeJob();
    }

    public void scheduleDaily() {
        long delay = 5;
        long period = 2 * 60;    //delay tam 2p de test
        log.info("bdau cronjob");
        scheduler.scheduleAtFixedRate(
                this::executeJob,
                period,
                delay,
                TimeUnit.SECONDS
        );
    }

    private void executeJob() {
        try {
            log.info("bdau doi soat");
            //tai file csv ve
            File csvFile = sftpService.downloadFile();
            if (csvFile == null || !csvFile.exists()) {
                log.error("Khong tai duoc file CSV");
                return;
            }
            // BướcRút ngày từ tên file
            String dateStr = csvFile.getName()
                    .replace("transactions_", "")
                    .replace(".csv", "");
            LocalDate date = LocalDate.parse(dateStr);
            log.info("Ngay doi soat: {}", date);
            // Bước 3: Đọc CSV → insert vào bảng partner_transactions
            convertService.convertAndSave(csvFile, date);
            // Bước 4: Đối soát → lưu kết quả vào bảng reconciliation_results
            ReconciliationResultEntity result = reconService.reconcile(date);
            // Bước 5: Gửi email cho sếp → lưu log vào bảng reconciliation_reports
            emailService.sendReport(result);
            log.info("doi soat hoang tat");
        } catch (Exception e) {
            log.error("loi trong job: {}", e.getMessage(), e);
        }
    }

    public void shutdown() {
        scheduler.shutdown();
        log.info("Da tat bo hen gio.");
    }
}
