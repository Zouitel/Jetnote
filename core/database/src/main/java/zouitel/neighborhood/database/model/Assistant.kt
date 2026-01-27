package zouitel.neighborhood.database.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity("assistant_table", [Index("id"), Index("currentModelUid")])
data class Assistant(
    @PrimaryKey @ColumnInfo(name = "id", defaultValue = "1")
    val id: Short,
    @ColumnInfo(name = "currentModelUid", defaultValue = "null")
    val currentModelUid: String?,
    @ColumnInfo(name = "name", defaultValue = "assistant")
    val name: String,
    @ColumnInfo(name = "useAssistantAvatar", defaultValue = "0")
    val useAssistantAvatar: Boolean,
    @ColumnInfo(name = "systemPrompt", defaultValue = "{{char}}")
    val systemPrompt: String,
    @ColumnInfo(name = "temperature", defaultValue = "null")
    val temperature: Float?,
    @ColumnInfo(name = "topP", defaultValue = "null")
    val topP: Float?,
    @ColumnInfo(name = "contextMessageSize", defaultValue = "64")
    val contextMessageSize: Int,
    @ColumnInfo(name = "streamOutput", defaultValue = "1")
    val streamOutput: Boolean,
    @ColumnInfo(name = "enableMemory", defaultValue = "0")
    val enableMemory: Boolean,
    @ColumnInfo(name = "enableRecentChatsReference", defaultValue = "0")
    val enableRecentChatsReference: Boolean,
    @ColumnInfo(name = "messageTemplate", defaultValue = "{{ message }}")
    val messageTemplate: String = "{{ message }}",
    @ColumnInfo("presetMessages", defaultValue = "[]")
    val presetMessages: String,
    @ColumnInfo("quickMessages", defaultValue = "[]")
    val quickMessages: String,
    @ColumnInfo("regexes", defaultValue = "[]")
    val regexes: String,
    @ColumnInfo(name = "thinkingBudget", defaultValue = "null")
    val thinkingBudget: Int?,
    @ColumnInfo(name = "maxTokens", defaultValue = "null")
    val maxTokens: Int?,
    @ColumnInfo("customHeaders", defaultValue = "[]")
    val customHeaders: String,
    @ColumnInfo("customBodies", defaultValue = "[]")
    val customBodies: String,
    @ColumnInfo("mcpServers", defaultValue = "[]")
    val mcpServers: String,
    @ColumnInfo("localTools", defaultValue = "[]")
    val localTools: String,
    @ColumnInfo(name = "background", defaultValue = "null")
    val background: String?,
    @ColumnInfo(name = "learningMode", defaultValue = "0")
    val learningMode: Boolean
)


