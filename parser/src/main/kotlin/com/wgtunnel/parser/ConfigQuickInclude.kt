package com.wgtunnel.parser

data class ConfigQuickInclude(
    val core: Boolean = true,
    val dns: Boolean = true,
    val amnezia: Boolean = true,
    val peers: Boolean = true,
    val placeholders: Boolean = false,
    // ExcludedIPs is a client-side routing hint, so the native backend never receives it by default.
    val excludedIps: Boolean = false,
) {
    companion object {
        val All = ConfigQuickInclude()

        fun global(dns: Boolean, amnezia: Boolean) =
            ConfigQuickInclude(
                core = false,
                dns = dns,
                amnezia = amnezia,
                peers = false,
                placeholders = true,
            )
    }
}
