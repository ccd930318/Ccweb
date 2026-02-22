package com.ccweb.babytracker.reminder;

import com.ccweb.babytracker.auth.AuthService;
import com.ccweb.babytracker.auth.dto.RegisterRequest;
import com.ccweb.babytracker.domain.baby.Baby;
import com.ccweb.babytracker.domain.baby.BabyRepository;
import com.ccweb.babytracker.domain.baby.FamilyMember;
import com.ccweb.babytracker.domain.baby.FamilyMemberId;
import com.ccweb.babytracker.domain.baby.FamilyMemberRepository;
import com.ccweb.babytracker.domain.notification.NotificationRepository;
import com.ccweb.babytracker.domain.user.User;
import com.ccweb.babytracker.domain.user.UserRepository;
import com.ccweb.babytracker.domain.vaccination.VaccineSchedule;
import com.ccweb.babytracker.domain.vaccination.VaccineScheduleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.annotation.DirtiesContext;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;

@SpringBootTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class VaccineReminderSchedulerTest {

    @Autowired VaccineReminderScheduler scheduler;
    @Autowired AuthService authService;
    @Autowired UserRepository userRepo;
    @Autowired BabyRepository babyRepo;
    @Autowired FamilyMemberRepository familyMemberRepo;
    @Autowired VaccineScheduleRepository scheduleRepo;
    @Autowired NotificationRepository notificationRepo;

    @MockBean ResendEmailService emailService;

    @BeforeEach
    void setUp() {
        authService.register(new RegisterRequest("mom@test.com", "Secret123!", "Mom"));
        User mom = userRepo.findByEmail("mom@test.com").orElseThrow();

        // Baby born exactly 6 months ago today → 6-month vaccine is due now
        Baby baby = new Baby();
        baby.setName("小明");
        baby.setBirthDate(LocalDate.now().minusMonths(6));
        baby.setGender("M");
        babyRepo.save(baby);

        FamilyMemberId fmId = new FamilyMemberId();
        fmId.setBabyId(baby.getId());
        fmId.setUserId(mom.getId());
        FamilyMember fm = new FamilyMember();
        fm.setId(fmId);
        fm.setBaby(baby);
        fm.setUser(mom);
        fm.setRole("owner");
        familyMemberRepo.save(fm);

        VaccineSchedule vs = new VaccineSchedule();
        vs.setVaccineName("輪狀病毒疫苗");
        vs.setDoseNumber(1);
        vs.setRecommendedAgeMonths(6);
        scheduleRepo.save(vs);
    }

    @Test
    void sendsEmailAndCreatesNotificationForDueVaccine() {
        scheduler.sendVaccineReminders();

        // Email sent to the baby's owner
        verify(emailService, atLeastOnce()).send(any(), any(), any());

        // In-app notification created
        long count = notificationRepo.count();
        assertThat(count).isGreaterThan(0);
    }
}
