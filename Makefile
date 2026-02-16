lint:
	./gradlew ktlintCheck

lintfix:
	./gradlew ktlintFormat

# Deploy the project into maven local
deploy:
	./gradlew publishToMavenLocal

test:
	./gradlew test

install:
	./gradlew clean assembleDist --stacktrace
	unzip -o tiny-cli/build/distributions/tiny-cli-DEV-SNAPSHOT.zip
	rm -rf ~/.bin/tiny-cli
	mv tiny-cli-DEV-SNAPSHOT ~/.bin/tiny-cli

docs: install
	./gradlew tiny-web-editor:tinyWebEditor
	tiny-cli docs --output tiny-doc/src/docs/asciidoc/dependencies/tiny-cli-commands.adoc
	tiny-cli export tiny-sample
	unzip -o -d tiny-doc/src/docs/asciidoc/sample/game-example tiny-sample/tiny-export.zip
	tiny-cli export tiny-cli/src/main/resources/sfx
	unzip -o -d tiny-doc/src/docs/asciidoc/sample/sfx-editor tiny-cli/src/main/resources/sfx/tiny-export.zip
	./gradlew asciidoctor -Pversion=$(uuidgen)

sfx:
	./gradlew :tiny-cli:run --args="run ." -Ptiny.workDir=tiny-cli/src/main/resources/sfx

sample:
	./gradlew :tiny-cli:run --args="run ." -Ptiny.workDir=tiny-sample

test-linux-export:
	./gradlew assembleDist -Pversion=DEV-SNAPSHOT
	docker run --rm \
		-v "$(PWD)/tiny-cli/build/distributions:/dist" \
		-v "$(PWD)/tiny-sample:/tiny-sample" \
		eclipse-temurin:17-jdk-jammy \
		bash -c '\
			apt-get update && apt-get install -y --no-install-recommends fakeroot binutils unzip && \
			cd /tmp && \
			unzip /dist/tiny-cli-DEV-SNAPSHOT.zip && \
			tiny-cli-DEV-SNAPSHOT/bin/tiny-cli export /tiny-sample -p desktop --include-jdk -o /tmp/exported-game-jdk && \
			ls /tmp/exported-game-jdk/*.deb \
		'
