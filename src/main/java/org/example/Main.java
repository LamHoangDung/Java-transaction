package org.example;

import org.example.config.AppConfig;
import org.example.config.DatabaseConfig;
import org.example.config.QuartzSchedulerConfig;
import org.example.entity.ReconciliationResultEntity;
import org.example.repository.PartnerTransactionRepository;
import org.example.repository.ReconciliationRepository;
import org.example.repository.TransactionRepository;
import org.example.service.*;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.File;
import java.security.GeneralSecurityException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.time.LocalDate;
import java.util.Scanner;


public class Main {
    private static final Logger log = LoggerFactory.getLogger(Main.class);
    private static LocalDate currentDate = null;
    private static ReconciliationResultEntity reconciliationResultEntity = null;
    private static File downloadFile = null;


    public static void main(String[] args) throws SchedulerException {


        //khoi tao cau hinh va ket noi
        AppConfig config = new AppConfig();

        RsaKeyService rsaKeyService = new RsaKeyService(config);

        try{
            PublicKey publicKey = rsaKeyService.loadPublicKey();
            PrivateKey privateKey = rsaKeyService.loadPrivateKey();

            log.info("da doc duoc 2 key");
        } catch (GeneralSecurityException e) {
            log.info("loi khong doc duoc Key: " + e.getMessage());
            throw new RuntimeException(e);
        }

        //ket noi database
        DatabaseConfig.init(config);

        TransactionRepository transactionRepository = new TransactionRepository();
        ReconciliationRepository reconciliationRepository = new ReconciliationRepository();
        PartnerTransactionRepository partnerTransactionRepository = new PartnerTransactionRepository();
        FileService fileService = new FileService(rsaKeyService,config);
        SftpService sftpService = new SftpService(config);
        ConvertService convertService = new ConvertService(partnerTransactionRepository);
        ReconciliationService reconService = new ReconciliationService(transactionRepository,
                partnerTransactionRepository, reconciliationRepository, fileService,convertService );
        InquiryService inquiryService = new InquiryService(transactionRepository);
        RabbitMQProducerService rabbitMQProducerService = new RabbitMQProducerService(config);

        Scheduler downloadFileScheduler = QuartzSchedulerConfig.begin(sftpService);

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
                    downloadFile = (File) downloadFileScheduler.getContext().get("downloadedFile");
                    reconciliationResultEntity = reconService.processEncryptedFile(downloadFile);
                    if (reconciliationResultEntity == null) {
                        System.out.println("Đối soát thất bại");
                        break;
                    }
                    System.out.println("Đối soát thành công");

                    try {
                        rabbitMQProducerService.sendReport(reconciliationResultEntity);
                        System.out.println("Đã gửi yêu cầu mail lên RabbitMQ");
                    } catch (Exception e) {
                        log.error("Không gửi được yêu cầu mail lên RabbitMQ", e);

                        System.out.println(
                                "Đối soát thành công nhưng gửi yêu cầu mail thất bại"
                        );
                    }
                    break;
                case "2":
                    System.out.println("thoat chuong trinh");
                    downloadFileScheduler.shutdown();
                    DatabaseConfig.close();
                    System.exit(0);
                    break;
                case "3":
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
