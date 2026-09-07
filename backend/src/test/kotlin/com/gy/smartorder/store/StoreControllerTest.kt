package com.gy.smartorder.store

import com.gy.smartorder.common.exception.NotFoundException
import com.gy.smartorder.config.passport.JwtAuthenticationEntryPoint
import com.gy.smartorder.config.passport.JwtTokenProvider
import com.gy.smartorder.config.security.SecurityConfig
import org.junit.jupiter.api.Test
import org.mockito.BDDMockito.given
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

/**
 * 컨트롤러 계층만 검증한다(`@WebMvcTest` — JPA/Redis/Kafka는 로드하지 않는다).
 * `GlobalExceptionHandler`는 `@ControllerAdvice`라 `@WebMvcTest`가 자동으로 스캔에 포함하지만,
 * `SecurityConfig`는 평범한 `@Configuration`이라 슬라이스가 스캔하지 않는다 — 명시적으로
 * `@Import`하지 않으면 스프링 시큐리티 기본값(전체 요청 인증 필요)이 적용돼 모든 요청이 401이 된다.
 * `JwtAuthenticationFilter`는 `@Component`라 슬라이스가 자동 스캔하지만, 그 생성자 의존성인
 * `JwtTokenProvider`(평범한 `@Component`)는 안 잡혀서 `@MockitoBean`으로 채워준다.
 * `JwtAuthenticationEntryPoint`도 슬라이스 화이트리스트 타입이 아니라 `@Import`가 필요하다
 * (AuthControllerTest와 동일한 패턴, PROGRESS.md 2.10절 참고).
 * 여기서 확인하는 JSON 필드명·구조(`result`/`meta`, `location.lat/lng`,
 * `estimatedPrepMinutes`, `isOpen` 등)는 프론트 `StoreSummary`/`StoreDetail`/`CursorResponse`
 * 타입(`src/types/store.types.ts`, `src/types/common.types.ts`)과 1:1로 맞춘 것이다.
 */
@WebMvcTest(StoreController::class)
@Import(SecurityConfig::class, JwtAuthenticationEntryPoint::class)
class StoreControllerTest(
    @Autowired private val mockMvc: MockMvc,
) {
    @MockitoBean
    private lateinit var storeService: StoreService

    @MockitoBean
    private lateinit var jwtTokenProvider: JwtTokenProvider

    private fun sampleDetail(isOpen: Boolean = true) = StoreDto.StoreDetailResponse(
        id = "1",
        name = "스마트오더 역삼역점",
        address = "서울특별시 강남구 역삼동",
        location = StoreDto.LocationDto(37.5006, 127.0364),
        distanceMeters = 0,
        waitingOrderCount = 0,
        estimatedPrepMinutes = 8,
        isOpen = isOpen,
        businessHours = "매일 07:00 - 22:00",
        phoneNumber = "02-1234-5678",
        description = "스페셜티 원두 카페",
    )

    /** `JwtAuthenticationFilter`가 정상 토큰 검증 시 심어주는 인증 정보와 동일한 모양(Long subject + ROLE_STORE_ADMIN)을 흉내낸다. */
    private fun storeAdminAuth(storeId: Long) =
        authentication(UsernamePasswordAuthenticationToken(storeId, null, listOf(SimpleGrantedAuthority("ROLE_STORE_ADMIN"))))

    @Test
    fun `매장 상세 조회는 프론트 StoreDetail 계약과 동일한 필드를 반환한다`() {
        given(storeService.getStore(1L)).willReturn(sampleDetail())

        mockMvc.perform(get("/stores/1"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.id").value("1"))
            .andExpect(jsonPath("$.name").value("스마트오더 역삼역점"))
            .andExpect(jsonPath("$.location.lat").value(37.5006))
            .andExpect(jsonPath("$.location.lng").value(127.0364))
            .andExpect(jsonPath("$.distanceMeters").value(0))
            .andExpect(jsonPath("$.waitingOrderCount").value(0))
            .andExpect(jsonPath("$.estimatedPrepMinutes").value(8))
            .andExpect(jsonPath("$.isOpen").value(true))
            .andExpect(jsonPath("$.businessHours").value("매일 07:00 - 22:00"))
            .andExpect(jsonPath("$.phoneNumber").value("02-1234-5678"))
            .andExpect(jsonPath("$.description").value("스페셜티 원두 카페"))
    }

    @Test
    fun `존재하지 않는 매장 상세 조회는 404와 ApiErrorBody를 반환한다`() {
        given(storeService.getStore(999L))
            .willThrow(NotFoundException("STORE_NOT_FOUND", "해당 매장을 찾을 수 없어요."))

        mockMvc.perform(get("/stores/999"))
            .andExpect(status().isNotFound)
            .andExpect(jsonPath("$.code").value("STORE_NOT_FOUND"))
            .andExpect(jsonPath("$.message").value("해당 매장을 찾을 수 없어요."))
    }

    @Test
    fun `숫자가 아닌 storeId로 조회하면 400을 반환한다`() {
        mockMvc.perform(get("/stores/abc"))
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.code").value("INVALID_PARAMETER"))
    }

    @Test
    fun `매장 리스트 조회는 result meta 봉투로 응답한다`() {
        val listResponse = StoreDto.StoreListResponse(
            result = listOf(
                StoreDto.StoreSummaryResponse(
                    id = "1",
                    name = "스마트오더 역삼역점",
                    address = "서울특별시 강남구 역삼동",
                    location = StoreDto.LocationDto(37.5006, 127.0364),
                    distanceMeters = 120,
                    waitingOrderCount = 2,
                    estimatedPrepMinutes = 8,
                    isOpen = true,
                ),
            ),
            meta = StoreDto.PageMetaDto(nextCursor = null, hasNext = false),
        )
        given(storeService.getStores(37.5, 127.0, null, 10)).willReturn(listResponse)

        mockMvc.perform(get("/stores").param("lat", "37.5").param("lng", "127.0"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.result[0].id").value("1"))
            .andExpect(jsonPath("$.result[0].distanceMeters").value(120))
            .andExpect(jsonPath("$.result[0].isOpen").value(true))
            .andExpect(jsonPath("$.meta.hasNext").value(false))
            .andExpect(jsonPath("$.meta.nextCursor").doesNotExist())
    }

    @Test
    fun `lat lng 파라미터 없이 리스트를 조회하면 400을 반환한다`() {
        mockMvc.perform(get("/stores"))
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.code").value("MISSING_PARAMETER"))
    }

    @Test
    fun `limit을 넘기지 않으면 기본값 10으로 서비스에 전달된다`() {
        given(storeService.getStores(37.5, 127.0, null, 10))
            .willReturn(StoreDto.StoreListResponse(emptyList(), StoreDto.PageMetaDto(null, false)))

        mockMvc.perform(get("/stores").param("lat", "37.5").param("lng", "127.0"))
            .andExpect(status().isOk)
    }

    @Test
    fun `매장 영업상태를 PAUSED로 변경하면 200을 반환한다`() {
        val now = java.time.LocalDateTime.of(2026, 1, 1, 0, 0)
        given(storeService.updateStoreStatus(1L, 1L, StoreDto.StatusUpdateRequest(StoreStatus.PAUSED))).willReturn(
            StoreDto.StoreResponse(
                id = 1L, name = "역삼역점", address = "서울시", addressDetail = null,
                phone = "02-0000-0000", status = StoreStatus.PAUSED, businessNumber = "123-45-67890",
                latitude = 37.5, longitude = 127.0, estimatedPreparationMinutes = 8,
                isAutoAccept = true, description = "", openTime = java.time.LocalTime.of(7, 0),
                closeTime = java.time.LocalTime.of(22, 0),
                createdAt = now, updatedAt = now,
            ),
        )

        mockMvc.perform(
            patch("/stores/1/status")
                .with(storeAdminAuth(1L))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"status":"PAUSED"}"""),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.status").value("PAUSED"))
    }

    @Test
    fun `PREPARING은 더 이상 유효한 매장 상태가 아니므로 400을 반환한다`() {
        mockMvc.perform(
            patch("/stores/1/status")
                .with(storeAdminAuth(1L))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"status":"PREPARING"}"""),
        ).andExpect(status().isBadRequest)
    }

    @Test
    fun `status 없이 요청하면 400을 반환한다`() {
        mockMvc.perform(
            patch("/stores/1/status")
                .with(storeAdminAuth(1L))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{}"""),
        ).andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
    }

    @Test
    fun `존재하지 않는 매장의 상태를 변경하면 404를 반환한다`() {
        given(storeService.updateStoreStatus(999L, 999L, StoreDto.StatusUpdateRequest(StoreStatus.PAUSED)))
            .willThrow(NotFoundException("STORE_NOT_FOUND", "해당 매장을 찾을 수 없어요."))

        mockMvc.perform(
            patch("/stores/999/status")
                .with(storeAdminAuth(999L))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"status":"PAUSED"}"""),
        ).andExpect(status().isNotFound)
    }
}
