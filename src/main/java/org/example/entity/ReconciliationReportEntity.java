package org.example.entity;


import java.time.LocalDate;
import java.time.LocalDateTime;

public class ReconciliationReportEntity {
    private Long id;
    private LocalDate reportDate;
    private int bothHave;
    private int ourOnly;
    private int partnerOnly;
    private String sento;
    private LocalDateTime sentAt;
}
