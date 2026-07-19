"""Tests for Kafka bootstrap URL resolution."""

from ingestion_service.kafka_settings import kafka_bootstrap_servers


def test_default_localhost(monkeypatch):
    monkeypatch.delenv("KAFKA_BOOTSTRAP_SERVERS", raising=False)
    assert kafka_bootstrap_servers() == "localhost:9092"


def test_env_override(monkeypatch):
    monkeypatch.setenv("KAFKA_BOOTSTRAP_SERVERS", "kafka:9092")
    assert kafka_bootstrap_servers() == "kafka:9092"
