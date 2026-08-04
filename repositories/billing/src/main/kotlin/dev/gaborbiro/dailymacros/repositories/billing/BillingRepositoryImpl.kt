package dev.gaborbiro.dailymacros.repositories.billing

import android.app.Activity
import android.content.Context
import android.util.Log
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import com.android.billingclient.api.acknowledgePurchase
import com.android.billingclient.api.queryProductDetails
import com.android.billingclient.api.queryPurchasesAsync
import com.google.firebase.auth.FirebaseAuth
import dev.gaborbiro.dailymacros.repositories.billing.domain.SUBSCRIPTION_BASE_PLAN_ID
import dev.gaborbiro.dailymacros.repositories.billing.domain.SUBSCRIPTION_OFFER_ID
import dev.gaborbiro.dailymacros.repositories.billing.domain.SUBSCRIPTION_PRODUCT_ID
import dev.gaborbiro.dailymacros.repositories.billing.domain.SubscriptionRepository
import dev.gaborbiro.dailymacros.repositories.billing.domain.model.SubscriptionState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.security.MessageDigest

/**
 * Wraps Play Billing (v9 API — enablePendingPurchases now requires a
 * PendingPurchasesParams, enableAutoServiceReconnection() replaces hand-rolled
 * reconnect-on-disconnect logic). The client is UX only: it decides what the
 * app *shows* (Settings row, soft paywall), never what the app is actually
 * allowed to do — that's `openaiProxy`'s job, checked server-side against the
 * verified state this class posts to `verifySubscription` via
 * [VerifyPurchaseWorker].
 */
internal class BillingRepositoryImpl(
    private val context: Context,
) : SubscriptionRepository {

    private companion object {
        // Temporary diagnostic logging while bringing up the purchase flow for the
        // first time — safe to keep (Log.d/w are no-ops in release without a debugger
        // attached to logcat), but fine to trim once the flow is confirmed working end to end.
        const val TAG = "BillingRepository"
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val prefs by lazy { BillingPrefs(context) }

    // Deliberately NOT a constructor parameter: resolving this repository (e.g. the
    // App.onCreate() refresh() call) must never force FirebaseAuth.getInstance() to run —
    // only an actual purchase attempt (obfuscatedAccountId(), below) needs it.
    private val firebaseAuth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }

    private val _state = MutableStateFlow<SubscriptionState>(SubscriptionState.Unknown)

    @Volatile
    private var cachedProductDetails: ProductDetails? = null

    private val purchasesUpdatedListener = PurchasesUpdatedListener { billingResult, purchases ->
        Log.d(TAG, "onPurchasesUpdated: code=${billingResult.responseCode} debugMessage=${billingResult.debugMessage} purchases=${purchases?.size}")
        if (billingResult.responseCode == BillingClient.BillingResponseCode.OK && purchases != null) {
            scope.launch { handlePurchases(purchases) }
        }
    }

    private val billingClient: BillingClient = BillingClient.newBuilder(context)
        .setListener(purchasesUpdatedListener)
        // v9.1.0 requires enableOneTimeProducts() even for a subs-only app — build()
        // throws IllegalArgumentException("Pending purchases for one-time products
        // must be supported.") otherwise, confirmed by actually running this code.
        .enablePendingPurchases(PendingPurchasesParams.newBuilder().enableOneTimeProducts().build())
        .enableAutoServiceReconnection()
        .build()

    init {
        startConnection()
    }

    override fun observeState(): Flow<SubscriptionState> = _state.asStateFlow()

    override fun currentState(): SubscriptionState = _state.value

    override fun refresh() {
        scope.launch {
            if (billingClient.isReady) {
                queryPurchasesAndUpdateState()
            }
        }
    }

    override fun launchPurchaseFlow(activity: Activity) {
        scope.launch {
            Log.d(TAG, "launchPurchaseFlow: billingClient.isReady=${billingClient.isReady} cachedProductDetails=$cachedProductDetails")
            val productDetails = cachedProductDetails ?: fetchProductDetails()
            if (productDetails == null) {
                Log.w(TAG, "launchPurchaseFlow: aborting, no ProductDetails for '$SUBSCRIPTION_PRODUCT_ID' (is the base plan Activated in Play Console? is this build installed via Internal testing, not a sideloaded debug/qa build?)")
                return@launch
            }
            val offerDetails = productDetails.subscriptionOfferDetails.orEmpty()
            Log.d(TAG, "launchPurchaseFlow: offers=${offerDetails.map { "${it.basePlanId}/${it.offerId}" }}")
            val offerToken = offerDetails
                .firstOrNull { it.basePlanId == SUBSCRIPTION_BASE_PLAN_ID && it.offerId == SUBSCRIPTION_OFFER_ID }
                ?.offerToken
                ?: offerDetails.firstOrNull { it.basePlanId == SUBSCRIPTION_BASE_PLAN_ID }?.offerToken
            if (offerToken == null) {
                Log.w(TAG, "launchPurchaseFlow: aborting, no offer matched basePlanId='$SUBSCRIPTION_BASE_PLAN_ID' offerId='$SUBSCRIPTION_OFFER_ID'")
                return@launch
            }

            val flowParams = BillingFlowParams.newBuilder()
                .setProductDetailsParamsList(
                    listOf(
                        BillingFlowParams.ProductDetailsParams.newBuilder()
                            .setProductDetails(productDetails)
                            .setOfferToken(offerToken)
                            .build()
                    )
                )
                .setObfuscatedAccountId(obfuscatedAccountId())
                .build()

            withContext(Dispatchers.Main) {
                val result = billingClient.launchBillingFlow(activity, flowParams)
                Log.d(TAG, "launchBillingFlow result: code=${result.responseCode} debugMessage=${result.debugMessage}")
            }
        }
    }

    private fun startConnection() {
        billingClient.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(billingResult: BillingResult) {
                Log.d(TAG, "onBillingSetupFinished: code=${billingResult.responseCode} debugMessage=${billingResult.debugMessage}")
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    scope.launch {
                        fetchProductDetails()
                        queryPurchasesAndUpdateState()
                    }
                }
            }

            // enableAutoServiceReconnection() above handles reconnecting; nothing to do here.
            override fun onBillingServiceDisconnected() {
                Log.w(TAG, "onBillingServiceDisconnected")
            }
        })
    }

    private suspend fun fetchProductDetails(): ProductDetails? {
        val params = QueryProductDetailsParams.newBuilder()
            .setProductList(
                listOf(
                    QueryProductDetailsParams.Product.newBuilder()
                        .setProductId(SUBSCRIPTION_PRODUCT_ID)
                        .setProductType(BillingClient.ProductType.SUBS)
                        .build()
                )
            )
            .build()
        val result = billingClient.queryProductDetails(params)
        Log.d(TAG, "queryProductDetails: code=${result.billingResult.responseCode} debugMessage=${result.billingResult.debugMessage} productDetailsList=${result.productDetailsList}")
        val details = result.productDetailsList?.firstOrNull()
        cachedProductDetails = details
        return details
    }

    private suspend fun queryPurchasesAndUpdateState() {
        val params = QueryPurchasesParams.newBuilder()
            .setProductType(BillingClient.ProductType.SUBS)
            .build()
        val result = billingClient.queryPurchasesAsync(params)
        handlePurchases(result.purchasesList.orEmpty())
    }

    private suspend fun handlePurchases(purchases: List<Purchase>) {
        val relevant = purchases.filter { it.products.contains(SUBSCRIPTION_PRODUCT_ID) }
        val active = relevant.any { it.purchaseState == Purchase.PurchaseState.PURCHASED }
        _state.value = if (active) SubscriptionState.Active else SubscriptionState.NotSubscribed

        relevant
            .filter { it.purchaseState == Purchase.PurchaseState.PURCHASED }
            .forEach { purchase ->
                if (!purchase.isAcknowledged) {
                    acknowledge(purchase)
                }
                if (purchase.purchaseToken != prefs.lastVerifiedPurchaseToken) {
                    VerifyPurchaseWorker.enqueue(context, purchase.purchaseToken, SUBSCRIPTION_PRODUCT_ID)
                }
            }
    }

    private suspend fun acknowledge(purchase: Purchase) {
        runCatching {
            billingClient.acknowledgePurchase(
                AcknowledgePurchaseParams.newBuilder()
                    .setPurchaseToken(purchase.purchaseToken)
                    .build()
            )
        }
    }

    private fun obfuscatedAccountId(): String {
        val uid = firebaseAuth.currentUser?.uid ?: return ""
        val digest = MessageDigest.getInstance("SHA-256").digest(uid.toByteArray())
        return digest.joinToString("") { "%02x".format(it) }
    }
}
