package org.example.entity;


import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@ToString
public class TransactionEntity {
    private Long id;
    private String transactionId;
    private String status;
    private LocalDateTime transactionTime;
    private String merchantId;
}
