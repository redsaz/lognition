package com.redsaz.lognition.convert;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.RecordComponent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Function;
import org.apache.avro.Schema;
import org.apache.commons.lang3.reflect.ConstructorUtils;
import org.apache.commons.lang3.reflect.MethodUtils;

public sealed interface TabSchema<T>
    permits TabSchema.StrS,
        TabSchema.IntS,
        TabSchema.LongS,
        TabSchema.FloatS,
        TabSchema.DoubleS,
        TabSchema.BooleanS,
        TabSchema.UnionS,
        TabSchema.StructS {

  String name();

  Class<T> type();

  /**
   * @return true if the value does not have to be provided, and the opt value can be used.
   */
  default boolean isOptional() {
    return opt().isOptional();
  }

  /** Opposite of isOptional. */
  default boolean isRequired() {
    return opt().isRequired();
  }

  Opt<T> opt();

  /**
   * @return the default value to use if one was not provided from a source. May be null if required
   *     or if the default value is null.
   */
  default Object defVal() {
    return opt().defVal();
  }

  static <U> TabSchema<U> fromAvro(Schema.Field field) {

    return (TabSchema<U>)
        switch (field.schema().getType()) {
          case RECORD ->
              throw new IllegalArgumentException(
                  "Tabular values (\"" + field.name() + "\") cannot be records.");
          case ENUM ->
              throw new IllegalArgumentException(
                  "Tabular values (\"" + field.name() + "\") cannot be enums.");
          case ARRAY ->
              throw new IllegalArgumentException(
                  "Tabular values (\"" + field.name() + "\") cannot be arrays.");
          case MAP ->
              throw new IllegalArgumentException(
                  "Tabular values (\"" + field.name() + "\") cannot be maps.");
          case UNION -> {
            List<Class<?>> types =
                field.schema().getTypes().stream()
                    .map(
                        t ->
                            switch (t.getType()) {
                              case STRING -> (Class<?>) String.class;
                              case INT -> (Class<?>) Integer.class;
                              case LONG -> (Class<?>) Long.class;
                              case FLOAT -> (Class<?>) Float.class;
                              case DOUBLE -> (Class<?>) Double.class;
                              case BOOLEAN -> (Class<?>) Boolean.class;
                              case NULL -> null;
                              default ->
                                  throw new IllegalArgumentException(
                                      "Tabular union cannot have type \"" + t.getType() + "\".");
                            })
                    .filter(Objects::nonNull)
                    .toList();
            yield new UnionS(
                field.name(), Opt.of(field.hasDefaultValue(), field.defaultVal()), types);
          }
          case FIXED ->
              throw new IllegalArgumentException(
                  "Tabular values (\"" + field.name() + "\") cannot be fixeds.");
          case STRING ->
              new StrS(field.name(), Opt.of(field.hasDefaultValue(), (String) field.defaultVal()));
          case BYTES ->
              throw new IllegalArgumentException(
                  "Tabular values (\"" + field.name() + "\") cannot be bytes.");
          case INT ->
              new IntS(field.name(), Opt.of(field.hasDefaultValue(), (Integer) field.defaultVal()));
          case LONG ->
              new LongS(field.name(), Opt.of(field.hasDefaultValue(), (Long) field.defaultVal()));
          case FLOAT ->
              new FloatS(field.name(), Opt.of(field.hasDefaultValue(), (Float) field.defaultVal()));
          case DOUBLE ->
              new DoubleS(
                  field.name(), Opt.of(field.hasDefaultValue(), (Double) field.defaultVal()));
          case BOOLEAN ->
              new BooleanS(
                  field.name(), Opt.of(field.hasDefaultValue(), (Boolean) field.defaultVal()));
          case NULL ->
              throw new IllegalArgumentException(
                  "Tabular values (\"" + field.name() + "\") cannot be nulls (alone).");
        };
  }

  record StrS(String name, Opt<String> opt) implements TabSchema<String> {
    public static StrS optional(String name) {
      return new StrS(name, Opt.nullOpt());
    }

    public static StrS optional(String name, String defVal) {
      return new StrS(name, Opt.of(defVal));
    }

    public static StrS required(String name) {
      return new StrS(name, Opt.required());
    }

    @Override
    public Class<String> type() {
      return String.class;
    }
  }

  record IntS(String name, Opt<Integer> opt) implements TabSchema<Integer> {
    public static IntS optional(String name) {
      return new IntS(name, Opt.nullOpt());
    }

    public static IntS optional(String name, Integer defVal) {
      return new IntS(name, Opt.of(defVal));
    }

    public static IntS required(String name) {
      return new IntS(name, Opt.required());
    }

    @Override
    public Class<Integer> type() {
      return Integer.class;
    }
  }

  record LongS(String name, Opt<Long> opt) implements TabSchema<Long> {
    public static LongS optional(String name) {
      return new LongS(name, Opt.nullOpt());
    }

    public static LongS optional(String name, Long defVal) {
      return new LongS(name, Opt.of(defVal));
    }

    public static LongS required(String name) {
      return new LongS(name, Opt.required());
    }

    @Override
    public Class<Long> type() {
      return Long.class;
    }
  }

  record FloatS(String name, Opt<Float> opt) implements TabSchema<Float> {
    public static FloatS optional(String name) {
      return new FloatS(name, Opt.nullOpt());
    }

    public static FloatS optional(String name, Float defVal) {
      return new FloatS(name, Opt.of(defVal));
    }

    public static FloatS required(String name) {
      return new FloatS(name, Opt.required());
    }

    @Override
    public Class<Float> type() {
      return Float.class;
    }
  }

  record DoubleS(String name, Opt<Double> opt) implements TabSchema<Double> {
    public static DoubleS optional(String name) {
      return new DoubleS(name, Opt.nullOpt());
    }

    public static DoubleS optional(String name, Double defVal) {
      return new DoubleS(name, Opt.of(defVal));
    }

    public static DoubleS required(String name) {
      return new DoubleS(name, Opt.required());
    }

    @Override
    public Class<Double> type() {
      return Double.class;
    }
  }

  record BooleanS(String name, Opt<Boolean> opt) implements TabSchema<Boolean> {
    public static BooleanS optional(String name) {
      return new BooleanS(name, Opt.nullOpt());
    }

    public static BooleanS optional(String name, Boolean defVal) {
      return new BooleanS(name, Opt.of(defVal));
    }

    public static BooleanS required(String name) {
      return new BooleanS(name, Opt.required());
    }

    @Override
    public Class<Boolean> type() {
      return Boolean.class;
    }
  }

  record UnionS(String name, Opt<Object> opt, List<Class<?>> types) implements TabSchema<Object> {
    public static UnionS optional(String name, Class<?>... types) {
      return new UnionS(name, Opt.nullOpt(), List.copyOf(Arrays.asList(types)));
    }

    public static UnionS optional(String name, Object defVal, Class<?>... types) {
      return new UnionS(name, Opt.of(defVal), List.copyOf(Arrays.asList(types)));
    }

    public static UnionS required(String name, Class<?>... types) {
      return new UnionS(name, Opt.required(), List.copyOf(Arrays.asList(types)));
    }

    @Override
    public Class<Object> type() {
      return Object.class;
    }
  }

  record StructS(String name, List<? extends TabSchema<?>> fields) implements TabSchema<Object> {
    public StructS {
      // Must not have duplicate names.
      Set<String> names = new HashSet<>();
      fields.stream()
          .map(TabSchema::name)
          .forEach(
              fname -> {
                if (!names.add(fname)) {
                  throw new IllegalArgumentException("Schema has duplicate names: " + fname);
                }
              });
    }

    /**
     * Essentially an Avro spec. Kinda. Hmm. Keep it simple.
     *
     * @param spec an Avro spec in string form.
     */
    public static StructS of(String spec) {
      return StructS.ofAvro((new Schema.Parser()).parse(spec));
    }

    public static StructS of(String name, TabSchema<?>... fields) {
      return new StructS(name, List.copyOf(Arrays.asList(fields)));
    }

    static StructS ofAvro(Schema schema) {
      return new StructS(
          schema.getFullName(), schema.getFields().stream().map(TabSchema::fromAvro).toList());
    }

    @Override
    public String name() {
      return "";
    }

    @Override
    public Class<Object> type() {
      return Object.class;
    }

    @Override
    public Opt<Object> opt() {
      return null;
    }

    //    private static class BoxApprox {
    //      // Key: Source type, Value: Acceptable Dest types in order of greatest preference to
    // lowest.
    //      // (Obviously the source type isn't in the acceptable list, since it is an exact match)
    //      private static final Map<Class<?>, Class<?>[]> APPROXES = Map.ofEntries(
    //          Map.entry(Integer.class, new Class<?>[] {int.class, long.class, Long.class,
    // double.class, Double.class, Number.class, float.class, Float.class}),
    //          Map.entry(Long.class, new Class<?>[] {long.class, Number.class, double.class,
    // Double.class, float.class, Float.class}),
    //          Map.entry(Double.class, new Class<?>[] {double.class, Number.class}),
    //          Map.entry(Float.class, new Class<?>[] {float.class, double.class, Double.class,
    // Number.class}),
    //          Map.entry(Boolean.class, new Class<?>[] {boolean.class})
    //          );
    //
    //      private static final Class<?>[] EMPTY = new Class<?>[] {};
    //      /**
    //       * Calculate the distance from the source class to the destination class. 0 is
    // identical.
    //       * -1 means they do not "connection", they do not approximate. The idea is that, when
    // there
    //       * are multiple setters for a field, that the best candidate can be picked by using the
    //       * one with the shortest distance.
    //       * @param src The data type of the source
    //       * @param dst The receiving type
    //       * @return the approximate distance, or -1 if there is no connection.
    //       */
    //      public int calcDistance(Class<?> src, Class<?> dst) {
    //        if (dst.equals(src)) {
    //          return 0;
    //        }
    //        Class<?>[] candidates = APPROXES.getOrDefault(src, EMPTY);
    //        for (int i = 0; i < candidates.length; ++i) {
    //          Class<?> candidate = candidates[i];
    //          if (candidate.equals(src)) {
    //            return i + 1; // Must be +1 because 0 is reserved for exact match.
    //          }
    //        }
    //        return hierarchyDistance(src, dst);
    //      }
    //
    //      private int hierarchyDistance(Class<?> src, Class<?> dst) {
    //        if (src.equals(dst)) {
    //          return 0;
    //        }
    //        if (!dst.isAssignableFrom(src)) {
    //          return -1;
    //        }
    //        // Make the distance from anything to (not)Object be a normal distance, and
    //        // anything to Object be maximum possible distance, so that interfaces rank higher
    // instead
    //        int minDist = Integer.MAX_VALUE;
    //        if (!dst.equals(Object.class) && src.getSuperclass() != null) {
    //          minDist = hierarchyDistance(src.getSuperclass(), dst) + 1;
    //        }
    //        Class<?>[] ifaces = src.getInterfaces();
    //        for (int i = 0; i < ifaces.length; ++i) {
    //          Class<?> iface = ifaces[i];
    //          if (!iface.isAssignableFrom(src)) {
    //            continue;
    //          }
    //          int dist = hierarchyDistance(iface, dst) + 1;
    //          if (dist >= 0 && dist < minDist) {
    //            minDist = dist;
    //          }
    //        }
    //        return minDist;
    //      }
    //    }


    // Sooooo.... This doesn't work as planned, because the Csvs#recordsAsStrings call does as it says... makes
    // everything strings, so it's a TabRecord of strings. It'll only work if the class to convert to is also only
    // Strings.
    public <U> Function<TabRecord, U> converter(Class<U> clazz) {
      // TODO make all this more robust, and probably in its own class
      MethodHandles.Lookup lookup = MethodHandles.lookup();
      if (clazz.isRecord()) {
        RecordComponent[] parts = clazz.getRecordComponents();
        Class<?>[] ctorParams =
            Arrays.stream(parts).map(RecordComponent::getType).toArray(Class<?>[]::new);
        Constructor<U> ctorReflect = ConstructorUtils.getAccessibleConstructor(clazz, ctorParams);
        if (ctorReflect == null) {
          throw new TabException("Could not find an acceptable constructor for " + clazz.getName());
        }
        MethodHandle ctor;
        try {
          ctor = lookup.unreflectConstructor(ctorReflect);
        } catch (IllegalAccessException e) {
          throw new TabException("Could not access constructor for " + clazz.getName(), e);
        }
        TabSchema[] ordered = new TabSchema[parts.length];
        int[] recPoses = new int[parts.length];
        // Yeah this is O(n^2). I should hope there's only at most a few dozen params.
        for (int i = 0; i < parts.length; ++i) {
          TabSchema<?> matching = null;
          int recPos = -1;
          for (int j = 0; j < fields.size(); ++j) {
            if (parts[i].getName().equals(fields.get(j).name())) {
              matching = fields.get(j);
              recPos = j;
              break;
            }
          }
          if (matching == null) {
            throw new TabException(
                "Schema for %s does not have field %s for class %s"
                    .formatted(name(), parts[i].getName(), clazz.getName()));
          }
          ordered[i] = matching;
          recPoses[i] = recPos;
        }
        return (TabRecord r) -> {
          Object[] values = new Object[parts.length];
          for (int i = 0; i < parts.length; ++i) {
            values[i] = r.get(recPoses[i]);
          }
          try {
            return (U) ctor.invoke(values);
          } catch (Throwable e) {
            throw new TabException(
                "Could not create a new instance of %s".formatted(clazz.getName()), e);
          }
        };
      }
      // Was not a record, so go the no-param constructor and setters route.
      MethodHandle ctor;
      try {
        ctor = lookup.findConstructor(clazz, MethodType.methodType(void.class));
      } catch (IllegalAccessException e) {
        throw new TabException("Could not access constructor for " + clazz.getName(), e);
      } catch (NoSuchMethodException e) {
        throw new TabException("No parameterless constructor found for " + clazz.getName(), e);
      }
      List<BiConsumer<TabRecord, U>> fillers = new ArrayList<>();
      // Find the setters
      for (int i = 0; i < fields().size(); ++i) {
        TabSchema<?> field = fields.get(i);
        String fname = field.name();
        String setterName =
            "set" + Character.toString(Character.toUpperCase(fname.codePointAt(0))) + fname.substring(1);
        Method setterReflect =
            MethodUtils.getMatchingAccessibleMethod(clazz, setterName, field.type());
        if (setterReflect == null) {
          // If there is no setter for the particular field, fine. It means we can't populate that
          // particular part of the target object.
          continue;
        }
        MethodHandle setter;
        try {
          setter = lookup.unreflect(setterReflect);
        } catch (IllegalAccessException e) {
          throw new TabException(
              "Cannot access %s#%s".formatted(clazz.getName(), setterReflect.getName()), e);
        }
        final int recPos = i;
        BiConsumer<TabRecord, U> filler =
            (TabRecord r, U target) -> {
              try {
                setter.invoke(target, r.get(recPos));
              } catch (Throwable e) {
                throw new TabException(
                    "Exception when calling %s#%s"
                        .formatted(clazz.getName(), setterReflect.getName()),
                    e);
              }
            };
        fillers.add(filler);
      }
      return (TabRecord r) -> {
        try {
          U target = (U) ctor.invoke();
          fillers.forEach(
              filler -> {
                filler.accept(r, target);
              });
          return target;
        } catch (Throwable e) {
          throw new TabException("Could not create new instance of class " + clazz.getName(), e);
        }
      };
    }
  }
}
