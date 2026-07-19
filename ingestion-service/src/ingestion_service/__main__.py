"""CLI entry point for ``python -m ingestion_service``."""

from ingestion_service.firehose import run_firehose


def main() -> None:
    """Run the simulated traffic firehose."""
    run_firehose()

if __name__ == "__main__":
    main()
