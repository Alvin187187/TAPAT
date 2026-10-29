package com.tapat.service.report;

import com.tapat.model.Application;
import com.tapat.model.ApplicationStatus;
import com.tapat.util.Money;
import com.tapat.util.Text;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class ApplicationSummaryReport implements Report {
    private final List<Application> applications;

    public ApplicationSummaryReport(List<Application> applications) {
        this.applications = List.copyOf(applications);
    }

    @Override
    public String getTitle() {
        return "Application summary";
    }

    @Override
    public List<String> columns() {
        return List.of("Reference", "Applicant", "Program", "Status", "Requested", "Approved", "Submitted");
    }

    @Override
    public List<List<String>> rows() {
        List<List<String>> rows = new ArrayList<>();
        for (Application application : applications) {
            rows.add(List.of(
                    application.getReferenceNo(),
                    application.getApplicantName(),
                    application.getProgramName(),
                    application.getStatus().getLabel(),
                    Money.format(application.getRequestedAmount()),
                    Money.format(application.getApprovedAmount()),
                    Text.dateTime(application.getSubmittedAt())));
        }
        return rows;
    }

    @Override
    public Map<String, Integer> chartSlices() {
        Map<String, Integer> slices = new LinkedHashMap<>();
        for (ApplicationStatus status : ApplicationStatus.values()) {
            slices.put(status.getLabel(), 0);
        }
        for (Application application : applications) {
            String label = application.getStatus().getLabel();
            slices.put(label, slices.get(label) + 1);
        }
        return slices;
    }
}
