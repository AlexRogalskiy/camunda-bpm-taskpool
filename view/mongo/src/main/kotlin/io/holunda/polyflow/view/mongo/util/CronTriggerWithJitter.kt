package io.holunda.polyflow.view.mongo.util

import org.springframework.scheduling.Trigger
import org.springframework.scheduling.TriggerContext
import org.springframework.scheduling.support.CronExpression
import java.time.Duration
import java.time.ZoneId
import java.time.ZoneOffset
import java.util.*
import kotlin.random.Random
import kotlin.random.nextLong

/**
 * Spring scheduling `Trigger` based on cron expression and jitter. This will reduce the probability of two application instances running the same job at the
 * same time. The higher the jitter duration, the lower the probability of a clash, though it cannot be completely ruled out.
 * By leaving some chance that the job is executed twice at the same time, this avoids the need for some centralized locking solution like ShedLock.
 */
data class CronTriggerWithJitter(val expression: CronExpression, val jitter: Duration, val zoneId: ZoneId = ZoneOffset.UTC) : Trigger {
  override fun nextExecutionTime(triggerContext: TriggerContext): Date? {
    val lastCompletionTime = triggerContext.lastCompletionTime()
    val lastCompletionTimeAdjusted = (lastCompletionTime?.coerceAtLeast(triggerContext.lastScheduledExecutionTime() ?: lastCompletionTime)?.toInstant()
      ?: triggerContext.clock.instant()).atZone(zoneId)
    val cronNextExecutionTime = expression.next(lastCompletionTimeAdjusted) ?: return null

    val offset = Random.nextLong(0..jitter.toNanos())
    return Date.from(cronNextExecutionTime.toInstant().plusNanos(offset))
  }
}
