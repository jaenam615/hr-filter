dependencies {
    implementation(project(":resume:schema"))
    implementation(project(":resume:api"))
    implementation(project(":resume:repository-exposed"))
    implementation(project(":resume:adapter-parsing"))
    implementation(project(":resume:adapter-llm"))
    implementation(project(":resume:adapter-storage-s3"))
    implementation(project(":resume:adapter-notifier"))

    runtimeOnly(rootProject.libs.postgres)
}
