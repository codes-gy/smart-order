package com.gy.smartorder.services.store

import com.gy.smartorder.common.CursorUtils
import com.gy.smartorder.common.GeoUtils
import com.gy.smartorder.common.exception.NotFoundException
import com.gy.smartorder.dtos.store.StoreDto
import com.gy.smartorder.entities.store.Store
import com.gy.smartorder.entities.store.StoreStatus
import com.gy.smartorder.repositories.store.StoreRepository
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import kotlin.math.roundToLong

@Service
@Transactional(readOnly = true)
class StoreService(
    private val storeRepository: StoreRepository,
) {
    @Transactional
    fun createStore(req: StoreDto.StoreCreateRequest): StoreDto.StoreResponse {

        if(storeRepository.existsByBusinessNumber(req.businessNumber)) {
            throw IllegalArgumentException("이미 등록된 사업자번호입니다.")
        }

        val store = Store(
            name = req.name,
            address = req.address,
            addressDetail = req.addressDetail,
            phone = req.phone,
            businessNumber = req.businessNumber,
            latitude = req.latitude,
            longitude = req.longitude,
            estimatedPreparationMinutes = req.estimatedPreparationMinutes,
            isAutoAccept = req.isAutoAccept,
            description = req.description,
            openTime = req.openTime,
            closeTime = req.closeTime,
        )
        val savedStore = storeRepository.save(store)
        return StoreDto.StoreResponse.from(savedStore)
    }

    fun getStore(storeId: Long): StoreDto.StoreDetailResponse {
        val store = findStoreOrThrow(storeId)
        return StoreDto.StoreDetailResponse.from(store)
    }

    /**
     * 사용자 좌표(lat/lng) 기준 거리순 정렬 + 커서 페이징 매장 목록.
     *
     * 지금은 전체 매장을 메모리로 읽어와 거리를 계산·정렬한다(프론트 mock인 `mockFetchStores`와
     * 동일한 접근). 매장 수가 커지면 DB 레벨 bounding-box 필터 + ORDER BY로 옮겨야 한다.
     */
    fun getStores(lat: Double, lng: Double, cursor: String?, limit: Int): StoreDto.StoreListResponse {
        val pageSize = limit.coerceIn(1, MAX_PAGE_SIZE)
        val offset = CursorUtils.decodeOffset(cursor)

        val sorted = storeRepository.findAll()
            .map { store -> store to GeoUtils.haversineMeters(lat, lng, store.latitude, store.longitude) }
            .sortedBy { (_, distance) -> distance }

        val page = sorted.drop(offset).take(pageSize)
        val nextOffset = offset + pageSize
        val hasNext = nextOffset < sorted.size

        val result = page.map { (store, distance) ->
            StoreDto.StoreSummaryResponse(
                id = store.id.toString(),
                name = store.name,
                address = store.address,
                location = StoreDto.LocationDto(store.latitude, store.longitude),
                distanceMeters = distance.roundToLong(),
                waitingOrderCount = 0, // 매장별 활성 주문 수 집계
                estimatedPrepMinutes = store.estimatedPreparationMinutes,
                isOpen = store.status == StoreStatus.OPEN,
            )
        }

        return StoreDto.StoreListResponse(
            result = result,
            meta = StoreDto.PageMetaDto(
                nextCursor = if (hasNext) CursorUtils.encode(nextOffset) else null,
                hasNext = hasNext,
            ),
        )
    }

    @Transactional
    fun updateStore(storeId: Long, req: StoreDto.StoreUpdateRequest): StoreDto.StoreResponse {
        val store = findStoreOrThrow(storeId)
        store.updateInfo(
            name = req.name,
            address = req.address,
            addressDetail = req.addressDetail,
            phone = req.phone,
            latitude = req.latitude,
            longitude = req.longitude,
            openTime = req.openTime,
            closeTime = req.closeTime,
            description = req.description,
        )
        return StoreDto.StoreResponse.from(store)
    }

    @Transactional
    fun updateStoreStatus(storeId: Long, req: StoreDto.StatusUpdateRequest): StoreDto.StoreResponse {
        val store = findStoreOrThrow(storeId)
        store.updateStatus(req.status!!)
        return StoreDto.StoreResponse.from(store)
    }

    @Transactional
    fun updatePreparationTime(storeId: Long, req: StoreDto.PreparationTimeUpdateRequest): StoreDto.StoreResponse {
        val store = findStoreOrThrow(storeId)
        store.updatePreparationTime(req.preparationMinutes!!)
        return StoreDto.StoreResponse.from(store)
    }

    private fun findStoreOrThrow(storeId: Long): Store =
        storeRepository.findByIdOrNull(storeId)
            ?: throw NotFoundException("STORE_NOT_FOUND", "해당 매장을 찾을 수 없어요. id=$storeId")

    companion object {
        private const val DEFAULT_PAGE_SIZE = 10
        private const val MAX_PAGE_SIZE = 100
    }
}
