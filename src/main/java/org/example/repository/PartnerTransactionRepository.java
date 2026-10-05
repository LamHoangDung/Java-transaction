package org.example.repository;

import org.example.config.DatabaseConfig;
import org.example.entity.PartnerTransactionEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class PartnerTransactionRepository {
    private static final Logger log = LoggerFactory.getLogger(PartnerTransactionRepository.class);

    public void saveAll(List<PartnerTransactionEntity> list, LocalDate fileDate) {
        String query = """
                INSERT INTO partner_transactions
                    (transaction_id, status, transaction_time, merchant_id, file_date)
                VALUES (?, ?, ?, ?, ?)
                ON CONFLICT DO NOTHING
                """;
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement statement = conn.prepareStatement(query)) {
            for (PartnerTransactionEntity entity : list) {
                statement.setString(1, entity.getTransactionId());
                statement.setString(2, entity.getStatus());
                statement.setTimestamp(3, Timestamp.valueOf(entity.getTransactionTime()));
                statement.setString(4, entity.getMerchantId());
                statement.setDate(5, Date.valueOf(fileDate));


                statement.addBatch();
            }
            statement.executeBatch();
            log.info("da them giao dich doi tac xuong DB");
        } catch (Exception e) {
            log.error("loi insert vao bang partner_transactions:" + e.getMessage());
            throw new RuntimeException(e);
        }
    }

    public Set<String> countByFileDate(LocalDate fileDate) {
        String query = """
                SELECT transaction_id
                            FROM partner_transactions
                            WHERE file_date = ?
                              AND status = 'SUCCESS'
                """;
        Set<String> ids = new HashSet<>();
        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement statement = conn.prepareStatement(query)){
             statement.setDate(1, Date.valueOf(fileDate));
             try (ResultSet resultSet = statement.executeQuery()) {
                 while (resultSet.next()) {
                    ids.add(resultSet.getString("transaction_id"));
                 }
            }
            return ids;

        } catch (SQLException e) {
            log.error("Lỗi lấy giao dịch đối tác ngày {}", fileDate +e.getMessage());
            throw new RuntimeException(
                    "Không thể lấy giao dịch đối tác"+ e.getMessage()
            );
        }
    }

}
