package com.omnirig.services;

import com.omnirig.controllers.CheckoutController.CustomerProfile;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class MockAuthService implements AuthService {

    private record Account(String password, CustomerProfile profile) {}

    private final Map<String, Account> accounts = new HashMap<>();

    public MockAuthService() {
        accounts.put("demo@omnirig.gr", new Account("1234", new CustomerProfile(
            "Γιώργος", "Παπαδόπουλος", "demo@omnirig.gr", "6971234567",
            "Τσιμισκή 45", "Θεσσαλονίκη", "54623"
        )));
        accounts.put("test@omnirig.gr", new Account("test", new CustomerProfile(
            "Μαρία", "Νικολάου", "test@omnirig.gr", "6987654321",
            "Εγνατία 112", "Θεσσαλονίκη", "54622"
        )));
    }

    @Override
    public Optional<CustomerProfile> login(String email, String password) {
        if (email == null || password == null) return Optional.empty();
        Account acc = accounts.get(email.trim().toLowerCase());
        if (acc != null && acc.password().equals(password)) {
            return Optional.of(acc.profile());
        }
        return Optional.empty();
    }

    @Override
    public boolean register(String password, CustomerProfile profile) {
        String key = profile.email().trim().toLowerCase();
        if (accounts.containsKey(key)) return false;
        accounts.put(key, new Account(password, profile));
        return true;
    }

    @Override
    public boolean updateProfile(CustomerProfile updated) {
        String key = updated.email().trim().toLowerCase();
        Account existing = accounts.get(key);
        if (existing == null) return false;
        accounts.put(key, new Account(existing.password(), updated));
        return true;
    }

    @Override
    public boolean deleteAccount(String email) {
        if (email == null) return false;
        return accounts.remove(email.trim().toLowerCase()) != null;
    }
}