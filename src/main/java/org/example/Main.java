package org.example;

import org.example.config.AppConfig;
import org.example.config.DatabaseConfig;
import org.example.cronjob.ReconciliationJob;
import org.example.entity.ReconciliationResultEntity;
import org.example.repository.PartnerTransactionRepository;
import org.example.repository.ReconciliationRepository;
import org.example.repository.TransactionRepository;
import org.example.service.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.File;
import java.time.LocalDate;
import java.util.Scanner;


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
        InquiryService inquiryService = new InquiryService(transactionRepository);

        ReconciliationJob reconciliationJob = new ReconciliationJob(sftpService);
        reconciliationJob.start();


        Scanner scanner = new Scanner(System.in);
        while (true) {
            System.out.println("1/ nap file vao DB + doi soat");
            System.out.println("2/ gui mail bao cao ");
            System.out.println("3/ thoat chuong trinh");
            System.out.println("4/ kiem tra giao dich");

            String choice = scanner.nextLine();
            switch (choice) {
                case "1":

                    System.out.println("bdau doi soat");
                    downloadFile = reconciliationJob.getDownloadedFile();

                    if (downloadFile != null) {
                        String dateStr = downloadFile.getName().replace("transactions_", "").replace(".csv", "");
                        currentDate= LocalDate.parse(dateStr);

                        convertService.convertAndSave(downloadFile, currentDate);

                        reconciliationResultEntity = reconService.reconcile(currentDate);

                        System.out.println("doi soat thanh cong, ktra du lieu trc khi gui mail");
                    }else{
                        System.out.println(("chua tai duoc file, hay doi hoac kiem tra"));
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
                    reconciliationJob.shutdown();
                    DatabaseConfig.close();
                    System.exit(0);
                    break;
                case "4":
                    System.out.println("Nhập mã giao dịch:");
                    String transactionId = scanner.nextLine().trim();

                    if (transactionId.isEmpty()) {
                        System.out.println("Vui lòng nhập mã giao dịch");
                        break;
                    }

                    try {
                        String result = inquiryService.inquire(transactionId);
                        System.out.println(result);
                    } catch (RuntimeException e) {
                        System.out.println("Không thể kiểm tra giao dịch, vui lòng thử lại");
                    }
                    break;

                default:
                    System.out.println("lua chon khong hop le, hay nhap lai");
            }
        }
    }
}
