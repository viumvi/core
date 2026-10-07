package com.wgtunnel.parser

import kotlin.test.Test
import kotlin.test.assertEquals

class Awg31ConfigTest {

    // Every key the vpnka client can send, as produced by buildAwgConfig().
    private val config =
        """
        [Interface]
        Address=100.67.14.213/32,fd42:42:42::3:0ed5/128
        PrivateKey=eLSBy8Si/f5IZ5RC8dkPUwUggkmLZMvDLAlT4eNV9WM=
        DNS=100.64.0.1
        MTU=1280
        IncludedApplications=com.example.a,com.example.b,
        Jc=4
        Jmin=8
        Jmax=80
        S1=15
        S2=20
        S3=18
        S4=22
        H1=100000-200000
        H2=300000-400000
        H3=500000-600000
        H4=700000-800000
        I1=<b 0xf6ab3267fa><r 16><t>
        I2=<b 0x01><r 16>
        I3=<t>
        I4=<d>
        I5=<r 8>
        HeaderProtectionKey=wwToICMToDNPF/WZE8oAONcMJMn6K+s5J6Av9VvCamg=
        ContentPaddingAddition=10-20
        RekeyAfterTime=120
        RekeyTimeout=5
        RejectAfterTime=180
        KeepaliveTimeout=10
        MaxHandshakeAttempts=20
        [Peer]
        PublicKey=wwToICMToDNPF/WZE8oAONcMJMn6K+s5J6Av9VvCamg=
        AllowedIPs=0.0.0.0/0,::/0
        ExcludedIPs=77.88.8.0/24,95.213.0.0/16
        Endpoint=147.45.237.32:35429
        PersistentKeepalive=25
        """
            .trimIndent()

    @Test
    fun parsesAndValidatesAllAwg31Keys() {
        val parsed = Config.parseQuickString(config)
        parsed.validate()

        val iface = parsed.`interface`
        assertEquals(4, iface.jC)
        assertEquals(15, iface.s1)
        assertEquals(22, iface.s4)
        assertEquals("100000-200000", iface.h1)
        assertEquals("<b 0xf6ab3267fa><r 16><t>", iface.i1)
        assertEquals("wwToICMToDNPF/WZE8oAONcMJMn6K+s5J6Av9VvCamg=", iface.headerProtectionKey)
        assertEquals("10-20", iface.contentPaddingAddition)
        assertEquals("120", iface.rekeyAfterTime)
        assertEquals("20", iface.maxHandshakeAttempts)
        assertEquals(listOf("com.example.a", "com.example.b"), iface.includedApplications)

        // the native backend must receive the AWG keys but never ExcludedIPs
        val quick = parsed.asQuickString()
        assertEquals(true, "HeaderProtectionKey" in quick)
        assertEquals(false, "ExcludedIPs" in quick)
    }
}
