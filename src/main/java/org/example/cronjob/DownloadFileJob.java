package org.example.cronjob;

import org.example.service.SftpService;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.SchedulerContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;

@DisallowConcurrentExecution
public class DownloadFileJob implements Job {

    private static final Logger log =
            LoggerFactory.getLogger(DownloadFileJob.class);

    @Override
    // execute() là phuong thuc dc quy đình trong interface job cua Quartz, sẽ tự động được gọi khi đăng ki job và kich hoat trigger bên quartzschedulerconfig
    public void execute(JobExecutionContext context)
            throws JobExecutionException {

        try {
            // Lấy vùng dữ liệu dùng chung của scheduler
            SchedulerContext schedulerContext = context.getScheduler().getContext();

            // Lấy service duoc cung cap ben file QuartzSchedulerConfig
            SftpService sftpService = (SftpService) schedulerContext.get("sftpService");

            if (sftpService == null) {
                throw new IllegalStateException(
                        "chua cung cap sftp service cho scheduler"
                );
            }

            log.info("Quartz bắt đầu tải file SFTP");
            File downloadedFile = sftpService.downloadFile();
            if (downloadedFile == null || !downloadedFile.isFile()) {
                log.warn("khong tai duoc file sftp");
                return;
            }

            //luu lai duong dan file
            schedulerContext.put("downloadedFile", downloadedFile);

            log.info(
                    "quartz tai file thanh cong: {}",
                    downloadedFile.getAbsolutePath()
            );

        } catch (Exception e) {
            log.error("loi tai file trong downloadfileJob", e);
            throw new JobExecutionException(e);
        }
    }
}