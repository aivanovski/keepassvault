package com.ivanovsky.passnotes.data.repository.keepass

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertWithMessage
import com.ivanovsky.passnotes.data.entity.FSAuthority
import com.ivanovsky.passnotes.data.entity.FileDescriptor
import com.ivanovsky.passnotes.data.entity.OperationResult
import com.ivanovsky.passnotes.data.repository.file.FSOptions
import com.ivanovsky.passnotes.data.repository.file.FileSystemResolver
import com.ivanovsky.passnotes.data.repository.keepass.DatabaseContentFactory.PASSWORD
import com.ivanovsky.passnotes.data.repository.keepass.keepassrs.KeepassRsDatabase
import com.ivanovsky.passnotes.data.repository.keepass.kotpass.KotpassDatabase
import com.ivanovsky.passnotes.data.repository.keepass.model.DatabaseFixture
import com.ivanovsky.passnotes.data.repository.keepass.model.DatabaseSize
import com.ivanovsky.passnotes.data.repository.keepass.model.HashAlgorithm
import java.io.ByteArrayInputStream
import java.io.InputStream
import kotlin.time.Duration
import kotlin.time.DurationUnit
import kotlin.time.measureTime
import kotlin.time.toDuration
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class KeepassDatabasePerformanceTest {

    @Test
    fun benchmarkKeepassImplementations() {
        printResults(
            measure(
                size = DatabaseSize.SMALL,
                iterations = 10,
                warmupIterations = 3
            )
        )

        printResults(
            measure(
                size = DatabaseSize.MEDIUM,
                iterations = 10,
                warmupIterations = 3
            )
        )

        printResults(
            measure(
                size = DatabaseSize.LARGE,
                iterations = 5,
                warmupIterations = 2
            )
        )
    }

    private fun measure(
        size: DatabaseSize,
        iterations: Int,
        warmupIterations: Int
    ): List<TestResult> {
        val results = mutableListOf<TestResult>()

        val hashes = HashAlgorithm.entries
        val implementations = KeepassImplementation.entries

        hashes.forEach { hashAlgorithm ->
            val db = DatabaseContentFactory.createDatabase(size, hashAlgorithm)

            implementations.forEach { implementation ->
                repeat(warmupIterations) {
                    measureTime {
                        openDatabase(
                            keepassImplementation = implementation,
                            database = db
                        )
                    }
                }

                repeat(iterations) {
                    val time = measureTime {
                        openDatabase(
                            keepassImplementation = implementation,
                            database = db
                        )
                    }

                    results.add(
                        TestResult(
                            size = db.requestedSize,
                            sizeInBytes = db.bytes.size,
                            hashAlgorithm = db.hash,
                            implementation = implementation,
                            time = time
                        )
                    )
                }
            }
        }

        return results
    }

    private fun openDatabase(
        keepassImplementation: KeepassImplementation,
        database: DatabaseFixture
    ) {
        val fileName = "file.kdbx"
        val file = FileDescriptor(
            fsAuthority = FSAuthority.INTERNAL_FS_AUTHORITY,
            path = "/benchmark/$fileName",
            uid = fileName,
            name = fileName,
            isDirectory = false,
            isRoot = false
        )

        val input: OperationResult<InputStream> =
            OperationResult.success(ByteArrayInputStream(database.bytes))

        val result = when (keepassImplementation) {
            KeepassImplementation.KOTPASS -> {
                KotpassDatabase.open(
                    fsResolver = EMPTY_FILE_SYSTEM_RESOLVER,
                    fsOptions = FSOptions.READ_ONLY,
                    file = file,
                    content = input,
                    key = PASSWORD_KEY
                )
            }

            KeepassImplementation.KEEPASS_RS -> {
                KeepassRsDatabase.open(
                    fsOptions = FSOptions.READ_ONLY,
                    file = file,
                    content = input,
                    key = PASSWORD_KEY
                )
            }
        }

        assertWithMessage("open result for $keepassImplementation: $result")
            .that(result.isSucceededOrDeferred)
            .isTrue()
    }

    private data class TestResult(
        val size: DatabaseSize,
        val sizeInBytes: Int,
        val hashAlgorithm: HashAlgorithm,
        val implementation: KeepassImplementation,
        val time: Duration
    )

    private fun printResults(results: List<TestResult>) {
        if (results.isEmpty()) {
            println("No results")
            return
        }

        val headers = listOf(
            "Size",
            "File size",
            "Hash",
            "Implementation",
            "Time"
        )

        val rows = results.map { result ->
            listOf(
                result.size.name,
                "${result.sizeInBytes} B",
                result.hashAlgorithm.name,
                result.implementation.name,
                result.time.toMillisString()
            )
        }

        val averageRows = results
            .groupBy { result ->
                result.hashAlgorithm to result.implementation
            }
            .map { (key, groupResults) ->
                val (hashAlgorithm, implementation) = key

                listOf(
                    groupResults.first().size.name,
                    "${groupResults.first().sizeInBytes} B",
                    hashAlgorithm.name,
                    implementation.name,
                    groupResults.map { it.time }.average().toMillisString()
                )
            }

        println("Measurements")
        printTable(headers, rows)
        println()
        println("Averages")
        printTable(headers, averageRows)
    }

    private fun printTable(
        headers: List<String>,
        rows: List<List<String>>
    ) {
        val widths = headers.indices.map { columnIndex ->
            maxOf(
                headers[columnIndex].length,
                rows.maxOf { row -> row[columnIndex].length }
            )
        }

        val separator = widths.joinToString(
            prefix = "+-",
            postfix = "-+",
            separator = "-+-"
        ) { "-".repeat(it) }

        fun formatRow(columns: List<String>): String {
            return columns.mapIndexed { index, value ->
                value.padEnd(widths[index])
            }.joinToString(
                prefix = "| ",
                postfix = " |",
                separator = " | "
            )
        }

        println(separator)
        println(formatRow(headers))
        println(separator)
        rows.forEach { row ->
            println(formatRow(row))
        }
        println(separator)
    }

    companion object {

        private const val TAG = "KeepassPerfTest"

        private const val ITERATIONS = 10

        private val PASSWORD_KEY = PasswordKeepassKey(PASSWORD)
        private val EMPTY_FILE_SYSTEM_RESOLVER = FileSystemResolver(emptyMap())
    }
}

private fun Duration.toMillisString(): String {
    return "%.3f ms".format(inWholeMicroseconds / 1000.0)
}

private fun List<Duration>.average(): Duration {
    val averageMicros = map { it.inWholeMicroseconds }
        .average()

    return averageMicros.toDuration(DurationUnit.MICROSECONDS)
}
