package org.devshred.gpstools.storage

import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.devshred.gpstools.api.model.FeatureCollectionDTO
import org.devshred.gpstools.api.model.FeatureDTO
import org.devshred.gpstools.api.model.PointDTO
import org.devshred.gpstools.formats.gps.GpsContainerMapper
import org.devshred.gpstools.formats.gpx.GpxService
import org.devshred.gpstools.formats.proto.ProtoPoiType
import org.devshred.gpstools.formats.proto.ProtoService
import org.devshred.gpstools.formats.proto.protoContainer
import org.devshred.gpstools.formats.proto.protoPointOfInterest
import org.devshred.gpstools.formats.proto.protoTrack
import org.devshred.gpstools.formats.proto.protoTrackPoint
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType.TEXT_PLAIN_VALUE
import java.math.BigDecimal
import java.util.UUID

class FileServiceTest {
    private val fileStore = mockk<FileStore>()
    private val ioService = mockk<IOService>()
    private val protoService = mockk<ProtoService>()
    private val gpxService = mockk<GpxService>()

    private val mapper = GpsContainerMapper()

    private var cut = FileService(fileStore, ioService, protoService, gpxService, mapper)

    private val trackId = UUID.randomUUID()
    private val storageLocation = "/path/to/file"
    private val fileName = Filename("test.txt")
    private val storedFile = StoredFile(trackId, fileName, TEXT_PLAIN_VALUE, "href", 123, storageLocation)

    @Test
    fun `replace single point`() {
        val protoContainer = createProtoContainer(Pair(UUID.randomUUID(), "some point"))

        every { fileStore.get(trackId) } returns storedFile
        every { protoService.readProtoContainer(storageLocation) } returns protoContainer
        every { ioService.createTempFile(any(), fileName) } returns storedFile
        every { fileStore.put(trackId, storedFile) } returns Unit
        every { ioService.delete(storageLocation) } returns Unit

        val pointDTO =
            PointDTO(
                coordinates = listOf(BigDecimal.valueOf(11), BigDecimal.valueOf(22)),
                type = "Point",
            )
        val featureDTO =
            FeatureDTO(
                geometry = pointDTO,
                properties =
                    mapOf(
                        "name" to "another point",
                        "type" to "GENERIC",
                        "uuid" to UUID.randomUUID().toString(),
                    ),
                type = "Feature",
            )

        // 'merge == false' => replace
        val wayPointUpdate = cut.handleWayPointUpdate(trackId, featureDTO, null, false)

        assertThat(wayPointUpdate.features).hasSize(1)
        assertThat(wayPointUpdate.features[0].properties["name"]).isEqualTo("another point")
    }

    @Test
    fun `replace multiple points`() {
        val protoContainer =
            createProtoContainer(Pair(UUID.randomUUID(), "first point"), Pair(UUID.randomUUID(), "second point"))

        every { fileStore.get(trackId) } returns storedFile
        every { protoService.readProtoContainer(storageLocation) } returns protoContainer
        every { ioService.createTempFile(any(), fileName) } returns storedFile
        every { fileStore.put(trackId, storedFile) } returns Unit
        every { ioService.delete(storageLocation) } returns Unit

        val pointDTO1 =
            PointDTO(
                coordinates = listOf(BigDecimal.valueOf(11), BigDecimal.valueOf(21)),
                type = "Point",
            )
        val featureDTO1 =
            FeatureDTO(
                geometry = pointDTO1,
                properties =
                    mapOf(
                        "name" to "another first point",
                        "type" to "GENERIC",
                        "uuid" to UUID.randomUUID().toString(),
                    ),
                type = "Feature",
            )
        val pointDTO2 =
            PointDTO(
                coordinates = listOf(BigDecimal.valueOf(12), BigDecimal.valueOf(22)),
                type = "Point",
            )
        val featureDTO2 =
            FeatureDTO(
                geometry = pointDTO2,
                properties =
                    mapOf(
                        "name" to "another second point",
                        "type" to "GENERIC",
                        "uuid" to UUID.randomUUID().toString(),
                    ),
                type = "Feature",
            )
        val featureCollectionDTO =
            FeatureCollectionDTO(
                features = listOf(featureDTO1, featureDTO2),
                type = "FeatureCollection",
            )
        // 'merge == false' => replace
        val wayPointUpdate = cut.handleWayPointUpdate(trackId, featureCollectionDTO, null, false)

        assertThat(wayPointUpdate.features).hasSize(2)
        assertThat(wayPointUpdate.features.map { it.properties["name"] }).containsOnly(
            "another first point",
            "another second point",
        )
    }

    @Test
    fun `merge point`() {
        val pointId = UUID.randomUUID()
        val protoContainer =
            createProtoContainer(Pair(pointId, "first point"), Pair(UUID.randomUUID(), "second point"))

        every { fileStore.get(trackId) } returns storedFile
        every { protoService.readProtoContainer(storageLocation) } returns protoContainer
        every { ioService.createTempFile(any(), fileName) } returns storedFile
        every { fileStore.put(trackId, storedFile) } returns Unit
        every { ioService.delete(storageLocation) } returns Unit

        val pointDTO1 =
            PointDTO(
                coordinates = listOf(BigDecimal.valueOf(11), BigDecimal.valueOf(21)),
                type = "Point",
            )
        val featureDTO1 =
            FeatureDTO(
                geometry = pointDTO1,
                properties =
                    mapOf(
                        "name" to "another first point",
                        "type" to "GENERIC",
                        "uuid" to pointId.toString(),
                    ),
                type = "Feature",
            )
        val featureCollectionDTO =
            FeatureCollectionDTO(
                features = listOf(featureDTO1),
                type = "FeatureCollection",
            )
        // 'merge == true' => replace if pointId already exists
        val wayPointUpdate = cut.handleWayPointUpdate(trackId, featureCollectionDTO, null, true)

        assertThat(wayPointUpdate.features).hasSize(2)
        assertThat(wayPointUpdate.features.map { it.properties["name"] }).containsOnly(
            "another first point",
            "second point",
        )
    }

    @Test
    fun `add point`() {
        val protoContainer = createProtoContainer(Pair(UUID.randomUUID(), "first point"))

        every { fileStore.get(trackId) } returns storedFile
        every { protoService.readProtoContainer(storageLocation) } returns protoContainer
        every { ioService.createTempFile(any(), fileName) } returns storedFile
        every { fileStore.put(trackId, storedFile) } returns Unit
        every { ioService.delete(storageLocation) } returns Unit

        val pointDTO1 =
            PointDTO(
                coordinates = listOf(BigDecimal.valueOf(11), BigDecimal.valueOf(21)),
                type = "Point",
            )
        val featureDTO1 =
            FeatureDTO(
                geometry = pointDTO1,
                properties =
                    mapOf(
                        "name" to "second point",
                        "type" to "GENERIC",
                        "uuid" to UUID.randomUUID().toString(),
                    ),
                type = "Feature",
            )
        val featureCollectionDTO =
            FeatureCollectionDTO(
                features = listOf(featureDTO1),
                type = "FeatureCollection",
            )
        // 'merge == true' => add if pointId not exists
        val wayPointUpdate = cut.handleWayPointUpdate(trackId, featureCollectionDTO, null, true)

        assertThat(wayPointUpdate.features).hasSize(2)
        assertThat(wayPointUpdate.features.map { it.properties["name"] }).containsOnly(
            "first point",
            "second point",
        )
    }

    private fun createProtoContainer(vararg points: Pair<UUID, String>) =
        protoContainer {
            name = "My Track"
            pointsOfInterest +=
                points.map {
                    protoPointOfInterest {
                        uuid = it.first.toString()
                        name = it.second
                        latitude = 1.0
                        longitude = 2.0
                        type = ProtoPoiType.SUMMIT
                    }
                }
            track =
                protoTrack {
                    trackPoints +=
                        protoTrackPoint {
                            latitude = 1.0
                            longitude = 2.0
                        }
                }
        }
}