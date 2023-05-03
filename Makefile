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
	./gradlew clean distZip
	unzip tiny-cli/build/distributions/tiny-cli-DEV-SNAPSHOT.zip
	rm -rf ~/.bin/tiny-cli
	mv tiny-cli-DEV-SNAPSHOT ~/.bin/tiny-cli
