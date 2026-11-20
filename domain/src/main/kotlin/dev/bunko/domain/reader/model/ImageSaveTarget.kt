package dev.bunko.domain.reader.model

import kotlinx.serialization.Serializable

@Serializable
enum class ImageSaveTarget {
    Ask,
    Phone,
    Server,
}
