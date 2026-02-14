package zouitel.neighborhood.logic

import android.content.Context
import android.content.Intent

val sharNote: (Context, String, String, then: () -> Unit) -> Unit = { context, title, description, then ->
    context.startActivity(
        Intent.createChooser(
            Intent().apply {
                action = Intent.ACTION_SEND
                putExtra(Intent.EXTRA_TEXT, "$title\n\n$description")
                type = "text/plain"
            },
            null
        )
    )
    then.invoke()
}

val sharApp: (Context, String) -> Unit = { context, txt ->
    context.startActivity(
        Intent.createChooser(
            Intent().apply {
                action = Intent.ACTION_SEND
                putExtra(Intent.EXTRA_TEXT, txt)
                type = "text/plain"
            },
            null
        )
    )
}