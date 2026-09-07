package com.gy.smartorder.menu

import com.gy.smartorder.common.exception.NotFoundException
import com.gy.smartorder.menu.MenuDto
import com.gy.smartorder.category.Category
import com.gy.smartorder.menu.Menu
import com.gy.smartorder.category.CategoryRepository
import com.gy.smartorder.menu.MenuRepository
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class MenuService(
    private val menuRepository: MenuRepository,
    private val categoryRepository: CategoryRepository,
)
{
    @Transactional
    fun createMenu(req: MenuDto.MenuCreateRequest): MenuDto.MenuResponse {
        val categoryId = req.categoryId!!
        val category = findCategoryOrThrow(categoryId)

        if (menuRepository.existsByCategoryIdAndName(categoryId, req.name)) {
            throw IllegalArgumentException("해당 카테고리에 이미 동일한 메뉴명이 존재합니다.")
        }

        val menu = Menu(
            category = category,
            name = req.name,
            price = req.price!!,
            description = req.description,
            imageUrl = req.imageUrl,
            isPopular = req.isPopular,
            displayOrder = req.displayOrder,
        )
        val savedMenu = menuRepository.save(menu)
        return MenuDto.MenuResponse.from(savedMenu)
    }

    fun getMenu(menuId: Long): MenuDto.MenuResponse {
        val menu = findMenuOrThrow(menuId)
        return MenuDto.MenuResponse.from(menu)
    }

    fun getMenusByCategory(categoryId: Long): List<MenuDto.MenuResponse> {
        val menus = menuRepository.findByCategoryIdOrderByDisplayOrderAsc(categoryId)
        return menus.map { MenuDto.MenuResponse.from(it) }
    }

    @Transactional
    fun updateMenu(menuId: Long, req: MenuDto.MenuUpdateRequest): MenuDto.MenuResponse {
        val menu = findMenuOrThrow(menuId)
        val category = findCategoryOrThrow(req.categoryId!!)

        menu.updateInfo(
            category = category,
            name = req.name,
            price = req.price!!,
            description = req.description,
            imageUrl = req.imageUrl,
            isPopular = req.isPopular,
            displayOrder = req.displayOrder,
        )
        return MenuDto.MenuResponse.from(menu)
    }

    @Transactional
    fun updateMenuStatus(menuId: Long, req: MenuDto.MenuStatusUpdateRequest): MenuDto.MenuResponse {
        val menu = findMenuOrThrow(menuId)
        menu.updateStatus(req.status!!)
        return MenuDto.MenuResponse.from(menu)
    }

    @Transactional
    fun deleteMenu(menuId: Long) {
        val menu = findMenuOrThrow(menuId)
        menuRepository.delete(menu)
    }

    private fun findCategoryOrThrow(categoryId: Long): Category =
        categoryRepository.findByIdOrNull(categoryId)
            ?: throw NotFoundException("CATEGORY_NOT_FOUND", "해당 카테고리를 찾을 수 없습니다. id=$categoryId")

    private fun findMenuOrThrow(menuId: Long): Menu =
        menuRepository.findByIdOrNull(menuId)
            ?: throw NotFoundException("MENU_NOT_FOUND", "해당 메뉴를 찾을 수 없습니다. id=$menuId")
}