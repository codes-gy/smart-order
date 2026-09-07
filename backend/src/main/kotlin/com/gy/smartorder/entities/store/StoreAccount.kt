package com.gy.smartorder.entities.store

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EntityListeners
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.LastModifiedDate
import org.springframework.data.jpa.domain.support.AuditingEntityListener
import java.time.LocalDateTime

/**
 * 매장 POS/태블릿 로그인 전용 인증 정보. Member(고객)와는 완전히 분리된 별도 principal —
 * storeId를 관계(@ManyToOne)가 아닌 단순 FK 컬럼으로만 두고 unique 제약으로 1:1을 강제해서,
 * 나중에 매장당 여러 계정(직원별)으로 확장할 때 unique 제약만 풀면 되도록 했다.
 */
@Entity
@Table(name = "store_account")
@EntityListeners(AuditingEntityListener::class)
class StoreAccount(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long = 0L,

    @Column(nullable = false, unique = true)
    var storeId: Long,

    @Column(nullable = false, unique = true, length = 50)
    var storeCode: String,

    @Column(nullable = false)
    var password: String,

    @CreatedDate
    @Column(nullable = false, updatable = false)
    var createdAt: LocalDateTime = LocalDateTime.now(),

    @LastModifiedDate
    @Column(nullable = false)
    var updatedAt: LocalDateTime = LocalDateTime.now(),
)
