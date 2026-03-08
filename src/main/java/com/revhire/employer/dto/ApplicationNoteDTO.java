package com.revhire.employer.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor; 

@Data 
@Builder
@NoArgsConstructor
@AllArgsConstructor 
public class ApplicationNoteDTO {
    private Long id;
    private String note;
    private String authorName;
    private String formattedDate; 
}