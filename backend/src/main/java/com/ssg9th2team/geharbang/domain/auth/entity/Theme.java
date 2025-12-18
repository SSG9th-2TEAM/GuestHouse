package com.ssg9th2team.geharbang.domain.auth.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "theme")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Theme {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "theme_id")
    private Long id;

    @Column(name = "theme_category", nullable = false, length = 50)
    private String themeCategory;

    @Column(name = "theme_name", nullable = false, unique = true, length = 50)
    private String themeName;

    @Builder
    public Theme(String themeCategory, String themeName) {
        this.themeCategory = themeCategory;
        this.themeName = themeName;
    }
}
