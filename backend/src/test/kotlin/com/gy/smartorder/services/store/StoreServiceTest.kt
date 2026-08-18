package com.gy.smartorder.services.store

import com.gy.smartorder.common.exception.NotFoundException
import com.gy.smartorder.dtos.store.StoreDto
import com.gy.smartorder.entities.store.Store
import com.gy.smartorder.entities.store.StoreStatus
import com.gy.smartorder.repositories.store.StoreRepository
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest
import java.time.LocalTime
import java.util.concurrent.atomic.AtomicInteger

/**
 * StoreService를 실제 StoreRepository + 임베디드 H2로 검증한다(Mockito 등 별도 mocking 없이).
 * `@DataJpaTest`는 JPA 관련 빈만 올리므로 Redis/Kafka/Security 자동 설정과 무관하게 가볍게 돈다.
 */
@DataJpaTest
class StoreServiceTest @Autowired constructor(
    private val storeRepository: StoreRepository,
) {
    private lateinit var storeService: StoreService
    private val businessNumberSeq = AtomicInteger(10000)

    @BeforeEach
    fun setUp() {
        storeService = StoreService(storeRepository)
        businessNumberSeq.set(10000)
    }

    private fun saveStore(
        name: String = "역삼역점",
        lat: Double = 37.5006,
        lng: Double = 127.0364,
        status: StoreStatus = StoreStatus.OPEN,
        prepMinutes: Int = 8,
        description: String = "스페셜티 원두 카페",
        businessNumber: String = "123-45-${businessNumberSeq.getAndIncrement()}",
    ): Store = storeRepository.save(
        Store(
            name = name,
            address = "서울특별시 강남구 $name 1길 10",
            phone = "02-1234-5678",
            businessNumber = businessNumber,
            status = status,
            latitude = lat,
            longitude = lng,
            estimatedPreparationMinutes = prepMinutes,
            description = description,
            openTime = LocalTime.of(7, 0),
            closeTime = LocalTime.of(22, 0),
        ),
    )

    @Test
    fun `매장을 생성하면 저장되고 조회로 다시 찾을 수 있다`() {
        val req = StoreDto.StoreCreateRequest(
            name = "선릉점",
            address = "서울특별시 강남구 선릉로 1",
            phone = "02-1111-2222",
            businessNumber = "999-88-77777",
            latitude = 37.5044,
            longitude = 127.0490,
            openTime = LocalTime.of(7, 0),
            closeTime = LocalTime.of(22, 0),
        )

        val created = storeService.createStore(req)

        assertThat(created.id).isGreaterThan(0)
        assertThat(storeRepository.findById(created.id)).isPresent
        assertThat(storeRepository.findById(created.id).get().name).isEqualTo("선릉점")
    }

    @Test
    fun `매장 상세 조회는 프론트 StoreDetail 계약에 맞는 필드를 채워 반환한다`() {
        val store = saveStore()

        val detail = storeService.getStore(store.id)

        assertThat(detail.id).isEqualTo(store.id.toString())
        assertThat(detail.location.lat).isEqualTo(store.latitude)
        assertThat(detail.location.lng).isEqualTo(store.longitude)
        assertThat(detail.businessHours).isEqualTo("매일 07:00 - 22:00")
        assertThat(detail.phoneNumber).isEqualTo(store.phone)
        assertThat(detail.description).isEqualTo(store.description)
        assertThat(detail.isOpen).isTrue()
        assertThat(detail.distanceMeters).isEqualTo(0)
    }

    @Test
    fun `존재하지 않는 매장을 조회하면 NotFoundException을 던진다`() {
        assertThatThrownBy { storeService.getStore(999_999L) }
            .isInstanceOf(NotFoundException::class.java)
            .hasMessageContaining("999999")
    }

    @Test
    fun `매장 상태가 PAUSED면 isOpen이 false로 내려간다`() {
        val store = saveStore(status = StoreStatus.PAUSED)

        val detail = storeService.getStore(store.id)

        assertThat(detail.isOpen).isFalse()
    }

    @Test
    fun `StoreStatus는 매장 영업 상태 3가지만 허용한다`() {
        // PRD의 매장 영업 상태 변경 API는 OPEN PAUSED CLOSED만 다룬다.
        // PREPARING(조리 중)은 주문 상태 개념이라 여기 다시 섞여 들어가면 안 된다.
        assertThat(StoreStatus.entries.map { it.name })
            .containsExactlyInAnyOrder("OPEN", "CLOSED", "PAUSED")
    }

    @Test
    fun `매장 목록은 사용자 좌표로부터 가까운 순으로 정렬된다`() {
        val far = saveStore(name = "먼매장", lat = 37.6006, lng = 127.1364)
        val near = saveStore(name = "가까운매장", lat = 37.5007, lng = 127.0365)
        val mid = saveStore(name = "중간매장", lat = 37.5100, lng = 127.0450)

        val page = storeService.getStores(lat = 37.5006, lng = 127.0364, cursor = null, limit = 10)

        assertThat(page.result.map { it.id })
            .containsExactly(near.id.toString(), mid.id.toString(), far.id.toString())
        assertThat(page.meta.hasNext).isFalse()
        assertThat(page.meta.nextCursor).isNull()
    }

    @Test
    fun `매장 목록은 닫혀있는 매장도 isOpen false로 포함한다`() {
        saveStore(name = "영업중", status = StoreStatus.OPEN)
        saveStore(name = "일시정지", status = StoreStatus.PAUSED)

        val page = storeService.getStores(lat = 37.5006, lng = 127.0364, cursor = null, limit = 10)

        assertThat(page.result).hasSize(2)
        assertThat(page.result.map { it.isOpen }).containsExactlyInAnyOrder(true, false)
    }

    @Test
    fun `매장 목록은 limit과 cursor로 페이지를 나눠 반환한다`() {
        repeat(3) { index ->
            saveStore(name = "매장$index", lat = 37.5006 + index * 0.001, lng = 127.0364 + index * 0.001)
        }

        val firstPage = storeService.getStores(lat = 37.5006, lng = 127.0364, cursor = null, limit = 2)
        assertThat(firstPage.result).hasSize(2)
        assertThat(firstPage.meta.hasNext).isTrue()
        assertThat(firstPage.meta.nextCursor).isNotNull()

        val secondPage = storeService.getStores(
            lat = 37.5006,
            lng = 127.0364,
            cursor = firstPage.meta.nextCursor,
            limit = 2,
        )
        assertThat(secondPage.result).hasSize(1)
        assertThat(secondPage.meta.hasNext).isFalse()
        assertThat(secondPage.meta.nextCursor).isNull()

        val allIds = firstPage.result.map { it.id } + secondPage.result.map { it.id }
        assertThat(allIds).doesNotHaveDuplicates()
    }

    @Test
    fun `매장 영업상태를 변경할 수 있다`() {
        val store = saveStore(status = StoreStatus.OPEN)

        val updated = storeService.updateStoreStatus(store.id, StoreDto.StatusUpdateRequest(StoreStatus.PAUSED))

        assertThat(updated.status).isEqualTo(StoreStatus.PAUSED)
        assertThat(storeRepository.findById(store.id).get().status).isEqualTo(StoreStatus.PAUSED)
    }

    @Test
    fun `존재하지 않는 매장의 상태를 바꾸려 하면 NotFoundException을 던진다`() {
        assertThatThrownBy {
            storeService.updateStoreStatus(999_999L, StoreDto.StatusUpdateRequest(StoreStatus.PAUSED))
        }.isInstanceOf(NotFoundException::class.java)
    }

    @Test
    fun `예상 조리 시간을 변경할 수 있다`() {
        val store = saveStore(prepMinutes = 5)

        val updated = storeService.updatePreparationTime(store.id, StoreDto.PreparationTimeUpdateRequest(15))

        assertThat(updated.estimatedPreparationMinutes).isEqualTo(15)
    }

    @Test
    fun `매장 정보를 수정할 수 있다`() {
        val store = saveStore()

        val updated = storeService.updateStore(
            store.id,
            StoreDto.StoreUpdateRequest(
                name = "역삼역점(리뉴얼)",
                address = "서울특별시 강남구 새주소 1",
                addressDetail = "2층",
                phone = "02-9999-0000",
                latitude = 37.51,
                longitude = 127.05,
                description = "리뉴얼 오픈",
                openTime = LocalTime.of(8, 0),
                closeTime = LocalTime.of(21, 0),
            ),
        )

        assertThat(updated.name).isEqualTo("역삼역점(리뉴얼)")
        assertThat(updated.addressDetail).isEqualTo("2층")
        assertThat(updated.description).isEqualTo("리뉴얼 오픈")
    }
}
