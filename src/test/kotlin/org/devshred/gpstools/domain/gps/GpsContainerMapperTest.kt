package org.devshred.gpstools.domain.gps

import io.jenetics.jpx.GPX
import org.apache.commons.lang3.RandomStringUtils
import org.apache.commons.math3.random.RandomDataGenerator
import org.assertj.core.api.Assertions.assertThat
import org.devshred.gpstools.formats.gps.ExtensionValues
import org.devshred.gpstools.formats.gps.GpsContainerMapper
import org.devshred.gpstools.formats.gps.PoiType
import org.devshred.gpstools.formats.gps.toGps
import org.devshred.gpstools.formats.gps.toProto
import org.devshred.gpstools.formats.proto.protoContainer
import org.devshred.gpstools.formats.proto.protoTrack
import org.devshred.gpstools.formats.proto.protoWayPoint
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import java.util.stream.Stream
import io.jenetics.jpx.WayPoint as GpxWayPoint

class GpsContainerMapperTest {
    private val randomGenerator = RandomDataGenerator().randomGenerator
    private val mapper: GpsContainerMapper = GpsContainerMapper()

    @Test
    fun `convert from proto to GpsContainer`() {
        val protoGpsContainer: org.devshred.gpstools.formats.proto.ProtoContainer =
            protoContainer {
                name = "My Track"
                wayPoints +=
                    listOf(
                        protoWayPoint {
                            latitude = 1.0
                            longitude = 2.0
                            type = org.devshred.gpstools.formats.proto.ProtoPoiType.SUMMIT
                        },
                    )
                track =
                    protoTrack {
                        wayPoints +=
                            protoWayPoint {
                                latitude = 1.0
                                longitude = 2.0
                            }
                    }
            }

        val domainGpsContainer = mapper.fromProto(protoGpsContainer)

        assertThat(domainGpsContainer.name).isEqualTo("My Track")
        assertThat(domainGpsContainer.wayPoints).hasSize(1)
        assertThat(domainGpsContainer.wayPoints[0].type).isEqualTo(PoiType.SUMMIT)
        assertThat(domainGpsContainer.track!!.wayPoints).hasSize(1)
    }

    @Test
    fun `set trackname from first track`() {
        val trackname = RandomStringUtils.randomAlphabetic(8)
        val gpx =
            GPX.builder()
                .metadata { m -> m.name("yet another name") }
                .addTrack { track ->
                    run {
                        track.name(trackname)
                        track.addSegment { s -> s.addPoint(randomWayPoint()).build() }
                    }
                }
                .build()

        val gpsContainer = mapper.fromGpx(gpx)

        assertThat(gpsContainer.name).isEqualTo(trackname)
    }

    @Test
    fun `set trackname from GPX metadata if no track was found`() {
        val trackname = RandomStringUtils.randomAlphabetic(8)
        val gpx =
            GPX.builder()
                .metadata { m -> m.name(trackname) }
                .build()

        val gpsContainer = mapper.fromGpx(gpx)

        assertThat(gpsContainer.name).isEqualTo(trackname)
    }

    @Test
    fun `skip setting trackname if neither track nor metadata was found`() {
        val gpx = GPX.builder().build()

        val gpsContainer = mapper.fromGpx(gpx)

        assertThat(gpsContainer.name).isNull()
    }

    @Test
    fun `map GPX WayPoint`() {
        val lat = randomGenerator.nextDouble()
        val lon = randomGenerator.nextDouble()
        val gpx = GpxWayPoint.of(lat, lon)

        val protoBuf = gpx.toGps()

        assertThat(protoBuf.latitude).isEqualTo(lat)
        assertThat(protoBuf.longitude).isEqualTo(lon)
    }

    @Test
    fun `map protoBuf WayPoint`() {
        val lat = randomGenerator.nextDouble()
        val lon = randomGenerator.nextDouble()
        val protoBuf =
            protoWayPoint {
                latitude = lat
                longitude = lon
            }

        val gpx = protoBuf.toGps()

        assertThat(gpx.latitude).isEqualTo(lat)
        assertThat(gpx.longitude).isEqualTo(lon)
    }

    @ParameterizedTest(name = "{0} should convert to {1}")
    @MethodSource("protoToGps")
    fun `convert PoiType to ProtoPoiType`(
        poiType: PoiType,
        protoPoiType: org.devshred.gpstools.formats.proto.ProtoPoiType,
    ) {
        assertThat(poiType.toProto()).isEqualTo(protoPoiType)
    }

    companion object {
        @JvmStatic
        private fun protoToGps(): Stream<Arguments> {
            return Stream.of(
                Arguments.of(PoiType.GENERIC, org.devshred.gpstools.formats.proto.ProtoPoiType.GENERIC),
                Arguments.of(PoiType.SUMMIT, org.devshred.gpstools.formats.proto.ProtoPoiType.SUMMIT),
                Arguments.of(PoiType.VALLEY, org.devshred.gpstools.formats.proto.ProtoPoiType.VALLEY),
                Arguments.of(PoiType.WATER, org.devshred.gpstools.formats.proto.ProtoPoiType.WATER),
                Arguments.of(PoiType.FOOD, org.devshred.gpstools.formats.proto.ProtoPoiType.FOOD),
                Arguments.of(PoiType.DANGER, org.devshred.gpstools.formats.proto.ProtoPoiType.DANGER),
                Arguments.of(PoiType.LEFT, org.devshred.gpstools.formats.proto.ProtoPoiType.LEFT),
                Arguments.of(PoiType.RIGHT, org.devshred.gpstools.formats.proto.ProtoPoiType.RIGHT),
                Arguments.of(PoiType.STRAIGHT, org.devshred.gpstools.formats.proto.ProtoPoiType.STRAIGHT),
                Arguments.of(PoiType.FIRST_AID, org.devshred.gpstools.formats.proto.ProtoPoiType.FIRST_AID),
                Arguments.of(PoiType.FOURTH_CATEGORY, org.devshred.gpstools.formats.proto.ProtoPoiType.FOURTH_CATEGORY),
                Arguments.of(PoiType.THIRD_CATEGORY, org.devshred.gpstools.formats.proto.ProtoPoiType.THIRD_CATEGORY),
                Arguments.of(PoiType.SECOND_CATEGORY, org.devshred.gpstools.formats.proto.ProtoPoiType.SECOND_CATEGORY),
                Arguments.of(PoiType.FIRST_AID, org.devshred.gpstools.formats.proto.ProtoPoiType.FIRST_AID),
                Arguments.of(PoiType.HORS_CATEGORY, org.devshred.gpstools.formats.proto.ProtoPoiType.HORS_CATEGORY),
                Arguments.of(PoiType.RESIDENCE, org.devshred.gpstools.formats.proto.ProtoPoiType.RESIDENCE),
                Arguments.of(PoiType.SPRINT, org.devshred.gpstools.formats.proto.ProtoPoiType.SPRINT),
            )
        }
    }

    @Test
    fun `union ExtensionValues (B overrides A)`() {
        val valuesA = ExtensionValues(1, 2, null, 4)
        val valuesB = ExtensionValues(null, null, 7, 8)
        val expected = ExtensionValues(1, 2, 7, 8)

        val actual = valuesA.union(valuesB)

        assertThat(actual).isEqualTo(expected)
    }

    private fun randomWayPoint(): GpxWayPoint =
        GpxWayPoint.builder().lat(randomGenerator.nextDouble()).lon(randomGenerator.nextDouble()).build()
}