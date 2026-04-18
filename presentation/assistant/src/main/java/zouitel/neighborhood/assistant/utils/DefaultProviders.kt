package zouitel.neighborhood.assistant.utils

import zouitel.neighborhood.ai.provider.BalanceOption
import zouitel.neighborhood.ai.provider.ProviderSetting

val DEFAULT_PROVIDERS = listOf(
    // LLM, Multimodal (Gemini)
    ProviderSetting.Google(
        name = "Google Gemini",
        logoUrl = "https://registry.npmmirror.com/@lobehub/icons-static-png/latest/files/dark/gemini-color.png",
        builtIn = true,
        description = """
            Provides the Gemini family of models for
            highly capable, multimodal AI that
            understands and generates content across
            text, images, and audio.
        """.trimIndent()
    ),
    // LLM (Claude)
    ProviderSetting.Claude(
        name = "Anthropic Claude",
        logoUrl = "https://registry.npmmirror.com/@lobehub/icons-static-png/latest/files/dark/claude-color.png",
        description = """
            Specializes in safe, constitutional AI
            providing the Claude series of powerful
            large language models known for
            long-context understanding and reliability.
        """.trimIndent()
    ),
    // LLM, Image (GPT-4, DALL-E)
    ProviderSetting.GlobalAI(
        id = 3,
        name = "OpenAI GPT",
        baseUrl = "https://api.openai.com/v1",
        logoUrl = "https://registry.npmmirror.com/@lobehub/icons-static-png/latest/files/dark/openai.png",
        apiLink = "https://platform.openai.com/api-keys",
        builtIn = true,
        description = """
            Offers access to cutting-edge language
            models (GPT series) for complex
            reasoning and human-like text, plus DALL-E
            for image generation.
        """.trimIndent()
    ),
    //Deepseek
    ProviderSetting.GlobalAI(
        id = 4,
        name = "Deepseek",
        logoUrl = "https://registry.npmmirror.com/@lobehub/icons-static-png/latest/files/dark/deepseek-color.png",
        baseUrl = "https://api.deepseek.com/v1",
        apiLink = "https://platform.deepseek.com/api_keys",
        builtIn = true,
        description = """
            Offers high-performance, open-source
            models (DeepSeek-Coder/DeepSeek-MoE)
            specialized for code generation,
            competitive benchmarking, and general
            reasoning.
        """.trimIndent(),
        balanceOption = BalanceOption(
            enabled = true,
            apiPath = "/user/balance",
            resultPath = "balance_infos[0].total_balance"
        )
    ),
    //Grok
    ProviderSetting.GlobalAI(
        id = 5,
        name = "xAI Grok",
        logoUrl = "https://registry.npmmirror.com/@lobehub/icons-static-png/latest/files/dark/grok.png",
        baseUrl = "",
        apiLink = "https://api.x.ai/v1",
        builtIn = true,
        description = """
            The large language model developed by
            xAI, known for its distinct, witty personality
            and its unique ability to access and
            leverage real-time data from the X
            platform.
        """.trimIndent()
    ),
    //
    ProviderSetting.GlobalAI(
        id = 6,
        name = "Silicon Cloud",
        logoUrl = "https://registry.npmmirror.com/@lobehub/icons-static-png/latest/files/dark/siliconcloud-color.png",
        baseUrl = "",
        apiLink = "https://api.siliconflow.cn/v1",
        builtIn = true,
        description = """
            General cloud providers specializing in
            offering large-scale computational
            infrastructure and hosting for various LLM
            models, often serving the Chinese market.
        """.trimIndent(),
        balanceOption = BalanceOption(
            enabled = true,
            apiPath = "/user/info",
            resultPath = "data.totalBalance",
        ),
    ),
    //
    ProviderSetting.GlobalAI(
        id = 7,
        name = "Moonshot AI Kimi",
        logoUrl = "https://registry.npmmirror.com/@lobehub/icons-static-png/latest/files/dark/kimi-color.png",
        baseUrl = "https://api.moonshot.cn/v1",
        apiLink = "https://platform.moonshot.ai/console/api-keys",
        builtIn = true,
        description = """
            Known for its extremely long context
            window capacity, making its models highly
            effective for processing massive
            documents, analyzing complex reports,
            and summarization.
        """.trimIndent(),
        balanceOption = BalanceOption(
            enabled = true,
            apiPath = "/users/me/balance",
            resultPath = "data.available_balance"
        )
    ),
    //
    ProviderSetting.GlobalAI(
        id = 8,
        name = "Alibaba Cloud Qwen",
        logoUrl = "https://registry.npmmirror.com/@lobehub/icons-static-png/latest/files/dark/qwen-color.png",
        baseUrl = "https://dashscope.aliyuncs.com/compatible-mode/v1",
        apiLink = "https://account.aliyun.com/login/login.htm?oauth_callback=https%3A%2F%2Fdashscope.console.aliyun.com%2FapiKey&clearRedirectCookie=1&lang=zh",
        builtIn = true,
        description = """
            Alibaba's large-scale LLM, Tongyi Qianwen
            (Qwen), offering robust performance
            across text, image, and audio generation
            via its cloud platform Dashscope.
        """.trimIndent()
    ),
    //
    ProviderSetting.GlobalAI(
        id = 9,
        name = "Zhipu AI",
        logoUrl = "https://registry.npmmirror.com/@lobehub/icons-static-png/latest/files/dark/zhipu-color.png",
        baseUrl = "https://open.bigmodel.cn/api/paas/v4",
        apiLink = "https://open.bigmodel.cn/login?redirect=%2Fusercenter%2Fproj-mgmt%2Fapikeys",
        builtIn = true,
        description = """
            Chinese LLM provider known for its GLM
            series, strong in function calling,
            reasoning, and code generation, often used
            for sophisticated enterprise applications.
        """.trimIndent()
    ),
    //
    ProviderSetting.GlobalAI(
        id = 10,
        name = "ByteDance",
        logoUrl = "https://registry.npmmirror.com/@lobehub/icons-static-png/latest/files/dark/bytedance-color.png",
        apiLink = "https://console.volcengine.com/ark/region:ark+cn-beijing/apiKey",
        baseUrl = "https://ark.cn-beijing.volces.com/api/v3",
        builtIn = true,
        description = """
            Parent company of TikTok, providing
            various AI services, including its Doubao
            LLM and advanced media generation tools,
            primarily through its enterprise cloud
            platform.
        """.trimIndent()
    ),
    //
    ProviderSetting.GlobalAI(
        id = 11,
        name = "OpenRouter",
        logoUrl = "https://registry.npmmirror.com/@lobehub/icons-static-png/latest/files/dark/openrouter.png",
        baseUrl = "https://openrouter.ai/api/v1",
        apiLink = "https://openrouter.ai/settings/keys",
        builtIn = true,
        description = """
            It is a unified API gateway (often called an
            LLM aggregator) that allows you to access
            hundreds of different AI models from
            multiple providers through a single point of
            entry.
        """.trimIndent(),
        balanceOption = BalanceOption(
            enabled = true,
            apiPath = "/credits",
            resultPath = "data.total_credits - data.total_usage",
        )
    ),
    //
    ProviderSetting.GlobalAI(
        id = 12,
        name = "AiHubMix",
        logoUrl = "https://registry.npmmirror.com/@lobehub/icons-static-png/latest/files/dark/aihubmix-color.png",
        baseUrl = "https://aihubmix.com/v1",
        apiLink = "https://console.aihubmix.com/token",
        builtIn = true,
        description = """
            AIHubMix is a high-performance API
            Aggregator and Router designed for
            developers who want to access multiple AI
            models (OpenAI, Claude, Gemini,
            DeepSeek, Llama, etc.) through a single,
            unified interface.
        """.trimIndent()
    ),
    // Groq.
    ProviderSetting.GlobalAI(
        id = 13,
        name = "Groq",
        logoUrl = "https://raw.githubusercontent.com/lobehub/lobe-icons/refs/heads/master/packages/static-png/dark/groq.png",
        baseUrl = "https://api.groq.com/openai/v1",
        apiLink = "https://console.groq.com/keys",
        builtIn = true,
        description = """
            Groq is the "Speed King" of the AI world.
            While most companies (like OpenAI or
            Google) rely on standard GPUs (Graphics
            Processing Units), Groq built its own
            custom hardware called the LPU
            (Language Processing Unit).
        """.trimIndent()
    ),
    // Nvidia.
    ProviderSetting.GlobalAI(
        id = 14,
        name = "Nvidia",
        logoUrl = "https://registry.npmmirror.com/@lobehub/icons-static-png/latest/files/dark/nvidia-color.png",
        baseUrl = "https://integrate.api.nvidia.com/v1",
        apiLink = "https://build.nvidia.com/explore/discover",
        builtIn = true,
        description = """
            Provides cloud APIs to run optimized
            versions of various third-party and
            open-source models, leveraging their
            powerful GPU hardware for high
            performance.
        """.trimIndent()
    ),
    // Mistral.
    ProviderSetting.GlobalAI(
        id = 15,
        name = "Mistral",
        logoUrl = "https://registry.npmmirror.com/@lobehub/icons-static-png/latest/files/dark/mistral-color.png",
        baseUrl = "https://api.mistral.ai/v1",
        apiLink = "https://console.mistral.ai/home",
        builtIn = true,
        description = """
            Mistral AI is a French artificial intelligence
            company that has quickly become a
            leading European challenger in the LLM space.
            Founded by former researchers from Meta and Google DeepMind, they are best known for their focus on efficiency, open-weight models, and high performance relative to model size.
        """.trimIndent()
    ),
    // Github.
//    ProviderSetting.GlobalAI(
//        id = 16,
//        name = "Github",
//        baseUrl = "https://models.github.ai/inference",
//        apiLink = "",
//        apiKey = "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",
//        description = """
//            ...
//        """.trimIndent()
//    ),
    // Image, LLM (Stable Diffusion)
//    ProviderSetting.GlobalAI(
//        id = 6,
//        name = "Stability AI",
//        baseUrl = "https://api.stability.ai/v1/",
//        description = """
//            The creator of the popular Stable Diffusion models, providing APIs for fast, high-quality, open-source-backed image and soon, video generation.
//        """.trimIndent()
//    ),
//    // Community/Platform, LLM
//    ProviderSetting.GlobalAI(
//        id = 7,
//        name = "Hugging Face",
//        baseUrl = "https://api-inference.huggingface.co/models/{model-id}",
//        description = """
//            Acts as a central hub for thousands of open-source and commercial models; their API allows developers to run inference on almost any hosted LLM or model instantly.
//        """.trimIndent()
//    ),
//    // LLM (Enterprise Focus)
//    ProviderSetting.GlobalAI(
//        id = 9,
//        name = "Cohere",
//        baseUrl = "https://api.cohere.ai/v1/",
//        description = """
//            Focuses on language models specifically built for enterprise needs, offering powerful tools for search, summarization, and RAG applications.
//        """.trimIndent()
//    ),

//    // LLM (ERNIE Bot)
//    ProviderSetting.GlobalAI(
//        id = 11,
//        name = "Baidu",
//        baseUrl = "https://aip.baidubce.com/rpc/2.0/ai_custom/v1/wenxinworkshop/",
//        description = """
//            The primary Chinese provider, offering the ERNIE Bot LLM for text generation and conversational AI, especially focused on the Chinese market and language.
//        """.trimIndent()
//    ),
//    // Image/Video (Firefly)
//    ProviderSetting.GlobalAI(
//        id = 12,
//        name = "Adobe",
//        baseUrl = "https://image.adobe.io/",
//        description = """
//            Provides generative AI features for creative professionals, including high-quality image filling, inpainting, and text-to-image generation, integrated into the Adobe ecosystem.
//        """.trimIndent()
//    ),
//    // LLM, CRM Integration
//    ProviderSetting.GlobalAI(
//        id = 14,
//        name = "Salesforce (Einstein GPT)",
//        baseUrl = "https://api.salesforce.com/",
//        description = """
//            Integrates generative AI directly into CRM workflows, allowing developers to build apps that summarize customer data, draft emails, and automate sales tasks.
//        """.trimIndent()
//    ),
//    // LLM (Luminous)
//    ProviderSetting.GlobalAI(
//        id = 16,
//        name = "Aleph Alpha",
//        baseUrl = "https://api.aleph-alpha.com/",
//        description = """
//            A European AI provider focusing on explainable and trustworthy AI, offering the Luminous LLM for text generation and reasoning.
//        """.trimIndent()
//    ),
//    // LLM (Jurassic-2)
//    ProviderSetting.GlobalAI(
//        id = 18,
//        name = "AI21 Labs",
//        baseUrl = "https://api.ai21.com/studio/v1/",
//        description = """
//            Provides the Jurassic-2 family of LLMs optimized for tasks like reading comprehension, summarization, and sophisticated text generation.
//        """.trimIndent()
//    ),
//    // Translation (AI-Powered)
//    ProviderSetting.GlobalAI(
//        id = 19,
//        name = "DeepL",
//        baseUrl = "https://api-free.deepl.com/v2/",
//        description = """
//            Widely regarded as a leader in neural machine translation, offering APIs for fast, accurate, and contextually nuanced translation across many languages.
//        """.trimIndent()
//    ),
//    // Video Generation (AI Avatars)
//    ProviderSetting.GlobalAI(
//        id = 20,
//        name = "Synthesia",
//        baseUrl = "https://api.synthesia.io/",
//        description = """
//            Allows users to generate professional videos using realistic, customizable AI avatars from a simple text script.
//        """.trimIndent()
//    ),
//    // Hosting Platform, Model Access
//    ProviderSetting.GlobalAI(
//        id = 21,
//        name = "Replicate",
//        baseUrl = "https://api.replicate.com/v1/",
//        description = """
//            A cloud platform that simplifies the deployment and scaling of popular open-source models (LLMs, image models, etc.) via a single API.
//        """.trimIndent()
//    ),
//    // LLM Access Platform (Model Agnostic)
//    ProviderSetting.GlobalAI(
//        id = 22,
//        name = "Amazon Bedrock",
//        baseUrl = "https://bedrock.us-east-1.amazonaws.com/",
//        description = """
//            An Amazon Web Services platform offering a single entry point to access various foundational models (Anthropic, Cohere, etc.) within the AWS ecosystem.
//        """.trimIndent()
//    ),
//    // LLM (Copywriting Focus)
//    ProviderSetting.GlobalAI(
//        id = 23,
//        name = "Writer",
//        baseUrl = "https://api.writer.com/",
//        description = """
//            Offers an LLM trained specifically for enterprise content and brand voice consistency, primarily used for marketing and internal communication.
//        """.trimIndent()
//    ),
//    // LLM (Search/Answer Engine)
//    ProviderSetting.GlobalAI(
//        id = 24,
//        name = "Perplexity AI",
//        baseUrl = "https://api.perplexity.ai/",
//        description = """
//            Provides an API focused on generating answers that are grounded in real-time search results, excellent for factual and up-to-date queries.
//        """.trimIndent()
//    ),
//    // Speech Synthesis (TTS/Voice Cloning)
//    ProviderSetting.GlobalAI(
//        id = 25,
//        name = "ElevenLabs",
//        baseUrl = "https://api.elevenlabs.io/v1/",
//        description = """
//            A leader in text-to-speech (TTS) and voice cloning technology, providing highly natural and emotional synthetic voices via API.
//        """.trimIndent()
//    ),
//    // Realistic Digital People (Video)
//    ProviderSetting.GlobalAI(
//        id = 26,
//        name = "D-ID",
//        baseUrl = "https://api.d-id.com/",
//        description = """
//            Specializes in generating photorealistic videos of digital people (avatars) speaking based on a text or audio input.
//        """.trimIndent()
//    ),
//    // Conversational AI Platform
//    ProviderSetting.GlobalAI(
//        id = 28,
//        name = "Kore.ai",
//        baseUrl = "https://api.kore.ai/",
//        description = """
//            An enterprise platform for building and deploying robust, complex virtual assistants (chatbots) and conversational AI solutions.
//        """.trimIndent()
//    ),

)
