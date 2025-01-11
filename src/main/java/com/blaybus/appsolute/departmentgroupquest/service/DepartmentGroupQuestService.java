package com.blaybus.appsolute.departmentgroupquest.service;

import com.blaybus.appsolute.commons.exception.ApplicationException;
import com.blaybus.appsolute.commons.exception.payload.ErrorStatus;
import com.blaybus.appsolute.departmentgroup.domain.DepartmentGroup;
import com.blaybus.appsolute.departmentgroup.repository.JpaDepartmentGroupRepository;
import com.blaybus.appsolute.departmentgroupquest.domain.entity.DepartmentGroupQuest;
import com.blaybus.appsolute.departmentgroupquest.domain.request.UpdateDepartmentGroupQuestRequest;
import com.blaybus.appsolute.departmentgroupquest.domain.response.ReadDepartmentGroupQuestResponse;
import com.blaybus.appsolute.departmentgroupquest.domain.type.QuestStatusType;
import com.blaybus.appsolute.departmentgroupquest.domain.type.QuestType;
import com.blaybus.appsolute.departmentgroupquest.repository.JpaDepartmentGroupQuestRepository;
import com.blaybus.appsolute.fcm.domain.response.ReadFcmTokenResponse;
import com.blaybus.appsolute.fcm.service.FcmTokenService;
import com.blaybus.appsolute.fcm.service.MessageService;
import com.blaybus.appsolute.user.domain.entity.User;
import com.blaybus.appsolute.user.repository.JpaUserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.temporal.WeekFields;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

@Service
@RequiredArgsConstructor
@Transactional
public class DepartmentGroupQuestService {

    private final JpaDepartmentGroupQuestRepository departmentGroupQuestRepository;
    private final JpaDepartmentGroupRepository departmentGroupRepository;
    private final JpaUserRepository userRepository;
    private final FcmTokenService tokenService;
    private final MessageService messageService;

    public void createOrUpdateXP(UpdateDepartmentGroupQuestRequest request) {
        DepartmentGroup departmentGroup = departmentGroupRepository.findByDepartmentNameAndDepartmentGroupName(request.department(), request.departmentGroup())
                .orElseThrow(() -> new ApplicationException(
                        ErrorStatus.toErrorStatus("해당하는 소속, 그룹이 없습니다.", 404, LocalDateTime.now())
                ));

        DepartmentGroupQuest departmentGroupQuest;
        QuestStatusType questStatus;

        if(Objects.equals(request.xp(), request.maxPoint())) {
            questStatus = QuestStatusType.MAX_COMPLETE;
        } else if (Objects.equals(request.xp(), request.mediumPoint())) {
            questStatus = QuestStatusType.MEDIUM_COMPLETE;
        } else {
            questStatus = QuestStatusType.INCOMPLETE;
        }

        if(request.questType() == QuestType.MONTHLY) {
            departmentGroupQuest = departmentGroupQuestRepository.findByDepartmentGroupAndYearAndMonth(departmentGroup, request.year(), request.period())
                    .orElseGet(
                            () -> departmentGroupQuestRepository.save(
                                    DepartmentGroupQuest.builder()
                                            .departmentQuestType(QuestType.MONTHLY)
                                            .maxThreshold(request.maxThreshold())
                                            .mediumThreshold(request.mediumThreshold())
                                            .departmentGroupQuestStatus(questStatus)
                                            .mediumPoint(request.mediumPoint())
                                            .maxPoint(request.maxPoint())
                                            .departmentGroup(departmentGroup)
                                            .year(request.year())
                                            .month(request.period())
                                            .nowXP(request.xp())
                                            .note(request.notes())
                                            .build()
                            )
                    );
        } else {
            departmentGroupQuest = departmentGroupQuestRepository.findByDepartmentGroupAndYearAndWeek(departmentGroup, request.year(), request.period())
                    .orElseGet(() -> departmentGroupQuestRepository.save(
                            DepartmentGroupQuest.builder()
                                    .departmentQuestType(QuestType.MONTHLY)
                                    .maxThreshold(request.maxThreshold())
                                    .mediumThreshold(request.mediumThreshold())
                                    .departmentGroupQuestStatus(questStatus)
                                    .mediumPoint(request.mediumPoint())
                                    .maxPoint(request.maxPoint())
                                    .departmentGroup(departmentGroup)
                                    .year(request.year())
                                    .week(request.period())
                                    .nowXP(request.xp())
                                    .note(request.notes())
                                    .build()
                            )
                    );
        }

        departmentGroupQuest.updateNowXP(request.xp());

        List<User> userList = userRepository.findByDepartmentGroup(departmentGroup);

        for(User user : userList) {
            List<ReadFcmTokenResponse> tokenList = tokenService.getFcmTokens(user.getId());

            for(ReadFcmTokenResponse token : tokenList) {
                messageService.sendMessageTo(user, token.fcmToken(), "경험치 획득!",
                        "직무별 퀘스트로 " + request.xp() + "경험치를 획득하였습니다.", null);
            }
        }
    }

    public ReadDepartmentGroupQuestResponse getDepartmentGroupQuest(Long userId, LocalDateTime date) {

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ApplicationException(
                        ErrorStatus.toErrorStatus("해당하는 유저가 없습니다.", 404, LocalDateTime.now())
                ));

        DepartmentGroup departmentGroup = user.getDepartmentGroup();

        int year = date.getYear();

        List<DepartmentGroupQuest> departmentGroupQuestList = departmentGroupQuestRepository.findByDepartmentGroupAndYear(departmentGroup, year);

        QuestType questType = departmentGroupQuestList.getFirst().getDepartmentQuestType();

        if(questType == QuestType.WEEKLY) {
            WeekFields weekFields = WeekFields.of(Locale.KOREA);
            int weekOfYear = date.get(weekFields.weekOfYear());

            return departmentGroupQuestList.stream()
                    .filter(item -> item.getWeek() == weekOfYear)
                    .map(ReadDepartmentGroupQuestResponse::fromEntity)
                    .findFirst()
                    .orElse(null);
        } else {
            return departmentGroupQuestList.stream()
                    .filter(item -> item.getMonth() == date.getMonth().getValue())
                    .map(ReadDepartmentGroupQuestResponse::fromEntity)
                    .findFirst()
                    .orElse(null);
        }
    }
}
