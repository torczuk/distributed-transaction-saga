package com.github.torczuk.infractructure.database

import com.fasterxml.jackson.databind.ObjectMapper
import com.github.torczuk.domain.BookingEvent
import com.github.torczuk.domain.BookingEventRepository
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Repository
import java.nio.file.Files
import java.nio.file.Paths
import java.nio.file.StandardOpenOption
import java.util.stream.Collectors.toList

@Repository
class FileBasedBookingEventRepository(
        @Value("\${application.file-based-db.path}") path: String,
        @Autowired val mapper: ObjectMapper) : BookingEventRepository {

    val log = LoggerFactory.getLogger(FileBasedBookingEventRepository::class.java)
    val location = Paths.get(path)

    override fun exist(transactionId: String): Boolean {
        log.debug("exist $transactionId")
        return Files.list(location)
                .anyMatch { path -> path.fileName.toFile().name.startsWith(transactionId) }
    }

    override fun findAll(): List<BookingEvent> {
        log.debug("findAll")
        return Files.list(location)
                .map { path -> Files.readAllBytes(path)  }
                .map { bytes -> String(bytes) }
                .map { payload -> mapper.readValue(payload, BookingEvent::class.java) }
                .collect(toList())
    }

    override fun findBy(transactionId: String): List<BookingEvent> {
        log.debug("findBy $transactionId")
        return Files.list(location)
                .filter { file -> file.fileName.toFile().name.startsWith(transactionId) }
                .map { file -> Files.readAllBytes(file)  }
                .map { bytes -> String(bytes) }
                .map { payload -> mapper.readValue(payload, BookingEvent::class.java) }
                .collect(toList())
    }

    override fun save(event: BookingEvent) {
        log.debug("save $event")
        Files.write(location.resolve("${event.transaction}_${event.type}"),
                mapper.writeValueAsString(event).toByteArray(Charsets.UTF_8),
                StandardOpenOption.CREATE)
    }
}