package com.example.testmusicfetcing

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import com.example.testmusicfetcing.ui.theme.TestMusicFetcingTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import okio.ByteString
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.TimeUnit

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val client = OkHttpClient.Builder()
            .readTimeout(0, TimeUnit.MILLISECONDS)
            .build()

        val request = Request.Builder()
            .url("ws://10.0.2.2:8080/audio")
            .build()

        val listener = object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                Log.i("websoket", "Соединение открыто")
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                Log.i("websoket", "Получено текстом: $text")
            }

            override fun onMessage(webSocket: WebSocket, bytes: ByteString) {
                Log.i("websoket", "Получено бинарно: ${bytes.size} байт")
            }

            override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                webSocket.close(1000, null)
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                Log.i("websoket", "Соединение закрыто")
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                println("Ошибка: ${t.message}")
            }
        }

        val webSocket = client.newWebSocket(request, listener)

        setContent {
            TestMusicFetcingTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Greeting(
                        name = "Android",
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    val startUrl = URL("http://10.0.2.2:8080/start")
    val pauseUrl = URL("http://10.0.2.2:8080/pause")

    val scope = rememberCoroutineScope()

    Column(modifier) {
        Button(
            onClick = {
                scope.launch {
                    val result = withContext(Dispatchers.IO) {
                        sendPost(startUrl)
                    }
                    Log.i("http", "start -> $result")
                }
            }
        ) {
            Text("start")
        }

        Button(
            onClick = {
                scope.launch {
                    val result = withContext(Dispatchers.IO) {
                        sendPost(pauseUrl)
                    }
                    Log.i("http", "pause -> $result")
                }
            }
        ) {
            Text("pause")
        }
    }
}

private fun sendPost(url: URL): String {
    var connection: HttpURLConnection? = null
    return try {
        connection = url.openConnection() as HttpURLConnection
        connection.requestMethod = "POST"
        connection.doOutput = true
        connection.connectTimeout = 5000
        connection.readTimeout = 5000

        // Отправляем пустое тело
        connection.outputStream.use { it.write(ByteArray(0)) }

        val code = connection.responseCode

        // Читаем тело ответа: для 2xx — inputStream, для ошибок — errorStream
        val stream = if (code in 200..299) connection.inputStream else connection.errorStream
        val body = stream?.bufferedReader()?.use { it.readText() } ?: ""

        "code=$code, body=$body"
    } catch (e: Exception) {
        "error: ${e.javaClass.simpleName}: ${e.message}"
    } finally {
        connection?.disconnect()
    }
}