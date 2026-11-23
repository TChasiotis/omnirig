package com.omnirig.models;

import java.util.Map;

public record SavedBuild(
    String id,
    String name,
    Map<Category, Product> parts,
    double totalPrice
) {}