package com.gy.smartorder.entities.store

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EntityListeners
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.Table
import org.hibernate.validator.constraints.Range
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.LastModifiedDate
import org.springframework.data.jpa.domain.support.AuditingEntityListener
import java.time.LocalDateTime
import java.time.LocalTime

/**
 * 매장 영업 상태. PRD의 "매장 영업 상태 변경" API(`PATCH /stores/{storeId}/operating-status`)가
 * 다루는 값은 OPEN/PAUSED/CLOSED 세 가지뿐이다. PREPARING(조리 중)은 개별 "주문"의 진행 상태이지
 * 매장 자체의 상태가 아니므로 여기 포함하지 않는다.
 */
enum class StoreStatus {
    OPEN,
    CLOSED,
    PAUSED,
}

@Entity
@Table(
    name = "store",
    indexes = [
        Index(name = "idx_store_status", columnList = "status"),
        Index(name = "idx_store_lat_lng", columnList = "latitude, longitude"),
    ]
)
@EntityListeners(AuditingEntityListener::class)
class Store(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long = 0L,

    @Column(nullable = false, length = 100)
    var name: String,

    @Column(nullable = false, length = 255)
    var address: String,

    @Column(length = 255)
    var addressDetail: String? = null,

    @Column(nullable = false, length = 30)
    var phone: String,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    var status: StoreStatus = StoreStatus.CLOSED,

    @Column(nullable = false, length = 20, unique = true)
    var businessNumber: String,

    @Column(nullable = false)
    var latitude: Double,

    @Column(nullable = false)
    var longitude: Double,

    @Column(nullable = false)
    var estimatedPreparationMinutes: Int = 10,

    @Column(nullable = false)
    var isAutoAccept: Boolean = true,

    @Column(nullable = false)
    var openTime: LocalTime,

    @Column(nullable = false)
    var closeTime: LocalTime,

    /** 매장 상세 화면(F-01)에 노출되는 한 줄 소개. */
    @Column(nullable = false, length = 500)
    var description: String = "",

    @CreatedDate
    @Column(nullable = false, updatable = false)
    var createdAt: LocalDateTime = LocalDateTime.now(),

    @LastModifiedDate
    @Column(nullable = false)
    var updatedAt: LocalDateTime = LocalDateTime.now(),
) {
    fun updateStatus(newStatus: StoreStatus) {
        this.status = newStatus
    }

    fun updatePreparationTime(minutes: Int) {
        this.estimatedPreparationMinutes = minutes
    }

    fun updateInfo(
        name: String,
        address: String,
        addressDetail: String?,
        phone: String,
        latitude: Double,
        longitude: Double,
        openTime: LocalTime,
        closeTime: LocalTime,
        description: String,
    ) {
        this.name = name
        this.address = address
        this.addressDetail = addressDetail
        this.phone = phone
        this.latitude = latitude
        this.longitude = longitude
        this.openTime = openTime
        this.closeTime = closeTime
        this.description = description
    }
}
