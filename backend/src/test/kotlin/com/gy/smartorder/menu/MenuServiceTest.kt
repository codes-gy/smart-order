package com.gy.smartorder.menu

import com.gy.smartorder.category.Category
import com.gy.smartorder.category.CategoryRepository
import com.gy.smartorder.common.exception.ForbiddenException
import com.gy.smartorder.common.exception.NotFoundException
import com.gy.smartorder.store.Store
import com.gy.smartorder.store.StoreStatus
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.any
import org.mockito.BDDMockito.given
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import java.time.LocalTime
import java.util.Optional

class MenuServiceTest {

    private lateinit var menuRepository: MenuRepository
    private lateinit var categoryRepository: CategoryRepository
    private lateinit var menuService: MenuService

    @BeforeEach
    fun setUp() {
        menuRepository = mock(MenuRepository::class.java)
        categoryRepository = mock(CategoryRepository::class.java)
        menuService = MenuService(menuRepository, categoryRepository)
    }

    private fun store(id: Long = 1L) = Store(
        id = id,
        name = "스마트오더 역삼역점",
        address = "서울시 강남구",
        phone = "02-0000-0000",
        status = StoreStatus.OPEN,
        businessNumber = "000-00-00000",
        latitude = 37.5,
        longitude = 127.0,
        openTime = LocalTime.of(9, 0),
        closeTime = LocalTime.of(22, 0),
    )

    private fun category(store: Store = store(), id: Long = 1L) = Category(id = id, store = store, name = "커피")

    private fun menu(category: Category = category(), id: Long = 1L) = Menu(
        id = id,
        category = category,
        name = "아메리카노",
        price = 4000,
        status = MenuStatus.ON_SALE,
    )

    /** 픽스처 매장(`store().id`)과 동일한 STORE_ADMIN 토큰 subject. */
    private val ownerStoreId = 1L

    /** 다른 매장 계정으로 로그인한 STORE_ADMIN 토큰 subject. */
    private val otherStoreId = 999L

    private fun createReq(categoryId: Long = 1L) = MenuDto.MenuCreateRequest(
        categoryId = categoryId,
        name = "아메리카노",
        price = 4000,
    )

    private fun updateReq(categoryId: Long = 1L) = MenuDto.MenuUpdateRequest(
        categoryId = categoryId,
        name = "아메리카노(HOT)",
        price = 4500,
    )

    @Test
    fun `메뉴를 생성한다`() {
        given(categoryRepository.findById(1L)).willReturn(Optional.of(category()))
        given(menuRepository.existsByCategoryIdAndName(1L, "아메리카노")).willReturn(false)
        given(menuRepository.save(any(Menu::class.java))).willAnswer { it.arguments[0] }

        val res = menuService.createMenu(ownerStoreId, createReq())

        assertThat(res.name).isEqualTo("아메리카노")
        assertThat(res.price).isEqualTo(4000)
    }

    @Test
    fun `다른 매장 계정으로 메뉴를 생성하면 ForbiddenException을 던진다`() {
        given(categoryRepository.findById(1L)).willReturn(Optional.of(category()))

        assertThatThrownBy {
            menuService.createMenu(otherStoreId, createReq())
        }.isInstanceOf(ForbiddenException::class.java)
        verify(menuRepository, never()).save(any(Menu::class.java))
    }

    @Test
    fun `존재하지 않는 카테고리에 메뉴를 생성하면 NotFoundException을 던진다`() {
        given(categoryRepository.findById(1L)).willReturn(Optional.empty())

        assertThatThrownBy {
            menuService.createMenu(ownerStoreId, createReq())
        }.isInstanceOf(NotFoundException::class.java)
    }

    @Test
    fun `메뉴를 수정한다`() {
        val menu = menu()
        given(menuRepository.findById(1L)).willReturn(Optional.of(menu))
        given(categoryRepository.findById(1L)).willReturn(Optional.of(menu.category))

        val res = menuService.updateMenu(ownerStoreId, 1L, updateReq())

        assertThat(res.name).isEqualTo("아메리카노(HOT)")
        assertThat(res.price).isEqualTo(4500)
    }

    @Test
    fun `다른 매장 계정으로 메뉴를 수정하면 ForbiddenException을 던진다`() {
        given(menuRepository.findById(1L)).willReturn(Optional.of(menu()))

        assertThatThrownBy {
            menuService.updateMenu(otherStoreId, 1L, updateReq())
        }.isInstanceOf(ForbiddenException::class.java)
    }

    @Test
    fun `다른 매장 소유의 카테고리로 메뉴를 옮기려 하면 ForbiddenException을 던진다`() {
        val menu = menu()
        val otherStoresCategory = category(store = store(id = otherStoreId), id = 2L)
        given(menuRepository.findById(1L)).willReturn(Optional.of(menu))
        given(categoryRepository.findById(2L)).willReturn(Optional.of(otherStoresCategory))

        assertThatThrownBy {
            menuService.updateMenu(ownerStoreId, 1L, updateReq(categoryId = 2L))
        }.isInstanceOf(ForbiddenException::class.java)
    }

    @Test
    fun `존재하지 않는 메뉴를 수정하면 NotFoundException을 던진다`() {
        given(menuRepository.findById(1L)).willReturn(Optional.empty())

        assertThatThrownBy {
            menuService.updateMenu(ownerStoreId, 1L, updateReq())
        }.isInstanceOf(NotFoundException::class.java)
    }

    @Test
    fun `메뉴 상태를 변경한다`() {
        val menu = menu()
        given(menuRepository.findById(1L)).willReturn(Optional.of(menu))

        val res = menuService.updateMenuStatus(ownerStoreId, 1L, MenuDto.MenuStatusUpdateRequest(status = MenuStatus.SOLD_OUT))

        assertThat(res.status).isEqualTo(MenuStatus.SOLD_OUT)
    }

    @Test
    fun `다른 매장 계정으로 메뉴 상태를 변경하면 ForbiddenException을 던진다`() {
        given(menuRepository.findById(1L)).willReturn(Optional.of(menu()))

        assertThatThrownBy {
            menuService.updateMenuStatus(otherStoreId, 1L, MenuDto.MenuStatusUpdateRequest(status = MenuStatus.SOLD_OUT))
        }.isInstanceOf(ForbiddenException::class.java)
    }

    @Test
    fun `메뉴를 삭제한다`() {
        val menu = menu()
        given(menuRepository.findById(1L)).willReturn(Optional.of(menu))

        menuService.deleteMenu(ownerStoreId, 1L)

        verify(menuRepository).delete(menu)
    }

    @Test
    fun `다른 매장 계정으로 메뉴를 삭제하면 ForbiddenException을 던진다`() {
        given(menuRepository.findById(1L)).willReturn(Optional.of(menu()))

        assertThatThrownBy {
            menuService.deleteMenu(otherStoreId, 1L)
        }.isInstanceOf(ForbiddenException::class.java)
        verify(menuRepository, never()).delete(any(Menu::class.java))
    }
}
