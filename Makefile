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
