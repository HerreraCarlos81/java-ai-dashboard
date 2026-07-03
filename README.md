# Java AI Usage Dashboard

A JavaFX desktop application for tracking AI model usage, token counts, and costs across multiple providers. Monitor per-API-key spending with budget alerts and admin-key-based grouping.

## Features

- **Multi-provider support** — OpenAI and Anthropic usage/cost APIs
- **Admin key grouping** — use one admin API key to see usage broken down by every API key in your organization
- **Per-key breakdown** — click a model to see each underlying API key's tokens, requests, and cost
- **Key name resolution** — API key IDs are resolved to human-readable names via the provider's admin API
- **Monthly budgets** — set a per-model budget; spending thresholds (50%, 75%, 100%) are color-coded in the list with a clickable budget badge
- **Encrypted key storage** — AES-256/GCM encryption for API keys at rest
- **Dark theme** — built-in dark UI theme
- **Async refresh** — data fetching runs on a background thread with a spinner indicator

## Requirements

- Java 21+ (Liberica JDK 21 Full recommended — includes JavaFX)
- API keys for your chosen providers

## Quick Start

1. Clone the repo
2. Set `JAVA_HOME` to your JDK 21+ installation
3. Run:

```bash
./gradlew run
```

4. Click **Settings** → **Add Model** → configure your provider and API keys
5. Mark one API key as **Admin** (required for OpenAI and Anthropic usage/cost APIs)
6. Click **Refresh** to fetch usage data

## Providers

| Provider | Default Base URL | Admin Key Required |
|---|---|---|
| OpenAI | `https://api.openai.com/v1` | Yes (for usage/cost API) |
| Anthropic | `https://api.anthropic.com/v1` | Yes (for usage/cost API) |

Admin API keys are created through each provider's console:
- **OpenAI**: Settings → Organization → Admin keys
- **Anthropic**: Console → Settings → API Keys (requires Team plan)

## Budget Alerts

Each model has a configurable monthly budget. The model list shows:

- **Green border**: spending < 50% of budget
- **Yellow border**: 50%–74%
- **Orange border**: 75%–99%
- **Red background + bold text**: ≥ 100% (overspent)

Click the budget badge (💰) to set or edit the budget.

## Running Tests

```bash
./gradlew runTests
```

## Configuration

Config is stored at `~/.ai-usage-dashboard/config.json`. Encryption keys are stored at `~/.ai-usage-dashboard/.keystore`.

## License

MIT
