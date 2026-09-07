package com.gy.smartorder.entities.member

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EntityListeners
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.LastModifiedDate
import org.springframework.data.jpa.domain.support.AuditingEntityListener
import com.fasterxml.jackson.annotation.JsonCreator
import java.time.LocalDateTime


enum class Role {
    USER,
    ADMIN,
}

enum class MemberStatus {
    ACTIVE,
    INACTIVE,
    BLOCKED

}

enum class SocialProvider {
    KAKAO,
    APPLE,
    NAVER,
    GOOGLE;

    companion object {
        // 프론트는 소문자("kakao"/"apple")로 보내므로 대소문자 구분 없이 매칭한다.
        @JsonCreator
        @JvmStatic
        fun from(value: String): SocialProvider = valueOf(value.uppercase())
    }
}

@Entity
@Table(
    name = "member",
    uniqueConstraints = [
        UniqueConstraint(columnNames = ["social_provider", "social_id"]),
    ],
    indexes = [
        Index(name = "idx_member_email", columnList = "email"),
        Index(name = "idx_member_phone", columnList = "phoneNumber")
    ]
)
@EntityListeners(AuditingEntityListener::class)
class Member(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    // 이메일/비밀번호 로그인 계정에만 존재 (소셜/SMS 전용 회원은 null)
    @Column(nullable = true, unique = true, length = 100)
    var email: String? = null,

    @Column(nullable = true)
    var password: String? = null,

    // 소셜 로그인 계정에만 존재
    @Enumerated(EnumType.STRING)
    @Column(name = "social_provider", nullable = true)
    var socialProvider: SocialProvider? = null,

    @Column(name = "social_id", nullable = true, length = 100)
    var socialId: String? = null,

    // SMS 인증 계정에만 존재
    @Column(nullable = true, unique = true, length = 20)
    var phoneNumber: String? = null,

    @Column(nullable = false, length = 50)
    var nickname: String,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var role: Role = Role.USER, // 권한 변경 가능

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var status: MemberStatus = MemberStatus.ACTIVE,

    @Column(name = "stamp_count", nullable = false)
    var stampCount: Int = 0,

)
{
    @CreatedDate
    @Column(nullable = false, updatable = false)
    var createdDate: LocalDateTime = LocalDateTime.now()
    protected set

    @LastModifiedDate
    @Column(nullable = false)
    var modifiedDate: LocalDateTime = LocalDateTime.now()
    protected set

    fun updateProfile(nickname: String, phoneNumber: String?) {
        this.nickname = nickname
        this.phoneNumber = phoneNumber
    }

    fun changePassword(encodedPassword: String) {
        this.password = encodedPassword
    }

    fun updateStatus(status: MemberStatus) {
        this.status = status
    }

    fun addStamp() {
        stampCount += 1
    }

    /** 스탬프 리워드 사용 처리. 개수 검증(목표치 도달 여부)은 MemberService에서 한다. */
    fun useStampReward(cost: Int) {
        stampCount -= cost
    }

    // email/social이 모두 없다는 것은 SMS 인증만으로 provision된 비회원(게스트) 계정이라는 뜻이다.
    val isGuest: Boolean
        get() = email == null && socialProvider == null

}