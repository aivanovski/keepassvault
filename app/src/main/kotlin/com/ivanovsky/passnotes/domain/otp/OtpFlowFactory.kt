package com.ivanovsky.passnotes.domain.otp

import kotlin.math.max
import kotlin.math.min
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

object OtpFlowFactory {

    private const val MIN_PROGRESS_REFRESH_DELAY_MILLIS = 200L

    fun createCodeFlow(generator: TotpGenerator): Flow<String> =
        flow {
            while (true) {
                val code = generator.generateCode()
                emit(code)

                val remainingTime = generator.getRemainingTime()

                if (remainingTime > 0) {
                    delay(remainingTime)
                }
            }
        }

    fun createProgressFlow(generator: TotpGenerator): Flow<Int> =
        flow {
            while (true) {
                var remainingTime = generator.getRemainingTime()
                val progressInPercents = min(
                    (remainingTime.toFloat() / generator.periodInMillis * 100).toInt(),
                    100
                )

                emit(progressInPercents)

                remainingTime = generator.getNextTimeslotStart() - System.currentTimeMillis()
                val delay = calculateProgressRefreshDelay(generator.periodInMillis, remainingTime)
                if (delay > 0) {
                    delay(delay)
                }
            }
        }

    private fun calculateProgressRefreshDelay(
        periodInMillis: Long,
        remainingTime: Long
    ): Long {
        val delay = periodInMillis / 100 // update for each 1% of progress

        return if (remainingTime < delay) {
            remainingTime
        } else {
            // delay should not be less
            max(MIN_PROGRESS_REFRESH_DELAY_MILLIS, delay)
        }
    }
}