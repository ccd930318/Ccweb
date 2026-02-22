package com.ccweb.babytracker.reminder;

import com.ccweb.babytracker.domain.baby.Baby;
import com.ccweb.babytracker.domain.baby.BabyRepository;
import com.ccweb.babytracker.domain.baby.FamilyMember;
import com.ccweb.babytracker.domain.baby.FamilyMemberRepository;
import com.ccweb.babytracker.domain.notification.Notification;
import com.ccweb.babytracker.domain.notification.NotificationRepository;
import com.ccweb.babytracker.domain.vaccination.VaccinationRecord;
import com.ccweb.babytracker.domain.vaccination.VaccinationRecordRepository;
import com.ccweb.babytracker.domain.vaccination.VaccineSchedule;
import com.ccweb.babytracker.domain.vaccination.VaccineScheduleRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
public class VaccineReminderScheduler {

    private static final Logger log = LoggerFactory.getLogger(VaccineReminderScheduler.class);
    private static final int DAYS_AHEAD = 7;

    private final BabyRepository babyRepo;
    private final FamilyMemberRepository familyMemberRepo;
    private final VaccineScheduleRepository scheduleRepo;
    private final VaccinationRecordRepository recordRepo;
    private final NotificationRepository notificationRepo;
    private final ResendEmailService emailService;

    public VaccineReminderScheduler(BabyRepository babyRepo,
                                    FamilyMemberRepository familyMemberRepo,
                                    VaccineScheduleRepository scheduleRepo,
                                    VaccinationRecordRepository recordRepo,
                                    NotificationRepository notificationRepo,
                                    ResendEmailService emailService) {
        this.babyRepo = babyRepo;
        this.familyMemberRepo = familyMemberRepo;
        this.scheduleRepo = scheduleRepo;
        this.recordRepo = recordRepo;
        this.notificationRepo = notificationRepo;
        this.emailService = emailService;
    }

    /** Runs daily at 08:00 server time */
    @Scheduled(cron = "0 0 8 * * *")
    @Transactional
    public void sendVaccineReminders() {
        List<VaccineSchedule> allSchedules = scheduleRepo.findAll();
        LocalDate today = LocalDate.now();
        LocalDate windowEnd = today.plusDays(DAYS_AHEAD);

        for (Baby baby : babyRepo.findAll()) {
            LocalDate birthDate = baby.getBirthDate();

            // Already-given vaccine IDs for this baby
            Set<Long> given = recordRepo.findByBabyIdOrderByAdministeredDateDesc(baby.getId())
                    .stream().map(r -> r.getSchedule().getId()).collect(Collectors.toSet());

            for (VaccineSchedule vs : allSchedules) {
                if (given.contains(vs.getId())) continue;

                LocalDate dueDate = birthDate.plusMonths(vs.getRecommendedAgeMonths());
                if (!dueDate.isBefore(today) && !dueDate.isAfter(windowEnd)) {
                    notifyOwners(baby, vs, dueDate);
                }
            }
        }
    }

    private void notifyOwners(Baby baby, VaccineSchedule vs, LocalDate dueDate) {
        List<FamilyMember> owners = familyMemberRepo.findAll().stream()
                .filter(fm -> fm.getBaby().getId().equals(baby.getId()) && "owner".equals(fm.getRole()))
                .toList();

        for (FamilyMember fm : owners) {
            String msg = String.format("提醒：%s 即將於 %s 需要接種「%s 第%d劑」",
                    baby.getName(), dueDate, vs.getVaccineName(), vs.getDoseNumber());

            Notification n = new Notification();
            n.setUser(fm.getUser());
            n.setMessage(msg);
            n.setType("VACCINE_REMINDER");
            notificationRepo.save(n);

            try {
                emailService.send(
                        fm.getUser().getEmail(),
                        "寶寶疫苗接種提醒",
                        "<p>" + msg + "</p>"
                );
            } catch (Exception e) {
                log.warn("Failed to send vaccine reminder email to {}", fm.getUser().getEmail(), e);
            }
        }
    }
}
