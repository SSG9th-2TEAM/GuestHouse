package com.ssg9th2team.geharbang.domain.auth.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "user_theme")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserTheme {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "theme_id", nullable = false)
    private Theme theme;

    @Builder
    public UserTheme(User user, Theme theme) {
        this.user = user;
        this.theme = theme;
    }
}
