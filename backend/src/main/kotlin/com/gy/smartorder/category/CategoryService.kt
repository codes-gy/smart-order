package com.gy.smartorder.category

import com.gy.smartorder.common.exception.ForbiddenException
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
    fun createCategory(authenticatedStoreId: Long, storeId: Long, req: CategoryDto.CategoryCreateRequest): CategoryDto.CategoryResponse {
        requireOwnStore(authenticatedStoreId, storeId)
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
    fun updateCategory(authenticatedStoreId: Long, categoryId: Long, req: CategoryDto.CategoryUpdateRequest): CategoryDto.CategoryResponse {
        val category = findCategoryOrThrow(categoryId)
        requireOwnStore(authenticatedStoreId, category.store.id)
        category.updateInfo(
            name = req.name,
            order = req.order,
        )
        return CategoryDto.CategoryResponse.from(category)
    }

    @Transactional
    fun deleteCategory(authenticatedStoreId: Long, categoryId: Long) {
        val category = findCategoryOrThrow(categoryId)
        requireOwnStore(authenticatedStoreId, category.store.id)
        categoryRepository.delete(category)
    }

    private fun findCategoryOrThrow(categoryId: Long): Category =
        categoryRepository.findByIdOrNull(categoryId)
            ?: throw NotFoundException("CATEGORY_NOT_FOUND", "해당 카테고리를 찾을 수 없습니다. id=$categoryId")

    // STORE_ADMIN 토큰의 subject(storeId)가 대상 매장과 일치하는지 확인 — 다른 매장 계정으로 로그인한
    // 관리자가 남의 매장 카테고리를 고치지 못하도록 막는다(SecurityConfig의 hasRole만으로는 못 막는 부분).
    private fun requireOwnStore(authenticatedStoreId: Long, storeId: Long) {
        if (authenticatedStoreId != storeId) {
            throw ForbiddenException("STORE_ACCESS_DENIED", "해당 매장에 대한 권한이 없습니다.")
        }
    }
}