# Copyright 2024 tison <wander4096@gmail.com>
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

FROM public.ecr.aws/docker/library/rust:1.81.0-alpine3.20 as build
ENV RUSTFLAGS="-C target-feature=-crt-static"
WORKDIR /build
COPY . .
RUN apk fix && \
    apk --no-cache --update add git musl-dev && \
    cargo build --release --bin hawkeye

FROM public.ecr.aws/docker/library/alpine:3.20.0
COPY --from=build /build/target/release/hawkeye /bin/
RUN apk fix && apk --no-cache --update add libgcc git
WORKDIR /github/workspace/
ENTRYPOINT ["/bin/hawkeye"]
