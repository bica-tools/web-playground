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
# These files exist locally but are gitignored; the glob trick (pd[f])
# silently skips missing files so CI builds succeed without PDFs.
COPY papers/reticulate-tool/main.pd[f] /app/web/static/papers/reticulate-tool.pdf
COPY papers/presentation/slides.pd[f] /app/web/static/papers/slides.pdf
COPY papers/definitions/definitions.pd[f] /app/web/static/papers/definitions.pdf

ENV PYTHONPATH="/app/reticulate:${PYTHONPATH}"
ENV SITE_PASSWORD=""

EXPOSE 8000

CMD ["uvicorn", "web.app:app", "--host", "0.0.0.0", "--port", "8000"]
