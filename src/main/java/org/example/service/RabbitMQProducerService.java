package org.example.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.MessageProperties;
import org.example.config.AppConfig;
import org.example.entity.ReconciliationResultEntity;

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
        reportMessage.put(
                "reconciliationDate",
                result.getReconciliationDate().toString()
        );
        reportMessage.put("ourCount", result.getOurCount());
        reportMessage.put("partnerCount", result.getPartnerCount());
        reportMessage.put("bothHave", result.getBothHave());
        reportMessage.put("ourOnly", result.getOurOnly());
        reportMessage.put("partnerOnly", result.getPartnerOnly());
        byte[] messageBody = objectMapper.writeValueAsBytes(reportMessage);


        //Cấu hình kết nối RabbitMQ
        ConnectionFactory connectionFactory = new ConnectionFactory();
        connectionFactory.setHost(config.get("rabbitmq.host"));
        connectionFactory.setPort(config.getInt("rabbitmq.port"));
        connectionFactory.setUsername(config.get("rabbitmq.username"));
        connectionFactory.setPassword(config.get("rabbitmq.password"));

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
}