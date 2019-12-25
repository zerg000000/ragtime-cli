# ragtime-cli

Ragtime Commandline Tools compiled as executable using GraalVM.

Duct Framework is a great framework overall, especially in development time.
However, it is just a bit too slow when it starts or migrates in production or CI test.
By compiling Ragtime to binary, I could at least save half of my CI/CD time (also money)!
And I hope this tools could give others the same benefit!

## Usage

You can use it as a commandline tools for db migration.

```
# command use environment vars
export DATABASE_URL=jdbc:postgresql://localhost:5432/postgres?user=postgres&password=abcd1234
export RESOURCES_DIR=migrations/
export CONFIG_FILE=resources/config.edn
# test connection
ragtime-cli q "select 1"
# show the diff between db and migrations
ragtime-cli info
# migrate
ragtime-cli migrate-all
# rollback-to
ragtime-cli rollback-to <migration-id>
# rollback-last
ragtime-cli rollback-last <n-migrations>
# help
ragtime-cli --help
```

But it is more useful when used in CI/CD steps or AWS Lambda.
ragtime-cli is already packaged as for lambda deployment.

### Package with function

```sh
# Download the zip
curl -L https://github.com/zerg000000/ragtime-cli/releases/download/v0.0.4/lambda-ubuntu-latest-postgres.zip -o lambda.zip

# If you are using duct framework, add config.edn and migrations/ to the zip file
zip -ur lambda.zip resources/<project>/config.edn resources/migrations/

# Create Lambda function
aws lambda create-function --function-name db-migration \
--zip-file fileb://lambda.zip --handler index.handler --runtime provided \
--environment Variables="{DATABASE_URL=abc,CONFIG_FILE=resources/<project>/config.edn,RESOURCES_DIR=resources/}" \
--role arn:aws:iam::123456789012:role/lambda-cli-role
```

### Use as a layer

```sh
# Download the zip
curl -L https://github.com/zerg000000/ragtime-cli/releases/download/v0.0.4/lambda-ubuntu-latest-postgres.zip -o lambda.zip

# Publish as a layer
aws lambda publish-layer-version --layer-name ragtime-runtime --zip-file fileb://lambda.zip

# Bundle all your migrations files
zip migrations.zip resources/<project>/config.edn resources/migrations/

# Create Lambda function
aws lambda create-function --function-name db-migration \
--layers arn:aws:lambda:us-west-2:123456789012:layer:ragtime-runtime:1 \
--environment Variables="{DATABASE_URL=abc,CONFIG_FILE=resources/<project>/config.edn,RESOURCES_DIR=resources/}" \
 --zip-file fileb://migrations.zip
```


## Limitation

Currently, only support/tested on linux/mac and postgres. If your Duct config is not so common, 
this commandline might not work for you.

## License
Copyright © 2019 Albert Lai

Distributed under the Eclipse Public License either version 1.0 or (at your option) any later version.
