package com.redsaz.lognition.convert;

/** Thrown when encountering an exception reading/writing value for TabRecords. */
public class TabException extends RuntimeException {
  public TabException(String message) {
    super(message);
  }
}
