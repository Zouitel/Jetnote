package zouitel.neighborhood.ui

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.*
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.lazy.layout.LazyLayoutCacheWindow
import androidx.compose.foundation.lazy.staggeredgrid.LazyStaggeredGridState
import androidx.compose.foundation.lazy.staggeredgrid.rememberLazyStaggeredGridState
import androidx.compose.ui.Modifier

@Composable
private fun Modifier.captureHeightForCache(heightState: MutableState<Dp>): Modifier {
    val density = LocalDensity.current

    return this.onSizeChanged { size ->
        heightState.value = with(density) { size.height.toDp() }
    }
}

@Composable
private fun rememberDynamicCacheLazyListState(heightState: State<Dp>): LazyListState {
    val cacheWindow = remember(heightState.value) {
        LazyLayoutCacheWindow(
            ahead = heightState.value,
            behind = heightState.value
        )
    }

    return rememberLazyListState(cacheWindow)
}

@Composable
fun rememberAutoHeightCacheLazyList(): Pair<LazyListState, Modifier> {
    val heightState = remember { mutableStateOf(0.dp) }
    val lazyListState = rememberDynamicCacheLazyListState(heightState)
    val heightModifier = Modifier.captureHeightForCache(heightState)

    return Pair(lazyListState, heightModifier)
}

@Composable
fun rememberAutoHeightCacheStaggeredGrid(
    itemsPerScreen: Int = 10
): Pair<LazyStaggeredGridState, Modifier> {
    val density = LocalDensity.current
    val heightState = remember { mutableStateOf(0.dp) }

    val beyondBoundsCount = remember(heightState.value, itemsPerScreen) {
        itemsPerScreen
    }

    val gridState = rememberLazyStaggeredGridState(
         beyondBoundsCount
    )

    val heightModifier = Modifier.onSizeChanged { size ->
        heightState.value = with(density) { size.height.toDp() }
    }

    return Pair(gridState, heightModifier)
}