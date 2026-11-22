package com.omnirig.services;

import com.omnirig.models.Category;
import com.omnirig.models.Product;
import java.util.List;

public interface ProductService {
    List<Product> getAllProducts();
    List<Product> getProductsByCategory(Category category);
    List<Product> searchProducts(String keyword, Double maxPrice, Category category);
}