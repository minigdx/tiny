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
	tiny-cli export tiny-samples/breakout
	unzip -o -d tiny-doc/src/docs/asciidoc/sample/game-example tiny-samples/breakout/tiny-export.zip
	tiny-cli export tiny-samples/home
	unzip -o -d tiny-doc/src/docs/asciidoc/sample/home tiny-samples/home/tiny-export.zip
	tiny-cli export tiny-cli/src/main/resources/sfx
	unzip -o -d tiny-doc/src/docs/asciidoc/sample/sfx-editor tiny-cli/src/main/resources/sfx/tiny-export.zip
	./gradlew asciidoctor -Pversion=$(uuidgen)

# Add a game to the showcase
# Usage: make add game=<path_to_game> [url=<itch.io_url>] [duration=<seconds>]
duration ?= 5
add: install
	@if [ -z "$(game)" ]; then echo "Error: 'game' is required. Usage: make add game=<path_to_game> [url=<itch.io_url>] [duration=<seconds>]"; exit 1; fi
	@command -v ffmpeg >/dev/null 2>&1 || { echo "Error: ffmpeg is required but not installed."; exit 1; }
	@set -e; \
	NAME=$$(basename "$(game)"); \
	GIF_NAME=$$(echo "$$NAME" | tr '[:upper:]' '[:lower:]'); \
	DISPLAY_NAME=$$(echo "$$NAME" | sed 's/[-_]/ /g' | awk '{for(i=1;i<=NF;i++) $$i=toupper(substr($$i,1,1)) tolower(substr($$i,2))}1'); \
	GIF_PATH="tiny-doc/src/docs/asciidoc/sample/$$GIF_NAME.gif"; \
	if grep -q "$$GIF_NAME.gif" README.md; then \
		echo "Warning: $$GIF_NAME.gif already referenced in README.md. Skipping."; \
		exit 0; \
	fi; \
	echo "Recording $$NAME..."; \
	tiny-cli record "$(game)" --headless -d $(duration) -o "/tmp/tiny-raw-$$GIF_NAME.gif"; \
	echo "Scaling to 256x256..."; \
	ffmpeg -i "/tmp/tiny-raw-$$GIF_NAME.gif" -vf "scale=256:256:force_original_aspect_ratio=decrease:flags=neighbor,pad=256:256:(ow-iw)/2:(oh-ih)/2:color=black" -loop 0 -y "$$GIF_PATH"; \
	rm -f "/tmp/tiny-raw-$$GIF_NAME.gif"; \
	echo "Updating README.md..."; \
	awk -v name="$$DISPLAY_NAME" -v gif="./$$GIF_PATH" -v url="$(url)" 'BEGIN{g=0} /Games Made With Tiny/{g=1} {if(g==1 && $$0=="---"){if(url!=""){printf "[![%s](%s)](%s)\n",name,gif,url}else{printf "![%s](%s)\n",name,gif} g=0} print}' README.md > README.md.tmp && mv README.md.tmp README.md; \
	echo "Updating tiny-showcase.adoc..."; \
	if [ -n "$(url)" ]; then \
		ADOC_LINE="image:sample/$$GIF_NAME.gif[$$DISPLAY_NAME - a game made with Tiny game engine,link=$(url)]"; \
	else \
		ADOC_LINE="image:sample/$$GIF_NAME.gif[$$DISPLAY_NAME - a game made with Tiny game engine]"; \
	fi; \
	awk -v line="$$ADOC_LINE" '/^image:/{w=1; print; next} w==1{print line; w=0} {print}' tiny-doc/src/docs/asciidoc/tiny-showcase.adoc > tiny-showcase.adoc.tmp && mv tiny-showcase.adoc.tmp tiny-doc/src/docs/asciidoc/tiny-showcase.adoc; \
	echo "Added $$DISPLAY_NAME to the showcase!"

sfx:
	./gradlew :tiny-cli:run --args="run ." -Ptiny.workDir=tiny-cli/src/main/resources/sfx

sample:
	./gradlew :tiny-cli:run --args="run ." -Ptiny.workDir=tiny-samples/breakout

home:
	./gradlew :tiny-cli:run --args="run ." -Ptiny.workDir=tiny-samples/home

test-linux-export:
	./gradlew assembleDist -Pversion=DEV-SNAPSHOT
	docker run --rm \
		-v "$(PWD)/tiny-cli/build/distributions:/dist" \
		-v "$(PWD)/tiny-samples/breakout:/tiny-sample" \
		eclipse-temurin:17-jdk-jammy \
		bash -c '\
			apt-get update && apt-get install -y --no-install-recommends fakeroot binutils unzip && \
			cd /tmp && \
			unzip /dist/tiny-cli-DEV-SNAPSHOT.zip && \
			tiny-cli-DEV-SNAPSHOT/bin/tiny-cli export /tiny-sample -p desktop --include-jdk -o /tmp/exported-game-jdk && \
			ls /tmp/exported-game-jdk/*.deb \
		'
