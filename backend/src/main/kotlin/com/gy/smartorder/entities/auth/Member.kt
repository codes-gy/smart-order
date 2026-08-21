package com.gy.smartorder.entities.auth

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EntityListeners
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.LastModifiedDate
import org.springframework.data.jpa.domain.support.AuditingEntityListener
import java.time.LocalDateTime


enum class Role {
    USER,
    ADMIN,
}

enum class MemberStatus {
    ACTIVE,
    INACTIVE,
    DELETED,
    BLOCKED

}

enum class SocialProvider {
    KAKAO,
    APPLE,
}

@Entity
@Table(
    name = "member",
    uniqueConstraints = [
        UniqueConstraint(columnNames = ["social_provider", "social_id"]),
    ],
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

    @CreatedDate
    @Column(nullable = false, updatable = false)
    var createdDate: LocalDateTime = LocalDateTime.now(),

    @LastModifiedDate
    @Column(nullable = false)
    var modifiedDate: LocalDateTime = LocalDateTime.now(),
)
{



}