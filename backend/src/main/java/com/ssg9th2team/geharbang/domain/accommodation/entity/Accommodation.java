package com.ssg9th2team.geharbang.domain.accommodation.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "accommodation")
@Getter
@NoArgsConstructor
public class Accommodation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "accommodations_id")
    private Long id;

    @Column(name = "accommodations_name")
    private String name;

    @Column(name = "accommodations_description")
    private String description;

    @Column(name = "short_description")
    private String shortDescription;

    @Column(name = "city")
    private String city;

    @Column(name = "district")
    private String district;

    @Column(name = "township")
    private String township;

    @Column(name = "address_detail")
    private String addressDetail;

    @Column(name = "phone")
    private String phone;

    @Column(name = "created_at")
    private LocalDateTime createdAt;
}
