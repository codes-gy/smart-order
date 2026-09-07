package com.gy.smartorder.services.menu

import com.gy.smartorder.common.exception.NotFoundException
import com.gy.smartorder.dtos.menu.MenuDto
import com.gy.smartorder.entities.menu.Menu
import com.gy.smartorder.entities.menu.MenuOptionChoice
import com.gy.smartorder.entities.menu.MenuOptionGroup
import com.gy.smartorder.entities.menu.MenuOptionType
import com.gy.smartorder.repositories.menu.MenuOptionChoiceRepository
import com.gy.smartorder.repositories.menu.MenuOptionGroupRepository
import com.gy.smartorder.repositories.menu.MenuRepository
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/** 매장 관리자가 메뉴 옵션 그룹/선택지를 등록·수정·삭제·품절처리하는 CRUD (조회/주문 반영은 MenuService/OrderService가 담당). */
@Service
@Transactional(readOnly = true)
class MenuOptionService(
    private val menuRepository: MenuRepository,
    private val menuOptionGroupRepository: MenuOptionGroupRepository,
    private val menuOptionChoiceRepository: MenuOptionChoiceRepository,
) {
    @Transactional
    fun createOptionGroup(menuId: Long, req: MenuDto.MenuOptionGroupCreateRequest): MenuDto.MenuOptionGroupResponse {
        val menu = findMenuOrThrow(menuId)
        val group = MenuOptionGroup(
            menu = menu,
            name = req.name,
            type = MenuOptionType.fromApiValue(req.type),
            required = req.required,
            displayOrder = req.displayOrder,
        )
        val savedGroup = menuOptionGroupRepository.save(group)
        return MenuDto.MenuOptionGroupResponse.from(savedGroup)
    }

    @Transactional
    fun updateOptionGroup(groupId: Long, req: MenuDto.MenuOptionGroupUpdateRequest): MenuDto.MenuOptionGroupResponse {
        val group = findGroupOrThrow(groupId)
        group.updateInfo(
            name = req.name,
            type = MenuOptionType.fromApiValue(req.type),
            required = req.required,
            displayOrder = req.displayOrder,
        )
        return MenuDto.MenuOptionGroupResponse.from(group)
    }

    @Transactional
    fun deleteOptionGroup(groupId: Long) {
        val group = findGroupOrThrow(groupId)
        menuOptionGroupRepository.delete(group)
    }

    @Transactional
    fun createOptionChoice(groupId: Long, req: MenuDto.MenuOptionChoiceCreateRequest): MenuDto.MenuOptionChoiceResponse {
        val group = findGroupOrThrow(groupId)
        val choice = MenuOptionChoice(
            optionGroup = group,
            label = req.label,
            priceDelta = req.priceDelta,
            displayOrder = req.displayOrder,
        )
        val savedChoice = menuOptionChoiceRepository.save(choice)
        return MenuDto.MenuOptionChoiceResponse.from(savedChoice)
    }

    @Transactional
    fun updateOptionChoice(choiceId: Long, req: MenuDto.MenuOptionChoiceUpdateRequest): MenuDto.MenuOptionChoiceResponse {
        val choice = findChoiceOrThrow(choiceId)
        choice.updateInfo(
            label = req.label,
            priceDelta = req.priceDelta,
            displayOrder = req.displayOrder,
        )
        return MenuDto.MenuOptionChoiceResponse.from(choice)
    }

    @Transactional
    fun updateOptionChoiceSoldOut(
        choiceId: Long,
        req: MenuDto.MenuOptionChoiceSoldOutUpdateRequest,
    ): MenuDto.MenuOptionChoiceResponse {
        val choice = findChoiceOrThrow(choiceId)
        choice.updateSoldOut(req.isSoldOut!!)
        return MenuDto.MenuOptionChoiceResponse.from(choice)
    }

    @Transactional
    fun deleteOptionChoice(choiceId: Long) {
        val choice = findChoiceOrThrow(choiceId)
        menuOptionChoiceRepository.delete(choice)
    }

    private fun findMenuOrThrow(menuId: Long): Menu =
        menuRepository.findByIdOrNull(menuId)
            ?: throw NotFoundException("MENU_NOT_FOUND", "해당 메뉴를 찾을 수 없습니다. id=$menuId")

    private fun findGroupOrThrow(groupId: Long): MenuOptionGroup =
        menuOptionGroupRepository.findByIdOrNull(groupId)
            ?: throw NotFoundException("MENU_OPTION_GROUP_NOT_FOUND", "해당 옵션 그룹을 찾을 수 없습니다. id=$groupId")

    private fun findChoiceOrThrow(choiceId: Long): MenuOptionChoice =
        menuOptionChoiceRepository.findByIdOrNull(choiceId)
            ?: throw NotFoundException("MENU_OPTION_CHOICE_NOT_FOUND", "해당 옵션 선택지를 찾을 수 없습니다. id=$choiceId")
}
