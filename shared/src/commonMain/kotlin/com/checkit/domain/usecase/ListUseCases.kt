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
