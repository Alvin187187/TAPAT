package com.tapat.model;

import java.util.List;

public class Administrator extends User {
    public Administrator(int id, String username, String fullName, String email, String phone, String address, boolean active) {
        super(id, username, fullName, email, phone, address, active);
    }

    @Override
    public Role getRole() {
        return Role.ADMINISTRATOR;
    }

    @Override
    public boolean canReviewApplications() {
        return true;
    }

    @Override
    public boolean canManagePrograms() {
        return true;
    }

    @Override
    public boolean canRecordFunds() {
        return false;
    }

    @Override
    public boolean canExportFullReports() {
        return false;
    }

    @Override
    public boolean canManageStaff() {
        return true;
    }

    @Override
    public boolean canSubmitApplications() {
        return false;
    }

    @Override
    public List<AppModule> accessibleModules() {
        return List.of(
                AppModule.REVIEW,
                AppModule.PROGRAMS,
                AppModule.FUND_SUMMARY,
                AppModule.NOTICES,
                AppModule.STAFF);
    }
}
