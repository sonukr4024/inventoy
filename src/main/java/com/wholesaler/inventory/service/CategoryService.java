package com.wholesaler.inventory.service;

import com.wholesaler.inventory.dto.request.CategoryRequest;
import com.wholesaler.inventory.entity.Category;
import com.wholesaler.inventory.exception.DuplicateResourceException;
import com.wholesaler.inventory.exception.ResourceNotFoundException;
import com.wholesaler.inventory.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class CategoryService {

    private final CategoryRepository categoryRepository;

    @Transactional
    public Category createCategory(CategoryRequest request) {
        log.info("Creating new category: {}", request.getCategoryName());

        // Check for duplicate category name
        if (categoryRepository.existsByCategoryName(request.getCategoryName())) {
            throw new DuplicateResourceException("Category with name '" + request.getCategoryName() + "' already exists");
        }

        Category category = Category.builder()
                .categoryName(request.getCategoryName())
                .description(request.getDescription())
                .build();

        category = categoryRepository.save(category);
        log.info("Category created successfully with ID: {}", category.getId());

        return category;
    }

    @Transactional(readOnly = true)
    public Category getCategory(Long id) {
        return categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category", "id", id));
    }

    @Transactional(readOnly = true)
    public List<Category> getAllCategories() {
        return categoryRepository.findAll();
    }

    @Transactional
    public Category updateCategory(Long id, CategoryRequest request) {
        log.info("Updating category: {}", id);

        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category", "id", id));

        // Check for duplicate name if name is being changed
        if (!category.getCategoryName().equals(request.getCategoryName()) &&
                categoryRepository.existsByCategoryName(request.getCategoryName())) {
            throw new DuplicateResourceException("Category with name '" + request.getCategoryName() + "' already exists");
        }

        category.setCategoryName(request.getCategoryName());
        category.setDescription(request.getDescription());

        category = categoryRepository.save(category);
        log.info("Category updated successfully: {}", id);

        return category;
    }

    @Transactional
    public void deleteCategory(Long id) {
        log.info("Deleting category: {}", id);

        Category category = categoryRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Category", "id", id));

        // Check if category has products
        if (!category.getProducts().isEmpty()) {
            throw new IllegalStateException("Cannot delete category with existing products");
        }

        categoryRepository.delete(category);
        log.info("Category deleted successfully: {}", id);
    }
}
