package com.gemmaheretic.app.data.repository

import com.gemmaheretic.app.data.local.dao.EndpointDao
import com.gemmaheretic.app.data.local.entity.EndpointEntity
import com.gemmaheretic.app.domain.model.ConnectionStatus
import com.gemmaheretic.app.domain.model.Endpoint
import com.gemmaheretic.app.domain.model.OllamaModel
import com.gemmaheretic.app.network.api.OllamaApiService
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

class EndpointRepository(
    private val endpointDao: EndpointDao
) {
    val allEndpoints: Flow<List<Endpoint>> = endpointDao.getAllEndpoints().map { entities ->
        entities.map { it.toDomain() }
    }

    suspend fun getEndpointById(id: Long): Endpoint? {
        return endpointDao.getEndpointById(id)?.toDomain()
    }

    suspend fun getDefaultEndpoint(): Endpoint? {
        return endpointDao.getDefaultEndpoint()?.toDomain()
    }

    suspend fun saveEndpoint(endpoint: Endpoint): Long {
        return endpointDao.insertEndpoint(endpoint.toEntity())
    }

    suspend fun updateEndpoint(endpoint: Endpoint) {
        endpointDao.updateEndpoint(endpoint.toEntity())
    }

    suspend fun deleteEndpoint(endpoint: Endpoint) {
        endpointDao.deleteEndpoint(endpoint.toEntity())
    }

    suspend fun setDefault(id: Long) {
        endpointDao.setDefaultEndpoint(id)
    }

    suspend fun testConnection(url: String): ConnectionStatus {
        return try {
            val api = createApiService(url, connectTimeout = 5, readTimeout = 5)
            val response = api.getVersion()
            if (response.isSuccessful) {
                val version = response.body()?.version ?: "unknown"
                ConnectionStatus.Connected
            } else {
                ConnectionStatus.Failed("HTTP ${response.code()}")
            }
        } catch (e: Exception) {
            ConnectionStatus.Failed(e.message ?: "Connection failed")
        }
    }

    suspend fun fetchModels(url: String): Result<List<OllamaModel>> {
        return try {
            val api = createApiService(url)
            val response = api.listModels()
            if (response.isSuccessful) {
                val models = response.body()?.models?.map { info ->
                    OllamaModel(
                        name = info.name,
                        size = info.size,
                        digest = info.digest,
                        modifiedAt = info.modifiedAt,
                        parameterSize = info.details?.parameterSize ?: "",
                        quantizationLevel = info.details?.quantizationLevel ?: ""
                    )
                } ?: emptyList()
                Result.success(models)
            } else {
                Result.failure(Exception("HTTP ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    companion object {
        fun createApiService(
            baseUrl: String,
            connectTimeout: Int = 10,
            readTimeout: Int = 120
        ): OllamaApiService {
            val normalizedUrl = baseUrl.trimEnd('/')
            val client = OkHttpClient.Builder()
                .connectTimeout(connectTimeout.toLong(), TimeUnit.SECONDS)
                .readTimeout(readTimeout.toLong(), TimeUnit.SECONDS)
                .build()
            return Retrofit.Builder()
                .baseUrl("$normalizedUrl/")
                .client(client)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(OllamaApiService::class.java)
        }
    }

    private fun EndpointEntity.toDomain() = Endpoint(
        id = id,
        name = name,
        url = url,
        isDefault = isDefault,
        defaultModel = defaultModel,
        createdAt = createdAt
    )

    private fun Endpoint.toEntity() = EndpointEntity(
        id = id,
        name = name,
        url = url,
        isDefault = isDefault,
        defaultModel = defaultModel,
        createdAt = createdAt
    )
}
