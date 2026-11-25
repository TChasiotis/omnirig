package com.omnirig.utils;

public final class AdminConfig {

    public static final String ADMIN_USERNAME = "admin@omnirig.gr";
    public static final String ADMIN_PASSWORD = "admin123";

    private AdminConfig() {}

    public static boolean isAdminCredentials(String emailOrUser, String password) {
        if (emailOrUser == null || password == null) return false;
        return ADMIN_USERNAME.equalsIgnoreCase(emailOrUser.trim())
            && ADMIN_PASSWORD.equals(password);
    }
}