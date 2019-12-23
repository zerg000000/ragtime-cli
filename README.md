# ragtime-cli

Ragtime Commandline. To largely reduce the startup time of db migration lambda.

## Usage

To use it for aws lambda, first add all the migration files to the zip
```sh
# Download the zip
curl -L https://github.com/zerg000000/ragtime-cli/releases/download/v0.0.4/lambda-ubuntu-latest-postgres.zip -o lambda.zip
# If you are using duct framework, add config.edn and migrations/ to the zip file
zip -ur lambda.zip resources/<project>/config.edn resources/migrations/
```

Create Custom Lambda Function using aws-cli

```sh
$ aws lambda create-function --function-name my-function \
--zip-file fileb://lambda.zip --handler index.handler --runtime custom \
--role arn:aws:iam::123456789012:role/lambda-cli-role
```
