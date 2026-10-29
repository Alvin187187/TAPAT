package com.tapat.model;

import java.util.List;

public class FinanceOfficer extends User {
    public FinanceOfficer(int id, String username, String fullName, String email, String phone, String address, boolean active) {
        super(id, username, fullName, email, phone, address, active);
    }

    @Override
    public Role getRole() {
        return Role.FINANCE_OFFICER;
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
        return true;
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
        return false;
    }

    @Override
    public List<AppModule> accessibleModules() {
        return List.of(AppModule.FUNDS, AppModule.FUND_SUMMARY, AppModule.NOTICES);
    }
}
