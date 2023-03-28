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

| Name                              | Description                                                                                                             | Default     |
|-----------------------------------|-------------------------------------------------------------------------------------------------------------------------|-------------|
| AWS_XRAY_DAEMON_ADDRESS | The AWS XRay daemon host. | |
| DB_HOST                           | The MongoDB host to connect to.                                                                                         | localhost   |
| DB_PORT                           | The port to connect to MongoDB on.                                                                                      | 27017       |
| DB_NAME                           | The name of the MongoDB database.                                                                                       | credentials |
| DB_USER                           | The username to access the MongoDB instance.                                                                            | admin       |
| DB_PASSWORD                       | The password to access the MongoDB instance.                                                                            | pwd         |
| ENVIRONMENT                | The environment to log events against.                                                                           | local       |
| GATEWAY_HOST                      | The credential gateway host.                                                                                            |             |
| GATEWAY_CLIENT_ID                 | The client ID for the credential gateway.                                                                               |             |
| GATEWAY_CLIENT_SECRET             | The client secret for the credential gateway.                                                                           |             |
| GATEWAY_TOKEN_SIGNING_KEY         | The Base64 encoded signing key for the credential data.                                                                 |             |
| GATEWAY_ISSUING_REDIRECT_URI      | Where the gateway issue should redirect to after issuing.                                                               |             |
| GATEWAY_VERIFICATION_REDIRECT_URI | Where the gateway issue should redirect to verify a supplied credential, e.g. `<host>/api/credentials/verify/callback`. |             |
| SENTRY_DSN                        | A Sentry error monitoring Data Source Name.                                                                             |             |
| SIGNATURE_SECRET_KEY              | The secret key used to validate signed data.                                                                            |             |

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
