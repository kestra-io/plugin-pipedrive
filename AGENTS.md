# Kestra Pipedrive Plugin

## What

- Provides plugin components under `io.kestra.plugin.pipedrive`.
- Includes classes such as `Create`, `Update`, `Deal`, `Person`.

## Why

- This plugin integrates Kestra with Pipedrive Deals.
- It provides tasks that create or update deals in Pipedrive.

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

## References

- https://kestra.io/docs/plugin-developer-guide
- https://kestra.io/docs/plugin-developer-guide/contribution-guidelines
