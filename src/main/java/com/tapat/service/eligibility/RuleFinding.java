package com.tapat.service.eligibility;

public class RuleFinding {
    private final String code;
    private final String message;

    public RuleFinding(String code, String message) {
        this.code = code;
        this.message = message;
    }

    public String getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }
}
