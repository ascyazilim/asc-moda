package com.ascmoda.catalog.application.service;

import com.ascmoda.catalog.api.dto.CategoryResponse;
import com.ascmoda.catalog.api.dto.CreateCategoryRequest;
import com.ascmoda.catalog.api.dto.UpdateCategoryRequest;
import com.ascmoda.catalog.api.error.DuplicateResourceException;
import com.ascmoda.catalog.api.error.ResourceNotFoundException;
import com.ascmoda.catalog.application.mapper.CategoryMapper;
import com.ascmoda.catalog.domain.model.Category;
import com.ascmoda.catalog.domain.repository.CategoryRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.function.Predicate;

@Service
public class CategoryService {

    private static final Logger log = LoggerFactory.getLogger(CategoryService.class);

    private final CategoryRepository categoryRepository;
    private final CategoryMapper categoryMapper;
    private final SlugGenerator slugGenerator;

    public CategoryService(CategoryRepository categoryRepository, CategoryMapper categoryMapper,
                           SlugGenerator slugGenerator) {
        this.categoryRepository = categoryRepository;
        this.categoryMapper = categoryMapper;
        this.slugGenerator = slugGenerator;
    }

    @Transactional
    public CategoryResponse create(CreateCategoryRequest request) {
        String slug = resolveCategorySlug(request.slug(), request.name(), null);

        Category category = new Category(
                request.name().trim(),
                slug,
                request.description(),
                request.active() == null || request.active()
        );

        Category saved = categoryRepository.save(category);
        log.info("Created catalog category id={} slug={}", saved.getId(), saved.getSlug());
        return categoryMapper.toResponse(saved);
    }

    @Transactional
    public CategoryResponse update(UUID id, UpdateCategoryRequest request) {
        Category category = getCategory(id);
        String slug = resolveCategorySlug(request.slug(), request.name(), id);

        category.setName(request.name().trim());
        category.setSlug(slug);
        category.setDescription(request.description());
        category.setActive(request.active() == null || request.active());

        Category saved = categoryRepository.save(category);
        log.info("Updated catalog category id={} slug={}", saved.getId(), saved.getSlug());
        return categoryMapper.toResponse(saved);
    }

    @Transactional
    public CategoryResponse activate(UUID id) {
        return changeActive(id, true);
    }

    @Transactional
    public CategoryResponse deactivate(UUID id) {
        return changeActive(id, false);
    }

    @Transactional(readOnly = true)
    public List<CategoryResponse> listActive() {
        return categoryRepository.findByActiveTrueOrderByNameAsc()
                .stream()
                .map(categoryMapper::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<CategoryResponse> listAllForAdmin(Boolean active) {
        List<Category> categories = active == null
                ? categoryRepository.findAllByOrderByNameAsc()
                : categoryRepository.findByActiveOrderByNameAsc(active);

        return categories
                .stream()
                .map(categoryMapper::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public Category getCategory(UUID id) {
        return categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found: " + id));
    }

    private CategoryResponse changeActive(UUID id, boolean active) {
        Category category = getCategory(id);
        category.setActive(active);

        Category saved = categoryRepository.save(category);
        log.info("Changed catalog category id={} active={}", saved.getId(), saved.isActive());
        return categoryMapper.toResponse(saved);
    }

    private String resolveCategorySlug(String requestedSlug, String name, UUID currentCategoryId) {
        String baseSlug = slugGenerator.generate(resolveSlugSource(requestedSlug, name));
        Predicate<String> exists = currentCategoryId == null
                ? categoryRepository::existsBySlug
                : slug -> categoryRepository.existsBySlugAndIdNot(slug, currentCategoryId);

        if (requestedSlug != null && !requestedSlug.isBlank()) {
            ensureSlugAvailable(baseSlug, exists);
            return baseSlug;
        }

        return slugGenerator.generateUnique(baseSlug, exists);
    }

    private void ensureSlugAvailable(String slug, Predicate<String> exists) {
        if (exists.test(slug)) {
            throw new DuplicateResourceException("Category slug already exists: " + slug);
        }
    }

    private String resolveSlugSource(String slug, String name) {
        return slug == null || slug.isBlank() ? name : slug;
    }
}
