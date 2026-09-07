package com.gy.smartorder.entities.menu

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

/**
 * 옵션 선택 방식. 프론트 `OptionType`은 소문자 문자열("single"/"multiple")이라
 * Jackson enum 기본 직렬화(대문자 name)와 어긋난다. 매핑 문제를 아예 없애기 위해
 * 이 enum은 JPA 저장/내부 로직에만 쓰고, 응답 DTO(`MenuDto.MenuOptionGroupResponse`)에서
 * 수동으로 소문자 문자열로 변환해서 내려준다.
 */
enum class MenuOptionType {
    SINGLE,
    MULTIPLE,
}

@Entity
@Table(
    name = "menu_option_group",
    indexes = [
        Index(name = "idx_menu_option_group_menu_order", columnList = "menu_id, display_order"),
    ]
)
@EntityListeners(AuditingEntityListener::class)
class MenuOptionGroup(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long = 0L,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "menu_id", nullable = false)
    var menu: Menu,

    /** 예: "온도", "샷 추가", "시럽 추가", "우유 변경" */
    @Column(nullable = false, length = 100)
    var name: String,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    var type: MenuOptionType,

    @Column(nullable = false)
    var required: Boolean = false,

    @Column(nullable = false)
    var displayOrder: Int = 0,

    @OneToMany(mappedBy = "optionGroup", cascade = [CascadeType.ALL], orphanRemoval = true)
    @OrderBy("displayOrder ASC")
    var choices: MutableList<MenuOptionChoice> = mutableListOf(),

    @CreatedDate
    @Column(nullable = false, updatable = false)
    var createdAt: LocalDateTime = LocalDateTime.now(),

    @LastModifiedDate
    @Column(nullable = false)
    var updatedAt: LocalDateTime = LocalDateTime.now(),
)
{
    fun updateInfo(name: String, type: MenuOptionType, required: Boolean, displayOrder: Int) {
        this.name = name
        this.type = type
        this.required = required
        this.displayOrder = displayOrder
    }
}
