package com.tapat.repository;

import com.tapat.model.Application;
import com.tapat.model.ApplicationStatus;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface ApplicationRepository {
    Application insert(Application draft);

    void updateReview(int id, ApplicationStatus status, BigDecimal approvedAmount, Integer reviewerId, String reviewNotes);

    void issueClaim(int id, String claimReference);

    void markVerified(int id, int officerId);

    Optional<Application> findByClaim(String claimReference);

    Optional<Application> findById(int id);

    List<Application> findByApplicant(int applicantId);

    List<Application> findAll();

    List<Application> findByProgram(int programId);

    int countApproved(int programId);
}
