rootProject.name = "securepay-api"

include(
    ":shared:platform-common",
    ":shared:platform-observability",
    ":shared:platform-web",
    ":shared:platform-testing",
    ":shared:platform-persistence",
    ":services:securepay-core",
    ":services:financial-ledger",
    ":services:choice-bank-connector",
    ":services:evidence-service",
    ":services:notification-service",
    ":services:webhook-service",
    ":applications:control-centre",
    ":testing:doctrine",
)
