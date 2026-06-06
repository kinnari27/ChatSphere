package com.chatsphere.data.remote

import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

interface ChatSphereApi {
    @POST("auth/login")
    suspend fun login(@Body request: LoginRequest): AuthResponse

    @POST("auth/register")
    suspend fun register(@Body request: RegisterRequest): AuthResponse

    @GET("conversations")
    suspend fun conversations(): List<ConversationDto>

    @GET("conversations/{conversationId}/messages")
    suspend fun messages(@Path("conversationId") conversationId: String): List<MessageDto>

    @POST("messages")
    suspend fun sendMessage(@Body request: SendMessageRequest): MessageDto

    @POST("groups")
    suspend fun createGroup(@Body request: CreateGroupRequest): ConversationDto
}
