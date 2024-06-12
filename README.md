# Backend of GPS-Tools based on Spring/Kotlin

Used as backend (REST-API) for [GPS-Tools Frontend](https://github.com/devshred/gps-tools-frontend).

## Prod-environment
The prod "server" is running at home on a [Raspberry Pi](https://www.raspberrypi.com/products/raspberry-pi-4-model-b/).

The prod website is hosted by [Cloudflare](https://www.cloudflare.com/) at: https://gps-tools.pages.dev/ 

## Features
* imports data as [FIT](https://developer.garmin.com/fit/overview/), [GPX](https://www.topografix.com/gpx.asp) and [GeoJSON](https://geojson.org/)
* exports data as [TCX](https://www8.garmin.com/xmlschemas/TrainingCenterDatabasev2.xsd), [GPX](https://www.topografix.com/gpx.asp) and [GeoJSON](https://geojson.org/)
* converts seamlessly between these formats
* optimizes waypoints to improve performance on GPS-devices
* merges files
* stores data locally as [proto3-files](https://protobuf.dev/).

Currently, no persisted storage is used, but the data is stored in memory. Every time the backend is restarted, all data is lost.

## OpenAPI spec
The backend is based on an [OpenAPI spec](https://editor.swagger.io/?url=https://raw.githubusercontent.com/devshred/gps-tools-backend/main/src/main/spec/api-spec.yaml) and interfaces are generated by the [kotlin-spring Generator](https://openapi-generator.tech/docs/generators/kotlin-spring/).
_(The errors shown in the Swagger-editor are the result of a workaround to work around a [bug at the OpenAPI generator](https://github.com/OpenAPITools/openapi-generator/issues/8333).)_

## How-to run
```shell
./gradlew bootRun
```
## How-to test
There are different ways to test the application
* start the [frontend](https://github.com/devshred/gps-tools-frontend)
* use the REST-API with [some example requests](src/test/http/gps-files.http)
* run all tests:
```shell
./gradlew test
```

## Release process
This project is using [semantic versioning](https://semver.org/) and [conventional commits](https://www.conventionalcommits.org/en/v1.0.0/).
Codequality will be checked by [ktlint](https://github.com/pinterest/ktlint).

To add some code:
* create branch, do some changes
* commit messages should respect [Angular Commit Guidelines](https://github.com/angular/angular/blob/main/CONTRIBUTING.md#-commit-message-format)

### create tag and increase SNAPSHOT-number locally
```shell
./gradlew release
```
### create release on GitHub
* rebase to main branch
* release manually
* a GitHub Actions workflow will build a new Docker image automatically
