package com.tapat.service.eligibility;

import com.tapat.model.Application;
import com.tapat.model.ApplicationStatus;
import com.tapat.util.Text;

import java.util.Optional;

public class DuplicateApplicationRule implements EligibilityRule {
    @Override
    public Optional<RuleFinding> evaluate(EligibilityContext context) {
        boolean sameApplicant = false;
        boolean sameName = false;
        String applicantName = Text.normalizeName(context.getApplicantName());
        for (Application existing : context.getExistingApplications()) {
            if (existing.getId() == context.getCurrentApplicationId()) {
                continue;
            }
            if (existing.getProgramId() != context.getProgram().getId()) {
                continue;
            }
            if (existing.getStatus() == ApplicationStatus.REJECTED) {
                continue;
            }
            if (existing.getApplicantId() == context.getApplicantId()) {
                sameApplicant = true;
            } else if (applicantName.equals(Text.normalizeName(existing.getApplicantName()))) {
                sameName = true;
            }
        }
        if (sameApplicant) {
            return Optional.of(new RuleFinding(
                    "duplicate",
                    "This applicant already has an open application for the program."));
        }
        if (sameName) {
            return Optional.of(new RuleFinding(
                    "duplicate",
                    "Another application for this program uses the same applicant name."));
        }
        return Optional.empty();
    }

    @Override
    public String label() {
        return "Duplicate application";
    }
}
