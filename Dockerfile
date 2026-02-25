FROM python:3.12-slim

RUN apt-get update \
    && apt-get install -y --no-install-recommends graphviz \
    && rm -rf /var/lib/apt/lists/*

WORKDIR /app

COPY web/requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt

COPY reticulate/ /app/reticulate/
COPY web/ /app/web/

ENV PYTHONPATH="/app/reticulate:${PYTHONPATH}"

EXPOSE 8000

CMD ["uvicorn", "web.app:app", "--host", "0.0.0.0", "--port", "8000"]
