# Simulated traffic firehose (Poetry deps, no dev groups).
FROM python:3.12-slim
WORKDIR /app
RUN pip install --no-cache-dir poetry==1.8.4
COPY pyproject.toml poetry.lock ./
RUN poetry config virtualenvs.create false \
    && poetry install --only main --no-interaction --no-ansi
COPY src ./src
ENV PYTHONPATH=/app/src
ENV PYTHONUNBUFFERED=1
CMD ["python", "-m", "ingestion_service"]
