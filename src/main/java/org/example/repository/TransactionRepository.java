package org.example.repository;

import org.example.config.DatabaseConfig;
import org.example.request.PaymentRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class TransactionRepository {
    private static final Logger log = LoggerFactory.getLogger(TransactionRepository.class);

    public List<String> countByDate(LocalDate date){
        String query = "SELECT transaction_id\n" +
                "            FROM transactions\n" +
                "            WHERE DATE(transaction_time) = ?\n" +
                "              AND status = 'SUCCESS'" ;


        List<String> ids = new ArrayList<>();
        try(Connection connection = DatabaseConfig.getConnection(); //mo ket noi toi database
            PreparedStatement statement = connection.prepareStatement(query)){
            statement.setDate(1, Date.valueOf(date));
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    ids.add(resultSet.getString("transaction_id"));
                }
            }
            return ids;
        } catch (SQLException e) {
            log.error("gap loi khi lay giao dich ngay: {}", date + e.getMessage());
        }
        return null;
    }


    public String findStatusByTransactionId(String transactionId){
        String query = "Select status from transactions where transaction_id = ?";

        try(Connection connection = DatabaseConfig.getConnection();
            PreparedStatement statement = connection.prepareStatement(query)) {

            statement.setString(1,transactionId);

            try (ResultSet resultSet = statement.executeQuery()) {
                if(resultSet.next()){
                    return resultSet.getString("status");
                }
                return null;
            }

        } catch (Exception e) {
            log.error("loi khi truy van giao dich: " + e.getMessage());
        }
        return null;
    }

    public int insertSuccess(PaymentRequest paymentRequest){
        String sql = """
            INSERT INTO transactions (
                transaction_id,
                transaction_name,
                transaction_time,
                merchant_id,
                status,
                noti_status
            )
            VALUES (?, ?, ?, ?, 'SUCCESS', 'PENDING')
            """;

        try(Connection connection = DatabaseConfig.getConnection();
            PreparedStatement statement = connection.prepareStatement(sql)){
            statement.setString(1, paymentRequest.getTransactionId());
            statement.setString(2, paymentRequest.getTransactionName());
            statement.setTimestamp(3, Timestamp.valueOf(LocalDateTime.now()));
            statement.setString(4, paymentRequest.getMerchantId());

            return statement.executeUpdate();
        } catch (Exception e) {
            log.error("loi khong insert duoc giao dich vao DB: " + e.getMessage());

        }
        return 0;
    }
}

