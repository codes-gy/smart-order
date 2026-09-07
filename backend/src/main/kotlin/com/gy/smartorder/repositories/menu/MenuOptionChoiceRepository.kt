package com.gy.smartorder.repositories.menu

import com.gy.smartorder.entities.menu.MenuOptionChoice
import org.springframework.data.jpa.repository.JpaRepository

interface MenuOptionChoiceRepository : JpaRepository<MenuOptionChoice, Long> {
    fun existsByOptionGroupIdAndLabel(optionGroupId: Long, label: String): Boolean
}
