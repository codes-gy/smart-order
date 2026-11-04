package com.gy.smartorder.config

import org.hamcrest.Matchers.not
import org.hamcrest.Matchers.containsString
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.content
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

/**
 * 운영 정보 노출 회귀 테스트.
 * 예전에는 actuator 경로 전체가 인증 없이 열리고 health 상세(DB · Redis 상태)까지 보였다.
 * 지금은 health의 UP/DOWN만 공개하고, metrics · info · prometheus는 인증 없이 닿지 않아야 한다.
 * 테스트에는 Redis가 없어 Redis 헬스 지표는 끈다(상세를 숨겨도 전체 상태는 하위 지표를 합쳐 계산하므로).
 */
@SpringBootTest(properties = ["management.health.redis.enabled=false"])
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ActuatorExposureTest(
    @Autowired private val mockMvc: MockMvc,
) {
    @Test
    fun `health는 인증 없이 상태만 보인다`() {
        mockMvc.perform(get("/actuator/health"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.status").value("UP"))
            .andExpect(jsonPath("$.components").doesNotExist())
            .andExpect(content().string(not(containsString("db"))))
    }

    @Test
    fun `metrics는 인증 없이 닿지 않는다`() {
        mockMvc.perform(get("/actuator/metrics"))
            .andExpect(status().isUnauthorized)
    }

    @Test
    fun `info는 인증 없이 닿지 않는다`() {
        mockMvc.perform(get("/actuator/info"))
            .andExpect(status().isUnauthorized)
    }

    @Test
    fun `prometheus는 인증 없이 닿지 않는다`() {
        mockMvc.perform(get("/actuator/prometheus"))
            .andExpect(status().isUnauthorized)
    }
}
