package com.blaybus.appsolute.leaderquest.domain.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "LE_QUEST_BOARD")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class LeQuestBoard {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "le_board_id")
    private Long leBoardId;

    @Column(name = "user_id")
    private Long userId;

    @Column(name = "leader_quest_id")
    private Long leaderQuestId;

    @Column(name="month")
    private Long month;

    public enum QuestStatus {
        READY, ONGOING, COMPLETED, FAILED
    }

    @Column(name = "status")
    @Enumerated(EnumType.STRING)
    private QuestStatus questStatus;

    @Column(name = "granted_point")
    private Long grantedPoint;

    @Column(name="note")
    private String note;

    public void updateGrantedPoint(Long grantedPoint) {
        if (grantedPoint == null || grantedPoint < 0) {
            throw new IllegalArgumentException("유효하지 않은 경험치 값입니다.");
        }
        this.grantedPoint = grantedPoint;

        if (grantedPoint == 0) {
            this.questStatus = QuestStatus.READY;
        } else if (grantedPoint < 50) {
            this.questStatus = QuestStatus.ONGOING;
        } else if (grantedPoint >= 50) {
            this.questStatus = QuestStatus.COMPLETED;
        }
    }
}
