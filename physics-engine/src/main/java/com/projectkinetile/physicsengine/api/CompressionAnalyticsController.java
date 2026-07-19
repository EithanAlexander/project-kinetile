package com.projectkinetile.physicsengine.api;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.projectkinetile.physicsengine.api.analytics.CompressionAnalyticsRowMapper;
import com.projectkinetile.physicsengine.api.analytics.EnergyUnitConverter;
import com.projectkinetile.physicsengine.api.analytics.LedgerSortParser;
import com.projectkinetile.physicsengine.api.validation.QueryParamValidator;
import com.projectkinetile.physicsengine.api.validation.TimeseriesBoundsValidator;
import com.projectkinetile.physicsengine.api.validation.TimeseriesBoundsValidator.TimeseriesBounds;
import com.projectkinetile.physicsengine.config.AppSecurityProperties;
import com.projectkinetile.physicsengine.domain.TileCompressionEventEntity;
import com.projectkinetile.physicsengine.repository.TileCompressionEventLedgerFilterCriteria;
import com.projectkinetile.physicsengine.repository.TileCompressionEventRepository;
import com.projectkinetile.physicsengine.repository.TileCompressionEventSpecifications;
import com.projectkinetile.physicsengine.service.TileCompressionLedgerQueryService;

/**
 * REST controller exposing read-only analytics over persisted piezoelectric tile compression
 * events.
 *
 * <p>Endpoints surface network-wide and per-location energy aggregates, a filterable/paginated
 * event ledger, and daily energy time series (total, by city, and by location).
 * Physics outputs stored in Joules are translated to Watt-hours via {@link EnergyUnitConverter} for device-uptime
 * evaluation.
 *
 * <p>Untrusted query parameters are length-, bound-, and type-validated before reaching the
 * persistence layer; aggregate labels are normalized so the frontend never receives null city or
 * location names.
 *
 * <p>Base path: {@link ApiPaths#ENERGY} ({@code /api/v1/energy}).
 */
@RestController
@RequestMapping(ApiPaths.ENERGY)
public class CompressionAnalyticsController {

  private static final String DEFAULT_LEDGER_SORT = "eventTimestamp,desc";

  private static final String KEY_CITY = "city";
  private static final String KEY_LOCATION = "location";
  private static final String KEY_TOTAL_JOULES = "totalJoules";
  private static final String KEY_TOTAL_COMPRESSIONS = "totalCompressions";
  private static final String KEY_SUCCESSFUL_ACTIVATIONS = "successfulActivations";

  private final TileCompressionEventRepository repository;
  private final TileCompressionLedgerQueryService ledgerQueryService;
  private final AppSecurityProperties appSecurityProperties;

  /**
   * @param repository aggregate and time-series queries over compression events
   * @param ledgerQueryService paginated, specification-based ledger queries
   * @param appSecurityProperties query-length and time-series window limits for untrusted input
   */
  public CompressionAnalyticsController(
      TileCompressionEventRepository repository,
      TileCompressionLedgerQueryService ledgerQueryService,
      AppSecurityProperties appSecurityProperties) {
    this.repository = repository;
    this.ledgerQueryService = ledgerQueryService;
    this.appSecurityProperties = appSecurityProperties;
  }

  /** Lightweight liveness probe confirming the API is reachable. */
  @GetMapping("/ping")
  public String ping() {
    return "Project Kinetile API is live!";
  }

  /**
   * Returns network-wide aggregate metrics across all recorded compression events.
   *
   * @return total compressions, successful activations, and harvested energy in Joules and Wh
   */
  @GetMapping("/summary")
  public CompressionSummaryDTO getNetworkSummary() {
    Map<String, Object> row = repository.aggregateNetworkSummary();
    double totalJoules = CompressionAnalyticsRowMapper.doubleValueOrZero(row.get(KEY_TOTAL_JOULES));
    long totalCompressions =
        CompressionAnalyticsRowMapper.longValueOrZero((Number) row.get(KEY_TOTAL_COMPRESSIONS));
    long successfulActivations =
        CompressionAnalyticsRowMapper.longValueOrZero((Number) row.get(KEY_SUCCESSFUL_ACTIVATIONS));
    return new CompressionSummaryDTO(
        totalCompressions,
        successfulActivations,
        totalJoules,
        EnergyUnitConverter.wattHoursFromJoules(totalJoules));
  }

  /**
   * Returns energy and activation metrics aggregated per city/location chokepoint.
   *
   * <p>Null or blank labels are normalized to {@code "Unknown"}.
   */
  @GetMapping("/locations")
  public List<LocationCompressionMetricsDTO> getMetricsByLocation() {
    return repository.aggregateByLocation().stream()
        .map(
            row -> {
              String city = CompressionAnalyticsRowMapper.normalizeLabel((String) row.get(KEY_CITY));
              String location =
                  CompressionAnalyticsRowMapper.normalizeLabel((String) row.get(KEY_LOCATION));
              double totalJoules =
                  CompressionAnalyticsRowMapper.doubleValueOrZero(row.get(KEY_TOTAL_JOULES));
              return new LocationCompressionMetricsDTO(
                  city,
                  location,
                  totalJoules,
                  EnergyUnitConverter.wattHoursFromJoules(totalJoules),
                  CompressionAnalyticsRowMapper.longValueOrZero(
                      (Number) row.get(KEY_TOTAL_COMPRESSIONS)),
                  CompressionAnalyticsRowMapper.longValueOrZero(
                      (Number) row.get(KEY_SUCCESSFUL_ACTIVATIONS)));
            })
        .toList();
  }

  /**
   * Returns a filterable, paginated ledger of individual compression events.
   *
   * <p>Optional filters: location substring, event time range ({@code since}/{@code until}),
   * harvested energy and impact-multiplier bounds, activation-only flag, and event/tile ID prefixes.
   * Free-text and sort parameters are validated against configured security limits. Page index is
   * clamped to {@code >= 0}; page size to {@code [1, app.api.ledger-max-page-size]}.
   *
   * @param sort whitelisted {@code "property,direction"} expression (defaults to event time desc)
   */
  @GetMapping("/ledger")
  public CompressionLedgerPageDTO getLedger(
      @RequestParam(required = false) String locationContains,
      @RequestParam(required = false) Instant since,
      @RequestParam(required = false) Instant until,
      @RequestParam(required = false) Double minEnergyJoules,
      @RequestParam(required = false) Double maxEnergyJoules,
      @RequestParam(required = false) Double minImpactMultiplier,
      @RequestParam(required = false) Double maxImpactMultiplier,
      @RequestParam(required = false) Boolean activationOnly,
      @RequestParam(required = false) String eventIdPrefix,
      @RequestParam(required = false) String tileIdPrefix,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "50") int size,
      @RequestParam(defaultValue = DEFAULT_LEDGER_SORT) String sort) {

    int maxQueryLen = appSecurityProperties.getApi().getMaxQueryStringLength();
    QueryParamValidator.requireMaxLength(locationContains, "locationContains", maxQueryLen);
    QueryParamValidator.requireMaxLength(eventIdPrefix, "eventIdPrefix", maxQueryLen);
    QueryParamValidator.requireMaxLength(tileIdPrefix, "tileIdPrefix", maxQueryLen);
    QueryParamValidator.requireValidSortParam(sort, maxQueryLen);

    int safePage = Math.max(0, page);
    int safeSize = Math.clamp(size, 1, appSecurityProperties.getApi().getLedgerMaxPageSize());
    Pageable pageable = PageRequest.of(safePage, safeSize, LedgerSortParser.parseLedgerSort(sort));

    Specification<TileCompressionEventEntity> spec =
        TileCompressionEventSpecifications.withOptionalFilters(
            new TileCompressionEventLedgerFilterCriteria(
                locationContains,
                since,
                until,
                minEnergyJoules,
                maxEnergyJoules,
                minImpactMultiplier,
                maxImpactMultiplier,
                activationOnly,
                eventIdPrefix,
                tileIdPrefix));

    Page<TileCompressionEventEntity> result = ledgerQueryService.findLedgerPage(spec, pageable);
    return CompressionLedgerPageDTO.from(result);
  }

  /**
   * Daily time-series buckets for total harvested energy across the network.
   *
   * @param since inclusive start; defaults applied when omitted
   * @param until inclusive end; span capped to {@code app.api.timeseries-max-months}
   */
  @GetMapping("/timeseries/daily/total")
  public List<CompressionDailySeriesBucketDTO> getDailySeriesTotal(
      @RequestParam(required = false) Instant since, @RequestParam(required = false) Instant until) {
    TimeseriesBounds bounds = resolveTimeseriesBounds(since, until);
    return repository.sumJoulesGroupedByDay(bounds.since(), bounds.until()).stream()
        .map(CompressionAnalyticsRowMapper::mapDailyTotalRow)
        .toList();
  }

  /** Daily time-series buckets grouped by city within the validated query window. */
  @GetMapping("/timeseries/daily/by-city")
  public List<CompressionDailySeriesBucketDTO> getDailySeriesByCity(
      @RequestParam(required = false) Instant since, @RequestParam(required = false) Instant until) {
    TimeseriesBounds bounds = resolveTimeseriesBounds(since, until);
    return repository.sumJoulesGroupedByDayAndCity(bounds.since(), bounds.until()).stream()
        .map(CompressionAnalyticsRowMapper::mapDailyCityRow)
        .toList();
  }

  /**
   * Daily time-series buckets for locations within a given city.
   *
   * @param city required city name (non-blank, max length enforced)
   * @throws ResponseStatusException {@code 400} when {@code city} is missing or blank
   */
  @GetMapping("/timeseries/daily/by-location")
  public List<CompressionDailySeriesBucketDTO> getDailySeriesByLocation(
      @RequestParam String city,
      @RequestParam(required = false) Instant since,
      @RequestParam(required = false) Instant until) {
    if (city == null || city.isBlank()) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "city is required");
    }
    QueryParamValidator.requireMaxLength(city, "city", appSecurityProperties.getApi().getMaxQueryStringLength());
    TimeseriesBounds bounds = resolveTimeseriesBounds(since, until);
    return repository
        .sumJoulesGroupedByDayCityAndLocation(bounds.since(), bounds.until(), city)
        .stream()
        .map(CompressionAnalyticsRowMapper::mapDailyLocationRow)
        .toList();
  }

  /** Resolves and caps the inclusive time-series window via {@link TimeseriesBoundsValidator}. */
  private TimeseriesBounds resolveTimeseriesBounds(Instant since, Instant until) {
    return TimeseriesBoundsValidator.resolve(
        since, until, appSecurityProperties.getApi().getTimeseriesMaxMonths());
  }
}
