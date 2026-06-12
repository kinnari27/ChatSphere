package com.chatsphere.data.remote

import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Path

interface ChatSphereApi {
    @POST("auth/login")
    suspend fun login(@Body request: LoginRequest): AuthResponse

    @POST("auth/register")
    suspend fun register(@Body request: RegisterRequest): AuthResponse

    @POST("auth/google")
    suspend fun loginWithGoogle(@Body request: GoogleLoginRequest): AuthResponse

    @GET("conversations")
    suspend fun conversations(): List<ConversationDto>

    @GET("conversations/{conversationId}/messages")
    suspend fun messages(@Path("conversationId") conversationId: String): List<MessageDto>

    @POST("messages")
    suspend fun sendMessage(@Body request: SendMessageRequest): MessageDto

    @Multipart
    @POST("messages/media")
    suspend fun sendMediaMessage(
        @Part("conversationId") conversationId: RequestBody,
        @Part file: MultipartBody.Part,
        @Part("type") type: RequestBody,
        @Part("replyToMessageId") replyToMessageId: RequestBody? = null
    ): MessageDto

    @POST("groups")
    suspend fun createGroup(@Body request: CreateGroupRequest): ConversationDto
}
