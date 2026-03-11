package com.revhire.employer.dto;

import lombok.Data;
import java.util.List;

@Data
public class BulkActionDTO {

    private List<Long> applicationIds;
    private String comment;
}