# Copyright 2023 Korandoru Contributors
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

FROM public.ecr.aws/docker/library/rust:1.76.0-alpine3.19 as rust-build
ENV LANG C.utf8
ENV RUSTFLAGS="-C target-feature=-crt-static"
WORKDIR /build
COPY . .
RUN apk fix && apk --no-cache --update add musl-dev && \
    cd hawkeye-core/native && cargo build --release

FROM public.ecr.aws/docker/library/eclipse-temurin:21-jdk-alpine as build
WORKDIR /build
COPY . .
RUN ./mvnw -B -ntp clean package -DskipTests

FROM public.ecr.aws/docker/library/eclipse-temurin:21-jre-alpine
ENV JAVA_OPTS="-Djava.library.path=/lib"
RUN apk fix && apk --no-cache --update add git && \
    git config --global --add safe.directory /github/workspace
COPY --from=rust-build /build/hawkeye-core/native/target/release/libhawkeye_core.so /lib/
COPY --from=build /build/hawkeye-cli/target/hawkeye.jar /bin/hawkeye
WORKDIR /github/workspace/
ENTRYPOINT ["/bin/hawkeye"]
