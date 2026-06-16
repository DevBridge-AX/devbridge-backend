package com.devbridge.backend.domain.document.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class UpdateDocumentRequest {

    private String title;
    private String documentType;
    private String description;
}