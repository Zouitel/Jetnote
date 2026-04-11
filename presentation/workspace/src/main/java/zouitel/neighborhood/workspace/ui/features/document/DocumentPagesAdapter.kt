package zouitel.neighborhood.workspace.ui.features.document

import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * RecyclerView adapter for document pages.
 *
 * Each item is a [DocumentPageView] that renders asynchronously via
 * [DocumentViewerScreenModel.renderPage]. When a page becomes visible the
 * adapter also calls [DocumentViewerScreenModel.prefetchAround] to warm up
 * neighbouring pages, exactly as ONLYOFFICE's viewer does.
 */
class DocumentPagesAdapter(
    private val pageCount : Int,
    private val model     : DocumentScreenModel,
    private val scope     : CoroutineScope
) : RecyclerView.Adapter<DocumentPagesAdapter.PageViewHolder>() {

    inner class PageViewHolder(val pageView: DocumentPageView) :
        RecyclerView.ViewHolder(pageView)

    override fun getItemCount() = pageCount

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PageViewHolder {
        val view = DocumentPageView(parent.context).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        }
        return PageViewHolder(view)
    }

    override fun onBindViewHolder(holder: PageViewHolder, position: Int) {
        // Reset immediately so a recycled view doesn't flash stale content
        holder.pageView.setBitmap(null)

        // Render the page asynchronously then push the bitmap to the view
        scope.launch {
            val bmp = model.renderPage(position)
            holder.pageView.setBitmap(bmp)
        }

        // Warm up pages around the current one
        model.prefetchAround(position, pageCount)
    }

    override fun onViewRecycled(holder: PageViewHolder) {
        super.onViewRecycled(holder)
        holder.pageView.setBitmap(null)
        holder.pageView.resetZoom()
    }
}