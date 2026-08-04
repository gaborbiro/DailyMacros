package dev.gaborbiro.dailymacros.repositories.billing.domain

/** Must match the Product ID configured in Play Console exactly. */
const val SUBSCRIPTION_PRODUCT_ID = "subscription_basic"

/** Base plan ID within [SUBSCRIPTION_PRODUCT_ID], for picking the right offer. */
const val SUBSCRIPTION_BASE_PLAN_ID = "monthly"

/** Offer ID (the free-trial offer) within the base plan above. */
const val SUBSCRIPTION_OFFER_ID = "trial"
