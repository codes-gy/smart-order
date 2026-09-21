package com.gy.smartorder.menu

import com.gy.smartorder.common.exception.BadRequestException
import com.gy.smartorder.common.exception.ForbiddenException
import com.gy.smartorder.common.exception.NotFoundException
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
    fun createOptionGroup(authenticatedStoreId: Long, menuId: Long, req: MenuDto.MenuOptionGroupCreateRequest): MenuDto.MenuOptionGroupResponse {
        val menu = findMenuOrThrow(menuId)
        requireOwnStore(authenticatedStoreId, menu.category.store.id)
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
    fun updateOptionGroup(authenticatedStoreId: Long, groupId: Long, req: MenuDto.MenuOptionGroupUpdateRequest): MenuDto.MenuOptionGroupResponse {
        val group = findOptionGroupOrThrow(groupId)
        requireOwnStore(authenticatedStoreId, group.menu.category.store.id)
        group.updateInfo(
            name = req.name,
            type = parseOptionType(req.type),
            required = req.required,
            displayOrder = req.displayOrder,
        )
        return MenuDto.MenuOptionGroupResponse.from(group)
    }

    @Transactional
    fun deleteOptionGroup(authenticatedStoreId: Long, groupId: Long) {
        val group = findOptionGroupOrThrow(groupId)
        requireOwnStore(authenticatedStoreId, group.menu.category.store.id)
        menuOptionGroupRepository.delete(group)
    }

    @Transactional
    fun createOptionChoice(authenticatedStoreId: Long, groupId: Long, req: MenuDto.MenuOptionChoiceCreateRequest): MenuDto.MenuOptionChoiceResponse {
        val group = findOptionGroupOrThrow(groupId)
        requireOwnStore(authenticatedStoreId, group.menu.category.store.id)

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
    fun updateOptionChoice(authenticatedStoreId: Long, choiceId: Long, req: MenuDto.MenuOptionChoiceUpdateRequest): MenuDto.MenuOptionChoiceResponse {
        val choice = findOptionChoiceOrThrow(choiceId)
        requireOwnStore(authenticatedStoreId, choice.optionGroup!!.menu.category.store.id)
        choice.updateInfo(
            label = req.label,
            priceDelta = req.priceDelta,
            displayOrder = req.displayOrder,
        )
        return MenuDto.MenuOptionChoiceResponse.from(choice)
    }

    @Transactional
    fun updateOptionChoiceSoldOut(
        authenticatedStoreId: Long,
        choiceId: Long,
        req: MenuDto.MenuOptionChoiceSoldOutUpdateRequest,
    ): MenuDto.MenuOptionChoiceResponse {
        val choice = findOptionChoiceOrThrow(choiceId)
        requireOwnStore(authenticatedStoreId, choice.optionGroup!!.menu.category.store.id)
        choice.updateSoldOut(req.isSoldOut!!)
        return MenuDto.MenuOptionChoiceResponse.from(choice)
    }

    @Transactional
    fun deleteOptionChoice(authenticatedStoreId: Long, choiceId: Long) {
        val choice = findOptionChoiceOrThrow(choiceId)
        requireOwnStore(authenticatedStoreId, choice.optionGroup!!.menu.category.store.id)
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

    // STORE_ADMIN 토큰의 subject(storeId)가 대상 매장과 일치하는지 확인 — 다른 매장 계정으로 로그인한
    // 관리자가 남의 매장 메뉴 옵션을 고치지 못하도록 막는다(SecurityConfig의 hasRole만으로는 못 막는 부분).
    private fun requireOwnStore(authenticatedStoreId: Long, storeId: Long) {
        if (authenticatedStoreId != storeId) {
            throw ForbiddenException("STORE_ACCESS_DENIED", "해당 매장에 대한 권한이 없습니다.")
        }
    }
}
