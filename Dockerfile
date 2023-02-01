FROM gradle:7.6-jdk19-alpine

RUN apk add --update --no-cache jq python3 nodejs npm

RUN npm install -g json-schema-faker-cli

VOLUME /home/gradle/src
WORKDIR /home/gradle/src
USER gradle

CMD ["gradle", ":io.basicbich.oui:test", "--rerun", "--no-daemon"]
