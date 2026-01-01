package com.redsaz.lognition.convert;

public interface TabVal {

  Object get();

  java.lang.String stringValue();

  record Null() implements TabVal {
    private static final Null INSTANCE = new Null();

    public static Null of() {
      return INSTANCE;
    }

    @Override
    public java.lang.String stringValue() {
      return null;
    }

    @Override
    public Object get() {
      return null;
    }
  }

  record String(java.lang.String value) implements TabVal {
    @Override
    public java.lang.String stringValue() {
      return value();
    }

    @Override
    public Object get() {
      return value;
    }
  }

  record Int(int value) implements TabVal {
    @Override
    public java.lang.String stringValue() {
      return Integer.toString(value());
    }

    @Override
    public Object get() {
      return value;
    }
  }

  record Long(long value) implements TabVal {
    @Override
    public java.lang.String stringValue() {
      return java.lang.Long.toString(value());
    }

    @Override
    public Object get() {
      return value;
    }
  }

  record Float(float value) implements TabVal {
    @Override
    public java.lang.String stringValue() {
      return java.lang.Float.toString(value());
    }

    @Override
    public Object get() {
      return value;
    }
  }

  record Double(double value) implements TabVal {
    @Override
    public java.lang.String stringValue() {
      return java.lang.Double.toString(value());
    }

    @Override
    public Object get() {
      return value;
    }
  }

  record Boolean(boolean value) implements TabVal {
    @Override
    public java.lang.String stringValue() {
      return java.lang.Boolean.toString(value());
    }

    @Override
    public Object get() {
      return value;
    }
  }
}
