package com.tapat.service;

import com.tapat.exception.AuthorizationException;
import com.tapat.exception.ValidationException;
import com.tapat.model.LoginRecord;
import com.tapat.model.Role;
import com.tapat.model.User;
import com.tapat.repository.AuditRepository;
import com.tapat.repository.UserRepository;
import com.tapat.security.PasswordHasher;
import com.tapat.security.StoredPassword;
import com.tapat.util.Text;

import java.util.List;
import java.util.Optional;

public class AuthService {
    private final UserRepository users;
    private final AuditRepository audit;
    private final NotificationService notifications;
    private final PasswordHasher hasher;

    public AuthService(UserRepository users, AuditRepository audit, NotificationService notifications, PasswordHasher hasher) {
        this.users = users;
        this.audit = audit;
        this.notifications = notifications;
        this.hasher = hasher;
    }

    public User login(String username, String password) {
        String name = Text.trimToNull(username);
        if (name == null || password == null || password.isEmpty()) {
            throw new ValidationException("Enter your username and password.");
        }
        Optional<LoginRecord> found = users.findLogin(name);
        if (found.isEmpty()) {
            throw new ValidationException("Unknown username or password.");
        }
        LoginRecord record = found.get();
        if (!record.getUser().isActive() || !hasher.verify(password, record.getPasswordHash(), record.getSalt())) {
            throw new ValidationException("Unknown username or password.");
        }
        audit.insert(record.getUser().getId(), "LOGIN", "user", record.getUser().getId(), record.getUser().getRole().name());
        notifications.ensureDeadlineReminders(record.getUser());
        return record.getUser();
    }

    public User registerApplicant(String fullName, String username, String email, String phone, String address, String password) {
        return create(null, Role.APPLICANT, fullName, username, email, phone, address, password);
    }

    public User registerStaff(User actor, Role role, String fullName, String username, String email, String phone, String password) {
        if (actor == null || !actor.canManageStaff()) {
            throw new AuthorizationException("You cannot create staff accounts.");
        }
        if (role != Role.FINANCE_OFFICER && role != Role.AUDITOR) {
            throw new ValidationException("Choose a finance officer or auditor account.");
        }
        return create(actor, role, fullName, username, email, phone, null, password);
    }

    private User create(User actor, Role role, String fullName, String username, String email, String phone,
                        String address, String password) {
        String name = require(fullName, "Enter the full name.");
        String login = require(username, "Enter a username.");
        if (!login.matches("[A-Za-z0-9._-]{3,32}")) {
            throw new ValidationException("Username must be 3 to 32 letters, numbers, dots, hyphens, or underscores.");
        }
        if (users.usernameExists(login)) {
            throw new ValidationException("That username is already in use.");
        }
        String mail = Text.trimToNull(email);
        if (mail != null && !mail.matches("[^@\\s]+@[^@\\s]+\\.[^@\\s]+")) {
            throw new ValidationException("Enter a valid email address.");
        }
        hasher.validate(password);
        StoredPassword stored = hasher.hash(password);
        User created = users.insert(login, stored.getHash(), stored.getSalt(), name, mail, Text.trimToNull(phone),
                Text.trimToNull(address), role);
        Integer actorId = actor == null ? created.getId() : actor.getId();
        audit.insert(actorId, actor == null ? "REGISTER" : "CREATE_STAFF", "user", created.getId(), role.name());
        return created;
    }

    public User requireUser(int id) {
        Optional<User> found = users.findById(id);
        if (found.isEmpty()) {
            throw new ValidationException("That account was not found.");
        }
        return found.get();
    }

    public List<User> listUsers(User actor) {
        if (actor == null || !actor.canManageStaff()) {
            throw new AuthorizationException("You cannot view staff accounts.");
        }
        return users.findAll();
    }

    private String require(String value, String message) {
        String trimmed = Text.trimToNull(value);
        if (trimmed == null) {
            throw new ValidationException(message);
        }
        return trimmed;
    }
}
