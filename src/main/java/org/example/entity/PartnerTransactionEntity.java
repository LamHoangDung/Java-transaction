package org.example.entity;


import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.time.LocalDate;
import java.time.LocalDateTime;


@Setter
@Getter
@NoArgsConstructor
@ToString
public class PartnerTransactionEntity {
    private Long id;
    private String transactionId;
    private String status;
    private LocalDateTime transactionTime;
    private String merchantId;
    private LocalDate fileDate;
}
