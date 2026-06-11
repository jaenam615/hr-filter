dependencies {
    api(project(":resume:model"))
    implementation(project(":resume:infrastructure"))

    implementation(rootProject.libs.tika.core)
    implementation(rootProject.libs.tika.parsers.standard)
}
