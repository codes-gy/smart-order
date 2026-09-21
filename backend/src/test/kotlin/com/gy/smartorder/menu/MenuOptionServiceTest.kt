package com.gy.smartorder.menu

import com.gy.smartorder.common.exception.BadRequestException
import com.gy.smartorder.common.exception.ForbiddenException
import com.gy.smartorder.common.exception.NotFoundException
import com.gy.smartorder.category.Category
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

class MenuOptionServiceTest {

    private lateinit var menuRepository: MenuRepository
    private lateinit var menuOptionGroupRepository: MenuOptionGroupRepository
    private lateinit var menuOptionChoiceRepository: MenuOptionChoiceRepository
    private lateinit var menuOptionService: MenuOptionService

    @BeforeEach
    fun setUp() {
        menuRepository = mock(MenuRepository::class.java)
        menuOptionGroupRepository = mock(MenuOptionGroupRepository::class.java)
        menuOptionChoiceRepository = mock(MenuOptionChoiceRepository::class.java)
        menuOptionService = MenuOptionService(menuRepository, menuOptionGroupRepository, menuOptionChoiceRepository)
    }

    private fun store() = Store(
        id = 1L,
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

    private fun menu() = Menu(
        id = 1L,
        category = Category(id = 1L, store = store(), name = "커피"),
        name = "아메리카노",
        price = 4000,
        status = MenuStatus.ON_SALE,
    )

    private fun optionGroup(menu: Menu = menu(), id: Long = 10L) = MenuOptionGroup(
        id = id,
        menu = menu,
        name = "온도",
        type = MenuOptionType.SINGLE,
        required = true,
    )

    private fun optionChoice(group: MenuOptionGroup, id: Long = 100L) = MenuOptionChoice(
        id = id,
        optionGroup = group,
        label = "아이스",
        priceDelta = 0,
    )

    /** 픽스처의 매장(`store().id`)과 동일한 STORE_ADMIN 토큰 subject. */
    private val ownerStoreId = 1L

    /** 다른 매장 계정으로 로그인한 STORE_ADMIN 토큰 subject. */
    private val otherStoreId = 999L

    @Test
    fun `옵션 그룹을 생성한다`() {
        val menu = menu()
        given(menuRepository.findById(1L)).willReturn(Optional.of(menu))
        given(menuOptionGroupRepository.existsByMenuIdAndName(1L, "온도")).willReturn(false)
        given(menuOptionGroupRepository.save(any(MenuOptionGroup::class.java))).willAnswer { it.arguments[0] }

        val req = MenuDto.MenuOptionGroupCreateRequest(name = "온도", type = "single", required = true)
        val res = menuOptionService.createOptionGroup(ownerStoreId, 1L, req)

        assertThat(res.name).isEqualTo("온도")
        assertThat(res.type).isEqualTo("single")
        assertThat(res.required).isTrue()
    }

    @Test
    fun `다른 매장 계정으로 옵션 그룹을 생성하면 ForbiddenException을 던진다`() {
        given(menuRepository.findById(1L)).willReturn(Optional.of(menu()))

        assertThatThrownBy {
            menuOptionService.createOptionGroup(otherStoreId, 1L, MenuDto.MenuOptionGroupCreateRequest(name = "온도", type = "single"))
        }.isInstanceOf(ForbiddenException::class.java)
        verify(menuOptionGroupRepository, never()).save(any(MenuOptionGroup::class.java))
    }

    @Test
    fun `존재하지 않는 메뉴에 옵션 그룹을 생성하면 NotFoundException을 던진다`() {
        given(menuRepository.findById(1L)).willReturn(Optional.empty())

        assertThatThrownBy {
            menuOptionService.createOptionGroup(ownerStoreId, 1L, MenuDto.MenuOptionGroupCreateRequest(name = "온도", type = "single"))
        }.isInstanceOf(NotFoundException::class.java)
    }

    @Test
    fun `이미 존재하는 옵션 그룹명이면 IllegalArgumentException을 던진다`() {
        given(menuRepository.findById(1L)).willReturn(Optional.of(menu()))
        given(menuOptionGroupRepository.existsByMenuIdAndName(1L, "온도")).willReturn(true)

        assertThatThrownBy {
            menuOptionService.createOptionGroup(ownerStoreId, 1L, MenuDto.MenuOptionGroupCreateRequest(name = "온도", type = "single"))
        }.isInstanceOf(IllegalArgumentException::class.java)
        verify(menuOptionGroupRepository, never()).save(any(MenuOptionGroup::class.java))
    }

    @Test
    fun `옵션 타입이 single, multiple이 아니면 BadRequestException을 던진다`() {
        given(menuRepository.findById(1L)).willReturn(Optional.of(menu()))
        given(menuOptionGroupRepository.existsByMenuIdAndName(1L, "온도")).willReturn(false)

        assertThatThrownBy {
            menuOptionService.createOptionGroup(ownerStoreId, 1L, MenuDto.MenuOptionGroupCreateRequest(name = "온도", type = "invalid"))
        }.isInstanceOf(BadRequestException::class.java)
    }

    @Test
    fun `옵션 그룹을 수정한다`() {
        val group = optionGroup()
        given(menuOptionGroupRepository.findById(10L)).willReturn(Optional.of(group))

        val req = MenuDto.MenuOptionGroupUpdateRequest(name = "온도 변경", type = "multiple", required = false, displayOrder = 2)
        val res = menuOptionService.updateOptionGroup(ownerStoreId, 10L, req)

        assertThat(res.name).isEqualTo("온도 변경")
        assertThat(res.type).isEqualTo("multiple")
        assertThat(res.required).isFalse()
    }

    @Test
    fun `다른 매장 계정으로 옵션 그룹을 수정하면 ForbiddenException을 던진다`() {
        given(menuOptionGroupRepository.findById(10L)).willReturn(Optional.of(optionGroup()))

        assertThatThrownBy {
            menuOptionService.updateOptionGroup(otherStoreId, 10L, MenuDto.MenuOptionGroupUpdateRequest(name = "x", type = "single"))
        }.isInstanceOf(ForbiddenException::class.java)
    }

    @Test
    fun `존재하지 않는 옵션 그룹을 수정하면 NotFoundException을 던진다`() {
        given(menuOptionGroupRepository.findById(10L)).willReturn(Optional.empty())

        assertThatThrownBy {
            menuOptionService.updateOptionGroup(ownerStoreId, 10L, MenuDto.MenuOptionGroupUpdateRequest(name = "x", type = "single"))
        }.isInstanceOf(NotFoundException::class.java)
    }

    @Test
    fun `옵션 그룹을 삭제한다`() {
        val group = optionGroup()
        given(menuOptionGroupRepository.findById(10L)).willReturn(Optional.of(group))

        menuOptionService.deleteOptionGroup(ownerStoreId, 10L)

        verify(menuOptionGroupRepository).delete(group)
    }

    @Test
    fun `옵션 선택지를 생성한다`() {
        val group = optionGroup()
        given(menuOptionGroupRepository.findById(10L)).willReturn(Optional.of(group))
        given(menuOptionChoiceRepository.existsByOptionGroupIdAndLabel(10L, "아이스")).willReturn(false)
        given(menuOptionChoiceRepository.save(any(MenuOptionChoice::class.java))).willAnswer { it.arguments[0] }

        val req = MenuDto.MenuOptionChoiceCreateRequest(label = "아이스", priceDelta = 0)
        val res = menuOptionService.createOptionChoice(ownerStoreId, 10L, req)

        assertThat(res.label).isEqualTo("아이스")
        assertThat(res.isSoldOut).isFalse()
    }

    @Test
    fun `다른 매장 계정으로 옵션 선택지를 생성하면 ForbiddenException을 던진다`() {
        given(menuOptionGroupRepository.findById(10L)).willReturn(Optional.of(optionGroup()))

        assertThatThrownBy {
            menuOptionService.createOptionChoice(otherStoreId, 10L, MenuDto.MenuOptionChoiceCreateRequest(label = "아이스"))
        }.isInstanceOf(ForbiddenException::class.java)
        verify(menuOptionChoiceRepository, never()).save(any(MenuOptionChoice::class.java))
    }

    @Test
    fun `이미 존재하는 옵션 선택지명이면 IllegalArgumentException을 던진다`() {
        given(menuOptionGroupRepository.findById(10L)).willReturn(Optional.of(optionGroup()))
        given(menuOptionChoiceRepository.existsByOptionGroupIdAndLabel(10L, "아이스")).willReturn(true)

        assertThatThrownBy {
            menuOptionService.createOptionChoice(ownerStoreId, 10L, MenuDto.MenuOptionChoiceCreateRequest(label = "아이스"))
        }.isInstanceOf(IllegalArgumentException::class.java)
        verify(menuOptionChoiceRepository, never()).save(any(MenuOptionChoice::class.java))
    }

    @Test
    fun `옵션 선택지를 수정한다`() {
        val choice = optionChoice(optionGroup())
        given(menuOptionChoiceRepository.findById(100L)).willReturn(Optional.of(choice))

        val req = MenuDto.MenuOptionChoiceUpdateRequest(label = "핫", priceDelta = 500, displayOrder = 1)
        val res = menuOptionService.updateOptionChoice(ownerStoreId, 100L, req)

        assertThat(res.label).isEqualTo("핫")
        assertThat(res.priceDelta).isEqualTo(500)
    }

    @Test
    fun `옵션 선택지를 품절 처리한다`() {
        val choice = optionChoice(optionGroup())
        given(menuOptionChoiceRepository.findById(100L)).willReturn(Optional.of(choice))

        val res = menuOptionService.updateOptionChoiceSoldOut(
            ownerStoreId,
            100L,
            MenuDto.MenuOptionChoiceSoldOutUpdateRequest(isSoldOut = true),
        )

        assertThat(res.isSoldOut).isTrue()
    }

    @Test
    fun `옵션 선택지를 삭제한다`() {
        val choice = optionChoice(optionGroup())
        given(menuOptionChoiceRepository.findById(100L)).willReturn(Optional.of(choice))

        menuOptionService.deleteOptionChoice(ownerStoreId, 100L)

        verify(menuOptionChoiceRepository).delete(choice)
    }

    @Test
    fun `다른 매장 계정으로 옵션 선택지를 삭제하면 ForbiddenException을 던진다`() {
        val choice = optionChoice(optionGroup())
        given(menuOptionChoiceRepository.findById(100L)).willReturn(Optional.of(choice))

        assertThatThrownBy {
            menuOptionService.deleteOptionChoice(otherStoreId, 100L)
        }.isInstanceOf(ForbiddenException::class.java)
        verify(menuOptionChoiceRepository, never()).delete(any(MenuOptionChoice::class.java))
    }
}
