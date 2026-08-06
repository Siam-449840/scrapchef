# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
-keepattributes SourceFile,LineNumberTable
-keepattributes *Annotation*

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile

# ─── Moshi: keep @JsonClass annotated models and generated adapters ───
-keep class com.scrapchef.android.data.model.** { *; }
-keep class com.scrapchef.android.data.remote.ChatMessage { *; }
-keep class com.scrapchef.android.data.remote.ChatCompletionRequest { *; }
-keep class com.scrapchef.android.data.remote.ChatCompletionResponse { *; }
-keep class com.scrapchef.android.data.remote.ChatChoice { *; }
-keep class com.scrapchef.android.data.remote.OpenRouterError { *; }
-keep class com.scrapchef.android.data.remote.StreamingRecipeState { *; }
-keep class com.scrapchef.android.data.remote.AiGenerationException
-keep class com.scrapchef.android.data.remote.AiTimeoutException
-keep class com.scrapchef.android.data.remote.ApiKeyInvalidException
-keep class com.scrapchef.android.data.remote.NetworkException
-keep class com.scrapchef.android.data.remote.RateLimitException

# ─── Room: keep entity classes ───
-keep class com.scrapchef.android.data.model.FavoriteRecipeEntity { *; }
-keep class com.scrapchef.android.data.model.IngredientEntity { *; }
-keep class com.scrapchef.android.data.model.PantryItemEntity { *; }

# ─── OkHttp / Retrofit ───
-dontwarn okhttp3.**
-dontwarn retrofit2.**
-keep class retrofit2.** { *; }

# ─── BuildConfig: keep generated config for API key access ───
-keep class com.scrapchef.android.BuildConfig { *; }

