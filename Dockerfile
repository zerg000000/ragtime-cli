FROM tokenmill/clojure:graalvm-ce-19.0.0-tools-deps-1.10.0.442 as builder

RUN mkdir -p /usr/src/app
WORKDIR /usr/src/app

COPY deps.edn /usr/src/app/
COPY . /usr/src/app
RUN clojure -R:native-image
RUN clojure -A:native-image
RUN cp $JAVA_HOME/jre/lib/amd64/libsunec.so .
RUN cp target/app app
RUN cp bin/bootstrap bootstrap
RUN chmod 755 app bootstrap


FROM amazonlinux:2 as archiver

RUN yum -y install zip

WORKDIR /usr/src/app
COPY --from=builder /usr/src/app/bootstrap bootstrap
COPY --from=builder /usr/src/app/app app
COPY --from=builder /usr/src/app/libsunec.so libsunec.so
RUN zip function.zip bootstrap app libsunec.so
