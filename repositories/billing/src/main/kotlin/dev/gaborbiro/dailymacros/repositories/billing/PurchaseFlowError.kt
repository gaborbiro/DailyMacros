package dev.gaborbiro.dailymacros.repositories.billing

/** Why Play's purchase sheet couldn't be opened. Developer-facing detail, carried as the cause of a DomainError. */
internal class PurchaseFlowError(message: String) : Exception(message)
