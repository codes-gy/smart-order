package com.gy.smartorder.category

import com.gy.smartorder.category.CategoryDto
import com.gy.smartorder.category.CategoryService
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping
class CategoryController(
    private val categoryService: CategoryService,
)
{
    @PostMapping("/stores/{storeId}/categories")
    fun createCategory(
        @AuthenticationPrincipal authenticatedStoreId: Long,
        @PathVariable storeId: Long,
        @Valid @RequestBody req: CategoryDto.CategoryCreateRequest,
    ): ResponseEntity<CategoryDto.CategoryResponse> {
        val res = categoryService.createCategory(authenticatedStoreId, storeId, req)
        return ResponseEntity.status(HttpStatus.CREATED).body(res)
    }

    @GetMapping("/stores/{storeId}/categories")
    fun getCategories(
        @PathVariable storeId: Long,
    ): ResponseEntity<List<CategoryDto.CategoryResponse>> {
        val res = categoryService.getCategories(storeId)
        return ResponseEntity.ok(res)
    }

    @PatchMapping("/categories/{categoryId}")
    fun updateCategory(
        @AuthenticationPrincipal authenticatedStoreId: Long,
        @PathVariable categoryId: Long,
        @Valid @RequestBody req: CategoryDto.CategoryUpdateRequest,
    ): ResponseEntity<CategoryDto.CategoryResponse> {
        val res = categoryService.updateCategory(authenticatedStoreId, categoryId, req)
        return ResponseEntity.ok(res)
    }

    @DeleteMapping("/categories/{categoryId}")
    fun deleteCategory(
        @AuthenticationPrincipal authenticatedStoreId: Long,
        @PathVariable categoryId: Long,
    ): ResponseEntity<Void> {
        categoryService.deleteCategory(authenticatedStoreId, categoryId)
        return ResponseEntity.noContent().build()
    }
}