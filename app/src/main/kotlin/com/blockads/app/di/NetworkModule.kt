package com.blockads.app.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.ConnectionSpec
import okhttp3.Dns
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import java.net.InetAddress
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {
    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient {
        val logging =
            HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.NONE
            }
        return OkHttpClient.Builder()
            .connectionSpecs(listOf(ConnectionSpec.MODERN_TLS)) // no cleartext
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .addInterceptor(logging)
            .dns(StaticDns())
            .build()
    }

    private class StaticDns : Dns {
        override fun lookup(hostname: String): List<InetAddress> {
            val staticIps =
                when (hostname) {
                    "cloudflare-dns.com" -> listOf("1.1.1.1", "1.0.0.1")
                    "dns.google" -> listOf("8.8.8.8", "8.8.4.4")
                    "dns.quad9.net" -> listOf("9.9.9.9")
                    "dns.adguard-dns.com" -> listOf("94.140.14.14", "94.140.14.15")
                    "dns-family.adguard-dns.com" -> listOf("94.140.14.140", "94.140.14.141")
                    "security.cloudflare-dns.com" -> listOf("1.1.1.2", "1.0.0.2")
                    "family.cloudflare-dns.com" -> listOf("1.1.1.3", "1.0.0.3")
                    else -> return Dns.SYSTEM.lookup(hostname)
                }
            return staticIps.map { InetAddress.getByName(it) }
        }
    }
}
