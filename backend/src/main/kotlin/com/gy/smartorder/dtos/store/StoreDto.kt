package com.gy.smartorder.dtos.store

import com.gy.smartorder.entities.store.Store
import com.gy.smartorder.entities.store.StoreStatus
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import org.hibernate.validator.constraints.Range
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter

class StoreDto {

    data class StoreCreateRequest(

        @field:NotBlank(message = "점포명은 필수입니다.")
        val name: String,

        @field:NotBlank(message = "주소는 필수입니다.")
        val address: String,

        val addressDetail: String? = null,

        @field:NotBlank(message = "전화번호는 필수입니다.")
        val phone: String,

        @field:NotBlank(message = "사업자번호는 필수입니다.")
        val businessNumber: String,

        @field:Range(min = -90, max = 90, message = "위도는 -90~90 사이의 값이어야 합니다.")
        val latitude: Double,

        @field:Range(min = -180, max = 180, message = "경도는 -180~180 사이의 값이어야 합니다.")
        val longitude: Double,

        @field:Min(value = 0, message = "조리 예상 시간은 0분 이상이어야 합니다.")
        val estimatedPreparationMinutes: Int = 10,

        val isAutoAccept: Boolean = true,

        val description: String = "",

        @field:NotNull(message = "오픈 시간은 필수입니다.")
        val openTime: LocalTime,

        @field:NotNull(message = "마감 시간은 필수입니다.")
        val closeTime: LocalTime,

        // 매장 생성과 동시에 매장 관리자(StoreAccount) 로그인 계정을 함께 발급한다(PROGRESS.md 4절 5번 결정).
        @field:NotBlank(message = "매장 코드는 필수입니다.")
        val storeCode: String,

        @field:NotBlank(message = "매장 관리자 비밀번호는 필수입니다.")
        val storeAccountPassword: String,
    )

    // 점포 전체 정보 수정 요청 DTO
    data class StoreUpdateRequest(

        @field:NotBlank(message = "점포명은 필수입니다.")
        val name: String,

        @field:NotBlank(message = "주소는 필수입니다.")
        val address: String,

        val addressDetail: String? = null,

        @field:NotBlank(message = "전화번호는 필수입니다.")
        val phone: String,

        @field:Range(min = -90, max = 90, message = "위도는 -90~90 사이의 값이어야 합니다.")
        val latitude: Double,

        @field:Range(min = -180, max = 180, message = "경도는 -180~180 사이의 값이어야 합니다.")
        val longitude: Double,

        val description: String = "",

        @field:NotNull(message = "오픈 시간은 필수입니다.")
        val openTime: LocalTime,

        @field:NotNull(message = "마감 시간은 필수입니다.")
        val closeTime: LocalTime,
    )

    data class StatusUpdateRequest(
        @field:NotNull(message = "변경할 상태는 필수입니다.")
        val status: StoreStatus?,
    )

    // 조리 예상 시간 변경 요청 DTO
    data class PreparationTimeUpdateRequest(
        @field:NotNull(message = "조리 예상 시간은 필수입니다.")
        @field:Min(value = 0, message = "조리 예상 시간은 0분 이상이어야 합니다.")
        val preparationMinutes: Int?,
    )

    data class StoreResponse(
        val id: Long,
        val name: String,
        val address: String,
        val addressDetail: String?,
        val phone: String,
        val status: StoreStatus,
        val businessNumber: String,
        val latitude: Double,
        val longitude: Double,
        val estimatedPreparationMinutes: Int,
        val isAutoAccept: Boolean,
        val description: String,
        val openTime: LocalTime,
        val closeTime: LocalTime,
        val createdAt: LocalDateTime,
        val updatedAt: LocalDateTime,
    ) {
        companion object {
            fun from(store: Store): StoreResponse = StoreResponse(
                id = store.id,
                name = store.name,
                address = store.address,
                addressDetail = store.addressDetail,
                phone = store.phone,
                status = store.status,
                businessNumber = store.businessNumber,
                latitude = store.latitude,
                longitude = store.longitude,
                estimatedPreparationMinutes = store.estimatedPreparationMinutes,
                isAutoAccept = store.isAutoAccept,
                description = store.description,
                openTime = store.openTime,
                closeTime = store.closeTime,
                createdAt = store.createdAt,
                updatedAt = store.updatedAt,
            )
        }
    }

    data class LocationDto(
        val lat: Double,
        val lng: Double,
    )

    data class StoreSummaryResponse(
        val id: String,
        val name: String,
        val address: String,
        val location: LocationDto,
        val distanceMeters: Long,
        val waitingOrderCount: Int,
        val estimatedPrepMinutes: Int,
        val isOpen: Boolean,
    )

    data class StoreDetailResponse(
        val id: String,
        val name: String,
        val address: String,
        val location: LocationDto,
        val distanceMeters: Long,
        val waitingOrderCount: Int,
        val estimatedPrepMinutes: Int,
        val isOpen: Boolean,
        val businessHours: String,
        val phoneNumber: String,
        val description: String,
    ) {
        companion object {
            private val HOUR_FORMAT = DateTimeFormatter.ofPattern("HH:mm")

            fun formatBusinessHours(openTime: LocalTime, closeTime: LocalTime): String =
                "매일 ${openTime.format(HOUR_FORMAT)} - ${closeTime.format(HOUR_FORMAT)}"


            fun from(store: Store): StoreDetailResponse = StoreDetailResponse(
                id = store.id.toString(),
                name = store.name,
                address = store.address,
                location = LocationDto(store.latitude, store.longitude),
                distanceMeters = 0,
                waitingOrderCount = 0,
                estimatedPrepMinutes = store.estimatedPreparationMinutes,
                isOpen = store.status == StoreStatus.OPEN,
                businessHours = formatBusinessHours(store.openTime, store.closeTime),
                phoneNumber = store.phone,
                description = store.description,
            )
        }
    }

    data class PageMetaDto(
        val nextCursor: String?,
        val hasNext: Boolean,
    )


    data class StoreListResponse(
        val result: List<StoreSummaryResponse>,
        val meta: PageMetaDto,
    )
}
