package com.otzrlabs.payney.capture.capture

/**
 * Single source of truth for what SmsReceiver and NotificationCaptureService
 * are willing to forward to the backend. Deliberately conservative: anything
 * not matched here is discarded, so arbitrary personal SMS/notifications are
 * never sent off-device.
 */
object AllowList {

    // Indian DLT-registered SMS sender IDs are usually shown as either
    // "<2-letter-operator-prefix>-<6-char-entity-code>" (e.g. "VM-HDFCBK") or,
    // on carriers/OEMs that strip the prefix, just the bare entity code
    // (e.g. "HDFCBK"). The patterns below cover both forms for a handful of
    // major Indian banks -- expand this list as you test against your own
    // bank's actual sender ID, which varies by carrier.
    private val bankEntityCodes = listOf(
        "HDFCBK", "HDFCBN",              // HDFC Bank
        "SBIINB", "SBIPSG", "ATMSBI", "SBICRD", // State Bank of India
        "ICICIB", "ICICIT",              // ICICI Bank
        "AXISBK",                        // Axis Bank
        "KOTAKB",                        // Kotak Mahindra Bank
        "YESBNK",                        // Yes Bank
        "PNBSMS",                        // Punjab National Bank
        "JKBANK", "JAKBNK",              // Jammu and Kashmir Bank
    )

    // Some banks/carriers deliver from a full alphanumeric sender *name* instead
    // of a DLT entity code (e.g. J&K Bank shows as "The Jammu & Kashmir Bank" on
    // some circles). Matching on the exact string is fragile — punctuation ("&"
    // vs "and"), spacing and case all vary — so instead we require ALL tokens of
    // a set to appear anywhere in the (uppercased) sender. "JAMMU" + "KASHMIR"
    // uniquely identifies J&K Bank without risking false positives on personal
    // senders. Add a token set per bank as you observe real sender names.
    private val bankNameTokenSets: List<List<String>> = listOf(
        listOf("JAMMU", "KASHMIR"), // The Jammu & Kashmir Bank
    )

    val smsSenderPatterns: List<Regex> = bankEntityCodes.flatMap { code ->
        listOf(
            Regex("^[A-Z]{2}-$code$"), // e.g. "VM-HDFCBK"
            Regex("^[A-Z]{2}-$code-S$"),  // e.g. VM-HDFCBK-S only
            Regex("^$code$"),          // e.g. "HDFCBK"
        )
    }

    fun isAllowedSmsSender(sender: String): Boolean {
        // Collapse repeated whitespace so "The  Jammu" still matches, and
        // uppercase so the comparison is case-insensitive.
        val normalized = sender.trim().uppercase().replace(Regex("\\s+"), " ")
        if (bankNameTokenSets.any { tokens -> tokens.all { normalized.contains(it) } }) return true
        if (smsSenderPatterns.any { it.matches(normalized) }) return true
        // Tolerant fallback: DLT headers vary a lot by carrier/circle
        // ("VK-JAKBNK-S", "JX-JKBANK-T", "BP-HDFCBK-P"...). Strip a leading
        // operator prefix and any trailing single-letter route suffix, then
        // match the bare entity code. Still exact-match against the known code
        // list, so this doesn't open the door to arbitrary senders.
        val core = normalized
            .replace(Regex("^[A-Z]{1,3}-"), "")
            .replace(Regex("-[A-Z]$"), "")
        return core in bankEntityCodes
    }

    // Package names of UPI/payment apps whose notifications we forward.
    // Starting set -- expand as you add support for more apps.
    val upiAppPackages: Set<String> = setOf(
        "com.google.android.apps.nbu.paisa.user", // Google Pay
        "com.phonepe.app",                         // PhonePe
        "net.one97.paytm",                         // Paytm
        "com.dreamplug.androidapp",                // CRED
    )

    fun isAllowedNotificationPackage(packageName: String): Boolean = packageName in upiAppPackages
}
