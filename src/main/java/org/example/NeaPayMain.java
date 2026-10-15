package org.example;

import org.example.config.AppConfig;
import org.example.config.DatabaseConfig;
import org.example.request.PaymentRequest;
import org.example.service.PaymentService;

import java.util.UUID;

public class NeaPayMain {

    public static void main(String[] args) {

        AppConfig config = new AppConfig();
        try {
            DatabaseConfig.init(config);
            PaymentRequest payment = new PaymentRequest();
            payment.setTransactionId("TXN555");
            payment.setTransactionName("Thanh toán hóa đơn thử nghiệm");
            payment.setMerchantId("MERCHANT_X");

            PaymentService service = new PaymentService();
            service.Pay(payment);


            System.out.println("thanh toan thanh cong");
            System.out.println("Ma GD: " + payment.getTransactionId());

        } catch (Exception e) {
            System.err.println("Loi: " + e.getMessage());
            e.printStackTrace();

        } finally {
            DatabaseConfig.close();
        }
    }
}