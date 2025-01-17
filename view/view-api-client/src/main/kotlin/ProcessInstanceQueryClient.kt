package io.holunda.polyflow.view

import io.holixon.axon.gateway.query.QueryResponseMessageResponseType
import io.holunda.polyflow.view.query.data.DataEntriesQueryResult
import io.holunda.polyflow.view.query.process.ProcessDefinitionsStartableByUserQuery
import io.holunda.polyflow.view.query.process.ProcessInstanceQueryResult
import io.holunda.polyflow.view.query.process.ProcessInstancesByStateQuery
import io.holunda.polyflow.view.query.task.*
import org.axonframework.messaging.responsetypes.ResponseTypes
import org.axonframework.queryhandling.QueryGateway
import java.util.concurrent.CompletableFuture

/**
 * Client encapsulating the correct query types (including response types)
 */
class ProcessInstanceQueryClient(
  private val queryGateway: QueryGateway
) {

  /**
   * @see io.holunda.polyflow.view.query.process.ProcessInstanceApi.query
   * @see io.holunda.polyflow.view.query.process.ProcessInstancesByStateQuery
   */
  fun query(query: ProcessInstancesByStateQuery): CompletableFuture<ProcessInstanceQueryResult> =
    queryGateway.query(
      query,
      QueryResponseMessageResponseType.queryResponseMessageResponseType<ProcessInstanceQueryResult>()
    )

}
