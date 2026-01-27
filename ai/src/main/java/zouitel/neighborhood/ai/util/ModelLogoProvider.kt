package zouitel.neighborhood.ai.util

object ModelLogoProvider {
    fun getLogoUrl(modelId: String): String? {
        return logoMappings.entries.find { it.key in modelId }?.value
    }

    private val logoMappings = hashMapOf(
        "gemini" to "https://registry.npmmirror.com/@lobehub/icons-static-png/latest/files/dark/gemini-color.png",
        "gemma" to "https://registry.npmmirror.com/@lobehub/icons-static-png/latest/files/dark/gemma-color.png",
        "claude" to "https://registry.npmmirror.com/@lobehub/icons-static-png/latest/files/dark/claude-color.png",
        "openai" to "https://registry.npmmirror.com/@lobehub/icons-static-png/latest/files/dark/openai.png",
        "meta-llama" to "https://registry.npmmirror.com/@lobehub/icons-static-png/latest/files/dark/ollama.png",
        "microsoft" to "https://registry.npmmirror.com/@lobehub/icons-static-png/latest/files/dark/microsoft-color.png",
        "mistral" to "https://registry.npmmirror.com/@lobehub/icons-static-png/latest/files/dark/mistral-color.png",
        "stability" to "https://registry.npmmirror.com/@lobehub/icons-static-png/latest/files/dark/stability-color.png",
        "huggingface" to "https://registry.npmmirror.com/@lobehub/icons-static-png/latest/files/dark/huggingface-color.png",
        "bytedance" to "https://registry.npmmirror.com/@lobehub/icons-static-png/latest/files/dark/bytedance-color.png",
        "bedrock" to "https://registry.npmmirror.com/@lobehub/icons-static-png/latest/files/dark/bedrock-color.png",
        "qwen" to "https://registry.npmmirror.com/@lobehub/icons-static-png/latest/files/dark/qwen-color.png",
        "grok" to "https://registry.npmmirror.com/@lobehub/icons-static-png/latest/files/dark/grok.png",
        "openrouter" to "https://registry.npmmirror.com/@lobehub/icons-static-png/latest/files/dark/openrouter.png",
        "zhipu" to "https://registry.npmmirror.com/@lobehub/icons-static-png/latest/files/dark/zhipu-color.png",
        "alibaba" to "https://registry.npmmirror.com/@lobehub/icons-static-png/latest/files/dark/alibaba-color.png",
        "aion-labs" to "https://registry.npmmirror.com/@lobehub/icons-static-png/latest/files/dark/aionlabs-color.png",
        "aihubmix" to "https://registry.npmmirror.com/@lobehub/icons-static-png/latest/files/dark/aihubmix-color.png",
        "ai21" to "https://registry.npmmirror.com/@lobehub/icons-static-png/latest/files/dark/ai21.png",
        "z-ai" to "https://registry.npmmirror.com/@lobehub/icons-static-png/latest/files/dark/zai.png",
        "siliconcloud" to "https://registry.npmmirror.com/@lobehub/icons-static-png/latest/files/dark/siliconcloud-color.png",
        "nous" to "https://registry.npmmirror.com/@lobehub/icons-static-png/latest/files/dark/nousresearch.png",
        "nvidia" to "https://registry.npmmirror.com/@lobehub/icons-static-png/latest/files/dark/nvidia-color.png",
        "minimax" to "https://registry.npmmirror.com/@lobehub/icons-static-png/latest/files/dark/minimax-color.png",
        "liquid" to "https://registry.npmmirror.com/@lobehub/icons-static-png/latest/files/dark/liquid.png",
        "deepseek" to "https://registry.npmmirror.com/@lobehub/icons-static-png/latest/files/dark/deepseek-color.png",
        "baidu" to "https://registry.npmmirror.com/@lobehub/icons-static-png/latest/files/dark/baidu-color.png",
        "kimi" to "https://registry.npmmirror.com/@lobehub/icons-static-png/latest/files/dark/kimi-color.png",
        "perplexity" to "https://registry.npmmirror.com/@lobehub/icons-static-png/latest/files/dark/perplexity-color.png",
        "cohere" to "https://registry.npmmirror.com/@lobehub/icons-static-png/latest/files/dark/cohere-color.png",
        "ibm" to "https://registry.npmmirror.com/@lobehub/icons-static-png/latest/files/dark/ibm.png",
        "nova" to "https://registry.npmmirror.com/@lobehub/icons-static-png/latest/files/dark/nova-color.png",
        "stepfun" to "https://registry.npmmirror.com/@lobehub/icons-static-png/latest/files/dark/stepfun-color.png",
        "jina" to "https://registry.npmmirror.com/@lobehub/icons-static-png/latest/files/dark/jina.png",
        "hunyuan" to "https://registry.npmmirror.com/@lobehub/icons-static-png/latest/files/dark/hunyuan-color.png",
        "aihubmix" to "https://registry.npmmirror.com/@lobehub/icons-static-png/latest/files/dark/aihubmix-color.png",
        "doubao" to "https://registry.npmmirror.com/@lobehub/icons-static-png/latest/files/dark/doubao-color.png",
        "anthropic" to "https://registry.npmmirror.com/@lobehub/icons-static-png/latest/files/dark/anthropic.png",
        "moonshot" to "https://registry.npmmirror.com/@lobehub/icons-static-png/latest/files/dark/moonshot.png",
    )
}