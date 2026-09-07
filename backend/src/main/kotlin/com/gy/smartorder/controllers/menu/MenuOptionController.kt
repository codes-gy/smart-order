package com.gy.smartorder.controllers.menu

import com.gy.smartorder.dtos.menu.MenuDto
import com.gy.smartorder.services.menu.MenuOptionService
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping
class MenuOptionController(
    private val menuOptionService: MenuOptionService,
) {

    @PostMapping("/menus/{menuId}/option-groups")
    fun createOptionGroup(
        @PathVariable menuId: Long,
        @Valid @RequestBody req: MenuDto.MenuOptionGroupCreateRequest,
    ): ResponseEntity<MenuDto.MenuOptionGroupResponse> {
        val res = menuOptionService.createOptionGroup(menuId, req)
        return ResponseEntity.status(HttpStatus.CREATED).body(res)
    }

    @PutMapping("/option-groups/{groupId}")
    fun updateOptionGroup(
        @PathVariable groupId: Long,
        @Valid @RequestBody req: MenuDto.MenuOptionGroupUpdateRequest,
    ): ResponseEntity<MenuDto.MenuOptionGroupResponse> {
        val res = menuOptionService.updateOptionGroup(groupId, req)
        return ResponseEntity.ok(res)
    }

    @DeleteMapping("/option-groups/{groupId}")
    fun deleteOptionGroup(
        @PathVariable groupId: Long,
    ): ResponseEntity<Void> {
        menuOptionService.deleteOptionGroup(groupId)
        return ResponseEntity.noContent().build()
    }

    @PostMapping("/option-groups/{groupId}/choices")
    fun createOptionChoice(
        @PathVariable groupId: Long,
        @Valid @RequestBody req: MenuDto.MenuOptionChoiceCreateRequest,
    ): ResponseEntity<MenuDto.MenuOptionChoiceResponse> {
        val res = menuOptionService.createOptionChoice(groupId, req)
        return ResponseEntity.status(HttpStatus.CREATED).body(res)
    }

    @PutMapping("/option-choices/{choiceId}")
    fun updateOptionChoice(
        @PathVariable choiceId: Long,
        @Valid @RequestBody req: MenuDto.MenuOptionChoiceUpdateRequest,
    ): ResponseEntity<MenuDto.MenuOptionChoiceResponse> {
        val res = menuOptionService.updateOptionChoice(choiceId, req)
        return ResponseEntity.ok(res)
    }

    @PatchMapping("/option-choices/{choiceId}/sold-out")
    fun updateOptionChoiceSoldOut(
        @PathVariable choiceId: Long,
        @Valid @RequestBody req: MenuDto.MenuOptionChoiceSoldOutUpdateRequest,
    ): ResponseEntity<MenuDto.MenuOptionChoiceResponse> {
        val res = menuOptionService.updateOptionChoiceSoldOut(choiceId, req)
        return ResponseEntity.ok(res)
    }

    @DeleteMapping("/option-choices/{choiceId}")
    fun deleteOptionChoice(
        @PathVariable choiceId: Long,
    ): ResponseEntity<Void> {
        menuOptionService.deleteOptionChoice(choiceId)
        return ResponseEntity.noContent().build()
    }
}
