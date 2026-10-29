package com.tapat.model;

import java.util.List;

public abstract class User {
    private final int id;
    private final String username;
    private final String fullName;
    private final String email;
    private final String phone;
    private final String address;
    private final boolean active;

    protected User(int id, String username, String fullName, String email, String phone, String address, boolean active) {
        this.id = id;
        this.username = username;
        this.fullName = fullName;
        this.email = email;
        this.phone = phone;
        this.address = address;
        this.active = active;
    }

    public int getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public String getFullName() {
        return fullName;
    }

    public String getEmail() {
        return email;
    }

    public String getPhone() {
        return phone;
    }

    public String getAddress() {
        return address;
    }

    public boolean isActive() {
        return active;
    }

    public abstract Role getRole();

    public abstract boolean canReviewApplications();

    public abstract boolean canManagePrograms();

    public abstract boolean canRecordFunds();

    public abstract boolean canExportFullReports();

    public abstract boolean canManageStaff();

    public abstract boolean canSubmitApplications();

    public abstract List<AppModule> accessibleModules();
}
