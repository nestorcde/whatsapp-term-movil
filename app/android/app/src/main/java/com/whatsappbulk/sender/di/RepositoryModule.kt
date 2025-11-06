package com.whatsappbulk.sender.di

import com.whatsappbulk.sender.data.repository.AuthRepository
import com.whatsappbulk.sender.data.repository.CampaignRepository
import com.whatsappbulk.sender.data.repository.WhatsAppRepository
import com.whatsappbulk.sender.data.repository.VpnRepository
import com.whatsappbulk.sender.domain.repository.IAuthRepository
import com.whatsappbulk.sender.domain.repository.ICampaignRepository
import com.whatsappbulk.sender.domain.repository.IWhatsAppRepository
import com.whatsappbulk.sender.domain.repository.IVpnRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindAuthRepository(
        authRepository: AuthRepository
    ): IAuthRepository

    @Binds
    @Singleton
    abstract fun bindWhatsAppRepository(
        whatsAppRepository: WhatsAppRepository
    ): IWhatsAppRepository

    @Binds
    @Singleton
    abstract fun bindCampaignRepository(
        campaignRepository: CampaignRepository
    ): ICampaignRepository

    @Binds
    @Singleton
    abstract fun bindVpnRepository(
        vpnRepository: VpnRepository
    ): IVpnRepository
}
