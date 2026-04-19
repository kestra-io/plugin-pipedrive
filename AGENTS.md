# Kestra Pipedrive Plugin

## What

- Provides plugin components under `io.kestra.plugin.pipedrive`.
- Includes classes such as `Create`, `Update`, `Deal`, `Person`.

## Why

- What user problem does this solve? Teams need to manage Pipedrive deals and persons from orchestrated workflows instead of relying on manual console work, ad hoc scripts, or disconnected schedulers.
- Why would a team adopt this plugin in a workflow? It keeps Pipedrive steps in the same Kestra flow as upstream preparation, approvals, retries, notifications, and downstream systems.
- What operational/business outcome does it enable? It reduces manual handoffs and fragmented tooling while improving reliability, traceability, and delivery speed for processes that depend on Pipedrive.

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
