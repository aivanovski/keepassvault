package com.ivanovsky.passnotes.domain.usecases.test

import com.ivanovsky.passnotes.BuildConfig
import com.ivanovsky.passnotes.data.entity.TestData
import com.ivanovsky.passnotes.data.repository.settings.Settings
import com.ivanovsky.passnotes.domain.DispatcherProvider
import com.ivanovsky.passnotes.util.FileUtils
import java.util.regex.Pattern
import kotlinx.coroutines.withContext

class GetTestPasswordUseCase(
    private val settings: Settings,
    private val dispatchers: DispatcherProvider
) {

    suspend fun getTestPasswordForFile(filename: String): String? =
        withContext(dispatchers.IO) {
            if (!BuildConfig.DEBUG) {
                return@withContext null
            }

            val data = settings.testData ?: return@withContext null

            val rules = createPasswordRules(data)
            val fileNameWithoutExtension = FileUtils.removeFileExtensionsIfNeed(filename)

            for (rule in rules) {
                if (rule.pattern.matcher(fileNameWithoutExtension).matches()) {
                    return@withContext rule.password
                }
            }

            null
        }

    private fun createPasswordRules(data: TestData): List<PasswordRule> {
        val rules = mutableListOf<PasswordRule>()

        for (idx in data.filenamePatterns.indices) {
            val fileNamePattern = data.filenamePatterns[idx]
            val password = data.passwords[idx]

            val pattern = Pattern.compile(fileNamePattern)

            rules.add(
                PasswordRule(
                    pattern,
                    password
                )
            )
        }

        return rules
    }

    private data class PasswordRule(
        val pattern: Pattern,
        val password: String
    )
}