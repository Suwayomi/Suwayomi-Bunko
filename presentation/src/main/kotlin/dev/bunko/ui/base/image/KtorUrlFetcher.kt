package dev.bunko.ui.base.image

import com.seiko.imageloader.component.fetcher.FetchResult
import com.seiko.imageloader.component.fetcher.Fetcher
import com.seiko.imageloader.model.ImageSource
import com.seiko.imageloader.model.ImageSourceFrom
import com.seiko.imageloader.model.KtorRequestData
import com.seiko.imageloader.model.extraData
import com.seiko.imageloader.model.mimeType
import com.seiko.imageloader.model.toImageSource
import com.seiko.imageloader.option.Options
import io.ktor.client.HttpClient
import io.ktor.client.request.headers
import io.ktor.client.request.prepareRequest
import io.ktor.client.request.url
import io.ktor.client.statement.bodyAsChannel
import io.ktor.http.HttpMethod
import io.ktor.http.Url
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.readRemaining
import kotlinx.io.readByteArray
import okio.Buffer

class KtorUrlFetcher private constructor(
    private val httpUrl: Url,
    private val httpClient: () -> HttpClient,
    private val ktorRequestData: KtorRequestData?,
) : Fetcher {

    override suspend fun fetch(): FetchResult {
        return httpClient().prepareRequest {
            url(httpUrl)
            method = ktorRequestData?.method ?: HttpMethod.Get
            ktorRequestData?.headers?.let {
                headers {
                    it.forEach { (key, value) ->
                        append(key, value)
                    }
                }
            }
        }.execute { response ->
            if (!response.status.isSuccess()) {
                throw KtorUrlRequestException("code:${response.status.value}, ${response.status.description}")
            }

            FetchResult.OfSource(
                imageSource = channelToImageSource(response.bodyAsChannel()),
                imageSourceFrom = ImageSourceFrom.Network,
                extra = extraData {
                    mimeType(response.contentType()?.toString())
                },
            )
        }
    }

    class Factory(
        private val httpClient: () -> HttpClient,
    ) : Fetcher.Factory {
        override fun create(data: Any, options: Options): Fetcher? {
            if (data !is Url) return null
            return KtorUrlFetcher(
                httpUrl = data,
                httpClient = httpClient,
                ktorRequestData = options.extra["KEY_KTOR_REQUEST_DATA"] as? KtorRequestData,
            )
        }
    }

    class ExternalFactory(
        private val httpClient: () -> HttpClient,
    ) : Fetcher.Factory {
        override fun create(data: Any, options: Options): Fetcher? {
            if (data !is ExternalImage) return null
            return KtorUrlFetcher(data.url, httpClient, null)
        }
    }
}

data class ExternalImage(val url: Url)

private const val MAX_IMAGE_BYTES = 32L * 1024 * 1024

// an endless body would otherwise fill the shared heap and take the ui thread down with it
private suspend fun channelToImageSource(channel: ByteReadChannel): ImageSource {
    val bytes = channel.readRemaining(MAX_IMAGE_BYTES + 1).readByteArray()
    if (bytes.size > MAX_IMAGE_BYTES) throw KtorUrlRequestException("image exceeds $MAX_IMAGE_BYTES bytes")
    return Buffer().write(bytes).toImageSource()
}

private class KtorUrlRequestException(msg: String) : RuntimeException(msg)
