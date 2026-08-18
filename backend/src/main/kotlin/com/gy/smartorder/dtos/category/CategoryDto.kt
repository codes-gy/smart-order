package com.gy.smartorder.dtos.category

import com.gy.smartorder.entities.category.Category
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import java.time.LocalDateTime

class CategoryDto {
    data class CategoryCreateRequest(
        @field:NotBlank(message = "카테고리명은 필수입니다.")
        val name: String,

        @field:Min(value = 0, message = "순서는 0 이상이어야 합니다.")
        val order: Int = 0,
    )

    data class CategoryUpdateRequest(
        @field:NotBlank(message = "카테고리명은 필수입니다.")
        val name: String,

        @field:Min(value = 0, message = "순서는 0 이상이어야 합니다.")
        val order: Int,
    )

    data class CategoryResponse(
        val id: Long,
        val storeId: Long,
        val name: String,
        val order: Int,
        val createdAt: LocalDateTime,
        val updatedAt: LocalDateTime,
    ) {
        companion object {
            fun from(category: Category): CategoryResponse = CategoryResponse(
                id = category.id,
                storeId = category.store.id,
                name = category.name,
                order = category.order,
                createdAt = category.createdAt,
                updatedAt = category.updatedAt,
            )
        }
    }
}