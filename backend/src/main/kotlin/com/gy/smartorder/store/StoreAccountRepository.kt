package com.gy.smartorder.store

import org.springframework.data.jpa.repository.JpaRepository

interface StoreAccountRepository : JpaRepository<StoreAccount, Long> {
    fun findByStoreCode(storeCode: String): StoreAccount?
}
