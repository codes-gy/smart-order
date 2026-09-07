package com.gy.smartorder.repositories.menu

import com.gy.smartorder.entities.menu.MenuOptionGroup
import org.springframework.data.jpa.repository.JpaRepository

interface MenuOptionGroupRepository : JpaRepository<MenuOptionGroup, Long> {
    fun existsByMenuIdAndName(menuId: Long, name: String): Boolean
}
