package org.example.config;

import org.example.cronjob.DownloadFileJob;
import org.example.cronjob.PaymentNotificationJob;
import org.example.repository.TransactionRepository;
import org.example.service.RabbitMQProducerService;
import org.example.service.SftpService;
import org.quartz.JobBuilder;
import org.quartz.JobDetail;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.SimpleScheduleBuilder;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;
import org.quartz.impl.StdSchedulerFactory;

public class QuartzSchedulerConfig {

    public static Scheduler beginDownloadFile(SftpService sftpService) throws SchedulerException {


        // Tạo scheduler
        Scheduler downloadFileScheduler = StdSchedulerFactory.getDefaultScheduler();

        // Cung cấp service cho DownloadFileJob
        downloadFileScheduler.getContext().put("sftpService", sftpService);

        // chi dinh class thuc hien job va dat ten cho job
        JobDetail downloadFileJobDetail = JobBuilder
                .newJob(DownloadFileJob.class)
                .withIdentity("downloadFileJob", "sftp")
                .build();

        // chay lan dau ngay lap tuc, sau do moi 120s
        Trigger downloadFileTrigger = TriggerBuilder
                .newTrigger()
                .withIdentity("downloadFileTrigger", "sftp")
                .startNow()
                .withSchedule(
                        SimpleScheduleBuilder.simpleSchedule()
                                .withIntervalInSeconds(120)
                                .repeatForever()
                )
                .build();

        // Đăng ki job va kich hoat trigger, scheduler tự động vào DownloadFileJob.class và chạy execute()
        downloadFileScheduler.scheduleJob(downloadFileJobDetail, downloadFileTrigger);

        // Khởi động scheduler
        downloadFileScheduler.start();
        return downloadFileScheduler;
    }

    public static Scheduler beginPaymentNotification(TransactionRepository repository, RabbitMQProducerService producer) throws SchedulerException {

        Scheduler scheduler = StdSchedulerFactory.getDefaultScheduler();
        scheduler.getContext().put("transactionRepository", repository);
        scheduler.getContext().put("rabbitMQProducerService", producer);


        JobDetail job = JobBuilder
                .newJob(PaymentNotificationJob.class)
                .withIdentity("paymentNotificationJob", "payment")
                .build();

        Trigger trigger = TriggerBuilder
                .newTrigger()
                .withIdentity("paymentNotificationTrigger", "payment")
                .startNow()
                .withSchedule(
                        SimpleScheduleBuilder.simpleSchedule()
                                .withIntervalInSeconds(5)
                                .repeatForever()
                )
                .build();

        scheduler.scheduleJob(job, trigger);
        scheduler.start();

        return scheduler;
    }
}