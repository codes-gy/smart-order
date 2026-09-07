package com.gy.smartorder.entities.menu

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table

@Entity
@Table(
    name = "menu_option_choice",
    indexes = [
        Index(name = "idx_menu_option_choice_group_order", columnList = "option_group_id, display_order"),
    ]
)
class MenuOptionChoice(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long = 0L,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "option_group_id", nullable = false)
    var optionGroup: MenuOptionGroup? = null,

    /** 예: "아이스", "핫", "샷 추가 1개" */
    @Column(nullable = false, length = 100)
    var label: String,

    /** 기본가 대비 추가/할인 금액(원). 할인은 음수. */
    @Column(nullable = false)
    var priceDelta: Int = 0,

    @Column(nullable = false)
    var isSoldOut: Boolean = false,

    @Column(nullable = false)
    var displayOrder: Int = 0,
)
{
    fun updateInfo(label: String, priceDelta: Int, displayOrder: Int) {
        this.label = label
        this.priceDelta = priceDelta
        this.displayOrder = displayOrder
    }

    fun updateSoldOut(isSoldOut: Boolean) {
        this.isSoldOut = isSoldOut
    }
}
