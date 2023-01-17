# TIS Trainee Credentials

## About
This service issues and verifies trainee digital credentials.

## Developing

### Running

```shell
gradlew bootRun
```

#### Pre-Requisites

#### Environmental Variables

| Name                         | Description                                               | Default   |
|------------------------------|-----------------------------------------------------------|-----------|
| GATEWAY_HOST                 | The credential gateway host.                              |           |
| GATEWAY_CLIENT_ID            | The client ID for the credential gateway.                 |           |
| GATEWAY_CLIENT_SECRET        | The client secret for the credential gateway.             |           |
| GATEWAY_TOKEN_ISSUER         | The issuer to add to the credential data.                 |           |
| GATEWAY_TOKEN_SIGNING_KEY    | The signing key for the credential data.                  |           |
| GATEWAY_ISSUING_REDIRECT_URI | Where the gateway issue should redirect to after issuing. |           |
| SENTRY_DSN                   | A Sentry error monitoring Data Source Name.               |           |
| SENTRY_ENVIRONMENT           | The environment to log Sentry events against.             | local     |
| SIGNATURE_SECRET_KEY         | The secret key used to validate signed data.              |           |

#### Usage Examples

### Testing

The Gradle `test` task can be used to run automated tests and produce coverage
reports.
```shell
gradlew test
```

The Gradle `check` lifecycle task can be used to run automated tests and also
verify formatting conforms to the code style guidelines.
```shell
gradlew check
```

### Building

```shell
gradlew bootBuildImage
```

## Versioning
This project uses [Semantic Versioning](semver.org).

## License
This project is license under [The MIT License (MIT)](LICENSE).
