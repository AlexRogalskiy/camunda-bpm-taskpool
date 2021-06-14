package io.holunda.camunda.taskpool.view.mongo.service

import io.holunda.camunda.taskpool.api.task.ProcessReference
import io.holunda.camunda.taskpool.api.task.TaskAssignedEngineEvent
import io.holunda.camunda.taskpool.view.mongo.ChangeTrackingMode
import io.holunda.camunda.taskpool.view.mongo.TaskPoolMongoViewProperties
import io.holunda.camunda.taskpool.view.mongo.repository.ProcessReferenceDocument
import io.holunda.camunda.taskpool.view.mongo.repository.TaskDocument
import io.holunda.camunda.taskpool.view.mongo.repository.TaskRepository
import org.axonframework.messaging.MetaData
import org.junit.Test
import org.mockito.kotlin.*
import reactor.core.publisher.Mono
import java.util.*


class PolyflowMongoServiceRetryTest {
  private val taskRepository: TaskRepository = mock()

  private val mongoViewService: MongoViewService = MongoViewService(
    properties = TaskPoolMongoViewProperties(changeTrackingMode = ChangeTrackingMode.CHANGE_STREAM),
    taskRepository = taskRepository,
    dataEntryRepository = mock(),
    taskWithDataEntriesRepository = mock(),
    taskChangeTracker = mock(),
    dataEntryChangeTracker = mock(),
    queryUpdateEmitter = mock(),
    configuration = mock()
  )

  private val taskId = "some-id"

  private val processReference = ProcessReference("", "", "", "", "", "")

  @Test
  fun `retries updates if task is not yet present in database`() {
    val taskDocument = TaskDocument(taskId, ProcessReferenceDocument(processReference), "foo:bar")
    val results = ArrayDeque<Mono<TaskDocument>>(listOf(
      Mono.empty(),
      Mono.empty(),
      Mono.just(taskDocument)
    ))
    whenever(taskRepository.findNotDeletedById(taskId)).thenReturn(Mono.defer { results.poll() })
    whenever(taskRepository.save(any<TaskDocument>())).thenAnswer { Mono.just(it.getArgument<TaskDocument>(0)) }
    mongoViewService.on(TaskAssignedEngineEvent(taskId, processReference, "foo:bar"), MetaData.emptyInstance())
    verify(taskRepository).save(taskDocument)
  }

  @Test
  fun `stops retrying after five attempts`() {
    whenever(taskRepository.findNotDeletedById(taskId)).thenReturn(Mono.empty())
    mongoViewService.on(TaskAssignedEngineEvent(taskId, processReference, "foo:bar"), MetaData.emptyInstance())
    verify(taskRepository, never()).save(any<TaskDocument>())
  }
}