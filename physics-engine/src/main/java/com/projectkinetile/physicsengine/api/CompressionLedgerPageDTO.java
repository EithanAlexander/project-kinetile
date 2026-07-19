package com.projectkinetile.physicsengine.api;

import java.util.List;

import org.springframework.data.domain.Page;

import com.projectkinetile.physicsengine.domain.TileCompressionEventEntity;

/** Paginated compression ledger API payload. */
public record CompressionLedgerPageDTO(
    List<CompressionLedgerEntryDTO> content,
    long totalElements,
    int totalPages,
    int page,
    int size,
    boolean first,
    boolean last) {

  public static CompressionLedgerPageDTO from(Page<TileCompressionEventEntity> page) {
    return new CompressionLedgerPageDTO(
        page.getContent().stream().map(CompressionLedgerEntryDTO::from).toList(),
        page.getTotalElements(),
        page.getTotalPages(),
        page.getNumber(),
        page.getSize(),
        page.isFirst(),
        page.isLast());
  }
}
