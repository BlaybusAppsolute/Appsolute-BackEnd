package com.blaybus.appsolute.leaderquest.domain.request;

import com.blaybus.appsolute.leaderquest.domain.entity.LeQuestBoard;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class LeQuestBoardRequest {

    private Long userId;
    private Long leaderQuestId;
    private Double actualPoint;
    public LeQuestBoard.QuestStatus questStatus;
    private String employeeNumber;

}