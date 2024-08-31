-keepattributes LineNumberTable,SourceFile
-renamesourcefileattribute SourceFile

## rx2
#-keep,allowobfuscation,allowshrinking class io.reactivex.Flowable
#-keep,allowobfuscation,allowshrinking class io.reactivex.Maybe
#-keep,allowobfuscation,allowshrinking class io.reactivex.Observable
#-keep,allowobfuscation,allowshrinking class io.reactivex.Single

# R8 missing rules
-dontwarn org.bouncycastle.jsse.BCSSLParameters
-dontwarn org.bouncycastle.jsse.BCSSLSocket
-dontwarn org.bouncycastle.jsse.provider.BouncyCastleJsseProvider
-dontwarn org.conscrypt.Conscrypt$Version
-dontwarn org.conscrypt.Conscrypt
-dontwarn org.openjsse.javax.net.ssl.SSLParameters
-dontwarn org.openjsse.javax.net.ssl.SSLSocket
-dontwarn org.openjsse.net.ssl.OpenJSSE
