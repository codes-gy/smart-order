package com.gy.smartorder.store

import org.springframework.data.jpa.repository.JpaRepository

interface StoreRepository : JpaRepository<Store, Long> {
    // 사업자번호 확인
    fun existsByBusinessNumber(busNumber: String): Boolean
}
