package com.example.mwallet.data.remote

import com.example.mwallet.BuildConfig
import com.example.mwallet.data.dto.AuthResponseDto
import com.example.mwallet.data.dto.DepositRequestDto
import com.example.mwallet.data.dto.ErrorResponseDto
import com.example.mwallet.data.dto.FirebaseAuthRequestDto
import com.example.mwallet.data.dto.LoginResolveRequestDto
import com.example.mwallet.data.dto.LoginResolveResponseDto
import com.example.mwallet.data.dto.ResetLookupRequestDto
import com.example.mwallet.data.dto.ResetLookupResponseDto
import com.example.mwallet.data.dto.TransactionResponseDto
import com.example.mwallet.data.dto.TransferRequestDto
import com.example.mwallet.data.dto.UserSearchResponseDto
import com.example.mwallet.data.dto.WalletResponseDto
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

class ApiException(val code: Int, override val message: String) : Exception(message)

class WalletApi(
    private val tokenProvider: suspend () -> String?
) {
    private val apiBaseUrl = BuildConfig.API_BASE_URL
        .trim()
        .trimEnd('/')

    private val client = HttpClient(CIO) {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                isLenient = true
            })
        }
        install(Logging) {
            logger = object : Logger {
                override fun log(message: String) {
                    android.util.Log.d("WalletApi", message)
                }
            }
            level = LogLevel.INFO
        }
        defaultRequest {
            url(apiBaseUrl)
            contentType(ContentType.Application.Json)
        }
    }

    suspend fun resolveLogin(login: String): LoginResolveResponseDto =
        post("/api/auth/resolve-login", LoginResolveRequestDto(login))

    suspend fun lookupPasswordReset(login: String): ResetLookupResponseDto =
        post("/api/auth/reset-lookup", ResetLookupRequestDto(login))

    suspend fun authenticateWithFirebase(request: FirebaseAuthRequestDto): AuthResponseDto =
        post("/api/auth/firebase", request)

    suspend fun searchUsers(query: String): List<UserSearchResponseDto> =
        authorizedGet("/api/users/search") {
            parameter("q", query)
        }

    suspend fun getWallet(): WalletResponseDto = authorizedGet("/api/wallet")

    suspend fun deposit(amount: String): WalletResponseDto =
        authorizedPost("/api/wallet/deposit", DepositRequestDto(amount))

    suspend fun transfer(toUsername: String, amount: String, description: String?): WalletResponseDto =
        authorizedPost("/api/wallet/transfer", TransferRequestDto(toUsername, amount, description))

    suspend fun getTransactions(): List<TransactionResponseDto> =
        authorizedGet("/api/wallet/transactions")

    private suspend inline fun <reified T> post(path: String, body: Any): T {
        val response = client.post(path) { setBody(body) }
        return handleResponse(response)
    }

    private suspend inline fun <reified T> authorizedGet(
        path: String,
        crossinline block: io.ktor.client.request.HttpRequestBuilder.() -> Unit = {}
    ): T {
        val token = tokenProvider() ?: throw ApiException(401, "Требуется авторизация")
        val response = client.get(path) {
            header(HttpHeaders.Authorization, "Bearer $token")
            block()
        }
        return handleResponse(response)
    }

    private suspend inline fun <reified T> authorizedPost(path: String, body: Any): T {
        val token = tokenProvider() ?: throw ApiException(401, "Требуется авторизация")
        val response = client.post(path) {
            header(HttpHeaders.Authorization, "Bearer $token")
            setBody(body)
        }
        return handleResponse(response)
    }

    private suspend inline fun <reified T> handleResponse(response: HttpResponse): T {
        if (response.status.isSuccess()) {
            return response.body()
        }
        val errorText = runCatching { response.body<ErrorResponseDto>().message }
            .getOrElse { response.bodyAsText() }
        throw ApiException(response.status.value, errorText)
    }
}
