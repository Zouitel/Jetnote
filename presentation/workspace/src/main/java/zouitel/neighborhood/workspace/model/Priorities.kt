package zouitel.neighborhood.workspace.model

import androidx.compose.ui.graphics.Color

enum class Priorities(val index: Int, val priority: String, val color: Color) {
    NON(0, "Non", Color(0xffc0c0c0)), // lowest
    LOW(1, "Low", Color(0xff32cd32)),
    MED(2, "Medium", Color(0xffffef00)),
    HIG(3, "High", Color(0xffffa500)),
    URG(4, "Urgent", Color(0xffff2800)); // critical
}