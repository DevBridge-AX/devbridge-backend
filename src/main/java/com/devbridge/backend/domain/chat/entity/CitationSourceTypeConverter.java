package com.devbridge.backend.domain.chat.entity;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class CitationSourceTypeConverter implements AttributeConverter<CitationSourceType, String> {

    @Override
    public String convertToDatabaseColumn(CitationSourceType attribute) {
        if (attribute == null) {
            return null;
        }
        return attribute.name().toLowerCase();
    }

    @Override
    public CitationSourceType convertToEntityAttribute(String dbData) {
        if (dbData == null) {
            return null;
        }
        try {
            return CitationSourceType.valueOf(dbData.toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
