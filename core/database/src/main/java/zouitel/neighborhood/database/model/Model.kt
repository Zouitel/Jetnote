package zouitel.neighborhood.database.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity("models_table", indices = [Index(value = ["uid"], unique = true)])
data class Model(
    @PrimaryKey
    @ColumnInfo("uid")
    val uid: String,
    @ColumnInfo("modelId")
    val modelId: String,
    @ColumnInfo("providerId")
    val providerId: Long,
    @ColumnInfo("name")
    val name: String,
    @ColumnInfo("type", defaultValue = "1")
    val type: Short,
    @ColumnInfo("customHeaders", defaultValue = "[]")
    val customHeaders: String,
    @ColumnInfo("logoUrl")
    val logoUrl: String?,
    @ColumnInfo("customBodies", defaultValue = "[]")
    val customBodies: String,
    @ColumnInfo("inputModalities", defaultValue = "[]")
    val inputModalities: String,
    @ColumnInfo("outputModalities", defaultValue = "[]")
    val outputModalities: String,
    @ColumnInfo("abilities", defaultValue = "[]")
    val abilities: String,
    @ColumnInfo("tools", defaultValue = "[]")
    val tools: String,
    @ColumnInfo("providerOverwrite", defaultValue = "null")
    val providerOverwrite: String?
)