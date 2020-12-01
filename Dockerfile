FROM debian:10 as base

RUN apt-get update && apt-get install -y \
    git \
    nano \
    openjdk-11-jre-headless

RUN useradd user -m -s /bin/bash


FROM debian:10 as builder

RUN apt-get update && apt-get install -y \
    git \
    openjdk-11-jdk-headless

RUN mkdir /build && \
    cd /build && \
    git clone https://github.com/jvff/terminal_proxy terminal-proxy && \
    cd terminal-proxy && \
    ./gradlew assemble && \
    cd /opt && \
    tar -xf /build/terminal-proxy/build/distributions/TerminalProxy.tar


FROM base

ENV PATH=${PATH}:/opt/TerminalProxy/bin

USER user

COPY --from=builder /opt/TerminalProxy /opt/TerminalProxy

RUN echo "source /usr/lib/git-core/git-sh-prompt" >> /home/user/.bashrc && \
    echo "PS1='[\\W\$(__git_ps1 \" (%s)\")]\\$ '" >> /home/user/.bashrc

CMD terminal-proxy
