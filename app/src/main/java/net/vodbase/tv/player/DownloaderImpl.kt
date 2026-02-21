package net.vodbase.tv.player

import org.schabi.newpipe.extractor.downloader.Downloader
import org.schabi.newpipe.extractor.downloader.Request
import org.schabi.newpipe.extractor.downloader.Response
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

class DownloaderImpl private constructor() : Downloader() {

    companion object {
        @Volatile private var instance: DownloaderImpl? = null

        fun getInstance(): DownloaderImpl = instance ?: synchronized(this) {
            instance ?: DownloaderImpl().also { instance = it }
        }
    }

    override fun execute(request: Request): Response {
        val url = URL(request.url())
        val connection = url.openConnection() as HttpURLConnection

        connection.requestMethod = request.httpMethod()
        connection.connectTimeout = 15000
        connection.readTimeout = 20000

        // Set headers
        for ((key, values) in request.headers()) {
            for (value in values) {
                connection.addRequestProperty(key, value)
            }
        }

        // User-Agent
        if (connection.getRequestProperty("User-Agent") == null) {
            connection.setRequestProperty("User-Agent",
                "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
        }

        // Write body if present
        val dataToSend = request.dataToSend()
        if (dataToSend != null) {
            connection.doOutput = true
            connection.outputStream.use { it.write(dataToSend) }
        }

        val responseCode = connection.responseCode
        val responseMessage = connection.responseMessage ?: ""
        val responseHeaders = connection.headerFields
            ?.filterKeys { it != null }
            ?.mapValues { it.value ?: emptyList() }
            ?: emptyMap()

        val responseBody = try {
            val stream = if (responseCode >= 400) connection.errorStream else connection.inputStream
            stream?.bufferedReader()?.readText() ?: ""
        } catch (e: IOException) {
            ""
        }

        return Response(responseCode, responseMessage, responseHeaders, responseBody, connection.url.toString())
    }
}
