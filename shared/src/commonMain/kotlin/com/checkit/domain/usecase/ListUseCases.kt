package com.checkit.domain.usecase

import com.checkit.data.CheckItRepository
import com.checkit.data.ListWriteInput

class AddListUseCase(
    private val repository: CheckItRepository
) {
    suspend operator fun invoke(input: ListWriteInput): Long = repository.addList(input)
}

class UpdateListUseCase(
    private val repository: CheckItRepository
) {
    suspend operator fun invoke(listId: Long, input: ListWriteInput) = repository.updateList(listId, input)
}

class DeleteListUseCase(
    private val repository: CheckItRepository
) {
    suspend operator fun invoke(listId: Long) = repository.deleteList(listId)
}

class AddSectionUseCase(
    private val repository: CheckItRepository
) {
    suspend operator fun invoke(listId: Long, title: String, color: String): Long =
        repository.addSection(listId, title, color)
}

class UpdateSectionUseCase(
    private val repository: CheckItRepository
) {
    suspend operator fun invoke(sectionId: Long, title: String, color: String, sortOrder: Int) =
        repository.updateSection(sectionId, title, color, sortOrder)
}

class DeleteSectionUseCase(
    private val repository: CheckItRepository
) {
    suspend operator fun invoke(sectionId: Long) = repository.deleteSection(sectionId)
}

