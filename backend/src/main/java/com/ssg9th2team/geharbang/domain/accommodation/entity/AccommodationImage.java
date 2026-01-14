package com.ssg9th2team.geharbang.domain.accommodation.entity;

import jakarta.persistence.*;
import lombok.*;

/**
 * 숙소 이미지 엔티티
 * 
 * 이 엔티티는 accommodation_image 테이블과 매핑됩니다.
 * Native SQL 쿼리에서 이 테이블을 참조하므로, 테스트 환경(H2)에서
 * 테이블이 자동 생성되도록 JPA 엔티티로 정의합니다.
 */
@Entity
@Table(name = "accommodation_image", indexes = {
        @Index(name = "idx_accommodation_image_acc_sort", columnList = "accommodations_id, sort_order"),
        @Index(name = "idx_accommodation_image_type", columnList = "accommodations_id, image_type, sort_order")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class AccommodationImage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "image_id")
    private Long imageId;

    @Column(name = "accommodations_id", nullable = false)
    private Long accommodationsId;

    @Column(name = "image_url", columnDefinition = "LONGTEXT", nullable = false)
    private String imageUrl;

    @Enumerated(EnumType.STRING)
    @Column(name = "image_type", nullable = false)
    private ImageType imageType;

    @Column(name = "sort_order", nullable = false)
    private Integer sortOrder;

    /**
     * 이미지 타입 enum
     */
    public enum ImageType {
        banner,
        detail
    }
}
