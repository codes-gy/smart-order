package com.gy.smartorder.repositories.menu

import com.gy.smartorder.entities.menu.Menu
import com.gy.smartorder.entities.menu.MenuStatus
import org.springframework.data.jpa.repository.JpaRepository

interface MenuRepository : JpaRepository<Menu, Long> {
    fun findByCategoryIdOrderByDisplayOrderAsc(categoryId: Long): List<Menu>
    fun findByCategoryIdAndStatusNotOrderByDisplayOrderAsc(categoryId: Long, status: MenuStatus): List<Menu>
    fun existsByCategoryIdAndName(categoryId: Long, name: String): Boolean
}