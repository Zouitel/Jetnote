package zouitel.neighborhood.database.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity("provider_table", [Index("id")])
data class Provider(
    @PrimaryKey(true)
    @ColumnInfo("id")
    val id: Long,
    @ColumnInfo("type")
    val type: Short,
    @ColumnInfo("logo_url")
    val logoUrl: String?,
    @ColumnInfo("name")
    val name: String,
    @ColumnInfo("api_key")
    val apiKey: String,
    @ColumnInfo("api_link")
    val apiLink: String,
    @ColumnInfo("proxy")
    val proxy: String,
    @ColumnInfo("balance_option")
    val balanceOption: String,
    @ColumnInfo("built_in")
    val builtIn: Boolean,
    @ColumnInfo("description")
    val description: String,
    @ColumnInfo("base_url")
    val baseUrl: String,
    @ColumnInfo("vertex_ai")
    val vertexAI: Boolean,
    @ColumnInfo("private_key")
    val privateKey: String,
    @ColumnInfo("service_account_email")
    val serviceAccountEmail: String,
    @ColumnInfo("location")
    val location: String,
    @ColumnInfo("project_id")
    val projectId: String,
    @ColumnInfo("chat_completions_path")
    val chatCompletionsPath: String?,
    @ColumnInfo("use_response_api")
    val useResponseApi: Boolean
)
