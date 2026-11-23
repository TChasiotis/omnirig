package com.omnirig.services;

import com.omnirig.controllers.CheckoutController.CustomerProfile;
import java.util.Optional;

public interface AuthService {
    Optional<CustomerProfile> login(String email, String password);
    boolean register(String password, CustomerProfile profile);
    boolean updateProfile(CustomerProfile updated);
    boolean deleteAccount(String email);
}