package com.redsaz.lognition.api.model;

import java.util.Objects;

public class Sample implements Comparable<Sample> {

  private long offset;
  private long duration;
  private String label;
  private String threadName;
  private String statusCode;
  private String statusMessage;
  private boolean success;
  private long responseBytes;
  private int totalThreads;

  public Sample() {}

  /**
   * All-args constructor.
   *
   * @param offset The new value for offset
   * @param duration The new value for duration
   * @param label The new value for labelRef
   * @param threadName The new value for threadNameRef
   * @param statusCode The new value for statusCode
   * @param statusMessage The new value for statusMessage
   * @param success The new value for success
   * @param responseBytes The new value for responseBytes
   * @param totalThreads The new value for totalThreads
   */
  public Sample(
      long offset,
      long duration,
      String label,
      String threadName,
      String statusCode,
      String statusMessage,
      boolean success,
      long responseBytes,
      int totalThreads) {
    this.offset = offset;
    this.duration = duration;
    this.label = label;
    this.threadName = threadName;
    this.statusCode = statusCode;
    this.success = success;
    this.responseBytes = responseBytes;
    this.totalThreads = totalThreads;
  }

  /**
   * Gets the value of the 'millisOffset' field.
   *
   * @return The value of the 'millisOffset' field.
   */
  public long getOffset() {
    return offset;
  }

  /**
   * Sets the value of the 'offset' field.
   *
   * @param value the value to set.
   */
  public void setOffset(long value) {
    this.offset = value;
  }

  /**
   * Gets the value of the 'duration' field.
   *
   * @return The value of the 'duration' field.
   */
  public long getDuration() {
    return duration;
  }

  /**
   * Sets the value of the 'duration' field.
   *
   * @param value the value to set.
   */
  public void setDuration(long value) {
    this.duration = value;
  }

  /**
   * Gets the value of the 'label' field.
   *
   * @return The value of the 'label' field.
   */
  public String getLabel() {
    return label;
  }

  /**
   * Sets the value of the 'label' field.
   *
   * @param value the value to set.
   */
  public void setLabel(String value) {
    this.label = value;
  }

  /**
   * Gets the value of the 'threadName' field.
   *
   * @return The value of the 'threadName' field.
   */
  public String getThreadName() {
    return threadName;
  }

  /**
   * Sets the value of the 'threadNameRef' field.
   *
   * @param value the value to set.
   */
  public void setThreadName(String value) {
    this.threadName = value;
  }

  /**
   * Gets the value of the 'statusCode' field.
   *
   * @return The value of the 'statusCode' field.
   */
  public String getStatusCode() {
    return statusCode;
  }

  /**
   * Sets the value of the 'statusCode' field.
   *
   * @param value the value to set.
   */
  public void setStatusCode(String value) {
    this.statusCode = value;
  }

  /**
   * Gets the value of the 'statusMessage' field.
   *
   * @return The value of the 'statusMessage' field.
   */
  public String getStatusMessage() {
    return statusMessage;
  }

  /**
   * Sets the value of the 'statusCode' field.
   *
   * @param value the value to set.
   */
  public void setStatusMessage(String value) {
    this.statusMessage = value;
  }

  /**
   * Gets the value of the 'success' field.
   *
   * @return The value of the 'success' field.
   */
  public boolean isSuccess() {
    return success;
  }

  /**
   * Sets the value of the 'success' field.
   *
   * @param value the value to set.
   */
  public void setSuccess(boolean value) {
    this.success = value;
  }

  /**
   * Gets the value of the 'responseBytes' field.
   *
   * @return The value of the 'responseBytes' field.
   */
  public long getResponseBytes() {
    return responseBytes;
  }

  /**
   * Sets the value of the 'responseBytes' field.
   *
   * @param value the value to set.
   */
  public void setResponseBytes(long value) {
    this.responseBytes = value;
  }

  /**
   * Gets the value of the 'totalThreads' field.
   *
   * @return The value of the 'totalThreads' field.
   */
  public int getTotalThreads() {
    return totalThreads;
  }

  /**
   * Sets the value of the 'totalThreads' field.
   *
   * @param value the value to set.
   */
  public void setTotalThreads(int value) {
    this.totalThreads = value;
  }

  @Override
  public int compareTo(Sample obj) {
    if (this == obj) {
      return 0;
    } else if (obj == null) {
      return 1;
    } else if (this.offset < obj.offset) {
      return -1;
    } else if (this.offset > obj.offset) {
      return 1;
    } else if (this.duration < obj.duration) {
      return -1;
    } else if (this.duration > obj.duration) {
      return 1;
    } else if (this.label == null && obj.label != null) {
      return -1;
    } else if (this.label != null && obj.label == null) {
      return 1;
    }
    int comp = this.label.compareTo(obj.label);
    if (comp != 0) {
      return comp;
    }
    if (this.threadName == null && obj.threadName != null) {
      return -1;
    } else if (this.threadName != null && obj.threadName == null) {
      return 1;
    }
    comp = this.threadName.compareTo(obj.threadName);
    if (comp != 0) {
      return comp;
    }
    if (this.responseBytes < obj.responseBytes) {
      return -1;
    } else if (this.responseBytes > obj.responseBytes) {
      return 1;
    }
    comp = this.statusCode.compareTo(obj.statusCode);
    if (comp != 0) {
      return comp;
    }
    comp = this.statusMessage.compareTo(obj.statusMessage);
    if (comp != 0) {
      return comp;
    }
    if (this.success != obj.success) {
      if (this.success) {
        return -1;
      } else {
        return 1;
      }
    }
    if (this.totalThreads < obj.totalThreads) {
      return -1;
    } else if (this.totalThreads > obj.totalThreads) {
      return 1;
    }
    return 0;
  }

  @Override
  public int hashCode() {
    int hash = 7;
    hash = 47 * hash + (int) (this.offset ^ (this.offset >>> 32));
    hash = 47 * hash + (int) (this.duration ^ (this.duration >>> 32));
    hash = 47 * hash + Objects.hashCode(this.label);
    hash = 47 * hash + Objects.hashCode(this.threadName);
    hash = 47 * hash + Objects.hashCode(this.statusCode);
    hash = 47 * hash + Objects.hashCode(this.statusMessage);
    hash = 47 * hash + (this.success ? 1 : 0);
    hash = 47 * hash + (int) (this.responseBytes ^ (this.responseBytes >>> 32));
    hash = 47 * hash + Objects.hashCode(this.totalThreads);
    return hash;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null) {
      return false;
    }
    if (getClass() != obj.getClass()) {
      return false;
    }
    final Sample other = (Sample) obj;
    if (this.offset != other.offset) {
      return false;
    }
    if (this.duration != other.duration) {
      return false;
    }
    if (!Objects.equals(this.statusCode, other.statusCode)) {
      return false;
    }
    if (!Objects.equals(this.statusMessage, other.statusMessage)) {
      return false;
    }
    if (this.success != other.success) {
      return false;
    }
    if (this.responseBytes != other.responseBytes) {
      return false;
    }
    if (!Objects.equals(this.label, other.label)) {
      return false;
    }
    if (!Objects.equals(this.threadName, other.threadName)) {
      return false;
    }
    if (!Objects.equals(this.totalThreads, other.totalThreads)) {
      return false;
    }
    return true;
  }
}
