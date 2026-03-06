package com.revhire.application.dto;

import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class WithdrawRequest {
    
    @Size(max = 500, message = "Withdrawal reason cannot exceed 500 characters")
    private String reason;
}