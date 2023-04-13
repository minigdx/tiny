lint:
	./gradlew ktlintCheck

lintfix:
	./gradlew ktlintFormat

# Deploy the project into maven local
deploy:
	./gradlew publishToMavenLocal

test:
	./gradlew test

