package com.ssg9th2team.geharbang.domain.accommodation.controller;

import com.ssg9th2team.geharbang.domain.accommodation.dto.AccommodationCreateRequestDto;
import com.ssg9th2team.geharbang.domain.accommodation.dto.AccommodationResponseDto;
import com.ssg9th2team.geharbang.domain.accommodation.dto.AccommodationUpdateRequestDto;
import com.ssg9th2team.geharbang.domain.accommodation.service.AccommodationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.java.Log;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/accommodations")
@RequiredArgsConstructor
public class AccommodationController {

    private final AccommodationService accommodationService;

    // 숙소 등록
    @PostMapping
    public ResponseEntity<?> createAccommodation(@Valid @RequestBody AccommodationCreateRequestDto requestDto) {
        try {
            Long userId = 1L; // 나중에 유저 api가져오기
            Long accommodationsId = accommodationService.createAccommodation(userId, requestDto);
            return ResponseEntity.status(HttpStatus.CREATED).body(accommodationsId);
        } catch (Exception e) {
            e.printStackTrace(); // 서버 콘솔에 에러 출력
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error: " + e.getMessage());
        }
    }


    // 숙소 상세조회
    @GetMapping("/{accommodationsId}")
    public ResponseEntity<?> getAccommodation(@PathVariable Long accommodationsId) {
        try {
            AccommodationResponseDto response = accommodationService.getAccommodation(accommodationsId);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error: " + e.getMessage());
        }
    }

    // 숙소 수정
    @PutMapping("/{accommodationsId}")
    public ResponseEntity<?> updateAccommodation(
            @PathVariable Long accommodationsId,
            @RequestBody AccommodationUpdateRequestDto requestDto) {
        try {
            accommodationService.updateAccommodation(accommodationsId, requestDto);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error: " + e.getMessage());
        }
    }

    // 숙소 삭제
    @DeleteMapping("/{accommodationsId}")
    public ResponseEntity<?> deleteAccommodation(@PathVariable Long accommodationsId) {
        try {
            accommodationService.deleteAccommodation(accommodationsId);
            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error: " + e.getMessage());
        }
    }
}
