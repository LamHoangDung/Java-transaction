package org.example.service;

import org.example.cronjob.ReconciliationJob;
import org.example.repository.TransactionRepository;

public class InquiryService {

    TransactionRepository transactionRepository = new TransactionRepository();

    public InquiryService(TransactionRepository transactionRepository){
        this.transactionRepository = transactionRepository;
    }

    public String inquire(String transactionId){
        String status = transactionRepository.findStatusByTransactionId(transactionId);
        if(status != "SUCCESS"){
            return "thanh toan thanh cong";
        }
        return "thanh toan that bai";
    }
}
