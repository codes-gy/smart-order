package com.gy.smartorder.dtos.menu

import com.gy.smartorder.entities.menu.Menu
import com.gy.smartorder.entities.menu.MenuOptionChoice
import com.gy.smartorder.entities.menu.MenuOptionGroup
import com.gy.smartorder.entities.menu.MenuOptionType
import com.gy.smartorder.entities.menu.MenuStatus
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import java.time.LocalDateTime

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

    /** 프론트 `MenuOptionChoice`({ id, label, priceDelta, isSoldOut })와 1:1로 대응한다. */
    data class MenuOptionChoiceResponse(
        val id: Long,
        val label: String,
        val priceDelta: Int,
        val isSoldOut: Boolean,
    ) {
        companion object {
            fun from(choice: MenuOptionChoice): MenuOptionChoiceResponse = MenuOptionChoiceResponse(
                id = choice.id,
                label = choice.label,
                priceDelta = choice.priceDelta,
                isSoldOut = choice.isSoldOut,
            )
        }
    }

    /**
     * 프론트 `MenuOptionGroup`({ id, name, type, required, choices })와 1:1로 대응한다.
     * `type`은 프론트가 소문자("single"/"multiple")를 기대하므로 여기서 수동 변환한다.
     */
    data class MenuOptionGroupResponse(
        val id: Long,
        val name: String,
        val type: String,
        val required: Boolean,
        val choices: List<MenuOptionChoiceResponse>,
    ) {
        companion object {
            fun from(group: MenuOptionGroup): MenuOptionGroupResponse = MenuOptionGroupResponse(
                id = group.id,
                name = group.name,
                type = if (group.type == MenuOptionType.MULTIPLE) "multiple" else "single",
                required = group.required,
                choices = group.choices.map { MenuOptionChoiceResponse.from(it) },
            )
        }
    }

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
        val optionGroups: List<MenuOptionGroupResponse>,
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
                optionGroups = menu.optionGroups.map { MenuOptionGroupResponse.from(it) },
                createdAt = menu.createdAt,
                updatedAt = menu.updatedAt,
            )
        }
    }
}
