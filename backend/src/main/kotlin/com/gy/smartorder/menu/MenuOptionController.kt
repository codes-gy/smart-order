package com.gy.smartorder.menu

import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController

/** 매장 관리자용 메뉴 옵션 그룹/선택지 CRUD. 조회는 기존 `GET /menus/{menuId}` 응답에 이미 포함돼 있다. */
@RestController
class MenuOptionController(
    private val menuOptionService: MenuOptionService,
) {

    @PostMapping("/menus/{menuId}/option-groups")
    fun createOptionGroup(
        @AuthenticationPrincipal authenticatedStoreId: Long,
        @PathVariable menuId: Long,
        @Valid @RequestBody req: MenuDto.MenuOptionGroupCreateRequest,
    ): ResponseEntity<MenuDto.MenuOptionGroupResponse> {
        val res = menuOptionService.createOptionGroup(authenticatedStoreId, menuId, req)
        return ResponseEntity.status(HttpStatus.CREATED).body(res)
    }

    @PatchMapping("/option-groups/{groupId}")
    fun updateOptionGroup(
        @AuthenticationPrincipal authenticatedStoreId: Long,
        @PathVariable groupId: Long,
        @Valid @RequestBody req: MenuDto.MenuOptionGroupUpdateRequest,
    ): ResponseEntity<MenuDto.MenuOptionGroupResponse> {
        val res = menuOptionService.updateOptionGroup(authenticatedStoreId, groupId, req)
        return ResponseEntity.ok(res)
    }

    @DeleteMapping("/option-groups/{groupId}")
    fun deleteOptionGroup(
        @AuthenticationPrincipal authenticatedStoreId: Long,
        @PathVariable groupId: Long,
    ): ResponseEntity<Void> {
        menuOptionService.deleteOptionGroup(authenticatedStoreId, groupId)
        return ResponseEntity.noContent().build()
    }

    @PostMapping("/option-groups/{groupId}/choices")
    fun createOptionChoice(
        @AuthenticationPrincipal authenticatedStoreId: Long,
        @PathVariable groupId: Long,
        @Valid @RequestBody req: MenuDto.MenuOptionChoiceCreateRequest,
    ): ResponseEntity<MenuDto.MenuOptionChoiceResponse> {
        val res = menuOptionService.createOptionChoice(authenticatedStoreId, groupId, req)
        return ResponseEntity.status(HttpStatus.CREATED).body(res)
    }

    @PatchMapping("/option-choices/{choiceId}")
    fun updateOptionChoice(
        @AuthenticationPrincipal authenticatedStoreId: Long,
        @PathVariable choiceId: Long,
        @Valid @RequestBody req: MenuDto.MenuOptionChoiceUpdateRequest,
    ): ResponseEntity<MenuDto.MenuOptionChoiceResponse> {
        val res = menuOptionService.updateOptionChoice(authenticatedStoreId, choiceId, req)
        return ResponseEntity.ok(res)
    }

    @PatchMapping("/option-choices/{choiceId}/sold-out")
    fun updateOptionChoiceSoldOut(
        @AuthenticationPrincipal authenticatedStoreId: Long,
        @PathVariable choiceId: Long,
        @Valid @RequestBody req: MenuDto.MenuOptionChoiceSoldOutUpdateRequest,
    ): ResponseEntity<MenuDto.MenuOptionChoiceResponse> {
        val res = menuOptionService.updateOptionChoiceSoldOut(authenticatedStoreId, choiceId, req)
        return ResponseEntity.ok(res)
    }

    @DeleteMapping("/option-choices/{choiceId}")
    fun deleteOptionChoice(
        @AuthenticationPrincipal authenticatedStoreId: Long,
        @PathVariable choiceId: Long,
    ): ResponseEntity<Void> {
        menuOptionService.deleteOptionChoice(authenticatedStoreId, choiceId)
        return ResponseEntity.noContent().build()
    }
}
