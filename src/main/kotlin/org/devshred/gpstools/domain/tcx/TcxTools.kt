package org.devshred.gpstools.domain.tcx

import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.dataformat.xml.XmlMapper
import com.fasterxml.jackson.dataformat.xml.ser.ToXmlGenerator
import io.jenetics.jpx.geom.Geoid
import org.devshred.gpstools.domain.gps.GpsContainer
import org.devshred.gpstools.domain.gps.WayPoint
import java.io.ByteArrayOutputStream
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.function.Consumer

object TcxTools {
    val XML_MAPPER: XmlMapper =
        XmlMapper.Builder(XmlMapper())
            .enable(SerializationFeature.INDENT_OUTPUT)
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            .build()

    init {
        XML_MAPPER.factory.enable(ToXmlGenerator.Feature.WRITE_XML_DECLARATION)
    }
}

fun tcxToByteArrayOutputStream(tcx: TrainingCenterDatabase): ByteArrayOutputStream {
    val out = ByteArrayOutputStream()
    TcxTools.XML_MAPPER.writeValue(out, tcx)
    return out
}

fun createTcxFromGpsContainer(gpsContainer: GpsContainer): TrainingCenterDatabase {
    val trainingCenterDatabase = TrainingCenterDatabase()

    val course = Course(gpsContainer.name!!)
    val waiPoints = gpsContainer.track!!.wayPoints
    val lap =
        Lap(
            totalTimeSeconds = (waiPoints[waiPoints.size - 1].time!!.epochSecond - waiPoints[0].time!!.epochSecond).toDouble(),
            distanceMeters = gpsContainer.track.calculateLength().toDouble(),
            beginPosition = Position(waiPoints[0].latitude, waiPoints[0].longitude),
            endPosition =
                Position(
                    waiPoints[waiPoints.size - 1].latitude,
                    waiPoints[waiPoints.size - 1].longitude,
                ),
            intensity = "Active",
        )
    course.setLap(lap)

    val track = Track()
    var distance = 0.0
    var previous: WayPoint? = null
    for (point in gpsContainer.track.wayPoints) {
        if (previous != null) {
            distance += Geoid.WGS84.distance(previous.toGpxPoint(), point.toGpxPoint()).toDouble()
        }
        track.addTrackpoint(
            Trackpoint(
                ZonedDateTime.ofInstant(point.time, ZoneId.of("UTC")),
                Position(point.latitude, point.longitude),
                point.elevation!!.toDouble(),
                distance,
            ),
        )
        previous = point
    }
    course.setTrack(track)

    gpsContainer.wayPoints.forEach(
        Consumer { wayPoint: WayPoint ->
            course.addCoursePoint(
                CoursePoint(
                    wayPoint.name!!,
                    wayPoint.time!!.atZone(ZoneId.of("UTC")),
                    Position(wayPoint.latitude, wayPoint.longitude),
                    wayPoint.type!!.tcxType,
                ),
            )
        },
    )

    trainingCenterDatabase.addCourse(course)

    return trainingCenterDatabase
}