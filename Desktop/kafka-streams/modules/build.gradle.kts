// Container module - no bootJar needed
tasks.bootJar {
    enabled = false
}

// Enable regular jar task
tasks.jar {
    enabled = true
}