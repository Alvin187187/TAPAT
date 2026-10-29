package com.tapat.service;

import com.tapat.model.Application;
import com.tapat.model.Program;
import com.tapat.model.Role;
import com.tapat.model.User;
import com.tapat.repository.ApplicationRepository;
import com.tapat.repository.NotificationRepository;
import com.tapat.repository.ProgramRepository;
import com.tapat.repository.UserRepository;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

public class NotificationService {
    private final NotificationRepository notifications;
    private final ApplicationRepository applications;
    private final ProgramRepository programs;
    private final UserRepository users;

    public NotificationService(NotificationRepository notifications, ApplicationRepository applications,
                               ProgramRepository programs, UserRepository users) {
        this.notifications = notifications;
        this.applications = applications;
        this.programs = programs;
        this.users = users;
    }

    public void notify(int userId, String title, String message) {
        notifications.insert(userId, title, message);
    }

    public void notifyRole(Role role, String title, String message) {
        for (User user : users.findByRole(role)) {
            notifications.insert(user.getId(), title, message);
        }
    }

    public java.util.List<com.tapat.model.Notification> list(User user) {
        ensureDeadlineReminders(user);
        return notifications.findByUser(user.getId());
    }

    public void markRead(User user, int id) {
        notifications.markRead(id, user.getId());
    }

    public void markAllRead(User user) {
        if (user != null) {
            notifications.markAllRead(user.getId());
        }
    }

    public int unread(User user) {
        if (user == null) {
            return 0;
        }
        return notifications.countUnread(user.getId());
    }

    public void ensureDeadlineReminders(User user) {
        if (user == null || !user.canSubmitApplications()) {
            return;
        }
        LocalDate today = LocalDate.now();
        for (Application application : applications.findByApplicant(user.getId())) {
            if (application.getStatus() != com.tapat.model.ApplicationStatus.PENDING
                    && application.getStatus() != com.tapat.model.ApplicationStatus.UNDER_REVIEW) {
                continue;
            }
            Program program = programs.findById(application.getProgramId()).orElse(null);
            if (program == null || program.getDeadline() == null) {
                continue;
            }
            String title;
            String message;
            if (program.getDeadline().isBefore(today)) {
                title = "Deadline passed: " + program.getName();
                message = application.getReferenceNo() + " is still open after the deadline of "
                        + com.tapat.util.Text.date(program.getDeadline()) + ".";
            } else if (ChronoUnit.DAYS.between(today, program.getDeadline()) <= 7) {
                title = "Deadline: " + program.getName();
                message = application.getReferenceNo() + " is due on " + com.tapat.util.Text.date(program.getDeadline()) + ".";
            } else {
                continue;
            }
            if (!notifications.existsWithTitle(user.getId(), title)) {
                notifications.insert(user.getId(), title, message);
            }
        }
    }
}
