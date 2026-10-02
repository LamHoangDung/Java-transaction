package org.example.repository;

import org.example.config.DatabaseConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.time.LocalDate;

public class TransactionRepository {
    private static final Logger log = LoggerFactory.getLogger(TransactionRepository.class);
    public int countByDate(LocalDate date){
        String query = "select count(*) from transactions where date(transaction_time) = ? AND status = 'SUCCESS'" ;

        try(Connection connection = DatabaseConfig.getConnection(); //mo ket noi toi database
            PreparedStatement statement = connection.prepareStatement(query)){
            statement.setDate(1, Date.valueOf(date));
            ResultSet resultSet = statement.executeQuery();
            if(resultSet.next()){
                return resultSet.getInt(1);
            }
        } catch (Exception e) {
            System.out.println("Loi countByDate: " + e.getMessage());
        }
        return 0;
    }
}
