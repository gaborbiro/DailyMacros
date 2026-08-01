package dev.gaborbiro.dailymacros.repositories.billing.di

import android.content.Context
import com.google.firebase.auth.FirebaseAuth
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import dev.gaborbiro.dailymacros.repositories.billing.BillingRepositoryImpl
import dev.gaborbiro.dailymacros.repositories.billing.domain.SubscriptionRepository
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal object BillingModule {

    @Provides
    @Singleton
    fun subscriptionRepository(
        @ApplicationContext context: Context,
    ): SubscriptionRepository = BillingRepositoryImpl(
        context = context,
        firebaseAuth = FirebaseAuth.getInstance(),
    )
}
