package com.ssg9th2team.geharbang.domain.main.service;

import com.ssg9th2team.geharbang.domain.main.dto.ListDto;
import com.ssg9th2team.geharbang.domain.main.repository.MainRepository;
import com.ssg9th2team.geharbang.domain.accommodation.entity.Accommodation;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class BaseMainService implements MainService {

    private final MainRepository mainRepository;

    @Override
    public List<ListDto> findByTheme() {
//        return mainRepository.findAll().stream().map(Accommodation::toRead).toList();
        return mainRepository.findAll().stream().toList();
    }


}
