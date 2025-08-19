rootProject.name = "NotificationService"
include("server")
include("server:test")
findProject(":server:test")?.name = "test"
