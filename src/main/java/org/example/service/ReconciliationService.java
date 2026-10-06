package org.example.service;

import org.example.entity.ReconciliationReportEntity;
import org.example.entity.ReconciliationResultEntity;
import org.example.repository.PartnerTransactionRepository;
import org.example.repository.ReconciliationRepository;
import org.example.repository.TransactionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Set;

public class ReconciliationService {
    private static final Logger log = LoggerFactory.getLogger(ReconciliationService.class);

    private final TransactionRepository transactionRepository;
    private final PartnerTransactionRepository partnerTransactionRepository;
    private final ReconciliationRepository reconciliationRepository;

    public ReconciliationService(TransactionRepository transactionRepository,
                                 PartnerTransactionRepository partnerTransactionRepository,
                                 ReconciliationRepository reconciliationRepository) {
        this.transactionRepository = transactionRepository;
        this.partnerTransactionRepository = partnerTransactionRepository;
        this.reconciliationRepository = reconciliationRepository;
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

