package org.example.service;

import org.example.entity.ReconciliationReportEntity;
import org.example.entity.ReconciliationResultEntity;
import org.example.repository.PartnerTransactionRepository;
import org.example.repository.ReconciliationRepository;
import org.example.repository.TransactionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.sql.Connection;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Set;

public class ReconciliationService {
    private static final Logger log = LoggerFactory.getLogger(ReconciliationService.class);

    private final TransactionRepository transactionRepository;
    private final PartnerTransactionRepository partnerTransactionRepository;
    private final ReconciliationRepository reconciliationRepository;
    private final FileService fileService;
    private final ConvertService convertService;

    public ReconciliationService(TransactionRepository transactionRepository,
                                 PartnerTransactionRepository partnerTransactionRepository,
                                 ReconciliationRepository reconciliationRepository, FileService fileService,
                                 ConvertService convertService) {
        this.transactionRepository = transactionRepository;
        this.partnerTransactionRepository = partnerTransactionRepository;
        this.reconciliationRepository = reconciliationRepository;
        this.convertService = convertService;
        this.fileService = fileService;
    }

    public ReconciliationResultEntity processEncryptedFile(File encryptedFile){ //truyen vao file ma hoa vua tai ve local
        // Kiểm tra file đầu vào.
        if (encryptedFile == null || !encryptedFile.isFile()) {
            System.out.println("khong tim thay file ma hoa");
            return null;
        }

        Path restoredCsv = null;

        try {
            // 1/giai ma file csv vua tai
            restoredCsv = fileService.decrypt(
                    encryptedFile.toPath()
            );
            // 2/trich xuat ngay tu file csv da khoi phuc
            LocalDate fileDate = fileService.extractCsvDate(restoredCsv.getFileName().toString());

            // 3/ Doc csv va luu giao dich doi tac vao DB
            convertService.convertAndSave(
                    restoredCsv.toFile(),
                    fileDate
            );
            // 4/ chay doi soat: tra kq doi soat + luu DB
            return reconcile(fileDate);
        }catch (Exception e) {

            log.error("xu ly that bai processEncryptedFile: " + e.getMessage());
            return null;
        }  finally {
            // 5/ xoa file csv giai ma de tranh bi ngkhac vao doc duoc
            if (restoredCsv != null) {
                try {
                    Files.deleteIfExists(restoredCsv);
                } catch (IOException e) {
                    log.warn("Không xóa được CSV tạm: {}", restoredCsv, e);
                }
            }
        }
    }

    public ReconciliationResultEntity reconcile(LocalDate date) {
        Set<String> ourIds = transactionRepository.countByDate(date);

        Set<String> partnerIds = partnerTransactionRepository.countByFileDate(date);

        int bothHave = 0;
        int ourOnly = 0;
        int partnerOnly = 0;

        //duyet qua cac giao dich seccess ben minh neu ben kia cung co id tuong ung thi la -> bothhave
        for (String id : ourIds) {
            if (partnerIds.contains(id)) {
                bothHave++;
            } else {
                ourOnly++;
            }
        }

        //duyet qua cac giao dich success ben doi tac
        for (String id : partnerIds) {
            if (!ourIds.contains(id)) {
                partnerOnly++;
            }
        }

        ReconciliationResultEntity result =
                new ReconciliationResultEntity();

        result.setReconciliationDate(date);
        result.setOurCount(ourIds.size());
        result.setPartnerCount(partnerIds.size());
        result.setBothHave(bothHave);
        result.setOurOnly(ourOnly);
        result.setPartnerOnly(partnerOnly);

        reconciliationRepository.saveResult(result);

        log.info(
                "Đối soát ngày {}: bothHave={}, ourOnly={}, partnerOnly={}",
                date, bothHave, ourOnly, partnerOnly
        );

        return result;
    }

    public void saveReport(ReconciliationResultEntity reconciliationResultEntity, String toEmail){
        ReconciliationReportEntity report = new ReconciliationReportEntity();
        report.setReportDate(reconciliationResultEntity.getReconciliationDate());
        report.setBothHave(reconciliationResultEntity.getBothHave());
        report.setPartnerOnly(reconciliationResultEntity.getPartnerOnly());
        report.setOurOnly(reconciliationResultEntity.getOurOnly());
        report.setSento(toEmail);
        report.setSentAt(LocalDateTime.now());
        reconciliationRepository.saveReport(report);

    }
}

