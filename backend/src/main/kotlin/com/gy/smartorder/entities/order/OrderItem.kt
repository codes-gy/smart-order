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

    /** 옵션이 반영된 1개 단가 (메뉴 기본가 + 선택한 옵션들의 priceDelta 합). 프론트 `CartLineItem.unitPrice`와 같은 의미. */
    @Column(nullable = false)
    var price: Int,

    @Column(nullable = false)
    var quantity: Int,

    @Column(nullable = false)
    var totalPrice: Int = price * quantity,

    /** 선택한 메뉴 옵션(온도, 샷 추가 등) choice ID 목록. 가격 반영은 OrderService.createOrder에서 처리한다. */
    @ElementCollection
    @CollectionTable(name = "order_item_option_choice", joinColumns = [JoinColumn(name = "order_item_id")])
    @Column(name = "option_choice_id")
    var optionChoiceIds: MutableList<Long> = mutableListOf(),
)
