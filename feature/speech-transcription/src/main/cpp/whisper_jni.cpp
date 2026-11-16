#include <jni.h>
#include <algorithm>
#include <cstdint>
#include <sstream>
#include <string>
#include <thread>
#include <vector>

#include "whisper.h"

namespace {

std::string json_escape(const char * value) {
    std::string result;
    if (value == nullptr) return result;

    for (const unsigned char ch : std::string(value)) {
        switch (ch) {
            case '"': result += "\\\""; break;
            case '\\': result += "\\\\"; break;
            case '\n': result += "\\n"; break;
            case '\r': result += "\\r"; break;
            case '\t': result += "\\t"; break;
            default:
                if (ch < 0x20) {
                    const char hex[] = "0123456789abcdef";
                    result += "\\u00";
                    result += hex[(ch >> 4) & 0x0f];
                    result += hex[ch & 0x0f];
                } else {
                    result += static_cast<char>(ch);
                }
        }
    }
    return result;
}

} // namespace

extern "C"
JNIEXPORT jstring JNICALL
Java_zouitel_neighborhood_speechtranscription_engine_WhisperNative_transcribe(
    JNIEnv * env,
    jobject /* thiz */,
    jstring model_path,
    jfloatArray pcm_samples,
    jstring language
) {
    if (model_path == nullptr || pcm_samples == nullptr || language == nullptr) {
        return env->NewStringUTF("{\"error\":\"Missing transcription input.\"}");
    }

    const char * model_chars = env->GetStringUTFChars(model_path, nullptr);
    const char * language_chars = env->GetStringUTFChars(language, nullptr);
    if (model_chars == nullptr || language_chars == nullptr) {
        if (model_chars != nullptr) env->ReleaseStringUTFChars(model_path, model_chars);
        if (language_chars != nullptr) env->ReleaseStringUTFChars(language, language_chars);
        return env->NewStringUTF("{\"error\":\"Could not read native input strings.\"}");
    }

    const jsize sample_count = env->GetArrayLength(pcm_samples);
    if (sample_count <= 0) {
        env->ReleaseStringUTFChars(model_path, model_chars);
        env->ReleaseStringUTFChars(language, language_chars);
        return env->NewStringUTF("{\"error\":\"Audio contains no samples.\"}");
    }

    std::vector<float> samples(static_cast<size_t>(sample_count));
    env->GetFloatArrayRegion(pcm_samples, 0, sample_count, samples.data());
    if (env->ExceptionCheck()) {
        env->ExceptionClear();
        env->ReleaseStringUTFChars(model_path, model_chars);
        env->ReleaseStringUTFChars(language, language_chars);
        return env->NewStringUTF("{\"error\":\"Could not read PCM samples.\"}");
    }

    whisper_context_params context_params = whisper_context_default_params();
    context_params.use_gpu = false;
    whisper_context * context = whisper_init_from_file_with_params(model_chars, context_params);

    env->ReleaseStringUTFChars(model_path, model_chars);
    if (context == nullptr) {
        env->ReleaseStringUTFChars(language, language_chars);
        return env->NewStringUTF("{\"error\":\"Whisper could not load the selected model.\"}");
    }

    whisper_full_params params = whisper_full_default_params(WHISPER_SAMPLING_GREEDY);
    const unsigned int hardware_threads = std::thread::hardware_concurrency();
    params.n_threads = static_cast<int>(std::max(1u, std::min(4u, hardware_threads == 0 ? 2u : hardware_threads)));
    params.language = language_chars;
    params.translate = false;
    params.no_context = true;
    params.print_realtime = false;
    params.print_progress = false;
    params.print_timestamps = false;
    params.print_special = false;

    const int status = whisper_full(
        context,
        params,
        samples.data(),
        static_cast<int>(samples.size())
    );
    env->ReleaseStringUTFChars(language, language_chars);

    if (status != 0) {
        whisper_free(context);
        return env->NewStringUTF("{\"error\":\"Whisper transcription failed.\"}");
    }

    std::ostringstream output;
    output << "{\"language\":\"" << json_escape(whisper_lang_str(whisper_full_lang_id(context)))
           << "\",\"segments\":[";
    const int segment_count = whisper_full_n_segments(context);
    for (int index = 0; index < segment_count; ++index) {
        if (index > 0) output << ",";
        const int64_t start_ms = whisper_full_get_segment_t0(context, index) * 10;
        const int64_t end_ms = whisper_full_get_segment_t1(context, index) * 10;
        output << "{\"text\":\"" << json_escape(whisper_full_get_segment_text(context, index))
               << "\",\"startMs\":" << start_ms
               << ",\"endMs\":" << end_ms << "}";
    }
    output << "]}";

    whisper_free(context);
    return env->NewStringUTF(output.str().c_str());
}
