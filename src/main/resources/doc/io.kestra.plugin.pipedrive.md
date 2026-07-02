# How to use the Pipedrive plugin

Manage deals and persons in Pipedrive from Kestra flows.

## Authentication

Set `apiToken` to your Pipedrive API token. Store it in a [secret](https://kestra.io/docs/concepts/secret) and apply it globally with [plugin defaults](https://kestra.io/docs/workflow-components/plugin-defaults).

## Tasks

**Deals** — `deals.Create` creates a deal with `title` (required) plus optional `value`, `currency`, `stageId`, `status`, `expectedCloseDate`, `probability`, `personId`, `orgId`, `userId`, and `customFields`. The output includes `dealId`. `deals.Update` updates a deal by `dealId` — set any of the same fields plus `lostReason`. `deals.Get` retrieves a deal by `dealId` — control result handling with `fetchType` (default `FETCH_ONE`). `deals.List` searches deals with field filters (`filterId`, `personId`, `orgId`, `pipelineId`, `stageId`, `status`, `ownerId`, `limit`, `cursor`) — `fetchType` is restricted to `FETCH` (default) or `STORE`, and the output's `nextCursor` can be fed back into `cursor` on a later run to page through results. `deals.Delete` removes a deal by `dealId`; Pipedrive only soft-deletes it (moved to trash, purged after 30 days), so for a normal close-out use `deals.Update` with `status: "won"` or `status: "lost"` instead — `Delete` is a cleanup tool.

**Persons** — `persons.Create` creates a person with `name` (required) plus optional `orgId`, `ownerId`, `emails`, `phones`, `visibleTo`, and `customFields`. The output includes `personId`. `persons.Update` updates a person by `personId` — set any of the same optional fields. `persons.Get` retrieves a person by `personId` — control result handling with `fetchType` (default `FETCH_ONE`). `persons.List` searches persons with field filters (`filterId`, `orgId`, `ownerId`, `limit`, `cursor`) — `fetchType` is restricted to `FETCH` (default) or `STORE`, with the same cursor-based pagination as `deals.List`. `persons.Delete` removes a person by `personId`; Pipedrive only soft-deletes it (moved to trash, purged after 30 days).
