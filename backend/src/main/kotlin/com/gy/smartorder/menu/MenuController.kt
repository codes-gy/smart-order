package com.gy.smartorder.menu

import com.gy.smartorder.menu.MenuDto
import com.gy.smartorder.menu.MenuService
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/menus")
class MenuController(
    private val menuService: MenuService,
) {

    @PostMapping
    fun createMenu(
        @Valid @RequestBody req: MenuDto.MenuCreateRequest,
    ): ResponseEntity<MenuDto.MenuResponse> {
        val res = menuService.createMenu(req)
        return ResponseEntity.status(HttpStatus.CREATED).body(res)
    }

    @GetMapping("/{menuId}")
    fun getMenu(
        @PathVariable menuId: Long,
    ): ResponseEntity<MenuDto.MenuResponse> {
        val res = menuService.getMenu(menuId)
        return ResponseEntity.ok(res)
    }

    @GetMapping
    fun getMenusByCategory(
        @RequestParam categoryId: Long,
    ): ResponseEntity<List<MenuDto.MenuResponse>> {
        val res = menuService.getMenusByCategory(categoryId)
        return ResponseEntity.ok(res)
    }

    @PutMapping("/{menuId}")
    fun updateMenu(
        @PathVariable menuId: Long,
        @Valid @RequestBody req: MenuDto.MenuUpdateRequest,
    ): ResponseEntity<MenuDto.MenuResponse> {
        val res = menuService.updateMenu(menuId, req)
        return ResponseEntity.ok(res)
    }

    @PatchMapping("/{menuId}/status")
    fun updateMenuStatus(
        @PathVariable menuId: Long,
        @Valid @RequestBody req: MenuDto.MenuStatusUpdateRequest,
    ): ResponseEntity<MenuDto.MenuResponse> {
        val res = menuService.updateMenuStatus(menuId, req)
        return ResponseEntity.ok(res)
    }

    @DeleteMapping("/{menuId}")
    fun deleteMenu(
        @PathVariable menuId: Long,
    ): ResponseEntity<Void> {
        menuService.deleteMenu(menuId)
        return ResponseEntity.noContent().build()
    }
}