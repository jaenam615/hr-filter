dependencies {
    api(project(":resume:model"))
    implementation(project(":resume:service"))
    implementation(project(":resume:exception"))

    implementation("org.springframework.boot:spring-boot-starter-thymeleaf")
    implementation("org.springframework.boot:spring-boot-starter-validation")
}
