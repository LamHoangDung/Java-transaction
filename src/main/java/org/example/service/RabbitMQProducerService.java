package org.example.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.MessageProperties;
import org.example.config.AppConfig;
import org.example.config.RabbitMQConfig;
import org.example.entity.ReconciliationResultEntity;
import org.example.entity.TransactionEntity;

public class RabbitMQProducerService {

    private final AppConfig config;
    private final ObjectMapper objectMapper = new ObjectMapper();
    public RabbitMQProducerService(AppConfig config) {
        this.config = config;
    }

    public void sendReport(ReconciliationResultEntity result)
            throws Exception {

        // Tạo JSON chứa người nhận và số liệu báo cáo
        ObjectNode reportMessage = objectMapper.createObjectNode();

        reportMessage.put("toEmail", config.get("mail.to"));
        reportMessage.put("reconciliationDate", result.getReconciliationDate().toString());
        reportMessage.put("ourCount", result.getOurCount());
        reportMessage.put("partnerCount", result.getPartnerCount());
        reportMessage.put("bothHave", result.getBothHave());
        reportMessage.put("ourOnly", result.getOurOnly());
        reportMessage.put("partnerOnly", result.getPartnerOnly());
        byte[] messageBody = objectMapper.writeValueAsBytes(reportMessage);


        //Cấu hình kết nối RabbitMQ
        ConnectionFactory connectionFactory = RabbitMQConfig.createConnectionFactory(config);
        String reportEmailQueue = "reconciliation.report.email.queue";
        String notificationExchange = "notification.exchange";
        String reportEmailKey = "reconciliation.report.email";


        //Mở connection và channel
        try (Connection connection = connectionFactory.newConnection();
             Channel channel = connection.createChannel()) {

            // Tạo direct exchange
            channel.exchangeDeclare(
                    notificationExchange,
                    "direct",
                    true
            );

            //Tạo queue
            channel.queueDeclare(
                    reportEmailQueue,
                    true,
                    false,
                    false,
                    null
            );

            // Nối queue với exchange bằng key "report.email", đây gọi là 1 binding - sự kết nối 1 queue và 1 exchange = 1 key
            channel.queueBind(
                    reportEmailQueue,
                    notificationExchange,
                    reportEmailKey
            );

            //Bật xác nhận từ broker
            channel.confirmSelect();

            //Gửi message với cùng key "report.email" để khớp key mà binding đã khai báo ở trên
            channel.basicPublish(
                    notificationExchange,
                    reportEmailKey,
                    MessageProperties.PERSISTENT_TEXT_PLAIN,
                    messageBody
            );
            //Chờ broker xác nhận, tối đa 5 giây
            channel.waitForConfirmsOrDie(5000);
        }
    }

    public void sendPaymentNotification(TransactionEntity transaction) throws Exception {

        ObjectNode message = objectMapper.createObjectNode();

        message.put("transactionId", transaction.getTransactionId());
        message.put("toEmail", config.get("mail.to"));
        message.put("transactionTime", transaction.getTransactionTime().toString());

        byte[] messageBody = objectMapper.writeValueAsBytes(message);

        ConnectionFactory connectionFactory = RabbitMQConfig.createConnectionFactory(config);

        String exchange = "notification.exchange";
        String queue = "payment.notification.queue";
        String routingKey = "payment.notification";

        try (Connection connection = connectionFactory.newConnection();
             Channel channel = connection.createChannel()) {


            channel.exchangeDeclare(exchange, "direct", true);
            channel.queueDeclare(queue, true, false, false, null);
            channel.queueBind(queue, exchange, routingKey);
            channel.confirmSelect();
            channel.basicPublish(
                    exchange,
                    routingKey,
                    MessageProperties.PERSISTENT_TEXT_PLAIN,
                    messageBody
            );

            channel.waitForConfirmsOrDie(5000);
        }
    }
}