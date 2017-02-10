/**
 * Autogenerated by Avro
 *
 * DO NOT EDIT DIRECTLY
 */
package com.redsaz.meterrier.importer.model;

import org.apache.avro.specific.SpecificData;

@SuppressWarnings("all")
@org.apache.avro.specific.AvroGenerated
public class Metadata extends org.apache.avro.specific.SpecificRecordBase implements org.apache.avro.specific.SpecificRecord {
  private static final long serialVersionUID = -8798755654355775569L;
  public static final org.apache.avro.Schema SCHEMA$ = new org.apache.avro.Schema.Parser().parse("{\"type\":\"record\",\"name\":\"Metadata\",\"namespace\":\"com.redsaz.meterrier.importer.model\",\"fields\":[{\"name\":\"earliestMillisUtc\",\"type\":\"long\"},{\"name\":\"LatestMillisUtc\",\"type\":\"long\"},{\"name\":\"totalEntries\",\"type\":\"long\"}]}");
  public static org.apache.avro.Schema getClassSchema() { return SCHEMA$; }
  @Deprecated public long earliestMillisUtc;
  @Deprecated public long LatestMillisUtc;
  @Deprecated public long totalEntries;

  /**
   * Default constructor.  Note that this does not initialize fields
   * to their default values from the schema.  If that is desired then
   * one should use <code>newBuilder()</code>.
   */
  public Metadata() {}

  /**
   * All-args constructor.
   * @param earliestMillisUtc The new value for earliestMillisUtc
   * @param LatestMillisUtc The new value for LatestMillisUtc
   * @param totalEntries The new value for totalEntries
   */
  public Metadata(java.lang.Long earliestMillisUtc, java.lang.Long LatestMillisUtc, java.lang.Long totalEntries) {
    this.earliestMillisUtc = earliestMillisUtc;
    this.LatestMillisUtc = LatestMillisUtc;
    this.totalEntries = totalEntries;
  }

  public org.apache.avro.Schema getSchema() { return SCHEMA$; }
  // Used by DatumWriter.  Applications should not call.
  public java.lang.Object get(int field$) {
    switch (field$) {
    case 0: return earliestMillisUtc;
    case 1: return LatestMillisUtc;
    case 2: return totalEntries;
    default: throw new org.apache.avro.AvroRuntimeException("Bad index");
    }
  }

  // Used by DatumReader.  Applications should not call.
  @SuppressWarnings(value="unchecked")
  public void put(int field$, java.lang.Object value$) {
    switch (field$) {
    case 0: earliestMillisUtc = (java.lang.Long)value$; break;
    case 1: LatestMillisUtc = (java.lang.Long)value$; break;
    case 2: totalEntries = (java.lang.Long)value$; break;
    default: throw new org.apache.avro.AvroRuntimeException("Bad index");
    }
  }

  /**
   * Gets the value of the 'earliestMillisUtc' field.
   * @return The value of the 'earliestMillisUtc' field.
   */
  public java.lang.Long getEarliestMillisUtc() {
    return earliestMillisUtc;
  }

  /**
   * Sets the value of the 'earliestMillisUtc' field.
   * @param value the value to set.
   */
  public void setEarliestMillisUtc(java.lang.Long value) {
    this.earliestMillisUtc = value;
  }

  /**
   * Gets the value of the 'LatestMillisUtc' field.
   * @return The value of the 'LatestMillisUtc' field.
   */
  public java.lang.Long getLatestMillisUtc() {
    return LatestMillisUtc;
  }

  /**
   * Sets the value of the 'LatestMillisUtc' field.
   * @param value the value to set.
   */
  public void setLatestMillisUtc(java.lang.Long value) {
    this.LatestMillisUtc = value;
  }

  /**
   * Gets the value of the 'totalEntries' field.
   * @return The value of the 'totalEntries' field.
   */
  public java.lang.Long getTotalEntries() {
    return totalEntries;
  }

  /**
   * Sets the value of the 'totalEntries' field.
   * @param value the value to set.
   */
  public void setTotalEntries(java.lang.Long value) {
    this.totalEntries = value;
  }

  /**
   * Creates a new Metadata RecordBuilder.
   * @return A new Metadata RecordBuilder
   */
  public static com.redsaz.meterrier.importer.model.Metadata.Builder newBuilder() {
    return new com.redsaz.meterrier.importer.model.Metadata.Builder();
  }

  /**
   * Creates a new Metadata RecordBuilder by copying an existing Builder.
   * @param other The existing builder to copy.
   * @return A new Metadata RecordBuilder
   */
  public static com.redsaz.meterrier.importer.model.Metadata.Builder newBuilder(com.redsaz.meterrier.importer.model.Metadata.Builder other) {
    return new com.redsaz.meterrier.importer.model.Metadata.Builder(other);
  }

  /**
   * Creates a new Metadata RecordBuilder by copying an existing Metadata instance.
   * @param other The existing instance to copy.
   * @return A new Metadata RecordBuilder
   */
  public static com.redsaz.meterrier.importer.model.Metadata.Builder newBuilder(com.redsaz.meterrier.importer.model.Metadata other) {
    return new com.redsaz.meterrier.importer.model.Metadata.Builder(other);
  }

  /**
   * RecordBuilder for Metadata instances.
   */
  public static class Builder extends org.apache.avro.specific.SpecificRecordBuilderBase<Metadata>
    implements org.apache.avro.data.RecordBuilder<Metadata> {

    private long earliestMillisUtc;
    private long LatestMillisUtc;
    private long totalEntries;

    /** Creates a new Builder */
    private Builder() {
      super(SCHEMA$);
    }

    /**
     * Creates a Builder by copying an existing Builder.
     * @param other The existing Builder to copy.
     */
    private Builder(com.redsaz.meterrier.importer.model.Metadata.Builder other) {
      super(other);
      if (isValidValue(fields()[0], other.earliestMillisUtc)) {
        this.earliestMillisUtc = data().deepCopy(fields()[0].schema(), other.earliestMillisUtc);
        fieldSetFlags()[0] = true;
      }
      if (isValidValue(fields()[1], other.LatestMillisUtc)) {
        this.LatestMillisUtc = data().deepCopy(fields()[1].schema(), other.LatestMillisUtc);
        fieldSetFlags()[1] = true;
      }
      if (isValidValue(fields()[2], other.totalEntries)) {
        this.totalEntries = data().deepCopy(fields()[2].schema(), other.totalEntries);
        fieldSetFlags()[2] = true;
      }
    }

    /**
     * Creates a Builder by copying an existing Metadata instance
     * @param other The existing instance to copy.
     */
    private Builder(com.redsaz.meterrier.importer.model.Metadata other) {
            super(SCHEMA$);
      if (isValidValue(fields()[0], other.earliestMillisUtc)) {
        this.earliestMillisUtc = data().deepCopy(fields()[0].schema(), other.earliestMillisUtc);
        fieldSetFlags()[0] = true;
      }
      if (isValidValue(fields()[1], other.LatestMillisUtc)) {
        this.LatestMillisUtc = data().deepCopy(fields()[1].schema(), other.LatestMillisUtc);
        fieldSetFlags()[1] = true;
      }
      if (isValidValue(fields()[2], other.totalEntries)) {
        this.totalEntries = data().deepCopy(fields()[2].schema(), other.totalEntries);
        fieldSetFlags()[2] = true;
      }
    }

    /**
      * Gets the value of the 'earliestMillisUtc' field.
      * @return The value.
      */
    public java.lang.Long getEarliestMillisUtc() {
      return earliestMillisUtc;
    }

    /**
      * Sets the value of the 'earliestMillisUtc' field.
      * @param value The value of 'earliestMillisUtc'.
      * @return This builder.
      */
    public com.redsaz.meterrier.importer.model.Metadata.Builder setEarliestMillisUtc(long value) {
      validate(fields()[0], value);
      this.earliestMillisUtc = value;
      fieldSetFlags()[0] = true;
      return this;
    }

    /**
      * Checks whether the 'earliestMillisUtc' field has been set.
      * @return True if the 'earliestMillisUtc' field has been set, false otherwise.
      */
    public boolean hasEarliestMillisUtc() {
      return fieldSetFlags()[0];
    }


    /**
      * Clears the value of the 'earliestMillisUtc' field.
      * @return This builder.
      */
    public com.redsaz.meterrier.importer.model.Metadata.Builder clearEarliestMillisUtc() {
      fieldSetFlags()[0] = false;
      return this;
    }

    /**
      * Gets the value of the 'LatestMillisUtc' field.
      * @return The value.
      */
    public java.lang.Long getLatestMillisUtc() {
      return LatestMillisUtc;
    }

    /**
      * Sets the value of the 'LatestMillisUtc' field.
      * @param value The value of 'LatestMillisUtc'.
      * @return This builder.
      */
    public com.redsaz.meterrier.importer.model.Metadata.Builder setLatestMillisUtc(long value) {
      validate(fields()[1], value);
      this.LatestMillisUtc = value;
      fieldSetFlags()[1] = true;
      return this;
    }

    /**
      * Checks whether the 'LatestMillisUtc' field has been set.
      * @return True if the 'LatestMillisUtc' field has been set, false otherwise.
      */
    public boolean hasLatestMillisUtc() {
      return fieldSetFlags()[1];
    }


    /**
      * Clears the value of the 'LatestMillisUtc' field.
      * @return This builder.
      */
    public com.redsaz.meterrier.importer.model.Metadata.Builder clearLatestMillisUtc() {
      fieldSetFlags()[1] = false;
      return this;
    }

    /**
      * Gets the value of the 'totalEntries' field.
      * @return The value.
      */
    public java.lang.Long getTotalEntries() {
      return totalEntries;
    }

    /**
      * Sets the value of the 'totalEntries' field.
      * @param value The value of 'totalEntries'.
      * @return This builder.
      */
    public com.redsaz.meterrier.importer.model.Metadata.Builder setTotalEntries(long value) {
      validate(fields()[2], value);
      this.totalEntries = value;
      fieldSetFlags()[2] = true;
      return this;
    }

    /**
      * Checks whether the 'totalEntries' field has been set.
      * @return True if the 'totalEntries' field has been set, false otherwise.
      */
    public boolean hasTotalEntries() {
      return fieldSetFlags()[2];
    }


    /**
      * Clears the value of the 'totalEntries' field.
      * @return This builder.
      */
    public com.redsaz.meterrier.importer.model.Metadata.Builder clearTotalEntries() {
      fieldSetFlags()[2] = false;
      return this;
    }

    @Override
    public Metadata build() {
      try {
        Metadata record = new Metadata();
        record.earliestMillisUtc = fieldSetFlags()[0] ? this.earliestMillisUtc : (java.lang.Long) defaultValue(fields()[0]);
        record.LatestMillisUtc = fieldSetFlags()[1] ? this.LatestMillisUtc : (java.lang.Long) defaultValue(fields()[1]);
        record.totalEntries = fieldSetFlags()[2] ? this.totalEntries : (java.lang.Long) defaultValue(fields()[2]);
        return record;
      } catch (Exception e) {
        throw new org.apache.avro.AvroRuntimeException(e);
      }
    }
  }

  private static final org.apache.avro.io.DatumWriter
    WRITER$ = new org.apache.avro.specific.SpecificDatumWriter(SCHEMA$);

  @Override public void writeExternal(java.io.ObjectOutput out)
    throws java.io.IOException {
    WRITER$.write(this, SpecificData.getEncoder(out));
  }

  private static final org.apache.avro.io.DatumReader
    READER$ = new org.apache.avro.specific.SpecificDatumReader(SCHEMA$);

  @Override public void readExternal(java.io.ObjectInput in)
    throws java.io.IOException {
    READER$.read(this, SpecificData.getDecoder(in));
  }

}