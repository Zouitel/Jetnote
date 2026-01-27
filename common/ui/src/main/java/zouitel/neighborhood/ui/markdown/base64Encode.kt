package zouitel.neighborhood.ui.markdown

import kotlin.io.encoding.Base64

fun String.base64Encode() = Base64.encode(this.toByteArray())