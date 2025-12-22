package com.ssg9th2team.geharbang.domain.room.repository.mybatis;

import com.ssg9th2team.geharbang.domain.room.dto.RoomCreateDto;
import com.ssg9th2team.geharbang.domain.room.dto.RoomResponseDto;
import com.ssg9th2team.geharbang.domain.room.entity.Room;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface RoomMapper {

    void insertRooms(@Param("accommodationsId") Long accommodationsId,  // 객실 등록
                     @Param("rooms") List<RoomCreateDto> rooms);

    RoomResponseDto selectRoomById(@Param("accommodationsId") Long accommodationsId,  // 조회
                                   @Param("roomId") Long roomId);

    void updateRoom(@Param("accommodationsId") Long accommodationsId,  // 수정
                    @Param("roomId") Long roomId,
                    @Param("room") Room room);

    void deleteRoom(@Param("accommodationsId") Long accommodationsId,  // 삭제
                    @Param("roomId") Long roomId);
}
