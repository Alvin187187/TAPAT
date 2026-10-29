package com.tapat.model;

import java.time.LocalDate;

public class PersonRecord {
    private final int id;
    private final String fullName;
    private final String phone;
    private final String address;
    private final LocalDate dateOfBirth;
    private final String barangay;

    public PersonRecord(int id, String fullName, String phone, String address, LocalDate dateOfBirth, String barangay) {
        this.id = id;
        this.fullName = fullName;
        this.phone = phone;
        this.address = address;
        this.dateOfBirth = dateOfBirth;
        this.barangay = barangay;
    }

    public int getId() {
        return id;
    }

    public String getFullName() {
        return fullName;
    }

    public String getPhone() {
        return phone;
    }

    public String getAddress() {
        return address;
    }

    public LocalDate getDateOfBirth() {
        return dateOfBirth;
    }

    public String getBarangay() {
        return barangay;
    }
}
