package com.ivanovsky.passnotes.presentation.service

import com.ivanovsky.passnotes.domain.DispatcherProvider
import com.ivanovsky.passnotes.presentation.service.model.TaskProcessorState
import com.ivanovsky.passnotes.presentation.service.task.LockServiceTask
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.atomic.AtomicReference
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import timber.log.Timber

class LockServiceTaskProcessor(
    private val service: LockServiceFacade,
    dispatchers: DispatcherProvider
) {

    private val queue = ConcurrentLinkedQueue<LockServiceTask>()
    private val job = Job()
    private val scope = CoroutineScope(dispatchers.Main + job)
    private val state = AtomicReference(TaskProcessorState.IDLE)
    private val lock = ReentrantLock()

    fun process(task: LockServiceTask) {
        when (state.get()) {
            TaskProcessorState.PROCESSING -> {
                lock.withLock {
                    queue.add(task)
                }
            }

            TaskProcessorState.IDLE -> {
                processTask(task)
            }

            else -> {}
        }
    }

    fun stop() {
        job.cancel()
        state.set(TaskProcessorState.STOPPED)
    }

    private fun processTask(task: LockServiceTask) {
        state.set(TaskProcessorState.PROCESSING)

        scope.launch {
            Timber.d("processTask: %s", task::class.java.simpleName)
            task.execute(service)
            onTaskFinished()
        }
    }

    private fun onTaskFinished() {
        Timber.d("onTaskFinished:")
        val nextTask = lock.withLock {
            if (queue.isNotEmpty()) {
                queue.poll()
            } else {
                state.set(TaskProcessorState.IDLE)
                null
            }
        }

        if (nextTask != null) {
            processTask(nextTask)
        }
    }
}