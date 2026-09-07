package com.gy.smartorder.category

import com.gy.smartorder.common.exception.NotFoundException
import com.gy.smartorder.category.CategoryDto
import com.gy.smartorder.category.Category
import com.gy.smartorder.category.CategoryRepository
import com.gy.smartorder.store.StoreRepository
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class CategoryService (
    private val categoryRepository: CategoryRepository,
    private val storeRepository: StoreRepository,
){
    @Transactional
    fun createCategory(storeId: Long, req: CategoryDto.CategoryCreateRequest): CategoryDto.CategoryResponse {
        val store = storeRepository.findByIdOrNull(storeId)
            ?: throw NotFoundException("STORE_NOT_FOUND", "해당 매장을 찾을 수 없습니다. id=$storeId")

        if (categoryRepository.existsByStoreIdAndName(storeId, req.name)) {
            throw IllegalArgumentException("이미 해당 매장에 존재하는 카테고리명입니다.")
        }

        val category = Category(
            store = store,
            name = req.name,
            order = req.order,
        )
        val savedCategory = categoryRepository.save(category)
        return CategoryDto.CategoryResponse.from(savedCategory)
    }

    fun getCategories(storeId: Long): List<CategoryDto.CategoryResponse> {
        val categories = categoryRepository.findByStoreIdOrderByOrderAsc(storeId)
        return categories.map { CategoryDto.CategoryResponse.from(it) }
    }

    @Transactional
    fun updateCategory(categoryId: Long, req: CategoryDto.CategoryUpdateRequest): CategoryDto.CategoryResponse {
        val category = findCategoryOrThrow(categoryId)
        category.updateInfo(
            name = req.name,
            order = req.order,
        )
        return CategoryDto.CategoryResponse.from(category)
    }

    @Transactional
    fun deleteCategory(categoryId: Long) {
        val category = findCategoryOrThrow(categoryId)
        categoryRepository.delete(category)
    }

    private fun findCategoryOrThrow(categoryId: Long): Category =
        categoryRepository.findByIdOrNull(categoryId)
            ?: throw NotFoundException("CATEGORY_NOT_FOUND", "해당 카테고리를 찾을 수 없습니다. id=$categoryId")
}