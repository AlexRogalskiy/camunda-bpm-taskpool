package io.holunda.polyflow.datapool.core.business

import io.holunda.camunda.taskpool.api.business.*
import io.holunda.polyflow.datapool.core.DataPoolCoreAxonConfiguration
import io.holunda.polyflow.datapool.core.DataPoolCoreConfiguration
import io.holunda.polyflow.datapool.core.DeletionStrategy
import mu.KLogging
import org.axonframework.commandhandling.CommandHandler
import org.axonframework.eventsourcing.AggregateDeletedException
import org.axonframework.eventsourcing.EventSourcingHandler
import org.axonframework.modelling.command.AggregateIdentifier
import org.axonframework.modelling.command.AggregateLifecycle
import org.axonframework.spring.stereotype.Aggregate

/**
 * Aggregate representing a data entry.
 * Currently, it has no state.
 * The aggregate is manually created by the CreateOrUpdateCommandHandler.
 */
@Aggregate(
  repository = DataPoolCoreAxonConfiguration.DATA_ENTRY_REPOSITORY,
  snapshotTriggerDefinition = DataPoolCoreAxonConfiguration.DATA_ENTRY_SNAPSHOTTER,
  cache = DataPoolCoreAxonConfiguration.DATA_ENTRY_CACHE,
)
class DataEntryAggregate() {

  companion object : KLogging()

  @AggregateIdentifier
  private lateinit var dataIdentity: String
  private var deleted: Boolean = false

  /**
   * Handle creation of data entry aggregate.
   */
  @CommandHandler
  constructor(command: CreateDataEntryCommand) : this() {
    AggregateLifecycle.apply(
      command.createdEvent()
    )
  }

  /**
   * Handle update.
   */
  @CommandHandler
  fun handle(command: UpdateDataEntryCommand, deletionStrategy: DeletionStrategy) {
    if (deletionStrategy.strictMode()) {
      if (deleted) {
        throw AggregateDeletedException(this.dataIdentity, "The data entry has already been deleted")
      }
    }
    AggregateLifecycle.apply(
      command.updatedEvent()
    )
  }

  /**
   * Handle delete.
   */
  @CommandHandler
  fun handle(command: DeleteDataEntryCommand, deletionStrategy: DeletionStrategy) {
    if (deletionStrategy.strictMode()) {
      if (deleted) {
        throw AggregateDeletedException(this.dataIdentity, "The data entry has already been deleted")
      }
    }
    AggregateLifecycle.apply(
      command.deletedEvent()
    )
  }

  /**
   * React on created event.
   */
  @EventSourcingHandler
  fun on(event: DataEntryCreatedEvent) {
    this.dataIdentity = dataIdentityString(entryType = event.entryType, entryId = event.entryId)
    if (this.deleted) {
      this.deleted = false
    }
    if (logger.isDebugEnabled) {
      logger.debug { "Created $dataIdentity." }
    }
    if (logger.isTraceEnabled) {
      logger.trace { "Created $dataIdentity with: $event" }
    }
  }

  /**
   * React on updated event.
   */
  @EventSourcingHandler
  fun on(event: DataEntryUpdatedEvent) {
    if (this.deleted) {
      this.deleted = false
    }
    if (logger.isDebugEnabled) {
      logger.debug { "Updated $dataIdentity." }
    }
    if (logger.isTraceEnabled) {
      logger.trace { "Updated $dataIdentity with: $event" }
    }
  }

  /**
   * React on deleted event.
   */
  @EventSourcingHandler
  fun on(event: DataEntryDeletedEvent) {
    if (logger.isDebugEnabled) {
      logger.debug { "Deleted $dataIdentity." }
    }
    if (logger.isTraceEnabled) {
      logger.trace { "Deleted $dataIdentity with: $event" }
    }
    // Don't use AggregateLifecycle.markDeleted() because then the combination of entryType / entryId is then really deleted forever
    this.deleted = true
  }
}
