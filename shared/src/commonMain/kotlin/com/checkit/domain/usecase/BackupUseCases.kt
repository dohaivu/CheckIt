package com.checkit.domain.usecase

import com.checkit.data.CheckItRepository

/** Serializes the whole local database (plus settings) to a JSON string. */
class ExportBackupUseCase(
    private val repository: CheckItRepository,
) {
    suspend operator fun invoke(): String = repository.exportBackupJson()
}

/** Replaces the whole local database (plus settings) from a JSON backup string. */
class ImportBackupUseCase(
    private val repository: CheckItRepository,
) {
    suspend operator fun invoke(json: String) = repository.importBackupJson(json)
}
