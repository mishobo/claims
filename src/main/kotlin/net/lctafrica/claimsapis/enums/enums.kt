package net.lctafrica.claimsapis.enums

enum class BenefitType(val type: String) {
    INSURED("Insured"),
    CAPITATION("Capitation"),
    FUNDED("Funded")
}

enum class CapitationType(val type: String) {
    FIXED("Fixed"),
    REIMBURSEMENT("Reimbursement")
}

enum class CapitationPeriod(val type: String) {
    MONTHLY("Monthly"),
    QUARTERLY("Quarterly"),
    SEMI_ANNUAL("Semi Annual"),
    ANNUAL("Annual"),
}