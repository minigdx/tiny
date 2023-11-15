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
	./gradlew clean assembleDist
	unzip tiny-cli/build/distributions/tiny-cli-DEV-SNAPSHOT.zip
	rm -rf ~/.bin/tiny-cli
	mv tiny-cli-DEV-SNAPSHOT ~/.bin/tiny-cli

docs: install
	./gradlew tiny-web-editor:tinyWebEditor
	tiny-cli export tiny-sample
	unzip -o -d tiny-doc/src/docs/asciidoc/sample tiny-sample/tiny-export.zip
	./gradlew asciidoctor -Pversion=$(uuidgen)
