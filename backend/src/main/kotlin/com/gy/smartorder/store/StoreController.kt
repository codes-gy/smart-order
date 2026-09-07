package com.gy.smartorder.store

import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/stores")
class StoreController(
    private val storeService: StoreService,
) {

    @PostMapping
    fun createStore(
        @Valid @RequestBody req: StoreDto.StoreCreateRequest,
    ): ResponseEntity<StoreDto.StoreResponse> {
        val res = storeService.createStore(req)
        return ResponseEntity.status(HttpStatus.CREATED).body(res)
    }

    /** PRD B: 매장 리스트 조회. lat/lng 기준 거리순 정렬 + 커서 페이징(PRD 4.3). */
    @GetMapping
    fun getStores(
        @RequestParam lat: Double,
        @RequestParam lng: Double,
        @RequestParam(required = false) cursor: String?,
        @RequestParam(required = false, defaultValue = "10") limit: Int,
    ): ResponseEntity<StoreDto.StoreListResponse> {
        val res = storeService.getStores(lat, lng, cursor, limit)
        return ResponseEntity.ok(res)
    }

    @GetMapping("/{storeId}")
    fun getStore(@PathVariable storeId: Long): ResponseEntity<StoreDto.StoreDetailResponse> {
        val res = storeService.getStore(storeId)
        return ResponseEntity.ok(res)
    }

    @PatchMapping("/{storeId}")
    fun updateStore(
        @AuthenticationPrincipal authenticatedStoreId: Long,
        @PathVariable storeId: Long,
        @Valid @RequestBody req: StoreDto.StoreUpdateRequest,
    ): ResponseEntity<StoreDto.StoreResponse> {
        val res = storeService.updateStore(authenticatedStoreId, storeId, req)
        return ResponseEntity.ok(res)
    }

    /** F-05: 매장 관리자 대시보드의 "주문 받기/일시정지" 스위치. */
    @PatchMapping("/{storeId}/status")
    fun updateStoreStatus(
        @AuthenticationPrincipal authenticatedStoreId: Long,
        @PathVariable storeId: Long,
        @Valid @RequestBody req: StoreDto.StatusUpdateRequest,
    ): ResponseEntity<StoreDto.StoreResponse> {
        val res = storeService.updateStoreStatus(authenticatedStoreId, storeId, req)
        return ResponseEntity.ok(res)
    }

    @PatchMapping("/{storeId}/preparation-time")
    fun updateStorePreparationTime(
        @AuthenticationPrincipal authenticatedStoreId: Long,
        @PathVariable storeId: Long,
        @Valid @RequestBody req: StoreDto.PreparationTimeUpdateRequest,
    ): ResponseEntity<StoreDto.StoreResponse> {
        val res = storeService.updatePreparationTime(authenticatedStoreId, storeId, req)
        return ResponseEntity.ok(res)
    }
}
