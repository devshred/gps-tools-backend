package org.devshred.gpstools.domain.gps

import io.jenetics.jpx.Length.Unit.METER
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.within
import org.devshred.gpstools.formats.gps.Track
import org.devshred.gpstools.formats.gps.TrackPoint
import org.junit.jupiter.api.Test

class TrackTest {
    @Test
    fun `test calculateLength`() {
        val track =
            Track(
                listOf(
                    TrackPoint(1.0, 1.0),
                    TrackPoint(2.0, 2.0),
                    TrackPoint(3.0, 3.0),
                ),
            )

        assertThat(track.calculateLength().to(METER)).isCloseTo(313705.4785, within(0.0001))
    }
}
