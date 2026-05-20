# How to use the Pipedrive plugin

Manage deals and persons in Pipedrive from Kestra flows.

## Authentication

Set `apiToken` to your Pipedrive API token. Store it in a [secret](https://kestra.io/docs/concepts/secret) and apply it globally with [plugin defaults](https://kestra.io/docs/workflow-components/plugin-defaults).

## Tasks

**Deals** — `deals.Create` creates a deal with `title` (required) plus optional `value`, `currency`, `stageId`, `status`, `expectedCloseDate`, `probability`, `personId`, `orgId`, `userId`, and `customFields`. The output includes `dealId`. `deals.Update` updates a deal by `dealId` — set any of the same fields plus `lostReason`.

**Persons** — `persons.Create` creates a person with `name` (required) plus optional `orgId`, `ownerId`, `emails`, `phones`, `visibleTo`, and `customFields`. The output includes `personId`. `persons.Get` retrieves a person by `personId` — control result handling with `fetchType` (default `FETCH_ONE`).
