package zouitel.neighborhood.widget.ui

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import zouitel.neighborhood.ui.CommonIcons
import zouitel.neighborhood.widget.model.WidgetComponent
import zouitel.neighborhood.widget.receivers.CreatorReceiver
import zouitel.neighborhood.widget.receivers.ListReceiver
import zouitel.neighborhood.widget.receivers.QuickReceiver
import coil.compose.AsyncImage
import zouitel.neighborhood.widget.R

class WidgetScreen: Screen {
    @Composable
    override fun Content() {
        val context = LocalContext.current
        val navigator = LocalNavigator.current
        val activity = context as Activity
        val appWidgetManager = AppWidgetManager.getInstance(context)
        val topAppBarState = rememberTopAppBarState()
        val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior(topAppBarState)

        val widgets = listOf(
            WidgetComponent(
                "Quick Note Widget",
                ComponentName(context, QuickReceiver::class.java),
                R.drawable.plus
            ),
            WidgetComponent(
                "List Widget",
                ComponentName(context, ListReceiver::class.java),
                R.drawable.notes_list
            ),
//            WidgetComponent(
//                "Creator Widget",
//                ComponentName(context, CreatorReceiver::class.java),
//                R.drawable.creator
//            )
        )

        Scaffold(
            modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            text = "Widgets",
                            style = MaterialTheme.typography.titleLarge,
                            modifier = Modifier.padding(start = 15.dp)
                        )
                    },
                    navigationIcon = {
                        IconButton({
                            navigator?.pop()
                        }) {
                            Icon(painterResource(CommonIcons.ARROW_LEFT), null)
                        }
                    }
                )
            },
        ) { paddingValues ->
            LazyColumn(contentPadding = paddingValues) {
                items(widgets) {
                    AsyncImage(
                        model = it.resource,
                        contentDescription = it.name,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(50.dp)
                            .padding(20.dp)
                            .clickable {
                                if (true && appWidgetManager.isRequestPinAppWidgetSupported) {
                                    val success = appWidgetManager.requestPinAppWidget(
                                        it.componentName,
                                        null,
                                        null
                                    )
                                    if (success) {
                                        activity.setResult(Activity.RESULT_OK)
                                        activity.finish()
                                    }
                                } else {
                                    activity.setResult(Activity.RESULT_CANCELED)
                                }

                            },
                        contentScale = ContentScale.Inside
                    )
                    HorizontalDivider(
                        modifier = Modifier.padding(10.dp),
                        thickness = 4.dp
                    )
                }
            }
        }
    }
}