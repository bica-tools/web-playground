FROM python:3.12-slim

RUN apt-get update \
    && apt-get install -y --no-install-recommends graphviz \
    && rm -rf /var/lib/apt/lists/*

WORKDIR /app

COPY web/requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt

COPY reticulate/ /app/reticulate/
COPY web/ /app/web/

# Copy paper PDFs into the static directory for serving.
# These files exist locally but are gitignored, so the directories may
# not exist in CI. Use a shell conditional to skip missing files.
COPY papers/ /tmp/papers/
RUN mkdir -p /app/web/static/papers && \
    cp /tmp/papers/reticulate-tool/main.pdf /app/web/static/papers/reticulate-tool.pdf 2>/dev/null || true && \
    cp /tmp/papers/presentation/slides.pdf /app/web/static/papers/slides.pdf 2>/dev/null || true && \
    cp /tmp/papers/definitions/definitions.pdf /app/web/static/papers/definitions.pdf 2>/dev/null || true && \
    rm -rf /tmp/papers

ENV PYTHONPATH="/app/reticulate:${PYTHONPATH}"
ENV SITE_PASSWORD=""

EXPOSE 8000

CMD ["uvicorn", "web.app:app", "--host", "0.0.0.0", "--port", "8000"]
