package org.example.request;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PaymentRequest {
    private String transactionId;
    private String transactionName;
    private String merchantId;
}