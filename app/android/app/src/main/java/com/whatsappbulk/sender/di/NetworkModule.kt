package com.whatsappbulk.sender.di

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.whatsappbulk.sender.data.remote.api.CampaignApi
import com.whatsappbulk.sender.data.remote.api.WhatsAppApi
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Qualifier
import javax.inject.Singleton

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class WhatsAppRetrofit

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class CampaignRetrofit

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class AuthInterceptor

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    private const val WHATSAPP_BASE_URL = "http://127.0.0.1:3000/"
    private const val CAMPAIGN_BASE_URL = "http://192.168.31.33:3001/"

    @Provides
    @Singleton
    fun provideHttpLoggingInterceptor(): HttpLoggingInterceptor =
        HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BODY }

    @Provides
    @Singleton
    @AuthInterceptor
    fun provideAuthInterceptor(@ApplicationContext context: Context): Interceptor {
        return Interceptor { chain ->
            val masterKey = MasterKey.Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()

            val encryptedPrefs = EncryptedSharedPreferences.create(
                context,
                "auth_prefs",
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
            val token = encryptedPrefs.getString("jwt_token", null)
            val req = chain.request()
            val newReq = if (!token.isNullOrEmpty()) {
                req.newBuilder().addHeader("Authorization", "Bearer $token").build()
            } else req
            chain.proceed(newReq)
        }
    }

    @Provides
    @Singleton
    @WhatsAppRetrofit
    fun provideWhatsAppOkHttpClient(
        logging: HttpLoggingInterceptor
    ): OkHttpClient = OkHttpClient.Builder()
        .addInterceptor(logging)
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    @Provides
    @Singleton
    @CampaignRetrofit
    fun provideCampaignOkHttpClient(
        logging: HttpLoggingInterceptor,
        @AuthInterceptor auth: Interceptor
    ): OkHttpClient = OkHttpClient.Builder()
        .addInterceptor(logging)
        .addInterceptor(auth)
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    @Provides
    @Singleton
    @WhatsAppRetrofit
    fun provideWhatsAppRetrofit(@WhatsAppRetrofit client: OkHttpClient): Retrofit =
        Retrofit.Builder()
            .baseUrl(WHATSAPP_BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

    @Provides
    @Singleton
    @CampaignRetrofit
    fun provideCampaignRetrofit(@CampaignRetrofit client: OkHttpClient): Retrofit =
        Retrofit.Builder()
            .baseUrl(CAMPAIGN_BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

    @Provides
    @Singleton
    fun provideWhatsAppApi(@WhatsAppRetrofit retrofit: Retrofit): WhatsAppApi =
        retrofit.create(WhatsAppApi::class.java)

    @Provides
    @Singleton
    fun provideCampaignApi(@CampaignRetrofit retrofit: Retrofit): CampaignApi =
        retrofit.create(CampaignApi::class.java)
}
