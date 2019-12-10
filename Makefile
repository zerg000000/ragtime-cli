build-native-image:
	docker build --target builder -t graalvm-compiler .
	docker rm build || true
	docker create --name build graalvm-compiler
	docker cp build:/usr/src/app/target/app app
	docker cp build:/usr/src/app/libsunec.so libsunec.so

build-lambda-zip:
	docker build --target archiver -t lambda-runtime-archiver .
	docker rm build || true
	docker create --name build lambda-runtime-archiver
	docker cp build:/usr/src/app/function.zip function.zip
