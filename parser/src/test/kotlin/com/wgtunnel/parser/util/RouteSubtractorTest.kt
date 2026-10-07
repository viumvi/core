package com.wgtunnel.parser.util

import com.wgtunnel.parser.Config
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class RouteSubtractorTest {

    @Test
    fun nothingExcludedKeepsIncluded() {
        assertEquals(listOf("0.0.0.0/0"), RouteSubtractor.subtract(listOf("0.0.0.0/0"), emptyList()))
    }

    @Test
    fun excludesUpperHalf() {
        assertEquals(listOf("0.0.0.0/1"), RouteSubtractor.subtract(listOf("0.0.0.0/0"), listOf("128.0.0.0/1")))
    }

    @Test
    fun excludesInnerBlocksWithMinimalCidrs() {
        assertEquals(
            listOf("10.0.0.0/30", "10.0.0.8/29"),
            RouteSubtractor.subtract(listOf("10.0.0.0/27"), listOf("10.0.0.4/30", "10.0.0.16/28")),
        )
    }

    @Test
    fun overlappingAndNestedExclusionsAreMerged() {
        assertEquals(
            listOf("10.0.0.0/25"),
            RouteSubtractor.subtract(
                listOf("10.0.0.0/24"),
                listOf("10.0.0.128/25", "10.0.0.192/26", "10.0.0.130/32"),
            ),
        )
    }

    @Test
    fun exclusionCoveringEverythingLeavesNothing() {
        assertTrue(RouteSubtractor.subtract(listOf("10.1.0.0/16"), listOf("10.0.0.0/8")).isEmpty())
    }

    @Test
    fun familiesAreIndependent() {
        assertEquals(
            listOf("0.0.0.0/1", "0:0:0:0:0:0:0:0/0"),
            RouteSubtractor.subtract(listOf("0.0.0.0/0", "::/0"), listOf("128.0.0.0/1")),
        )
        assertEquals(
            listOf("0.0.0.0/0", "0:0:0:0:0:0:0:0/1"),
            RouteSubtractor.subtract(listOf("0.0.0.0/0", "::/0"), listOf("8000::/1")),
        )
    }

    @Test
    fun defaultRouteMinusOneHostYields32Blocks() {
        val result = RouteSubtractor.subtract(listOf("0.0.0.0/0"), listOf("1.2.3.4/32"))
        assertEquals(32, result.size)
        assertEquals("0.0.0.0/8", result.first())
        assertTrue("1.2.3.5/32" in result)
        assertEquals("128.0.0.0/1", result.last())
    }

    @Test
    fun excludedIpsAreParsedAndKeptOutOfNativeConfig() {
        val config =
            Config.parseQuickString(
                """
                [Interface]
                Address = 192.0.2.2/32
                PrivateKey = TFlmmEUC7V7VtiDYLKsbP5rySTKLIZq1yn8lMqK83wo=
                [Peer]
                PublicKey = vBN7qyUTb5lJtWYJ8LhbPio1Z4RcyBPGnqFBGn6O6Qg=
                AllowedIPs = 0.0.0.0/0, ::/0
                ExcludedIPs = 77.88.8.0/24, 95.213.0.0/16
                ExcludedIPs = 5.255.252.0/22
                Endpoint = 192.0.2.1:51820
                """
                    .trimIndent()
            )
        config.validate()
        assertEquals("77.88.8.0/24, 95.213.0.0/16, 5.255.252.0/22", config.peers[0].excludedIPs)
        assertTrue("ExcludedIPs" !in config.asQuickString())
        assertTrue("ExcludedIPs = 77.88.8.0/24" in config.asQuickString(config.run { com.wgtunnel.parser.ConfigQuickInclude(excludedIps = true) }))
    }
}
