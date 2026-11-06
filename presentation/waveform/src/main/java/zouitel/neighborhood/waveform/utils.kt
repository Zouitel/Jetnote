package zouitel.neighborhood.waveform

internal fun <T> Iterable<T>.fillToSize(
    size: Int,
    transform: (List<T>) -> T
): List<T> {
    if (size <= 0) return emptyList()
    val values = toList()
    if (values.isEmpty()) return emptyList()
    if (values.size >= size) return values.chunkToSize(size, transform)

    val repetitions = (size + values.size - 1) / values.size
    val expanded = values.flatMap { value -> List(repetitions) { value } }
    return expanded.chunkToSize(size, transform)
}

internal fun <T> Iterable<T>.chunkToSize(
    size: Int,
    transform: (List<T>) -> T
): List<T> {
    if (size <= 0) return emptyList()
    val values = toList()
    if (values.isEmpty()) return emptyList()
    if (size >= values.size) {
        return if (size == values.size) {
            values.map { transform(listOf(it)) }
        } else {
            values.fillToSize(size, transform)
        }
    }

    return List(size) { bucketIndex ->
        val start = bucketIndex * values.size / size
        val end = (bucketIndex + 1) * values.size / size
        transform(values.subList(start, end))
    }
}

internal fun Iterable<Float>.normalize(min: Float, max: Float): List<Float> {
    val values = toList()
    if (values.isEmpty()) return emptyList()
    val sourceMin = values.minOrNull() ?: return emptyList()
    val sourceMax = values.maxOrNull() ?: return emptyList()
    if (sourceMax == sourceMin) return List(values.size) { min }

    val range = max - min
    return values.map { value ->
        min + ((value - sourceMin) / (sourceMax - sourceMin)) * range
    }
}
