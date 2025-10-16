<p align="center">
  <a href="https://www.kestra.io">
    <img src="https://kestra.io/banner.png"  alt="Kestra workflow orchestrator" />
  </a>
</p>

<h1 align="center" style="border-bottom: none">
    Event-Driven Declarative Orchestrator
</h1>

<div align="center">
 <a href="https://github.com/kestra-io/kestra/releases"><img src="https://img.shields.io/github/tag-pre/kestra-io/kestra.svg?color=blueviolet" alt="Last Version" /></a>
  <a href="https://github.com/kestra-io/kestra/blob/develop/LICENSE"><img src="https://img.shields.io/github/license/kestra-io/kestra?color=blueviolet" alt="License" /></a>
  <a href="https://github.com/kestra-io/kestra/stargazers"><img src="https://img.shields.io/github/stars/kestra-io/kestra?color=blueviolet&logo=github" alt="Github star" /></a> <br>
<a href="https://kestra.io"><img src="https://img.shields.io/badge/Website-kestra.io-192A4E?color=blueviolet" alt="Kestra infinitely scalable orchestration and scheduling platform"></a>
<a href="https://kestra.io/slack"><img src="https://img.shields.io/badge/Slack-Join%20Community-blueviolet?logo=slack" alt="Slack"></a>
</div>

<br />

<p align="center">
  <a href="https://twitter.com/kestra_io" style="margin: 0 10px;">
        <img src="https://kestra.io/twitter.svg" alt="twitter" width="35" height="25" /></a>
  <a href="https://www.linkedin.com/company/kestra/" style="margin: 0 10px;">
        <img src="https://kestra.io/linkedin.svg" alt="linkedin" width="35" height="25" /></a>
  <a href="https://www.youtube.com/@kestra-io" style="margin: 0 10px;">
        <img src="https://kestra.io/youtube.svg" alt="youtube" width="35" height="25" /></a>
</p>

<br />

# Kestra Plugin Pipedrive

> A Kestra plugin for integrating with Pipedrive CRM

This plugin provides tasks for interacting with [Pipedrive CRM](https://www.pipedrive.com/), enabling you to automate your sales workflows by managing contacts, deals, notes, and other CRM activities directly from Kestra.

![Kestra orchestrator](https://kestra.io/video.gif)

## Features

✅ **Person Management**
- Create new contacts in Pipedrive
- Retrieve contact information
- Associate contacts with organizations

✅ **Deal Management**
- Create new sales opportunities
- Update deal status and value
- Move deals through pipeline stages
- Link deals to contacts and organizations

✅ **Notes & Activities**
- Add notes to deals, contacts, or organizations
- Pin important notes
- Track customer interactions

✅ **Robust Integration**
- API token authentication
- Automatic retry with exponential backoff
- Rate limit handling
- Comprehensive error handling

## Installation

Add the plugin to your Kestra instance. The plugin will be available in the task list under the Pipedrive category.

## Prerequisites

- A Pipedrive account
- Pipedrive API token (found in Settings → Personal Preferences → API)
- Java 21 or higher (for development)

## Configuration

### API Token

Store your Pipedrive API token as a secret in Kestra:

```yaml
secrets:
  PIPEDRIVE_API_TOKEN: "your-api-token-here"
```

## Available Tasks

### Person Tasks

#### CreatePerson
Creates a new person (contact) in Pipedrive.

```yaml
- id: create_contact
  type: io.kestra.plugin.pipedrive.persons.CreatePerson
  apiToken: "{{ secret('PIPEDRIVE_API_TOKEN') }}"
  name: "John Doe"
  emails:
    - value: "john.doe@example.com"
      primary: true
  phones:
    - value: "+1234567890"
      primary: true
```

#### GetPerson
Retrieves information about a specific person.

```yaml
- id: get_contact
  type: io.kestra.plugin.pipedrive.persons.GetPerson
  apiToken: "{{ secret('PIPEDRIVE_API_TOKEN') }}"
  personId: 123
```

### Deal Tasks

#### CreateDeal
Creates a new deal in Pipedrive.

```yaml
- id: create_opportunity
  type: io.kestra.plugin.pipedrive.deals.CreateDeal
  apiToken: "{{ secret('PIPEDRIVE_API_TOKEN') }}"
  title: "Enterprise Software License"
  value: 50000
  currency: "USD"
  personId: 123
  stageId: 1
```

#### UpdateDeal
Updates an existing deal.

```yaml
- id: update_opportunity
  type: io.kestra.plugin.pipedrive.deals.UpdateDeal
  apiToken: "{{ secret('PIPEDRIVE_API_TOKEN') }}"
  dealId: 456
  status: "won"
  value: 75000
```

### Note Tasks

#### AddNote
Adds a note to a deal, person, or organization.

```yaml
- id: add_customer_note
  type: io.kestra.plugin.pipedrive.notes.AddNote
  apiToken: "{{ secret('PIPEDRIVE_API_TOKEN') }}"
  content: "Customer requested follow-up call next week"
  dealId: 123
  pinnedToDealFlag: true
```

## Complete Workflow Example

Here's a complete example that creates a contact, creates a deal, and adds a note:

```yaml
id: pipedrive_sales_automation
namespace: company.sales

tasks:
  - id: create_person
    type: io.kestra.plugin.pipedrive.persons.CreatePerson
    apiToken: "{{ secret('PIPEDRIVE_API_TOKEN') }}"
    name: "{{ inputs.customer_name }}"
    emails:
      - value: "{{ inputs.customer_email }}"
        primary: true

  - id: create_deal
    type: io.kestra.plugin.pipedrive.deals.CreateDeal
    apiToken: "{{ secret('PIPEDRIVE_API_TOKEN') }}"
    title: "{{ inputs.deal_title }}"
    value: "{{ inputs.deal_value }}"
    currency: "USD"
    personId: "{{ outputs.create_person.personId }}"
    stageId: 1

  - id: add_note
    type: io.kestra.plugin.pipedrive.notes.AddNote
    apiToken: "{{ secret('PIPEDRIVE_API_TOKEN') }}"
    content: "Lead generated from website form"
    dealId: "{{ outputs.create_deal.dealId }}"
    pinnedToDealFlag: true

inputs:
  - id: customer_name
    type: STRING
    required: true
  - id: customer_email
    type: STRING
    required: true
  - id: deal_title
    type: STRING
    required: true
  - id: deal_value
    type: INT
    required: true
```

## Use Cases

- **Lead Automation**: Automatically create contacts and deals from form submissions
- **Sales Pipeline Automation**: Move deals through stages based on customer actions
- **Customer Follow-up**: Add notes and schedule activities based on workflow triggers
- **Data Synchronization**: Sync customer data between Pipedrive and other systems
- **Reporting**: Extract deal data for custom analytics and reporting

## Development

### Running Locally

```bash
# Build the plugin
./gradlew build

# Run tests
./gradlew test

# Run with test coverage
./gradlew test jacocoTestReport
```

### Project Structure

```
src/
├── main/java/io/kestra/plugin/pipedrive/
│   ├── client/          # HTTP client for Pipedrive API
│   ├── models/          # Data models (Person, Deal, Note, etc.)
│   ├── persons/         # Person-related tasks
│   ├── deals/           # Deal-related tasks
│   └── notes/           # Note-related tasks
└── test/
    ├── java/            # Unit tests
    └── resources/flows/ # Example flows
```

## API Documentation

For more information about the Pipedrive API, visit:
- [Pipedrive API Documentation](https://pipedrive.readme.io/docs/api-introduction)
- [Pipedrive API Reference](https://developers.pipedrive.com/docs/api/v1)

## Contributing

Contributions are welcome! Please feel free to submit a Pull Request.

1. Fork the repository
2. Create your feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add some amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

## License

This project is licensed under the Apache License 2.0 - see the [LICENSE](LICENSE) file for details.

## Support

- [Kestra Documentation](https://kestra.io/docs)
- [Kestra Community Slack](https://kestra.io/slack)
- [GitHub Issues](https://github.com/kestra-io/plugin-pipedrive/issues)

---

Built with ❤️ by the Kestra community


### Running tests
```
./gradlew check --parallel
```

### Development

`VSCode`:

Follow the README.md within the `.devcontainer` folder for a quick and easy way to get up and running with developing plugins if you are using VSCode.

`Other IDEs`:

```
./gradlew shadowJar && docker build -t kestra-custom . && docker run --rm -p 8080:8080 kestra-custom server local
```
> [!NOTE]
> You need to relaunch this whole command everytime you make a change to your plugin

go to http://localhost:8080, your plugin will be available to use

## Documentation
* Full documentation can be found under: [kestra.io/docs](https://kestra.io/docs)
* Documentation for developing a plugin is included in the [Plugin Developer Guide](https://kestra.io/docs/plugin-developer-guide/)


## License
Apache 2.0 © [Kestra Technologies](https://kestra.io)


## Stay up to date

We release new versions every month. Give the [main repository](https://github.com/kestra-io/kestra) a star to stay up to date with the latest releases and get notified about future updates.

![Star the repo](https://kestra.io/star.gif)
