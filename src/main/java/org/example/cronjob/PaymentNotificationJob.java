package org.example.cronjob;

import org.example.entity.TransactionEntity;
import org.example.repository.TransactionRepository;
import org.example.service.RabbitMQProducerService;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.SchedulerContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

@DisallowConcurrentExecution
public class PaymentNotificationJob implements Job {

    private static final Logger log = LoggerFactory.getLogger(PaymentNotificationJob.class);

    @Override
    public void execute(JobExecutionContext context){

        try {
            // Lấy các đối tượng mà Scheduler cung cấp cho Job.
            SchedulerContext schedulerContext = context.getScheduler().getContext();

            TransactionRepository repository = (TransactionRepository) schedulerContext.get("transactionRepository");
            RabbitMQProducerService rabbitMQProducerService = (RabbitMQProducerService) schedulerContext.get("rabbitMQProducerService");

            if (repository == null || rabbitMQProducerService == null) {
                System.out.println("Chưa cung cấp repository hoặc producer cho Job");
                return;
            }

            // Tìm những giao dịch chưa gửi mail thành công.
            List<TransactionEntity> transactions = repository.findPendingTransaction();

            for (TransactionEntity transaction : transactions) {
                try {
                    rabbitMQProducerService.sendPaymentNotification(transaction);

                    log.info(
                            "Đã gửi thông báo lên RabbitMQ: {}",
                            transaction.getTransactionId()
                    );

                } catch (Exception e) {
                    log.error(
                            "Không gửi được thông báo giao dịch: {}",
                            transaction.getTransactionId(),
                            e
                    );
                }
            }

        } catch (Exception e) {
            log.error("loi thong bao payment: " +e.getMessage());
        }
    }
}