# Stage 1: Build Angular frontend
FROM node:22-slim AS frontend
WORKDIR /app/frontend
COPY web/frontend/package.json web/frontend/package-lock.json ./
RUN npm ci
COPY web/frontend/ ./
RUN npx ng build --configuration production

# Stage 2: Build Java (bica-reborn library + Spring Boot backend)
FROM eclipse-temurin:21-jdk AS backend
RUN apt-get update && apt-get install -y --no-install-recommends maven graphviz && rm -rf /var/lib/apt/lists/*
WORKDIR /app

# Build bica-reborn dependency first (cacheable layer)
COPY bica/pom.xml bica/pom.xml
RUN cd bica && mvn dependency:go-offline -B || true
COPY bica/ bica/
RUN cd bica && mvn install -DskipTests -B

# Copy Angular build output into Spring Boot static resources
COPY web/backend/pom.xml web/backend/pom.xml
RUN cd web/backend && mvn dependency:go-offline -B || true
COPY web/backend/ web/backend/
COPY --from=frontend /app/frontend/dist/frontend/browser/ web/backend/src/main/resources/static/

# Copy publication PDFs into static resources
COPY papers/publications/step5-lattice/main.pdf web/backend/src/main/resources/static/papers/step5-lattice.pdf
COPY papers/publications/ice-2026/main.pdf web/backend/src/main/resources/static/papers/ice-2026.pdf
COPY papers/publications/ice-2026-oral/main.pdf web/backend/src/main/resources/static/papers/ice-2026-oral.pdf
COPY papers/tools/reticulate-tool/main.pdf web/backend/src/main/resources/static/papers/reticulate-tool.pdf
COPY papers/tools/bica-reborn/main.pdf web/backend/src/main/resources/static/papers/bica-reborn.pdf
COPY papers/tools/pipeline-article/main.pdf web/backend/src/main/resources/static/papers/pipeline-article.pdf
COPY papers/notes/presentation/slides.pdf web/backend/src/main/resources/static/papers/slides.pdf
COPY papers/notes/definitions/definitions.pdf web/backend/src/main/resources/static/papers/definitions.pdf
# Step 1 paper is built locally and committed as a tracked PDF
# If the file doesn't exist, the COPY will fail — rebuild with:
#   cd papers/steps/step1-statespace && pdflatex main.tex && bibtex main && pdflatex main.tex && pdflatex main.tex
COPY papers/steps/step1-statespace/main.pdf web/backend/src/main/resources/static/papers/step1-statespace.pdf

# Build Spring Boot fat JAR
RUN cd web/backend && mvn package -DskipTests -B

# Stage 3: Minimal runtime image
FROM eclipse-temurin:21-jre
RUN apt-get update && apt-get install -y --no-install-recommends graphviz && rm -rf /var/lib/apt/lists/*
WORKDIR /app
COPY --from=backend /app/web/backend/target/bica-web-0.1.0-SNAPSHOT.jar app.jar

ENV SERVER_PORT=8000
EXPOSE 8000

CMD ["java", "-jar", "app.jar"]
