package com.ssg9th2team.geharbang.domain.accommodation.repository.jpa;

import com.ssg9th2team.geharbang.domain.accommodation.entity.Accommodation;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AccommodationJpaRepository extends JpaRepository<Accommodation, Long>, JpaSpecificationExecutor<Accommodation> {

    @Query("select a from Accommodation a where a.latitude is null or a.longitude is null")
    List<Accommodation> findMissingCoordinates(Pageable pageable);

    long countByLatitudeIsNullOrLongitudeIsNull();

}
