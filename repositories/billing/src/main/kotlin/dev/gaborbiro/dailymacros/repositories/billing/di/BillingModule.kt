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

    // Only for VerifyPurchaseWorker, which is @AssistedInject-constructed and so must
    // resolve every non-@Assisted parameter from the graph. BillingRepositoryImpl
    // deliberately does NOT take FirebaseAuth as a constructor param (see its own
    // lazy property) — resolving the repository itself (e.g. App.onCreate()'s
    // refresh() call) must never force FirebaseAuth.getInstance() to run.
    @Provides
    @Singleton
    fun firebaseAuth(): FirebaseAuth = FirebaseAuth.getInstance()

    @Provides
    @Singleton
    fun subscriptionRepository(
        @ApplicationContext context: Context,
    ): SubscriptionRepository = BillingRepositoryImpl(context = context)
}
