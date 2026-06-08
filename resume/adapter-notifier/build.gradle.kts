dependencies {
    api(project(":resume:model"))
    implementation(project(":resume:infrastructure"))

    implementation(rootProject.libs.okhttp)
    implementation(rootProject.libs.jackson.module.kotlin)
    implementation(rootProject.libs.jakarta.mail.api)
    implementation(rootProject.libs.angus.mail)
}
