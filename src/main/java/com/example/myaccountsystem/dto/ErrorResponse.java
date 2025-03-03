package com.example.myaccountsystem.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ErrorResponse {
    private String errorCode;
    private String errorMessage;
}
