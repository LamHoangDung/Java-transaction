package org.example.entity;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.time.LocalDate;


@Setter
@Getter
@ToString
@NoArgsConstructor
public class ReconciliationResultEntity {
    private Long id;
    private LocalDate reconciliationDate;
    private int ourCount; //Tổng giao dịch bên mình
    private int partnerCount; //Tổng giao dịch been KH
    private int bothHave; //CASE 1
    private int partnerOnly; //CASE 2
    private int ourOnly; //CASE 3

}
