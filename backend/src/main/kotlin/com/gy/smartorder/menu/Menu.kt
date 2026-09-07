package com.gy.smartorder.menu

import com.gy.smartorder.category.Category
import jakarta.persistence.CascadeType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EntityListeners
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.OneToMany
import jakarta.persistence.OrderBy
import jakarta.persistence.Table
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.LastModifiedDate
import org.springframework.data.jpa.domain.support.AuditingEntityListener
import java.time.LocalDateTime

enum class MenuStatus {
    ON_SALE,   // 판매중
    SOLD_OUT,  // 품절
    HIDDEN,    // 숨김
}

@Entity
@Table(
    name = "menu",
    indexes = [
        Index(name = "idx_menu_category_order", columnList = "category_id, display_order"),
        Index(name = "idx_menu_status", columnList = "status")
    ]
)
@EntityListeners(AuditingEntityListener::class)
class Menu(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long = 0L,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    var category: Category,

    @Column(nullable = false, length = 100)
    var name: String,

    @Column(nullable = false)
    var price: Int,

    @Column(length = 500)
    var description: String? = null,

    @Column(length = 500)
    var imageUrl: String? = null,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    var status: MenuStatus = MenuStatus.ON_SALE,

    @Column(nullable = false)
    var isPopular: Boolean = false,

    @Column(nullable = false)
    var displayOrder: Int = 0,

    /** 온도, 샷 추가 등 이 메뉴에 달린 옵션 그룹들 (PRD, menu.types.ts의 MenuOptionGroup과 대응). */
    @OneToMany(mappedBy = "menu", cascade = [CascadeType.ALL], orphanRemoval = true)
    @OrderBy("displayOrder ASC")
    var optionGroups: MutableList<MenuOptionGroup> = mutableListOf(),

    @CreatedDate
    @Column(nullable = false, updatable = false)
    var createdAt: LocalDateTime = LocalDateTime.now(),

    @LastModifiedDate
    @Column(nullable = false)
    var updatedAt: LocalDateTime = LocalDateTime.now(),

    )
{
    fun updateInfo(
        category: Category,
        name: String,
        price: Int,
        description: String?,
        imageUrl: String?,
        isPopular: Boolean,
        displayOrder: Int,
    ) {
        this.category = category
        this.name = name
        this.price = price
        this.description = description
        this.imageUrl = imageUrl
        this.isPopular = isPopular
        this.displayOrder = displayOrder
    }

    fun updateStatus(status: MenuStatus) {
        this.status = status
    }
}
