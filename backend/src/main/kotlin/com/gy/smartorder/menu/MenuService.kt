package com.gy.smartorder.menu

import com.gy.smartorder.common.exception.ForbiddenException
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
    fun createMenu(authenticatedStoreId: Long, req: MenuDto.MenuCreateRequest): MenuDto.MenuResponse {
        val categoryId = req.categoryId!!
        val category = findCategoryOrThrow(categoryId)
        requireOwnStore(authenticatedStoreId, category.store.id)

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
    fun updateMenu(authenticatedStoreId: Long, menuId: Long, req: MenuDto.MenuUpdateRequest): MenuDto.MenuResponse {
        val menu = findMenuOrThrow(menuId)
        requireOwnStore(authenticatedStoreId, menu.category.store.id)
        val category = findCategoryOrThrow(req.categoryId!!)
        // 메뉴를 다른 카테고리로 옮기는 것도 허용하므로, 이관 대상 카테고리도 같은 매장 소유인지 확인한다
        // (그렇지 않으면 다른 매장의 카테고리 밑으로 메뉴를 옮겨버릴 수 있음).
        requireOwnStore(authenticatedStoreId, category.store.id)

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
    fun updateMenuStatus(authenticatedStoreId: Long, menuId: Long, req: MenuDto.MenuStatusUpdateRequest): MenuDto.MenuResponse {
        val menu = findMenuOrThrow(menuId)
        requireOwnStore(authenticatedStoreId, menu.category.store.id)
        menu.updateStatus(req.status!!)
        return MenuDto.MenuResponse.from(menu)
    }

    @Transactional
    fun deleteMenu(authenticatedStoreId: Long, menuId: Long) {
        val menu = findMenuOrThrow(menuId)
        requireOwnStore(authenticatedStoreId, menu.category.store.id)
        menuRepository.delete(menu)
    }

    private fun findCategoryOrThrow(categoryId: Long): Category =
        categoryRepository.findByIdOrNull(categoryId)
            ?: throw NotFoundException("CATEGORY_NOT_FOUND", "해당 카테고리를 찾을 수 없습니다. id=$categoryId")

    private fun findMenuOrThrow(menuId: Long): Menu =
        menuRepository.findByIdOrNull(menuId)
            ?: throw NotFoundException("MENU_NOT_FOUND", "해당 메뉴를 찾을 수 없습니다. id=$menuId")

    // STORE_ADMIN 토큰의 subject(storeId)가 대상 매장과 일치하는지 확인 — 다른 매장 계정으로 로그인한
    // 관리자가 남의 매장 메뉴를 고치지 못하도록 막는다(SecurityConfig의 hasRole만으로는 못 막는 부분).
    private fun requireOwnStore(authenticatedStoreId: Long, storeId: Long) {
        if (authenticatedStoreId != storeId) {
            throw ForbiddenException("STORE_ACCESS_DENIED", "해당 매장에 대한 권한이 없습니다.")
        }
    }
}