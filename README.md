# TIS Trainee Credentials

## About
This service issues and verifies trainee digital credentials.

## TODO
 - Sonar:
    - Add repository to SonarCloud.
    - Add SonarCloud API key to repository secrets.

## Developing

### Running

```shell
gradlew bootRun
```

#### Pre-Requisites

#### Environmental Variables

| Name               | Description                                     | Default   |
|--------------------|-------------------------------------------------|-----------|
| SENTRY_DSN         | A Sentry error monitoring Data Source Name.     |           |
| SENTRY_ENVIRONMENT | The environment to log Sentry events against.   | local     |

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
