package com.projectkinetile.physicsengine.service;

import com.projectkinetile.physicsengine.domain.TileCompressionEventEntity;
import com.projectkinetile.physicsengine.repository.TileCompressionEventRepository;
import java.util.Objects;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

/** Paginated compression ledger reads. */
@Service
public class TileCompressionLedgerQueryService {

  private final TileCompressionEventRepository repository;

  public TileCompressionLedgerQueryService(TileCompressionEventRepository repository) {
    this.repository = repository;
  }

  /**
   * Returns a single page of ledger events matching the given filter specification.
   *
   * @param spec the query that selects which rows to return, typically built from a
   *     {@code TileCompressionEventLedgerFilterCriteria}. For example, to fetch only events in
   *     "Berlin" with at least 0.5 J of harvested energy:
   *     <pre>{@code
   * var criteria = new TileCompressionEventLedgerFilterCriteria(
   *     "Berlin", null, null, 0.5, null, null, null, null, null);
   * Specification<TileCompressionEventEntity> spec =
   *     TileCompressionEventSpecifications.withOptionalFilters(criteria);
   * }</pre>
   * @param pageable the page slice and sort order to fetch (e.g. {@code PageRequest.of(0, 20,
   *     Sort.by("eventTimestamp").descending())} for the 20 newest events)
   * @return the matching page of events
   */
  public Page<TileCompressionEventEntity> findLedgerPage(
      Specification<TileCompressionEventEntity> spec, Pageable pageable) {
    Objects.requireNonNull(spec, "spec");
    Objects.requireNonNull(pageable, "pageable");
    return repository.findAll(spec, pageable);
  }
}
