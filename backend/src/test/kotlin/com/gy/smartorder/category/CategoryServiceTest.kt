package com.gy.smartorder.category

import com.gy.smartorder.common.exception.ForbiddenException
import com.gy.smartorder.common.exception.NotFoundException
import com.gy.smartorder.store.Store
import com.gy.smartorder.store.StoreRepository
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

class CategoryServiceTest {

    private lateinit var categoryRepository: CategoryRepository
    private lateinit var storeRepository: StoreRepository
    private lateinit var categoryService: CategoryService

    @BeforeEach
    fun setUp() {
        categoryRepository = mock(CategoryRepository::class.java)
        storeRepository = mock(StoreRepository::class.java)
        categoryService = CategoryService(categoryRepository, storeRepository)
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

    private fun category(store: Store = store(), id: Long = 10L) = Category(
        id = id,
        store = store,
        name = "커피",
        order = 0,
    )

    /** 픽스처 매장(`store().id`)과 동일한 STORE_ADMIN 토큰 subject. */
    private val ownerStoreId = 1L

    /** 다른 매장 계정으로 로그인한 STORE_ADMIN 토큰 subject. */
    private val otherStoreId = 999L

    @Test
    fun `카테고리를 생성한다`() {
        given(storeRepository.findById(1L)).willReturn(Optional.of(store()))
        given(categoryRepository.existsByStoreIdAndName(1L, "커피")).willReturn(false)
        given(categoryRepository.save(any(Category::class.java))).willAnswer { it.arguments[0] }

        val res = categoryService.createCategory(ownerStoreId, 1L, CategoryDto.CategoryCreateRequest(name = "커피", order = 0))

        assertThat(res.name).isEqualTo("커피")
        assertThat(res.storeId).isEqualTo(1L)
    }

    @Test
    fun `다른 매장 계정으로 카테고리를 생성하면 ForbiddenException을 던진다`() {
        assertThatThrownBy {
            categoryService.createCategory(otherStoreId, 1L, CategoryDto.CategoryCreateRequest(name = "커피", order = 0))
        }.isInstanceOf(ForbiddenException::class.java)
        verify(categoryRepository, never()).save(any(Category::class.java))
    }

    @Test
    fun `존재하지 않는 매장에 카테고리를 생성하면 NotFoundException을 던진다`() {
        given(storeRepository.findById(1L)).willReturn(Optional.empty())

        assertThatThrownBy {
            categoryService.createCategory(ownerStoreId, 1L, CategoryDto.CategoryCreateRequest(name = "커피", order = 0))
        }.isInstanceOf(NotFoundException::class.java)
    }

    @Test
    fun `이미 존재하는 카테고리명이면 IllegalArgumentException을 던진다`() {
        given(storeRepository.findById(1L)).willReturn(Optional.of(store()))
        given(categoryRepository.existsByStoreIdAndName(1L, "커피")).willReturn(true)

        assertThatThrownBy {
            categoryService.createCategory(ownerStoreId, 1L, CategoryDto.CategoryCreateRequest(name = "커피", order = 0))
        }.isInstanceOf(IllegalArgumentException::class.java)
        verify(categoryRepository, never()).save(any(Category::class.java))
    }

    @Test
    fun `카테고리를 수정한다`() {
        val category = category()
        given(categoryRepository.findById(10L)).willReturn(Optional.of(category))

        val res = categoryService.updateCategory(ownerStoreId, 10L, CategoryDto.CategoryUpdateRequest(name = "디카페인", order = 1))

        assertThat(res.name).isEqualTo("디카페인")
        assertThat(res.order).isEqualTo(1)
    }

    @Test
    fun `다른 매장 계정으로 카테고리를 수정하면 ForbiddenException을 던진다`() {
        given(categoryRepository.findById(10L)).willReturn(Optional.of(category()))

        assertThatThrownBy {
            categoryService.updateCategory(otherStoreId, 10L, CategoryDto.CategoryUpdateRequest(name = "디카페인", order = 1))
        }.isInstanceOf(ForbiddenException::class.java)
    }

    @Test
    fun `존재하지 않는 카테고리를 수정하면 NotFoundException을 던진다`() {
        given(categoryRepository.findById(10L)).willReturn(Optional.empty())

        assertThatThrownBy {
            categoryService.updateCategory(ownerStoreId, 10L, CategoryDto.CategoryUpdateRequest(name = "디카페인", order = 1))
        }.isInstanceOf(NotFoundException::class.java)
    }

    @Test
    fun `카테고리를 삭제한다`() {
        val category = category()
        given(categoryRepository.findById(10L)).willReturn(Optional.of(category))

        categoryService.deleteCategory(ownerStoreId, 10L)

        verify(categoryRepository).delete(category)
    }

    @Test
    fun `다른 매장 계정으로 카테고리를 삭제하면 ForbiddenException을 던진다`() {
        given(categoryRepository.findById(10L)).willReturn(Optional.of(category()))

        assertThatThrownBy {
            categoryService.deleteCategory(otherStoreId, 10L)
        }.isInstanceOf(ForbiddenException::class.java)
        verify(categoryRepository, never()).delete(any(Category::class.java))
    }
}
