package com.tapat.model;

public class UserFactory {
    private UserFactory() {
    }

    public static User create(int id, String username, String fullName, String email, String phone,
                               String address, boolean active, Role role) {
        if (role == Role.APPLICANT) {
            return new Applicant(id, username, fullName, email, phone, address, active);
        }
        if (role == Role.ADMINISTRATOR) {
            return new Administrator(id, username, fullName, email, phone, address, active);
        }
        if (role == Role.FINANCE_OFFICER) {
            return new FinanceOfficer(id, username, fullName, email, phone, address, active);
        }
        return new Auditor(id, username, fullName, email, phone, address, active);
    }
}
