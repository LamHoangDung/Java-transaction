package org.example.repository;

import org.example.config.DatabaseConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;

public class TransactionRepository {
    private static final Logger log = LoggerFactory.getLogger(TransactionRepository.class);
    public Set<String> countByDate(LocalDate date){
        String query = "SELECT transaction_id\n" +
                "            FROM transactions\n" +
                "            WHERE DATE(transaction_time) = ?\n" +
                "              AND status = 'SUCCESS'" ;


        Set<String> ids = new HashSet<>();
        try(Connection connection = DatabaseConfig.getConnection(); //mo ket noi toi database
            PreparedStatement statement = connection.prepareStatement(query)){
            statement.setDate(1, Date.valueOf(date));
            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    ids.add(rs.getString("transaction_id"));
                }
            }
            return ids;
        } catch (SQLException e) {
            log.error("gap loi khi lay giao dich ngay: {}", date + e.getMessage());
            throw new RuntimeException(
                    "Không thể lấy giao dịch bên mình" + e.getMessage()
            );
        }
    }
}
