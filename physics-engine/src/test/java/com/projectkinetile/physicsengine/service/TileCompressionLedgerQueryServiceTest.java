package com.projectkinetile.physicsengine.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.projectkinetile.physicsengine.domain.TileCompressionEventEntity;
import com.projectkinetile.physicsengine.repository.TileCompressionEventRepository;
import java.util.ArrayList;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;

@ExtendWith(MockitoExtension.class)
@DisplayName("Tile compression ledger query service")
class TileCompressionLedgerQueryServiceTest {

  @Mock private TileCompressionEventRepository repository;

  @InjectMocks private TileCompressionLedgerQueryService service;

  @Test
  @DisplayName("delegates to repository findAll")
  void findLedgerPage_delegatesToRepository() {
    Specification<TileCompressionEventEntity> spec = (root, query, cb) -> cb.conjunction();
    var pageable = PageRequest.of(0, 10);
    when(repository.findAll(spec, pageable)).thenReturn(Page.empty());
    assertThat(service.findLedgerPage(spec, pageable).getTotalElements()).isZero();
  }

  @Test
  @DisplayName("passes the supplied spec and pageable through and returns the repository page")
  void findLedgerPage_passesArgumentsAndReturnsPage() {
    Specification<TileCompressionEventEntity> spec = (root, query, cb) -> cb.conjunction();
    var pageable = PageRequest.of(2, 5);
    var entity = new TileCompressionEventEntity();
    var content = new ArrayList<TileCompressionEventEntity>();
    content.add(entity);
    Page<TileCompressionEventEntity> expected = new PageImpl<>(content, pageable, 11);
    when(repository.findAll(spec, pageable)).thenReturn(expected);

    Page<TileCompressionEventEntity> result = service.findLedgerPage(spec, pageable);

    assertThat(result).isSameAs(expected);
    assertThat(result.getTotalElements()).isEqualTo(11);
    assertThat(result.getContent()).containsExactly(entity);
    verify(repository).findAll(spec, pageable);
  }

  @Test
  @DisplayName("rejects a null spec without touching the repository")
  void findLedgerPage_rejectsNullSpec() {
    assertThatNullPointerException()
        .isThrownBy(() -> service.findLedgerPage(null, PageRequest.of(0, 10)))
        .withMessage("spec");
    verifyNoInteractions(repository);
  }

  @Test
  @DisplayName("rejects a null pageable without touching the repository")
  void findLedgerPage_rejectsNullPageable() {
    Specification<TileCompressionEventEntity> spec = (root, query, cb) -> cb.conjunction();
    assertThatNullPointerException()
        .isThrownBy(() -> service.findLedgerPage(spec, null))
        .withMessage("pageable");
    verifyNoInteractions(repository);
  }
}
