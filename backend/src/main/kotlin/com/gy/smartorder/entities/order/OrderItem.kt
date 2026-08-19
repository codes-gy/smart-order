package com.gy.smartorder.entities.order

import jakarta.persistence.Column
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
)