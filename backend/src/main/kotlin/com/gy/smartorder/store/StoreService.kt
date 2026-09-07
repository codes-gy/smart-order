package com.gy.smartorder.store

import com.gy.smartorder.common.CursorUtils
import com.gy.smartorder.common.GeoUtils
import com.gy.smartorder.common.exception.ConflictException
import com.gy.smartorder.common.exception.ForbiddenException
import com.gy.smartorder.common.exception.NotFoundException
import org.springframework.data.repository.findByIdOrNull
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import kotlin.math.roundToLong

@Service
@Transactional(readOnly = true)
class StoreService(
    private val storeRepository: StoreRepository,
    private val storeAccountRepository: StoreAccountRepository,
    private val passwordEncoder: PasswordEncoder,
) {
    // 매장 생성과 매장 관리자(StoreAccount) 계정 발급을 같은 트랜잭션에서 함께 처리한다 —
    // PROGRESS.md 4절 5번에서 별도 프로비저닝 API 대신 이 방식으로 결정.
    @Transactional
    fun createStore(req: StoreDto.StoreCreateRequest): StoreDto.StoreResponse {

        if(storeRepository.existsByBusinessNumber(req.businessNumber)) {
            throw IllegalArgumentException("이미 등록된 사업자번호입니다.")
        }
        if (storeAccountRepository.findByStoreCode(req.storeCode) != null) {
            throw ConflictException("STORE_CODE_ALREADY_EXISTS", "이미 사용 중인 매장 코드입니다.")
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

        storeAccountRepository.save(
            StoreAccount(
                storeId = savedStore.id,
                storeCode = req.storeCode,
                password = passwordEncoder.encode(req.storeAccountPassword)!!,
            ),
        )

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
    fun updateStore(authenticatedStoreId: Long, storeId: Long, req: StoreDto.StoreUpdateRequest): StoreDto.StoreResponse {
        requireOwnStore(authenticatedStoreId, storeId)
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
    fun updateStoreStatus(authenticatedStoreId: Long, storeId: Long, req: StoreDto.StatusUpdateRequest): StoreDto.StoreResponse {
        requireOwnStore(authenticatedStoreId, storeId)
        val store = findStoreOrThrow(storeId)
        store.updateStatus(req.status!!)
        return StoreDto.StoreResponse.from(store)
    }

    @Transactional
    fun updatePreparationTime(authenticatedStoreId: Long, storeId: Long, req: StoreDto.PreparationTimeUpdateRequest): StoreDto.StoreResponse {
        requireOwnStore(authenticatedStoreId, storeId)
        val store = findStoreOrThrow(storeId)
        store.updatePreparationTime(req.preparationMinutes!!)
        return StoreDto.StoreResponse.from(store)
    }

    private fun findStoreOrThrow(storeId: Long): Store =
        storeRepository.findByIdOrNull(storeId)
            ?: throw NotFoundException("STORE_NOT_FOUND", "해당 매장을 찾을 수 없어요. id=$storeId")

    // STORE_ADMIN 토큰의 subject(storeId)가 수정하려는 매장과 일치하는지 확인 — 다른 매장 계정으로
    // 로그인한 관리자가 남의 매장을 수정하지 못하도록 막는다(SecurityConfig의 hasRole만으로는 이 부분을 못 막음).
    private fun requireOwnStore(authenticatedStoreId: Long, storeId: Long) {
        if (authenticatedStoreId != storeId) {
            throw ForbiddenException("STORE_ACCESS_DENIED", "해당 매장에 대한 권한이 없습니다.")
        }
    }

    companion object {
        private const val DEFAULT_PAGE_SIZE = 10
        private const val MAX_PAGE_SIZE = 100
    }
}
