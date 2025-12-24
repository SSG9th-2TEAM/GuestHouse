package com.ssg9th2team.geharbang.domain.user.converter;

import com.ssg9th2team.geharbang.domain.user.entity.Gender;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.util.stream.Stream;

@Converter(autoApply = true)
public class GenderConverter implements AttributeConverter<Gender, String> {

    @Override
    public String convertToDatabaseColumn(Gender gender) {
        if (gender == null) {
            return null;
        }
        return gender.getDescription();
    }

    @Override
    public Gender convertToEntityAttribute(String dbData) {
        if (dbData == null) {
            return null;
        }

        return Stream.of(Gender.values())
                .filter(g -> g.getDescription().equals(dbData))
                .findFirst()
                .orElseThrow(IllegalArgumentException::new);
    }
}