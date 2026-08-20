package com.gy.smartorder.entities.order

import jakarta.persistence.CollectionTable
import jakarta.persistence.Column
import jakarta.persistence.ElementCollection
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table

@Entity
@Table(name = "order_item")
class OrderItem(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long = 0L,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    var order: Order? = null,

    @Column(nullable = false)
    var menuId: Long,

    @Column(nullable = false, length = 100)
    var menuName: String,

    @Column(nullable = false)
    var price: Int,

    @Column(nullable = false)
    var quantity: Int,

    @Column(nullable = false)
    var totalPrice: Int = price * quantity,

    /**
     * 선택한 메뉴 옵션(온도, 샷 추가 등) choice ID 목록.
     * Menu 엔티티에 아직 옵션 그룹 도메인이 없어 지금은 선택값만 그대로 저장하고,
     * 옵션별 추가 금액(priceDelta)은 totalPrice 계산에 반영하지 않는다.
     * 메뉴 옵션 도메인이 추가되면 이 필드를 기준으로 가격 재계산 로직을 붙여야 한다.
     */
    @ElementCollection
    @CollectionTable(name = "order_item_option_choice", joinColumns = [JoinColumn(name = "order_item_id")])
    @Column(name = "option_choice_id")
    var optionChoiceIds: MutableList<Long> = mutableListOf(),
)
