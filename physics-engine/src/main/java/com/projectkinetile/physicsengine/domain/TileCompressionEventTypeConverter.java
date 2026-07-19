package com.projectkinetile.physicsengine.domain;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/** JPA converter for {@link TileCompressionEventType} stored as lowercase wire strings. */
@Converter(autoApply = true)
public class TileCompressionEventTypeConverter
    implements AttributeConverter<TileCompressionEventType, String> {

  @Override
  public String convertToDatabaseColumn(TileCompressionEventType attribute) {
    return attribute == null ? null : attribute.toWireValue();
  }

  @Override
  public TileCompressionEventType convertToEntityAttribute(String dbData) {
    return dbData == null ? null : TileCompressionEventType.fromString(dbData);
  }
}
