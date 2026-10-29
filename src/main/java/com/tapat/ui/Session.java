package com.tapat.ui;

import com.tapat.model.AppModule;
import com.tapat.model.User;

import java.util.List;
import java.util.Optional;

public class Session {
    private User user;

    public Optional<User> current() {
        return Optional.ofNullable(user);
    }

    public void signIn(User user) {
        this.user = user;
    }

    public void signOut() {
        this.user = null;
    }

    public List<AppModule> modules() {
        if (user == null) {
            return List.of(AppModule.DIRECTORY, AppModule.FUND_SUMMARY);
        }
        return user.accessibleModules();
    }
}
