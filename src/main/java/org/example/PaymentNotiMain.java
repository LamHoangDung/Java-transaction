package org.example;

import org.example.config.AppConfig;
import org.example.config.DatabaseConfig;
import org.example.config.QuartzSchedulerConfig;
import org.example.repository.TransactionRepository;
import org.example.service.RabbitMQProducerService;
import org.quartz.Scheduler;

import java.util.Scanner;

public class PaymentNotiMain {
    public static void main(String[] args){
        AppConfig appConfig = new AppConfig();
        DatabaseConfig.init(appConfig);
        TransactionRepository transactionRepository = new TransactionRepository();
        RabbitMQProducerService rabbitMQProducerService = new RabbitMQProducerService(appConfig);
        try {
            Scheduler scheduler = QuartzSchedulerConfig.beginPaymentNotification(transactionRepository, rabbitMQProducerService);
            System.out.println("nhan enter de dung");
            new Scanner(System.in).nextLine();
        }catch (Exception e){

        }
    }
}
