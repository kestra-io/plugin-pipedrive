# Kestra Pipedrive Plugin

## What

description = 'Plugin Pipedrive for Kestra Exposes 4 plugin components (tasks, triggers, and/or conditions).

## Why

Enables Kestra workflows to interact with Pipedrive, allowing orchestration of Pipedrive-based operations as part of data pipelines and automation workflows.

## How

### Architecture

Single-module plugin. Source packages under `io.kestra.plugin`:

- `pipedrive`

Infrastructure dependencies (Docker Compose services):

- `kestra`
- `kestra-data`
- `postgres`

### Key Plugin Classes

- `io.kestra.plugin.pipedrive.deals.Create`
- `io.kestra.plugin.pipedrive.deals.Update`
- `io.kestra.plugin.pipedrive.persons.Create`
- `io.kestra.plugin.pipedrive.persons.Get`

### Project Structure

```
plugin-pipedrive/
├── src/main/java/io/kestra/plugin/pipedrive/persons/
├── src/test/java/io/kestra/plugin/pipedrive/persons/
├── build.gradle
└── README.md
```

### Important Commands

```bash
# Build the plugin
./gradlew shadowJar

# Run tests
./gradlew test

# Build without tests
./gradlew shadowJar -x test
```

### Configuration

All tasks and triggers accept standard Kestra plugin properties. Credentials should use
`{{ secret('SECRET_NAME') }}` — never hardcode real values.

## Agents

**IMPORTANT:** This is a Kestra plugin repository (prefixed by `plugin-`, `storage-`, or `secret-`). You **MUST** delegate all coding tasks to the `kestra-plugin-developer` agent. Do NOT implement code changes directly — always use this agent.
