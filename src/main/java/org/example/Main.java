package org.example;

import org.example.config.AppConfig;
import org.example.config.DatabaseConfig;
import org.example.cronjob.ReconciliationJob;
import org.example.entity.ReconciliationReportEntity;
import org.example.entity.ReconciliationResultEntity;
import org.example.repository.PartnerTransactionRepository;
import org.example.repository.ReconciliationRepository;
import org.example.repository.TransactionRepository;
import org.example.service.ConvertService;
import org.example.service.EmailService;
import org.example.service.ReconciliationService;
import org.example.service.SftpService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.sql.Connection;
import java.time.LocalDate;
import java.util.Scanner;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class Main {
    private static final Logger log = LoggerFactory.getLogger(Main.class);
    private static LocalDate currentDate = null;
    private static ReconciliationResultEntity reconciliationResultEntity = null;
    private static File downloadFile = null;


    public static void main(String[] args) {


        //khoi tao cau hinh va ket noi
        AppConfig config = new AppConfig();
        DatabaseConfig.init(config);


        TransactionRepository transactionRepository = new TransactionRepository();
        ReconciliationRepository reconciliationRepository = new ReconciliationRepository();
        PartnerTransactionRepository partnerTransactionRepository = new PartnerTransactionRepository();


        SftpService sftpService = new SftpService(config);
        ConvertService convertService = new ConvertService(partnerTransactionRepository);
        ReconciliationService reconService = new ReconciliationService(transactionRepository, partnerTransactionRepository, reconciliationRepository);
        EmailService emailService = new EmailService(config, reconService);


        ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
        scheduler.scheduleAtFixedRate(() -> {
            try {
                log.info("cronjob - chay va download file ");
                downloadFile = sftpService.downloadFile();
            } catch (Exception e) {
                log.error("Loi cronjob: " + e.getMessage());
            }
        }, 5, 120, TimeUnit.SECONDS);



        Scanner scanner = new Scanner(System.in);
        while (true) {
            System.out.println("1/ nap file vao DB + doi soat");
            System.out.println("2/ gui mail bao cao ");
            System.out.println("3/ thoat chuong trinh");

            String choice = scanner.nextLine();
            switch (choice) {
                case "1":

                    System.out.println("bdau doi soat");

                    if (downloadFile != null) {
                        String dateStr = downloadFile.getName().replace("transactions_", "").replace(".csv", "");
                        currentDate= LocalDate.parse(dateStr);

                        convertService.convertAndSave(downloadFile, currentDate);

                        reconciliationResultEntity = reconService.reconcile(currentDate);

                        System.out.println("doi soat thanh cong, ktra du lieu trc khi gui mail");
                    }
                    break;
                case "2":

                    if (reconciliationResultEntity == null) {
                        System.out.println("phai doi soat truoc khi gui mail");
                    } else {
                        System.out.println("\n bat dau gui mail");
                        emailService.sendReport(reconciliationResultEntity);
                        System.out.println("da gui mail thanh cong ");
                    }
                    break;
                case "3":
                    System.out.println("thoat chuong trinh");
                    scheduler.shutdown();
                    DatabaseConfig.close();
                    System.exit(0);
                    break;
                default:
                    System.out.println("lua chon khong hop le, hay nhap lai");
            }
        }
    }
}
