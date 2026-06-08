dependencies {
    api(project(":resume:model"))
    implementation(project(":resume:infrastructure"))

    implementation(rootProject.libs.aws.s3)
}
