package com.omnirig.models;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class Product {
    private int id;
    private Category category;
    private String brand;
    private String model;
    private double price;
    private int stock;
    private Map<String, Object> specs = new LinkedHashMap<>();

    public Product(int id, Category category, String brand, String model, double price, int stock) {
        this.id = id;
        this.category = category;
        this.brand = brand;
        this.model = model;
        this.price = price;
        this.stock = stock;
    }

    public Product addSpec(String key, Object value) {
        if (value != null) {
            this.specs.put(key, value);
        }
        return this;
    }

    // Βασικοί Getters (το getName() μένει για συμβατότητα με το Employee TableView)
    public int getId() { return id; }
    public Category getCategory() { return category; }
    public String getBrand() { return brand; }
    public String getModel() { return model; }
    public String getName() { return brand + " " + model; }
    public double getPrice() { return price; }
    public int getStock() { return stock; }
    public void setStock(int stock) { this.stock = stock; }
    public Map<String, Object> getSpecs() { return specs; }

    // Helper μέθοδοι για τον PC Builder
    public String getStringSpec(String key) {
        Object val = specs.get(key);
        return val != null ? val.toString() : "";
    }

    public int getIntSpec(String key) {
        Object val = specs.get(key);
        return (val instanceof Number) ? ((Number) val).intValue() : 0;
    }

    public double getDoubleSpec(String key) {
        Object val = specs.get(key);
        return (val instanceof Number) ? ((Number) val).doubleValue() : 0.0;
    }

    public boolean getBoolSpec(String key) {
        Object val = specs.get(key);
        return (val instanceof Boolean) && (Boolean) val;
    }

    @SuppressWarnings("unchecked")
    public List<String> getListSpec(String key) {
        Object val = specs.get(key);
        return (val instanceof List<?>) ? (List<String>) val : Collections.emptyList();
    }
}