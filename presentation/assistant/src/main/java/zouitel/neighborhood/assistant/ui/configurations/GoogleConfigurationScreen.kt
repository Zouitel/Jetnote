package zouitel.neighborhood.assistant.ui.configurations

import android.annotation.SuppressLint
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.TextUnitType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.koin.getScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import zouitel.neighborhood.ai.provider.ProviderManager
import zouitel.neighborhood.ai.provider.ProviderSetting
import zouitel.neighborhood.assistant.R
import zouitel.neighborhood.assistant.ui.ai_model.ModelsScreenModel
import zouitel.neighborhood.assistant.ui.provider.ProviderScreenModel
import zouitel.neighborhood.assistant.utils.SaveSheet
import zouitel.neighborhood.ui.CommonActivationItem
import zouitel.neighborhood.ui.CommonIcons
import zouitel.neighborhood.ui.CommonTextField
import zouitel.neighborhood.ui.webview.WebViewPageScreen
import coil.compose.AsyncImage
import com.skydoves.cloudy.cloudy
import org.koin.compose.koinInject
import zouitel.neighborhood.assistant.ui.ai_model.ModelsList
import zouitel.neighborhood.ui.rememberAutoHeightCacheLazyList

data class GoogleConfigurationScreen(private val provider: ProviderSetting.Google): Screen {
    @Composable
    override fun Content() {
        Configuration(getScreenModel(), getScreenModel())
    }

    @SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
    @Composable
    private fun Configuration(
        modelsModel: ModelsScreenModel,
        providerModel: ProviderScreenModel
    ) {
        val navigator = LocalNavigator.current
        val uriHandler = LocalUriHandler.current
        val providerManager = koinInject<ProviderManager>()
        var currentProvider by remember {
            mutableStateOf(provider)
        }
        var placeholder by remember { mutableStateOf(
            runCatching {
                provider.apiKey.replaceRange(3, provider.apiKey.length - 4, "************")
            }.getOrElse { "key.." }
        ) }
        var showSaveSheet by remember { mutableStateOf(false) }
        val hasUnsavedChanges = remember(currentProvider) {
            currentProvider != provider || currentProvider.apiKey != provider.apiKey
        }
        val topAppBarState = rememberTopAppBarState()
        val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior(topAppBarState)
        val (lazyListState, listHeight) = rememberAutoHeightCacheLazyList()

        LaunchedEffect(Unit) {
            providerModel.upsert(provider)
        }

        BackHandler(enabled = hasUnsavedChanges) {
            showSaveSheet = true
        }

        if (showSaveSheet) {
            SaveSheet(
                onSave = {
                    providerModel.upsert(currentProvider)
                    navigator?.pop()
                },
                onDiscard = {
                    navigator?.pop()
                },
                onClose = {
                    showSaveSheet = false
                }
            )
        }

        Scaffold(
            topBar = {
                TopAppBar(
                    {
                        Text("Provider Settings")
                    },
                    navigationIcon = {
                        IconButton({
                            if (hasUnsavedChanges) {
                                showSaveSheet = true
                            } else {
                                navigator?.pop()
                            }
                        }) {
                            Icon(painterResource(CommonIcons.ARROW_LEFT), "go back")
                        }
                    },
                    actions = {
                        IconButton({
                            currentProvider = provider
                        }) {
                            Icon(painterResource(CommonIcons.REFRESH), null)
                        }
                    },
                    scrollBehavior = scrollBehavior
                )
            },
            modifier = Modifier
                .fillMaxSize()
                .nestedScroll(scrollBehavior.nestedScrollConnection)
                .cloudy(enabled = showSaveSheet)
        ) { contentPadding ->
            LazyColumn(
                Modifier.fillMaxSize().then(listHeight),
                lazyListState,
                contentPadding = contentPadding
            ) {
                item { Spacer(Modifier.padding(vertical = 10.dp)) }
                item {
                    CommonActivationItem(
                        currentProvider.name,
                        currentProvider.description,
                        {
                            AsyncImage(
                                currentProvider.logoUrl,
                                null,
                                modifier = Modifier.size(32.dp)
                            )
                        },
                        null
                    )
                }
                item { HorizontalDivider(Modifier.padding(10.dp, 20.dp)) }
                item {
                    CommonActivationItem(
                        "Vertex AI",
                        "",
                        {
                            Image(
                                painterResource(R.drawable.vertex_logo),
                                null,
                                modifier = Modifier.size(32.dp)
                            )
                        },
                        currentProvider.vertexAI,
                        { currentProvider = currentProvider.copy(vertexAI = it) }
                    )
                    Column(Modifier.cloudy(enabled = currentProvider.vertexAI.not())) {
                        CommonTextField(
                            currentProvider.serviceAccountEmail,
                            { currentProvider = currentProvider.copy(serviceAccountEmail = it) },
                            Modifier.fillMaxWidth().padding(vertical = 10.dp),
                            "Service account @email.com",
                            currentProvider.vertexAI
                        )
                        CommonTextField(
                            currentProvider.privateKey,
                            { currentProvider = currentProvider.copy(privateKey = it) },
                            Modifier.fillMaxWidth().padding(vertical = 10.dp),
                            "Private key..",
                            currentProvider.vertexAI
                        )
                        CommonTextField(
                            currentProvider.location,
                            { currentProvider = currentProvider.copy(location = it) },
                            Modifier.fillMaxWidth().padding(vertical = 10.dp),
                            "Location..",
                            currentProvider.vertexAI
                        )
                        CommonTextField(
                            currentProvider.projectId,
                            { currentProvider = currentProvider.copy(projectId = it) },
                            Modifier.fillMaxWidth().padding(vertical = 10.dp),
                            "Project UID..",
                            currentProvider.vertexAI
                        )
                    }
                }
                item { HorizontalDivider(Modifier.padding(10.dp, 20.dp)) }
                item {
                    Column(Modifier.cloudy(enabled = currentProvider.vertexAI)) {
                        Text(
                            "Api key",
                            Modifier.padding(horizontal = 8.dp),
                            fontSize = 19.sp,
                            fontWeight = FontWeight.Bold
                        )
                        OutlinedTextField(
                            value = if (currentProvider.apiKey == provider.apiKey) "" else currentProvider.apiKey,
                            onValueChange = { currentProvider = currentProvider.copy(apiKey = it) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            enabled = currentProvider.vertexAI.not(),
                            placeholder = { Text(placeholder, fontSize = 19.sp) },
                            trailingIcon = {
                                IconButton({
                                    runCatching {
                                        val link = currentProvider.apiLink.ifEmpty { "https://www.google.com" }
                                        uriHandler.openUri(link)
                                    }.getOrElse {
                                        navigator?.push(WebViewPageScreen(currentProvider.apiLink.ifEmpty { "" }))
                                    }
                                }) {
                                    Icon(painterResource(CommonIcons.INFO), null)
                                }
                            },
                            textStyle = TextStyle(
                                fontSize = 19.sp
                            ),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedPlaceholderColor = Color.Gray,
                                unfocusedPlaceholderColor = Color.Gray,
                                focusedBorderColor = Color.Transparent,
                                unfocusedBorderColor = Color.Transparent,
                                disabledBorderColor = Color.Transparent,
                                focusedTrailingIconColor = Color.Gray,
                                unfocusedTrailingIconColor = Color.Gray
                            )
                        )
                        Spacer(Modifier.padding(vertical = 8.dp))
                        Text(
                            "Base URL",
                            Modifier.padding(horizontal = 8.dp),
                            fontSize = 19.sp,
                            fontWeight = FontWeight.Bold
                        )
                        CommonTextField(
                            currentProvider.baseUrl,
                            { currentProvider = currentProvider.copy(baseUrl = it) },
                            Modifier.fillMaxWidth().padding(vertical = 8.dp),
                            "Url..",
                            currentProvider.vertexAI.not()
                        )
                    }
                }
                item { HorizontalDivider(Modifier.padding(10.dp, 20.dp)) }
                item {
                    CommonActivationItem(
                        "Network proxy",
                        "",
                        null,
                        currentProvider.proxy.enabled,
                        {
                            currentProvider =
                                currentProvider.copy(proxy = currentProvider.proxy.copy(enabled = it))
                        },
                    )
                    Column(Modifier.cloudy(enabled = currentProvider.proxy.enabled.not())) {
                        CommonTextField(
                            currentProvider.proxy.address ?: "",
                            {
                                currentProvider = currentProvider.copy(
                                    proxy = currentProvider.proxy.copy(address = it)
                                )
                            },
                            Modifier.fillMaxWidth().padding(vertical = 10.dp),
                            "Host..",
                            currentProvider.proxy.enabled
                        )

                        BasicTextField(
                            currentProvider.proxy.port.toString(),
                            {
                                currentProvider =
                                    currentProvider.copy(proxy = currentProvider.proxy.copy(port = it.toInt()))
                            },
                            modifier = Modifier
                                .padding(7.dp)
                                .background(Color.Transparent),
                            enabled = currentProvider.proxy.enabled,
                            textStyle = TextStyle(
                                fontSize =  TextUnit(19f, TextUnitType.Sp),
                                fontWeight = FontWeight.Normal,
                                fontFamily = FontFamily.Default,
                                color = Color.LightGray
                            ),
                            keyboardOptions = KeyboardOptions(
                                capitalization = KeyboardCapitalization.Sentences,
                                autoCorrectEnabled = false,
                                keyboardType = KeyboardType.Number,
                                imeAction = ImeAction.Default
                            )
                        )

                        CommonTextField(
                            currentProvider.proxy.username ?: "",
                            {
                                currentProvider = currentProvider.copy(
                                    proxy = currentProvider.proxy.copy(username = it)
                                )
                            },
                            Modifier.fillMaxWidth().padding(vertical = 10.dp),
                            "Username (Optional)..",
                            currentProvider.proxy.enabled
                        )

                        CommonTextField(
                            currentProvider.proxy.password ?: "",
                            {
                                currentProvider = currentProvider.copy(
                                    proxy = currentProvider.proxy.copy(password = it)
                                )
                            },
                            Modifier.fillMaxWidth().padding(vertical = 10.dp),
                            "Password (Optional)..",
                            currentProvider.proxy.enabled
                        )
                    }
                }
                item { HorizontalDivider(Modifier.padding(10.dp, 20.dp)) }
                item(currentProvider.apiKey) {
                    ModelsList(
                        provider = currentProvider,
                        modelsModel = modelsModel,
                        providerManager = providerManager
                    )
                }
            }
        }
    }
}