package com.devbridge.backend.domain.chat.entity;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class FeedbackTypeConverter implements AttributeConverter<FeedbackType, String> {

    @Override
    public String convertToDatabaseColumn(FeedbackType attribute) {
        if (attribute == null) {
            return "none";
        }
        return attribute.name().toLowerCase();
    }

    @Override
    public FeedbackType convertToEntityAttribute(String dbData) {
        if (dbData == null) {
            return FeedbackType.NONE;
        }
        try {
            return FeedbackType.valueOf(dbData.toUpperCase());
        } catch (IllegalArgumentException e) {
            return FeedbackType.NONE;
        }
    }
}
