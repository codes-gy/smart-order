package com.gy.smartorder.services.menu

import com.gy.smartorder.common.exception.NotFoundException
import com.gy.smartorder.dtos.menu.MenuDto
import com.gy.smartorder.entities.category.Category
import com.gy.smartorder.entities.menu.Menu
import com.gy.smartorder.entities.menu.MenuOptionChoice
import com.gy.smartorder.entities.menu.MenuOptionGroup
import com.gy.smartorder.entities.menu.MenuOptionType
import com.gy.smartorder.entities.store.Store
import com.gy.smartorder.repositories.menu.MenuOptionChoiceRepository
import com.gy.smartorder.repositories.menu.MenuOptionGroupRepository
import com.gy.smartorder.repositories.menu.MenuRepository
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.any
import org.mockito.BDDMockito.given
import org.mockito.Mockito.mock
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
    )

    private fun group(type: MenuOptionType = MenuOptionType.SINGLE) = MenuOptionGroup(
        id = 1L,
        menu = menu(),
        name = "온도",
        type = type,
    )

    private fun choice() = MenuOptionChoice(
        id = 1L,
        optionGroup = group(),
        label = "아이스",
    )

    @Test
    fun `옵션 그룹을 생성하면 프론트 소문자 type 문자열을 enum으로 변환해 저장한다`() {
        given(menuRepository.findById(1L)).willReturn(Optional.of(menu()))
        given(menuOptionGroupRepository.save(any(MenuOptionGroup::class.java))).willAnswer { it.arguments[0] }

        val req = MenuDto.MenuOptionGroupCreateRequest(name = "샷 추가", type = "multiple", required = true, displayOrder = 1)
        val res = menuOptionService.createOptionGroup(1L, req)

        assertThat(res.type).isEqualTo("multiple")
        assertThat(res.required).isTrue()
    }

    @Test
    fun `존재하지 않는 메뉴에 옵션 그룹을 생성하면 NotFoundException을 던진다`() {
        given(menuRepository.findById(1L)).willReturn(Optional.empty())

        val req = MenuDto.MenuOptionGroupCreateRequest(name = "온도", type = "single")
        assertThatThrownBy { menuOptionService.createOptionGroup(1L, req) }
            .isInstanceOf(NotFoundException::class.java)
    }

    @Test
    fun `옵션 그룹을 수정하면 필드가 갱신된다`() {
        val target = group(type = MenuOptionType.SINGLE)
        given(menuOptionGroupRepository.findById(1L)).willReturn(Optional.of(target))

        val req = MenuDto.MenuOptionGroupUpdateRequest(name = "온도 변경", type = "multiple", required = true, displayOrder = 2)
        val res = menuOptionService.updateOptionGroup(1L, req)

        assertThat(res.name).isEqualTo("온도 변경")
        assertThat(res.type).isEqualTo("multiple")
        assertThat(target.required).isTrue()
        assertThat(target.displayOrder).isEqualTo(2)
    }

    @Test
    fun `옵션 그룹을 삭제한다`() {
        val target = group()
        given(menuOptionGroupRepository.findById(1L)).willReturn(Optional.of(target))

        menuOptionService.deleteOptionGroup(1L)

        verify(menuOptionGroupRepository).delete(target)
    }

    @Test
    fun `옵션 선택지를 품절 처리한다`() {
        val target = choice()
        given(menuOptionChoiceRepository.findById(1L)).willReturn(Optional.of(target))

        val res = menuOptionService.updateOptionChoiceSoldOut(1L, MenuDto.MenuOptionChoiceSoldOutUpdateRequest(isSoldOut = true))

        assertThat(res.isSoldOut).isTrue()
        assertThat(target.isSoldOut).isTrue()
    }

    @Test
    fun `존재하지 않는 옵션 선택지를 수정하면 NotFoundException을 던진다`() {
        given(menuOptionChoiceRepository.findById(1L)).willReturn(Optional.empty())

        assertThatThrownBy {
            menuOptionService.updateOptionChoice(1L, MenuDto.MenuOptionChoiceUpdateRequest(label = "핫", priceDelta = 0))
        }.isInstanceOf(NotFoundException::class.java)
    }
}
