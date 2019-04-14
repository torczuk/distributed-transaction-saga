package com.github.torczuk

import com.github.torczuk.domain.BookingEvent
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import java.util.*

internal class BookingServiceTest {
    val queue = ArrayDeque<BookingEvent>()
    val producer = InMemoryProducer(queue)
    val consumer = InMemoryConsumer(queue)
    val clock = Clock.fixed(Instant.now(), ZoneId.of("UTC"))

    val bookingService = BookingService(clock, producer)

    @Test
    internal fun `published booking should be available by consumer`() {
        val transactionId = UUID.randomUUID().toString()
        val expectedTimestamp = Instant.now(clock).toEpochMilli()

        val event = bookingService.create(transactionId)

        assertThat(event).isEqualTo(BookingEvent(transactionId, "created", expectedTimestamp))
        assertThat(consumer.get()).isEqualTo(event)
    }

}