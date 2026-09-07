package com.gy.smartorder.category

import com.gy.smartorder.category.Category
import org.springframework.data.jpa.repository.JpaRepository

interface CategoryRepository : JpaRepository<Category, Long> {
    fun findByStoreIdOrderByOrderAsc(storeId: Long): List<Category>
    fun existsByStoreIdAndName(storeId: Long, name: String): Boolean
}