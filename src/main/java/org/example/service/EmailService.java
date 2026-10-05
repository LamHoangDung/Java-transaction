package org.example.service;

import jakarta.mail.*;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import org.example.config.AppConfig;
import org.example.entity.ReconciliationReportEntity;
import org.example.entity.ReconciliationResultEntity;
import org.example.repository.ReconciliationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.Properties;

public class EmailService {
    private static final Logger log = LoggerFactory.getLogger(EmailService.class);
    private final AppConfig config;
    ;
    private final ReconciliationService reconciliationService;

    public EmailService(AppConfig config, ReconciliationService reconciliationService) {
        this.config = config;
        this.reconciliationService = reconciliationService;
    }


    public void sendReport(ReconciliationResultEntity reconciliationResultEntity){
        String smtpHost = config.get("mail.smtp.host");
        String smtpPort = config.get("mail.smtp.port");
        String username = config.get("mail.username");
        String password = config.get("mail.password");
        String fromEmail = config.get("mail.from");
        String toEmail = config.get("mail.to");


        //cau hinh smtp de gui mail
        Properties props = new Properties();
        props.put("mail.smtp.auth", "true");           // Yêu cầu đăng nhập
        props.put("mail.smtp.starttls.enable", "true"); // Bật mã hóa TLS cho bảo mật
        props.put("mail.smtp.host", smtpHost);          // Server gửi mail (smtp.gmail.com)
        props.put("mail.smtp.port", smtpPort);          // Cổng 587

        //dang nhap gmail bang ten mail va app password
        Session session = Session.getInstance(props, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(username, password);
            }
        });


        try {
            //Soạn nội dung email
            MimeMessage message = new MimeMessage(session);
            message.setFrom(new InternetAddress(fromEmail));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(toEmail));
            message.setSubject("Bao cao doi soat ngay " + reconciliationResultEntity.getReconciliationDate());

            //tao form html
            String htmlContent = buildHtmlReport(reconciliationResultEntity);
            message.setContent(htmlContent, "text/html; charset=UTF-8");


            //guimail
            Transport.send(message);
            log.info("Gui email thanh cong den: {}", toEmail);



            // 6. Lưu log gửi mail vào bảng reconciliation_reports
            reconciliationService.saveReport(reconciliationResultEntity,toEmail);


        } catch (MessagingException e) {
            log.error("Loi gui email: {}", e.getMessage(), e);
            throw new RuntimeException(e);
        }
    }

    private String buildHtmlReport(ReconciliationResultEntity reconciliationResultEntity) {
        return """
                <html>
                <body style="font-family: Arial, sans-serif; padding: 20px;">
                    <h2 style="color: #2c3e50;">Bao cao doi soat giao dich QR</h2>
                    <p>Ngay doi soat: <strong>%s</strong></p>
                    <table border="1" cellpadding="10" cellspacing="0"
                           style="border-collapse: collapse; width: 500px;">
                        <tr style="background-color: #3498db; color: white;">
                            <th>Chi tieu</th>
                            <th>So luong</th>
                        </tr>
                        <tr>
                            <td>Giao dich ben minh (SUCCESS)</td>
                            <td style="text-align: center;"><strong>%d</strong></td>
                        </tr>
                        <tr style="background-color: #f2f2f2;">
                            <td>Giao dich doi tac (SUCCESS)</td>
                            <td style="text-align: center;"><strong>%d</strong></td>
                        </tr>
                        <tr>
                            <td style="color: green;">Ca 2 ben deu co</td>
                            <td style="text-align: center; color: green;"><strong>%d</strong></td>
                        </tr>
                        <tr style="background-color: #f2f2f2;">
                            <td style="color: orange;">Doi tac co, minh khong co</td>
                            <td style="text-align: center; color: orange;"><strong>%d</strong></td>
                        </tr>
                        <tr>
                            <td style="color: red;">Minh co, doi tac khong co</td>
                            <td style="text-align: center; color: red;"><strong>%d</strong></td>
                        </tr>
                    </table>
                    <br>
                    <p style="color: gray; font-size: 12px;">Email duoc gui tu dong boi he thong doi soat QR.</p>
                </body>
                </html>
                """.formatted(
                reconciliationResultEntity.getReconciliationDate(),
                reconciliationResultEntity.getOurCount(),
                reconciliationResultEntity.getPartnerCount(),
                reconciliationResultEntity.getBothHave(),
                reconciliationResultEntity.getPartnerOnly(),
                reconciliationResultEntity.getOurOnly()
        );
    }

}
