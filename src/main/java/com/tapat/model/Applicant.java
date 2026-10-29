package com.tapat.model;

import java.util.List;

public class Applicant extends User {
    public Applicant(int id, String username, String fullName, String email, String phone, String address, boolean active) {
        super(id, username, fullName, email, phone, address, active);
    }

    @Override
    public Role getRole() {
        return Role.APPLICANT;
    }

    @Override
    public boolean canReviewApplications() {
        return false;
    }

    @Override
    public boolean canManagePrograms() {
        return false;
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
        return false;
    }

    @Override
    public boolean canSubmitApplications() {
        return true;
    }

    @Override
    public List<AppModule> accessibleModules() {
        return List.of(AppModule.MY_APPLICATIONS, AppModule.DIRECTORY, AppModule.NOTICES, AppModule.FUND_SUMMARY);
    }
}
