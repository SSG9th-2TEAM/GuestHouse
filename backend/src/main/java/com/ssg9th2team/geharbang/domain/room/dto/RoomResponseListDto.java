package com.ssg9th2team.geharbang.domain.room.dto;

import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RoomResponseListDto {
    private String roomName;
    private String roomDescription;
    private int maxGuests;
    private int price;
    private int weekendPrice;
    private String mainImageUrl;
}
