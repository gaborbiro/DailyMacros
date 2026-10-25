package dev.gaborbiro.dailymacros.features.shared.healthconnect

/** Whether Health Connect can be used on this device, from [GetHealthConnectStatusUseCase]. */
enum class HealthConnectStatus {
    AVAILABLE,

    /** Health Connect is missing or outdated; installing or updating it from the Play Store fixes it. */
    INSTALL_OR_UPDATE_REQUIRED,

    /** This device can't run Health Connect at all. */
    NOT_SUPPORTED,
}
