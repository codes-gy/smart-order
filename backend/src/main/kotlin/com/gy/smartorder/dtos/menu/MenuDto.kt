package com.gy.smartorder.dtos.menu

import com.google.firebase.database.annotations.NotNull

class MenuDto {

    data class MenuCreateRequest(
        @field:NotNull(message = "카테고리 ID는 필수입니다.")
        val categoryId: Long?,

        @field:NotBlank(message = "메뉴명은 필수입니다.")
        val name: String,

        @field:NotNull(message = "가격은 필수입니다.")
        @field:Min(value = 0, message = "가격은 0원 이상이어야 합니다.")
        val price: Int?,

        val description: String? = null,
        val imageUrl: String? = null,
        val isPopular: Boolean = false,

        @field:Min(value = 0, message = "노출 순서는 0 이상이어야 합니다.")
        val displayOrder: Int = 0,
    )

    data class MenuUpdateRequest(
        @field:NotNull(message = "카테고리 ID는 필수입니다.")
        val categoryId: Long?,

        @field:NotBlank(message = "메뉴명은 필수입니다.")
        val name: String,

        @field:NotNull(message = "가격은 필수입니다.")
        @field:Min(value = 0, message = "가격은 0원 이상이어야 합니다.")
        val price: Int?,

        val description: String? = null,
        val imageUrl: String? = null,
        val isPopular: Boolean = false,

        @field:Min(value = 0, message = "노출 순서는 0 이상이어야 합니다.")
        val displayOrder: Int = 0,
    )

    data class MenuStatusUpdateRequest(
        @field:NotNull(message = "변경할 메뉴 상태는 필수입니다.")
        val status: MenuStatus?,
    )

    data class MenuResponse(
        val id: Long,
        val categoryId: Long,
        val name: String,
        val price: Int,
        val description: String?,
        val imageUrl: String?,
        val status: MenuStatus,
        val isPopular: Boolean,
        val displayOrder: Int,
        val createdAt: LocalDateTime,
        val updatedAt: LocalDateTime,
    ) {
        companion object {
            fun from(menu: Menu): MenuResponse = MenuResponse(
                id = menu.id,
                categoryId = menu.category.id,
                name = menu.name,
                price = menu.price,
                description = menu.description,
                imageUrl = menu.imageUrl,
                status = menu.status,
                isPopular = menu.isPopular,
                displayOrder = menu.displayOrder,
                createdAt = menu.createdAt,
                updatedAt = menu.updatedAt,
            )
        }
    }
}