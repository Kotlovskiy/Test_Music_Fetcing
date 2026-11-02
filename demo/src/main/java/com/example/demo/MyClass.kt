package com.example.demo

import okhttp3.*
import java.io.IOException
import java.util.concurrent.CountDownLatch

fun main() {
    println("=== OkHttp3 Demonstration ===")

    // 1. Создаем клиент с кастомным перехватчиком (Interceptors)
    val client = OkHttpClient.Builder()
        .addInterceptor(LoggingInterceptor()) // Добавляем наш перехватчик
        .build()

    // 2. Строим запрос
    val request = Request.Builder()
        .url("https://api.github.com/users/octocat")
        .build()

    // 3. Демонстрация СИНХРОННОГО вызова (execute)
    println("\n--- Synchronous call (execute) ---")
    try {
        client.newCall(request).execute().use { response ->
            println("HTTP code: ${response.code}")
            if (response.isSuccessful) {
                val body = response.body?.string()
                println("Body (first 100 chars): ${body?.take(100)}...")
            }
        }
    } catch (e: IOException) {
        e.printStackTrace()
    }

    // 4. Демонстрация АСИНХРОННОГО вызова (enqueue)
    println("\n--- Asynchronous call (enqueue) ---")
    val latch = CountDownLatch(1)
    client.newCall(request).enqueue(object : Callback {
        override fun onFailure(call: Call, e: IOException) {
            println("Async error: ${e.message}")
            latch.countDown()
        }

        override fun onResponse(call: Call, response: Response) {
            response.use {
                println("Async HTTP code: ${it.code}")
                println("Async body (first 50 chars): ${it.body?.string()?.take(50)}...")
            }
            latch.countDown()
        }
    })

    latch.await()
    println("\n=== Demonstration finished ===")
}

// Простой перехватчик для демонстрации механизма
class LoggingInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        println("[Interceptor] Request to: ${request.url}")
        println("[Interceptor] Headers: ${request.headers}")

        val response = chain.proceed(request) // Передаем запрос дальше по цепочке

        println("[Interceptor] Response code: ${response.code}")
        return response
    }
}