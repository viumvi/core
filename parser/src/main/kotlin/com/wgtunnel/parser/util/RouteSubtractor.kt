package com.wgtunnel.parser.util

import inet.ipaddr.IPAddressString
import java.math.BigInteger
import java.net.InetAddress

/**
 * Computes `included - excluded` for IPv4/IPv6 CIDR lists. Used on Android versions without
 * `VpnService.Builder.excludeRoute()` (API < 33), where the only way to leave a network out of the
 * tunnel is to not route it.
 */
object RouteSubtractor {

    private class Range(val bits: Int, val start: BigInteger, val end: BigInteger)

    fun subtract(included: List<String>, excluded: List<String>): List<String> {
        val blocked =
            excluded.map(::toRange).groupBy { it.bits }.mapValues { (_, ranges) -> merge(ranges) }

        val result = mutableListOf<String>()
        for (network in included) {
            val range = toRange(network)
            val blockedRanges = blocked[range.bits].orEmpty()
            var cursor = range.start
            for (i in firstOverlap(blockedRanges, range.start) until blockedRanges.size) {
                val block = blockedRanges[i]
                if (block.start > range.end) break
                if (block.start > cursor) toCidrs(cursor, block.start - BigInteger.ONE, range.bits, result)
                cursor = cursor.max(block.end + BigInteger.ONE)
            }
            if (cursor <= range.end) toCidrs(cursor, range.end, range.bits, result)
        }
        return result
    }

    private fun toRange(cidr: String): Range {
        val address =
            IPAddressString(cidr.trim()).address ?: throw IllegalArgumentException("Invalid CIDR: $cidr")
        val bits = if (address.isIPv4) 32 else 128
        val prefix = address.networkPrefixLength ?: bits
        val size = BigInteger.ONE.shiftLeft(bits - prefix)
        val start = BigInteger(1, address.bytes).andNot(size - BigInteger.ONE)
        return Range(bits, start, start + size - BigInteger.ONE)
    }

    private fun merge(ranges: List<Range>): List<Range> {
        val merged = mutableListOf<Range>()
        for (range in ranges.sortedBy { it.start }) {
            val last = merged.lastOrNull()
            if (last != null && range.start <= last.end + BigInteger.ONE) {
                if (range.end > last.end) merged[merged.lastIndex] = Range(last.bits, last.start, range.end)
            } else {
                merged.add(range)
            }
        }
        return merged
    }

    // Index of the first merged block whose end is not before `start`.
    private fun firstOverlap(blocked: List<Range>, start: BigInteger): Int {
        var low = 0
        var high = blocked.size
        while (low < high) {
            val mid = (low + high) ushr 1
            if (blocked[mid].end < start) low = mid + 1 else high = mid
        }
        return low
    }

    // Splits [start, end] into the smallest list of aligned CIDR blocks.
    private fun toCidrs(from: BigInteger, end: BigInteger, bits: Int, out: MutableList<String>) {
        var start = from
        while (start <= end) {
            val remaining = end - start + BigInteger.ONE
            var sizeLog = remaining.bitLength() - 1
            if (start.signum() != 0) sizeLog = minOf(sizeLog, start.lowestSetBit)
            out.add("${toAddress(start, bits)}/${bits - sizeLog}")
            start += BigInteger.ONE.shiftLeft(sizeLog)
        }
    }

    private fun toAddress(value: BigInteger, bits: Int): String {
        val raw = value.toByteArray()
        val bytes = ByteArray(bits / 8)
        val copy = minOf(raw.size, bytes.size)
        System.arraycopy(raw, raw.size - copy, bytes, bytes.size - copy, copy)
        return InetAddress.getByAddress(bytes).hostAddress
    }
}
