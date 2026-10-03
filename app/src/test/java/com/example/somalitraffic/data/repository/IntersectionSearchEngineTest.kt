package com.example.somalitraffic.data.repository

import com.example.somalitraffic.data.model.Intersection
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class IntersectionSearchEngineTest {

    private lateinit var sampleIntersections: List<Intersection>

    @Before
    fun setUp() {
        sampleIntersections = listOf(
            Intersection(
                id = "int-km4",
                name = "KM4 (Kilometer 4)",
                roadNames = "Wadada Maka Al-Mukarama, Wadada Garoonka Diyaaradaha, Wadada Taleex",
                aliases = "KM4, Kilometer 4, Isgoyska KM4, Shaqaalaha",
                connectingRoads = "Maka Al-Mukarama, Taleex, Garoonka",
                district = "Hodan",
                latitude = 2.0335,
                longitude = 45.3190,
                currentStatus = "Saxmad badan",
                latestVerifiedReport = "Saxmad culus ayaa ka jirta dhinaca Taleex",
                verifiedByReporter = true
            ),
            Intersection(
                id = "int-zoobe",
                name = "Zoobe (KM5)",
                roadNames = "Wadada Maka Al-Mukarama, Wadada 21 October, Wadada Garoonka",
                aliases = "Zoobe, KM5, Kilometer 5, 21 October, Isgoyska Zoobe",
                connectingRoads = "Maka Al-Mukarama, 21 October, Garoonka",
                district = "Hodan",
                latitude = 2.0298,
                longitude = 45.3115,
                currentStatus = "Mashquul",
                latestVerifiedReport = "Gaadiidka xamuulka ayaa hakad yar galiyay",
                verifiedByReporter = true
            ),
            Intersection(
                id = "int-bakaaraha",
                name = "Isgoyska Bakaaraha",
                roadNames = "Wadada Wadnaha, Wadada Hawlwadaag",
                aliases = "Bakaaraha, Suuqa Bakaaraha, Suuqa, Wadnaha",
                connectingRoads = "Wadnaha, Hawlwadaag",
                district = "Hawlwadaag",
                latitude = 2.0460,
                longitude = 45.3255,
                currentStatus = "Dhexdhexaad",
                latestVerifiedReport = "Socodka gawaarida waa caadi",
                verifiedByReporter = true
            )
        )
    }

    @Test
    fun testDirectNameSearch() {
        val results = IntersectionSearchEngine.search(
            query = "KM4",
            intersections = sampleIntersections
        )
        assertFalse(results.isEmpty())
        assertEquals("int-km4", results.first().intersection.id)
    }

    @Test
    fun testPrefixStripping_isgoyska() {
        val results = IntersectionSearchEngine.search(
            query = "isgoyska zoobe",
            intersections = sampleIntersections
        )
        assertFalse(results.isEmpty())
        assertEquals("int-zoobe", results.first().intersection.id)
    }

    @Test
    fun testTwoRoadsIntersectionSearch() {
        // Maka Al-Mukarama & 21 October should match Zoobe which connects both roads
        val results = IntersectionSearchEngine.search(
            query = "Maka Al-Mukarama & 21 October",
            intersections = sampleIntersections
        )
        assertFalse(results.isEmpty())
        assertEquals("int-zoobe", results.first().intersection.id)
    }

    @Test
    fun testTwoRoadsWithAndWord() {
        val results = IntersectionSearchEngine.search(
            query = "Maka Al-Mukarama iyo Taleex",
            intersections = sampleIntersections
        )
        assertFalse(results.isEmpty())
        assertEquals("int-km4", results.first().intersection.id)
    }

    @Test
    fun testDistanceSortingWhenLocationProvided() {
        // User is at (2.0300, 45.3120) which is very close to Zoobe (2.0298, 45.3115)
        val results = IntersectionSearchEngine.search(
            query = "Maka Al-Mukarama", // both KM4 and Zoobe have Maka Al-Mukarama
            intersections = sampleIntersections,
            userLat = 2.0300,
            userLng = 45.3120
        )
        assertFalse(results.isEmpty())
        // Closest should be ranked higher
        assertEquals("int-zoobe", results.first().intersection.id)
        assertNotNull(results.first().formattedDistance)
    }

    @Test
    fun testEmptyQueryReturnsAllSortedByDistance() {
        val results = IntersectionSearchEngine.search(
            query = "",
            intersections = sampleIntersections,
            userLat = 2.0335,
            userLng = 45.3190
        )
        assertEquals(3, results.size)
        // KM4 should be first since user is exactly at KM4 coordinates
        assertEquals("int-km4", results.first().intersection.id)
    }

    @Test
    fun testNonExistentQueryReturnsEmpty() {
        val results = IntersectionSearchEngine.search(
            query = "Hargeisa Central Junction",
            intersections = sampleIntersections
        )
        assertTrue(results.isEmpty())
    }
}
