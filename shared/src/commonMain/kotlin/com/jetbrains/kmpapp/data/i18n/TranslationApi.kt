package com.jetbrains.kmpapp.data.i18n

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.http.isSuccess
import io.ktor.utils.io.errors.IOException
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
private data class MyMemoryResponse(
    @SerialName("responseStatus") val responseStatus: Int = 0,
    @SerialName("responseData") val responseData: MyMemoryResponseData = MyMemoryResponseData()
)

@Serializable
private data class MyMemoryResponseData(
    @SerialName("translatedText") val translatedText: String = ""
)

/**
 * Бесплатный онлайн-переводчик MyMemory (без ключа). Переводит всегда с русского:
 * русский — источник, приложение хранит только пары «русский → целевой».
 */
class TranslationApi(private val client: HttpClient) {

    private val baseUrl = "https://api.mymemory.translated.net/get"

    suspend fun translate(text: String, target: AppLanguage): String {
        if (target == AppLanguage.RUSSIAN) return text
        val query = text.trim().take(MAX_QUERY_CHARS)
        if (query.isEmpty()) return text

        val response = client.get(baseUrl) {
            parameter("q", query)
            parameter("langpair", "${AppLanguage.RUSSIAN.code}|${target.code}")
        }
        if (!response.status.isSuccess()) {
            throw IOException("Перевод недоступен: HTTP ${response.status.value}")
        }
        val body: MyMemoryResponse = response.body()
        if (body.responseStatus != 200) {
            throw IOException("Перевод недоступен: статус ${body.responseStatus}")
        }
        val translated = body.responseData.translatedText.trim()
        if (translated.isEmpty()) {
            throw IOException("Перевод вернул пустой результат")
        }
        return translated
    }

    private companion object {
        // Лимит MyMemory на один запрос — около 500 символов.
        const val MAX_QUERY_CHARS = 450
    }
}
