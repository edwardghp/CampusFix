// build.gradle.kts  (RAIZ del proyecto)
// Plugins declarados a nivel de proyecto. 'apply false' = se aplican en :app, no aqui.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.hilt) apply false
    alias(libs.plugins.google.services) apply false
}
