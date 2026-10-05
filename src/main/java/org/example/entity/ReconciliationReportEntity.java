package org.example.entity;


import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.time.LocalDate;
import java.time.LocalDateTime;


@Setter
@Getter
@ToString
@NoArgsConstructor
public class ReconciliationReportEntity {
    private Long id;
    private LocalDate reportDate;
    private int bothHave;
    private int ourOnly;
    private int partnerOnly;
    private String sento;
    private LocalDateTime sentAt;
}
