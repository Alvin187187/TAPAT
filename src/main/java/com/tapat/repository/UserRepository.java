package com.tapat.repository;

import com.tapat.model.LoginRecord;
import com.tapat.model.Role;
import com.tapat.model.User;

import java.util.List;
import java.util.Optional;

public interface UserRepository {
    Optional<LoginRecord> findLogin(String username);

    Optional<User> findById(int id);

    boolean usernameExists(String username);

    User insert(String username, String passwordHash, String salt, String fullName, String email,
                String phone, String address, Role role);

    List<User> findAll();

    List<User> findByRole(Role role);

    int count();

    java.util.List<com.tapat.model.PersonRecord> people();

    void saveProfile(int userId, java.time.LocalDate dateOfBirth, String barangay);
}
