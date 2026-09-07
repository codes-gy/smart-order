package com.gy.smartorder.menu

import org.springframework.data.jpa.repository.JpaRepository

interface MenuOptionGroupRepository : JpaRepository<MenuOptionGroup, Long> {
    fun existsByMenuIdAndName(menuId: Long, name: String): Boolean
}
