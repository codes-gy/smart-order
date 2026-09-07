package com.gy.smartorder.menu

import org.springframework.data.jpa.repository.JpaRepository

interface MenuOptionChoiceRepository : JpaRepository<MenuOptionChoice, Long> {
    fun existsByOptionGroupIdAndLabel(optionGroupId: Long, label: String): Boolean
}
