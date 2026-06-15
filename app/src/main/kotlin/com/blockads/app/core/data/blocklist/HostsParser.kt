package com.blockads.app.core.data.blocklist

private val DOMAIN_REGEX = Regex("^[a-z0-9]([a-z0-9._-]{0,251}[a-z0-9])?$")
private val PRINTABLE_REGEX = Regex("^[ -~]+$")

object HostsParser {
    fun parseLines(lines: Sequence<String>): Sequence<String> =
        sequence {
            for (raw in lines) {
                val line = raw.trim()
                if (line.isEmpty() || line.startsWith('#')) continue
                if (!PRINTABLE_REGEX.matches(line)) continue

                val commentStripped = line.substringBefore('#').trim()
                val parts = commentStripped.split(Regex("\\s+"))

                // Hosts file format: "0.0.0.0 example.com" or just "example.com"
                val domain =
                    when {
                        parts.size >= 2 -> parts[1].lowercase()
                        parts.size == 1 -> parts[0].lowercase()
                        else -> continue
                    }

                // Reject localhost and similar
                if (domain == "localhost" || domain == "broadcasthost" ||
                    domain == "local" || domain.endsWith(".local")
                ) {
                    continue
                }

                if (DOMAIN_REGEX.matches(domain)) {
                    yield(domain)
                }
            }
        }
}
