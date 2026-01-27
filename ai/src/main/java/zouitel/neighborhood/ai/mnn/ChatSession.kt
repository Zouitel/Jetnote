package zouitel.neighborhood.ai.mnn

import zouitel.neighborhood.ai.mnn.model.ChatDataItem


interface ChatSession  {
    val debugInfo: String
    val sessionId: String?

    val supportOmni: Boolean
    fun load()

    fun generate(prompt: String, params: Map<String, Any>, progressListener: GenerateProgressListener): HashMap<String, Any>

    fun reset(): String

    fun release()
    fun setKeepHistory(keepHistory: Boolean)
    fun setEnableAudioOutput(enable: Boolean)
    fun getHistory(): List<ChatDataItem>?
    fun setHistory(history:List<ChatDataItem>?)
}
