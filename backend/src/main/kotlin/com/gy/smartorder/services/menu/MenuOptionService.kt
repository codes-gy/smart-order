package com.gy.smartorder.services.menu

import com.gy.smartorder.common.exception.BadRequestException
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

/** 매장 관리자용 메뉴 옵션 그룹/선택지 CRUD (조회는 기존 `GET /menus/{menuId}`가 이미 담당). */
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
        val type = parseOptionType(req.type)

        if (menuOptionGroupRepository.existsByMenuIdAndName(menuId, req.name)) {
            throw IllegalArgumentException("해당 메뉴에 이미 동일한 옵션 그룹명이 존재합니다.")
        }

        val group = MenuOptionGroup(
            menu = menu,
            name = req.name,
            type = type,
            required = req.required,
            displayOrder = req.displayOrder,
        )
        val saved = menuOptionGroupRepository.save(group)
        return MenuDto.MenuOptionGroupResponse.from(saved)
    }

    @Transactional
    fun updateOptionGroup(groupId: Long, req: MenuDto.MenuOptionGroupUpdateRequest): MenuDto.MenuOptionGroupResponse {
        val group = findOptionGroupOrThrow(groupId)
        group.updateInfo(
            name = req.name,
            type = parseOptionType(req.type),
            required = req.required,
            displayOrder = req.displayOrder,
        )
        return MenuDto.MenuOptionGroupResponse.from(group)
    }

    @Transactional
    fun deleteOptionGroup(groupId: Long) {
        val group = findOptionGroupOrThrow(groupId)
        menuOptionGroupRepository.delete(group)
    }

    @Transactional
    fun createOptionChoice(groupId: Long, req: MenuDto.MenuOptionChoiceCreateRequest): MenuDto.MenuOptionChoiceResponse {
        val group = findOptionGroupOrThrow(groupId)

        if (menuOptionChoiceRepository.existsByOptionGroupIdAndLabel(groupId, req.label)) {
            throw IllegalArgumentException("해당 옵션 그룹에 이미 동일한 선택지명이 존재합니다.")
        }

        val choice = MenuOptionChoice(
            optionGroup = group,
            label = req.label,
            priceDelta = req.priceDelta,
            displayOrder = req.displayOrder,
        )
        val saved = menuOptionChoiceRepository.save(choice)
        return MenuDto.MenuOptionChoiceResponse.from(saved)
    }

    @Transactional
    fun updateOptionChoice(choiceId: Long, req: MenuDto.MenuOptionChoiceUpdateRequest): MenuDto.MenuOptionChoiceResponse {
        val choice = findOptionChoiceOrThrow(choiceId)
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
        val choice = findOptionChoiceOrThrow(choiceId)
        choice.updateSoldOut(req.isSoldOut!!)
        return MenuDto.MenuOptionChoiceResponse.from(choice)
    }

    @Transactional
    fun deleteOptionChoice(choiceId: Long) {
        val choice = findOptionChoiceOrThrow(choiceId)
        menuOptionChoiceRepository.delete(choice)
    }

    private fun parseOptionType(type: String): MenuOptionType = when (type.lowercase()) {
        "single" -> MenuOptionType.SINGLE
        "multiple" -> MenuOptionType.MULTIPLE
        else -> throw BadRequestException("INVALID_OPTION_TYPE", "옵션 타입은 single 또는 multiple이어야 합니다.")
    }

    private fun findMenuOrThrow(menuId: Long): Menu =
        menuRepository.findByIdOrNull(menuId)
            ?: throw NotFoundException("MENU_NOT_FOUND", "해당 메뉴를 찾을 수 없습니다. id=$menuId")

    private fun findOptionGroupOrThrow(groupId: Long): MenuOptionGroup =
        menuOptionGroupRepository.findByIdOrNull(groupId)
            ?: throw NotFoundException("OPTION_GROUP_NOT_FOUND", "해당 옵션 그룹을 찾을 수 없습니다. id=$groupId")

    private fun findOptionChoiceOrThrow(choiceId: Long): MenuOptionChoice =
        menuOptionChoiceRepository.findByIdOrNull(choiceId)
            ?: throw NotFoundException("OPTION_CHOICE_NOT_FOUND", "해당 옵션 선택지를 찾을 수 없습니다. id=$choiceId")
}
