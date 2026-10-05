package org.example.service;

import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvException;
import org.example.entity.PartnerTransactionEntity;
import org.example.repository.PartnerTransactionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class ConvertService {
    private static final Logger log = LoggerFactory.getLogger(ConvertService.class);
    private static final DateTimeFormatter FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final PartnerTransactionRepository partnerTransactionRepository;
    public ConvertService(PartnerTransactionRepository partnerTransactionRepository) {
        this.partnerTransactionRepository = partnerTransactionRepository;
    }


    public  void convertAndSave(File csvFile, LocalDate fileDate){
        log.info("bdau doc file");

        List<PartnerTransactionEntity> list = new ArrayList<>();

        //CSVReader la tool doc file csv lay tu thu vien opencsv
        try (CSVReader reader = new CSVReader(new FileReader(csvFile))) {
            List<String[]> allRows = reader.readAll(); //1 list các mảng string, mỗi mảng là 1 dòng dữ liệu giao dịch

            //bo qua dong thuoc tinh trong bang -> i=1
            for (int i = 1; i < allRows.size(); i++) {
                String[] row = allRows.get(i);

                PartnerTransactionEntity partnerTransactionEntity = new PartnerTransactionEntity();

                partnerTransactionEntity.setTransactionId(row[0].trim());
                partnerTransactionEntity.setStatus(row[1].trim());
                partnerTransactionEntity.setTransactionTime(LocalDateTime.parse(row[2].trim(), FORMATTER));
                partnerTransactionEntity .setMerchantId(row[3].trim());
                partnerTransactionEntity .setFileDate(fileDate);
                list.add(partnerTransactionEntity );
            }
            log.info("doc xong file csv");
        } catch (IOException | CsvException e) {
            log.error("loi doc file csv" + e.getMessage());
            throw new RuntimeException(e);
        }

        //luu vao Db
        partnerTransactionRepository.saveAll(list, fileDate);
    }
}
