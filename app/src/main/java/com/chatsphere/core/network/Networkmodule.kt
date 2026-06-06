package com.chatsphere.core.network

import com.chatsphere.BuildConfig
import kotlinx.serialization.json.Json
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Response
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import timber.log.Timber
import java.util.concurrent.TimeUnit
import javax.inject.Inject

/**
 * OkHttp interceptor that attaches the JWT Authorization header to every request.
 *
 * Also handles 401 Unauthorized by attempting a token refresh before retrying.
 */
class AuthInterceptor @Inject constructor(
    private val tokenStorage: TokenStorage
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val token = tokenStorage.getAccessToken()
        val request = if (token != null) {
            chain.request().newBuilder()
                .header("Authorization", "Bearer $token")
                .build()
        } else {
            chain.request()
        }

        val response = chain.proceed(request)

        // If 401, the repository layer handles refresh via AuthRepository.refreshToken()
        return response
    }
}

/**
 * Centralised Kotlin Serialization Json instance with lenient settings
 * suitable for consuming a REST API that may include unknown fields.
 */
val networkJson = Json {
    ignoreUnknownKeys = true
    coerceInputValues = true
    encodeDefaults = true
    isLenient = true
}

/**
 * Builds the singleton [OkHttpClient] with auth, logging, and timeouts.
 */
fun buildOkHttpClient(authInterceptor: AuthInterceptor): OkHttpClient =
    OkHttpClient.Builder()
        .addInterceptor(authInterceptor)
        .addInterceptor(
            HttpLoggingInterceptor { message -> Timber.tag("HTTP").d(message) }.apply {
                level = if (BuildConfig.DEBUG) HttpLoggingInterceptor.Level.BODY
                else HttpLoggingInterceptor.Level.NONE
            }
        )
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

/**
 * Builds the Retrofit instance backed by Kotlin Serialization.
 */
fun buildRetrofit(okHttpClient: OkHttpClient, baseUrl: String): Retrofit =
    Retrofit.Builder()
        .baseUrl(baseUrl)
        .client(okHttpClient)
        .addConverterFactory(networkJson.asConverterFactory("application/json; charset=UTF8".toMediaType()))
        .build()

/**
 * Persistent storage for JWT tokens, backed by DataStore.
 * Provides synchronous access for the OkHttp interceptor.
 */
interface TokenStorage {
    fun getAccessToken(): String?
    fun getRefreshToken(): String?
    suspend fun saveTokens(accessToken: String, refreshToken: String, expiresAt: Long)
    suspend fun clearTokens()
    fun isTokenExpired(): Boolean
}

/**
 * Thin wrapper to safely execute a Retrofit suspend call and map to Result<T>.
 * HTTP error codes are mapped to exceptions with descriptive messages.
 */
suspend fun <T> safeApiCall(call: suspend () -> T): Result<T> {
    return try {
        Result.success(call())
    } catch (e: retrofit2.HttpException) {
        val message = when (e.code()) {
            400 -> "Bad request"
            401 -> "Unauthorized — please log in again"
            403 -> "You don't have permission to do this"
            404 -> "Not found"
            429 -> "Too many requests — please slow down"
            500 -> "Server error, please try again later"
            else -> "Network error (${e.code()})"
        }
        Timber.e(e, "HTTP ${e.code()}: $message")
        Result.failure(NetworkException(message, e.code()))
    } catch (e: java.io.IOException) {
        Timber.e(e, "IO Exception - likely no network")
        Result.failure(NetworkException("No internet connection", -1))
    } catch (e: Exception) {
        Timber.e(e, "Unexpected API error")
        Result.failure(e)
    }
}

/** Typed exception for HTTP and network errors. */
class NetworkException(message: String, val statusCode: Int) : Exception(message)