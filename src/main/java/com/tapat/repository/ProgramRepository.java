package com.tapat.repository;

import com.tapat.model.Program;

import java.util.List;
import java.util.Optional;

public interface ProgramRepository {
    List<Program> findAll();

    List<Program> findOpen();

    Optional<Program> findById(int id);

    Program insert(Program program);

    void update(Program program);
}
