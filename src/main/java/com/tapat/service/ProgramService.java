package com.tapat.service;

import com.tapat.exception.AuthorizationException;
import com.tapat.exception.ValidationException;
import com.tapat.model.Program;
import com.tapat.model.ProgramStatus;
import com.tapat.model.User;
import com.tapat.repository.AuditRepository;
import com.tapat.repository.ProgramRepository;
import com.tapat.util.Text;

import java.util.List;
import java.util.Optional;

public class ProgramService {
    private final ProgramRepository programs;
    private final AuditRepository audit;

    public ProgramService(ProgramRepository programs, AuditRepository audit) {
        this.programs = programs;
        this.audit = audit;
    }

    public List<Program> listOpen() {
        return programs.findOpen();
    }

    public List<Program> listAll(User actor) {
        requireManager(actor);
        return programs.findAll();
    }

    public Program require(int id) {
        Optional<Program> found = programs.findById(id);
        if (found.isEmpty()) {
            throw new ValidationException("That program was not found.");
        }
        return found.get();
    }

    public Program save(User actor, Program program) {
        requireManager(actor);
        if (Text.trimToNull(program.getName()) == null) {
            throw new ValidationException("Enter the program name.");
        }
        if (Text.trimToNull(program.getDescription()) == null || Text.trimToNull(program.getEligibilitySummary()) == null) {
            throw new ValidationException("Enter the description and eligibility summary.");
        }
        if (program.getStatus() == null || program.getCategory() == null) {
            throw new ValidationException("Choose a category and status.");
        }
        if (program.getMaxAssistance() != null && program.getMaxAssistance().signum() < 0) {
            throw new ValidationException("Maximum assistance cannot be negative.");
        }
        if (program.getIncomeLimit() != null && program.getIncomeLimit().signum() < 0) {
            throw new ValidationException("Income limit cannot be negative.");
        }
        if (program.getId() == 0) {
            Program saved = programs.insert(program);
            audit.insert(actor.getId(), "CREATE_PROGRAM", "program", saved.getId(), saved.getName());
            return saved;
        }
        programs.update(program);
        audit.insert(actor.getId(), "UPDATE_PROGRAM", "program", program.getId(), program.getName());
        Optional<Program> saved = programs.findById(program.getId());
        if (saved.isEmpty()) {
            throw new ValidationException("That program was not found.");
        }
        return saved.get();
    }

    private void requireManager(User actor) {
        if (actor == null || !actor.canManagePrograms()) {
            throw new AuthorizationException("You cannot manage assistance programs.");
        }
    }

    public void requireOpen(Program program) {
        if (program.getStatus() != ProgramStatus.OPEN) {
            throw new ValidationException("This program is closed.");
        }
    }
}
