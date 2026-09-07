package com.gy.smartorder.repositories.store

import com.gy.smartorder.entities.store.StoreAccount
import org.springframework.data.jpa.repository.JpaRepository

interface StoreAccountRepository : JpaRepository<StoreAccount, Long> {
    fun findByStoreCode(storeCode: String): StoreAccount?
}
