package org.example.config;

import com.rabbitmq.client.ConnectionFactory;

public class RabbitMQConfig {

    public static ConnectionFactory createConnectionFactory(AppConfig config) {
        ConnectionFactory factory = new ConnectionFactory();

        factory.setHost(config.get("rabbitmq.host"));
        factory.setPort(config.getInt("rabbitmq.port"));
        factory.setUsername(config.get("rabbitmq.username"));
        factory.setPassword(config.get("rabbitmq.password"));

        return factory;
    }
}