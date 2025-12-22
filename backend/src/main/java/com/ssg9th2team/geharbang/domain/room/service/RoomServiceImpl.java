package com.ssg9th2team.geharbang.domain.room.service;

import com.ssg9th2team.geharbang.domain.room.dto.RoomCreateDto;
import com.ssg9th2team.geharbang.domain.room.dto.RoomResponseDto;
import com.ssg9th2team.geharbang.domain.room.dto.RoomUpdateDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RoomServiceImpl implements RoomService {

    @Override
    public Long createRoom(Long accommodationsId, RoomCreateDto createDto) {
        return null;
    }

    @Override
    public RoomResponseDto getRoom(Long accommodationsId, Long roomId) {
        return null;
    }

    @Override
    public void updateRoom(Long accommodationsId, Long roomId, RoomUpdateDto updateDto) {

    }

    @Override
    public void deleteRoom(Long accommodationsId, Long roomId) {

    }
}
   