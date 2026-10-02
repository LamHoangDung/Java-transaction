package org.example;

import org.example.config.AppConfig;
import org.example.config.DatabaseConfig;
import org.example.repository.TransactionRepository;

import java.sql.Connection;
import java.time.LocalDate;

public class Main {
    public static void main(String[] args) {
        //khoi tao cau hinh va ket noi
        AppConfig config = new AppConfig();
        DatabaseConfig.init(config);

        TransactionRepository transactionRepository = new TransactionRepository();
        int count = 0;
        count = transactionRepository.countByDate(LocalDate.of(2024, 10, 1));
        System.out.println("so giao dich la: " + count);
    }

}
