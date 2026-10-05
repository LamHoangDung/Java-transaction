package org.example.repository;

import org.example.config.DatabaseConfig;
import org.example.entity.ReconciliationReportEntity;
import org.example.entity.ReconciliationResultEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;

public class ReconciliationRepository {
    private static final Logger log = LoggerFactory.getLogger(ReconciliationRepository.class);
    public void saveResult(ReconciliationResultEntity result){
        String query = "INSERT INTO reconciliation_results \n" +
                "                    (reconciliation_date, our_count, partner_count, both_have, partner_only, our_only)\n" +
                "                VALUES (?, ?, ?, ?, ?, ?)\n" +
                "                ON CONFLICT (reconciliation_date) \n" +
                "                DO UPDATE SET \n" +
                "                    our_count = EXCLUDED.our_count,\n" +
                "                    partner_count = EXCLUDED.partner_count,\n" +
                "                    both_have = EXCLUDED.both_have,\n" +
                "                    partner_only = EXCLUDED.partner_only,\n" +
                "                    our_only = EXCLUDED.our_only,\n" +
                "                    created_at = NOW()";
        try(Connection conn = DatabaseConfig.getConnection();
            PreparedStatement statement = conn.prepareStatement(query)){

            statement.setDate(1, Date.valueOf(result.getReconciliationDate()));
            statement.setInt(2, result.getOurCount());
            statement.setInt(3,result.getPartnerCount());
            statement.setInt(4,result.getBothHave());
            statement.setInt(5,result.getPartnerOnly());
            statement.setInt(6, result.getOurOnly());

            statement.executeUpdate();
            log.info("luu thanh cong kq doi soat ngay {} vào DB", result.getReconciliationDate());
            ;
        } catch (Exception e) {
            log.error("loi luu ket qua doi soat: " + e.getMessage());
            throw new RuntimeException(e);
        }
    }

    public void saveReport(ReconciliationReportEntity report){
        String query = "INSERT INTO reconciliation_reports" +
                "(report_date, both_have, partner_only, our_only,sent_to,sent_at)" +
                "VALUES(?, ?, ?, ?, ?, ?)";
        try(Connection conn = DatabaseConfig.getConnection();
            PreparedStatement statement = conn.prepareStatement(query)){

            statement.setDate(1,Date.valueOf(report.getReportDate()));
            statement.setInt(2,report.getBothHave());
            statement.setInt(3, report.getPartnerOnly());
            statement.setInt(4,report.getOurOnly());
            statement.setString(5,report.getSento());
            statement.setTimestamp(6, Timestamp.valueOf(report.getSentAt()));

            statement.executeUpdate();
            log.info("da luu log gui toi mail ngay {} vao", report.getReportDate());
        } catch (Exception e) {
            log.error("loi luu log gui mail {}", e.getMessage(), e);
            throw new RuntimeException(e);
        }
    }


}
