package util.functional;

import java.io.File;
import java.time.LocalDateTime;
import java.time.Year;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import javafx.util.Callback;

import AudioPlayer.playlist.PlaylistItem;
import AudioPlayer.tagging.Metadata;
import AudioPlayer.tagging.MetadataGroup;
import Configuration.AccessorConfig;
import Configuration.Config;
import Configuration.Configurable;
import gui.itemnode.StringSplitParser;
import gui.itemnode.StringSplitParser.Split;
import gui.itemnode.StringSplitParser.SplitData;
import util.File.AudioFileFormat;
import util.File.FileUtil;
import util.File.ImageFileFormat;
import util.Util;
import util.access.Ѵ;
import util.collections.PrefList;
import util.collections.map.PrefListMap;
import util.units.Bitrate;
import util.units.FileSize;
import util.units.FormattedDuration;
import util.units.NofX;

import static java.lang.Integer.min;
import static java.lang.Math.max;
import static java.util.Collections.EMPTY_LIST;
import static java.util.Collections.singletonList;
import static org.atteo.evo.inflector.English.plural;
import static util.File.AudioFileFormat.Use.APP;
import static util.File.AudioFileFormat.Use.PLAYBACK;
import static util.Util.*;
import static util.dev.Util.noØ;
import static util.functional.Functors.StringDirection.FROM_START;
import static util.functional.Util.ALL;
import static util.functional.Util.ISNTØ;
import static util.functional.Util.ISØ;
import static util.functional.Util.NONE;
import static util.functional.Util.isInR;
import static util.functional.Util.list;
import static util.functional.Util.map;

public class Functors {

    /** Marker interface for lambda. */
    public interface Λ {}
    /** Marker interface for lambda denoting its first input and output. */
    public interface IO<I,O> extends Λ {}
    public static interface Ƒ extends Λ, IO<Void,Void>, Runnable {
        void apply();

        /** Equivalent to {@link #apply()}. Exists for compatibility with {@link Runnable}. */
        @Deprecated
        default void run() {
            apply();
        }
    }
    public static interface Ƒ0<O> extends Λ, IO<Void,O>, Supplier<O> {
        O apply();

        /** Equivalent to {@link #apply()}. Exists for compatibility with {@link Supplier}. */
        @Deprecated
        default O get() {
            return apply();
        }

        default <M> Ƒ0<M> map(Ƒ1<? super O,? extends M> f) {
            return () -> f.apply(apply());
        }

        /**
         * Returns ewuivalent function to this returning no output. The computation will still
         * take place as normal, so this function should have side effects. If it does not, a
         * function that does nothing should be used instead of this method.
         */
        default Ƒ toƑ() {
            return () -> apply();
        }
    }
    public static interface Ƒ1<I,O> extends Λ, IO<I,O>, Function<I,O>, Callback<I,O>, Consumer<I> {

        public static Ƒ1<Void,Void> f1(Runnable r) {
            return i -> {
                r.run();
                return null;
            };
        }

        public static <T> Ƒ1<Void,T> f1(Supplier<T> s) {
            return i -> s.get();
        }

        public static <T> Ƒ1<T,Void> f1(Consumer<T> c) {
            return i -> {
                c.accept(i);
                return null;
            };
        }

        @Override
        O apply(I i);

        /** Equivalent to {@link #apply()}. Exists for compatibility with {@link Callback}. */
        @Deprecated
        @Override
        default O call(I i) {
            return apply(i);
        }

        /** Equivalent to {@link #apply()}, ignoring the result. Exists for compatibility with {@link Consumer}. */
        @Deprecated
        @Override
        public default void accept(I i) {
            apply(i); // and ignore result as a proper Consumer
        }

        /** Partially applies this function with 1st parameter. */
        default Ƒ0<O> toƑ0(I i) {
            return () -> apply(i);
        }

        /**
         * Returns function equivalent to this, except for when certain exception types are thrown.
         * These will be caught and alternative output returned.
         */
        default Ƒ1<I,O> onEx(O or, Class<?>... ecs) {
            return i -> {
                try {
                    return apply(i);
                } catch(Exception e) {
                    for(Class<?> ec : ecs)
                        if(ec.isAssignableFrom(ec.getClass()))
                            return or;
                    throw e;
                }
            };
        }

        /** Lazy version of {@link #onEx(java.lang.Object, java.lang.Class...) } */
        default Ƒ1<I,O> onEx(Supplier<O> or, Class<?>... ecs) {
            return i -> {
                try {
                    return apply(i);
                } catch(Exception e) {
                    for(Class<?> ec : ecs)
                        if(ec.isAssignableFrom(ec.getClass()))
                            return or.get();
                    throw e;
                }
            };
        }
        /** Function version of {@link #onEx(java.lang.Object, java.lang.Class...) }. */
        default Ƒ1<I,O> onEx(Ƒ1<I,O> or, Class<?>... ecs) {
            return i -> {
                try {
                    return apply(i);
                } catch(Exception e) {
                    for(Class<?> ec : ecs)
                        if(ec.isAssignableFrom(ec.getClass()))
                            return or.apply(i);
                    throw e;
                }
            };
        }

        @Override
        default <V> Ƒ1<I, V> andThen(Function<? super O, ? extends V> after) {
            noØ(after);
            return (I t) -> after.apply(apply(t));
        }

        default <V> Ƒ1<I, V> andThen(Ƒ1<? super O, ? extends V> after) {
            noØ(after);
            return (I t) -> after.apply(apply(t));
        }

        // this change return type from Consumer to Function in a typesafe way!!
        @Override
        default Ƒ1<I,Void> andThen(Consumer<? super I> after) {
            return i -> {
                apply(i);
                after.accept(i);
                return null;
            };
        }

        @Override
        default <V> Ƒ1<V,O> compose(Function<? super V, ? extends I> before) {
            noØ(before);
            return (V v) -> apply(before.apply(v));
        }

        /**
         * @param mutator consumer that takes the input of this function and applies it
         * on output of this function after this function finishes
         * @return composed function that applies this function to its input and then
         * mutates the output before returning it.
         */
        public default Ƒ1<I,O> andApply(Consumer<O> mutator) {
            return in -> {
                O o = apply(in);
                mutator.accept(o);
                return o;
            };
        }

        /**
         * Similar to {@link #andApply(java.util.function.Consumer)} but the mutator takes
         * additional parameter - initial input of this function.
         *
         * @param mutator consumer that takes the input of this function and applies it
         * on output of this function after this function finishes
         * @return composed function that applies this function to its input and then
         * mutates the output before returning it.
         */
        public default Ƒ1<I,O> andApply(BiConsumer<I,O> mutator) {
            return in -> {
                O o = apply(in);
                mutator.accept(in,o);
                return o;
            };
        }
        /**
         * Similar to {@link #andThen(java.util.function.Function)} but the mutator
         * takes additional parameter - the original input to this function.
         *
         * @param mutator consumer that takes the input of this function and applies it
         * on output of this function after this function finishes
         * @return composed function that applies this function to its input and then
         * applies the mutator before returning it.
         */
        public default <O2> Ƒ1<I,O2> andThen(Ƒ2<I,O,O2> mutator) {
            return in -> {
                O o = apply(in);
                return mutator.apply(in,o);
            };
        }

        /**
         * Returns nullless version of this f, which returns its input instead of null. The input
         * type must conform to output type! This mostly makes sense when input and output type
         * match.
         */
        default Ƒ1<I,O> nonNull() {
            return in -> {
                O out = apply(in);
                return out==null ? (O)in : out;
            };
        }

        default Ƒ1<I,O> nonNull(O or) {
            return andThen(o -> o==null ? or : o);
        }

        default Ƒ1<I,O> passNull() {
            return in -> in==null ? null : apply(in);
        }

        default Ƒ1<I,O> wrap(NullIn i, NullOut o) {
            if(i==NullIn.NULL && o==NullOut.NULL)
                return in -> in==null ? null : apply(in);
            if(i==NullIn.APPLY && o==NullOut.NULL)
                return (Ƒ1) this;
            if(i==NullIn.APPLY && o==NullOut.INPUT)
                return in -> {
                    O out = apply(in);
                    return out==null ? (O)in : out;
                };
            if(i==NullIn.NULL && o==NullOut.INPUT)
                return in -> {
                    if(in==null) return null;
                    O out = apply(in);
                    return out==null ? (O)in : out;
                };

            throw new AssertionError("Illegal switch case");
        }
    }
    // Ƒ can not extend Predicate, doing so would not be typesafe, hence this subclass
    // this class also preserves predicate identity during predicate combination operations
    public static interface ƑP<I> extends Ƒ1<I,Boolean>, Predicate<I> {

        /** Equivalent to {@link #apply()}. Exists for compatibility with {@link Predicate}. */
        @Deprecated
        @Override
        public default boolean test(I i) {
            return apply(i);
        }

        @Override
        default ƑP<I> negate() {
            // we should retain the predicate identity if possible. Of course it can be leveraged
            // only if unique predicates are used, not dynamically created ones, e.g. (o -> o==null)
            if(this==ISØ) return (ƑP)ISNTØ;
            else if(this==ISNTØ) return (ƑP)ISØ;
            else if(this==ALL) return (ƑP)ALL;
            else if(this==NONE) return (ƑP)NONE;
            return i -> !test(i);
        }

        @Override
        default ƑP<I> and(Predicate<? super I> p) {
            // we should retain the predicate identity if possible
            if(this==p) return this;
            else if(this==NONE) return (ƑP)NONE;
            else if((this==ISØ && p==ISNTØ) || (this==ISNTØ && p==ISØ)) return (ƑP)NONE;
            else if((this==ALL && p==NONE) || (this==NONE && p==ALL)) return (ƑP)NONE;
            return i -> test(i) && test(i);
        }

        @Override
        default ƑP<I> or(Predicate<? super I> p) {
            // we should retain the predicate identity if possible
            if(this==p) return this;
            else if(this==ALL) return (ƑP)NONE;
            else if((this==ISØ && p==ISNTØ) || (this==ISNTØ && p==ISØ)) return (ƑP)ALL;
            else if((this==ALL && p==NONE) || (this==NONE && p==ALL)) return (ƑP)ALL;
            return i -> test(i) || test(i);
        }

    }
    public static interface Ƒ1E<I,O> extends Λ, IO<I,O> {
        O apply(I i) throws Exception;

        default Ƒ1E<I,O> onEx(O or, Class<?>... ecs) {
            return i -> {
                try {
                    return apply(i);
                } catch(Exception e) {
                    for(Class<?> ec : ecs) if(ec.isAssignableFrom(e.getClass())) return or;
                    throw e;
                }
            };
        }
    }
    public static interface Ƒ2<I,I2,O> extends Λ, IO<I,O>, BiFunction<I,I2,O> {
        @Override
        O apply(I i, I2 i2);

        default Ƒ1<I,O> toƑ1(I2 i2) {
            return (i) -> apply(i, i2);
        }

        default Ƒ2<I,I2,O> onEx(O or, Class<?>... ecs) {
            return (i1,i2) -> {
                try {
                    return apply(i1,i2);
                } catch(Exception e) {
                    for(Class<?> ec : ecs) if(ec.isAssignableFrom(e.getClass())) return or;
                    throw e;
                }
            };
        }
    }
    public static interface Ƒ3<I,I2,I3,O> extends Λ, IO<I,O> {
        O apply(I i, I2 i2, I3 i3);

        default Ƒ2<I,I2,O> toƑ2(I3 i3) {
            return (i,i2) -> apply(i, i2, i3);
        }

        default Ƒ3<I,I2,I3,O> onEx(O or, Class<?>... ecs) {
            return (i1,i2,i3) -> {
                try {
                    return apply(i1,i2,i3);
                } catch(Exception e) {
                    for(Class<?> ec : ecs) if(ec.isAssignableFrom(e.getClass())) return or;
                    throw e;
                }
            };
        }
    }
    public static interface Ƒ4<I,I2,I3,I4,O> extends Λ, IO<I,O> {
        O apply(I i, I2 i2, I3 i3, I4 i4);

        default Ƒ3<I,I2,I3,O> toƑ3(I4 i4) {
            return (i,i2,i3) -> apply(i, i2, i3, i4);
        }

        default Ƒ4<I,I2,I3,I4,O> onEx(O or, Class<?>... ecs) {
            return (i1,i2,i3,i4) -> {
                try {
                    return apply(i1,i2,i3,i4);
                } catch(Exception e) {
                    for(Class<?> ec : ecs) if(ec.isAssignableFrom(e.getClass())) return or;
                    throw e;
                }
            };
        }
    }
    public static interface Ƒ5<I,I2,I3,I4,I5,O> extends Λ, IO<I,O> {
        O apply(I i, I2 i2, I3 i3, I4 i4, I5 i5);

        default Ƒ4<I,I2,I3,I4,O> toƑ4(I5 i5) {
            return (i,i2,i3,i4) -> apply(i, i2, i3, i4, i5);
        }

        default Ƒ5<I,I2,I3,I4,I5,O> onEx(O or, Class<?>... ecs) {
            return (i1,i2,i3,i4,i5) -> {
                try {
                    return apply(i1,i2,i3,i4,i5);
                } catch(Exception e) {
                    for(Class<?> ec : ecs) if(ec.isAssignableFrom(ec.getClass())) return or;
                    throw e;
                }
            };
        }
    }
    public static interface Ƒ6<I,I2,I3,I4,I5,I6,O> extends Λ, IO<I,O> {
        O apply(I i, I2 i2, I3 i3, I4 i4, I5 i5, I6 i6);

        default Ƒ5<I,I2,I3,I4,I5,O> toƑ5(I6 i6) {
            return (i,i2,i3,i4,i5) -> apply(i, i2, i3, i4, i5, i6);
        }

        default Ƒ6<I,I2,I3,I4,I5,I6,O> onEx(O or, Class<?>... ecs) {
            return (i1,i2,i3,i4,i5,i6) -> {
                try {
                    return apply(i1,i2,i3,i4,i5,i6);
                } catch(Exception e) {
                    for(Class<?> ec : ecs) if(ec.isAssignableFrom(ec.getClass())) return or;
                    throw e;
                }
            };
        }
    }

    public static enum NullIn {
        NULL,
        APPLY;
    }
    public static enum NullOut {
        NULL,
        INPUT;
    }

    private static final PrefListMap<PƑ,Class> fsI = new PrefListMap<>(pf -> pf.in);
    private static final PrefListMap<PƑ,Class> fsO = new PrefListMap<>(pf -> pf.out);
    private static final PrefListMap<PƑ,Integer> fsIO = new PrefListMap<>(pf -> Objects.hash(pf.in,pf.out));

    static {
        add("Is null",      Object.class, Boolean.class, ISØ);
        add("Is not null",  Object.class, Boolean.class, ISNTØ);

        add("As is",        Object.class, Object.class, x->x, true, true, true);
        add("As String",    Object.class, String.class, Objects::toString);
        add("As Boolean",   String.class, Boolean.class, Boolean::parseBoolean);

        add("Is true",      Boolean.class, Boolean.class, ALL);
        add("Is false",     Boolean.class, Boolean.class, NONE);
        add("Negate",       Boolean.class, Boolean.class, b -> !b);
        add("And",          Boolean.class, Boolean.class, Boolean::logicalAnd, Boolean.class,true);
        add("Or",           Boolean.class, Boolean.class, Boolean::logicalOr, Boolean.class,true);
        add("Xor",          Boolean.class, Boolean.class, Boolean::logicalXor, Boolean.class,true);

        add("'_' -> ' '", String.class,String.class, s -> s.replace("_", " "));
        add("-> file name", String.class,String.class, Util::filenamizeString);
        add("Anime", String.class,String.class, s -> {
            // remove the super annoying '_'
            s = s.replaceAll("_", " ");

            // remove hash
            if(s.endsWith("]") && s.lastIndexOf('[')==s.length()-10)
                s = s.substring(0,s.length()-10);

            // remove fangroups
            String fangroup = null;
            if(s.startsWith("[")) {
                int i = s.indexOf(']');
                if(i!=-1) {
                    fangroup = s.substring(0,i+1);
                    s = s.substring(i+1);
                }
            }

            // remove leading and trailing shit
            s = s.trim();

            // add fangroup at the end
            if(fangroup!=null)
                s = s+"." + fangroup;

            return s;
        });
        add("Plural",       String.class,String.class, (t) -> plural(t));
        add("Replace 1st (regex)",  String.class,String.class, (t,r,n) -> r.matcher(t).replaceFirst(n), Pattern.class,String.class ,Pattern.compile(""),"");
        add("Remove first (regex)", String.class,String.class, (t,r) -> r.matcher(t).replaceFirst(""), Pattern.class, Pattern.compile(""));
        add("Replace all",          String.class,String.class, (t,o,n) -> t.replace(o,n), String.class,String.class, "","");
        add("Replace all (regex)",  String.class,String.class, (t,r,n) -> r.matcher(t).replaceAll(n), Pattern.class,String.class, Pattern.compile(""),"");
        add("Remove all",           String.class,String.class, (t,r) -> t.replace(r,""), String.class, "");
        add("Remove all (regex)",   String.class,String.class, (t,r) -> r.matcher(t).replaceAll(""), Pattern.class, Pattern.compile(""));
        add("Text",         String.class,String.class, (t,r) -> r, String.class,"");
        add("Add text",     String.class,String.class, (t,a,d) -> d==FROM_START ? a+t : t+a, String.class, StringDirection.class,"",FROM_START);
        add("Remove chars", String.class,String.class, (t,i,d) -> d==FROM_START ? t.substring(min(i,t.length()-1)) : t.substring(0, max(t.length()-i,0)), Integer.class, StringDirection.class,0,FROM_START);
        add("Retain chars", String.class,String.class, (t,i,d) -> d==FROM_START ? t.substring(0,min(i,t.length()-1)) : t.substring(min(i,t.length()-1)), Integer.class, StringDirection.class,0,FROM_START);
        add("Trim",         String.class,String.class, (t) -> t.trim());
        add("Split",        String.class,SplitData.class, (t,splitter) ->
                splitter.applyM(t).entrySet().stream().map(e -> new Split(e.getKey(),e.getValue())).collect(Collectors.toCollection(SplitData::new))
            , StringSplitParser.class,new StringSplitParser("%all%"));
        add("Split-join",   String.class,String.class, (t,spliter,joiner) -> {
                Map<String,String> splits = spliter.applyM(t);
                List<String> keys = joiner.parse_keys;
                List<String> seps = joiner.key_separators;
                StringBuilder o = new StringBuilder("");
                for(int i=0; i<keys.size()-1; i++) {
                    if(!splits.containsKey(keys.get(i))) return null;
                    o.append(splits.get(keys.get(i)));
                    o.append(seps.get(i));
                }
                    if(!splits.containsKey(keys.get(keys.size()-1))) return null;
                    o.append(splits.get(keys.get(keys.size()-1)));
                return o.toString();
        }, StringSplitParser.class, StringSplitParser.class,new StringSplitParser("%all%"),new StringSplitParser("%all%"));

        add("Name",       File.class,String.class, FileUtil::getName);
        add("Suffix",     File.class,String.class, FileUtil::getSuffix);
        add("Name.Suffix",File.class,String.class, File::getName);
        add("Path",       File.class,String.class, File::getAbsolutePath);
        add("Size",       File.class,FileSize.class, FileSize::new);

        // fielded values
        for(Metadata.Field f : Metadata.Field.values())
            add(f.name(), Metadata.class, f.getType(), m->m.getField(f));
        for(PlaylistItem.Field f : PlaylistItem.Field.values())
            add(f.name(), PlaylistItem.class, f.getType(), m->m.getField(f));
        for(MetadataGroup.Field f : MetadataGroup.Field.values())
            add(f.name(), MetadataGroup.class, f.getType(), m->m.getField(f));

        add("Is",                   String.class,Boolean.class, (text,b) -> text.equals(b), String.class,"");
        add("Contains",             String.class,Boolean.class, (text,b) -> text.contains(b), String.class,"");
        add("Ends with",            String.class,Boolean.class, (text,b) -> text.endsWith(b), String.class,"");
        add("Starts with",          String.class,Boolean.class, (text,b) -> text.startsWith(b), String.class,"");
        add("Is (no case)",         String.class,Boolean.class,(text,b) -> text.equalsIgnoreCase(b), String.class,"");
        add("Contains (no case)",   String.class,Boolean.class,(text,b) -> text.toLowerCase().contains(b.toLowerCase()),String.class,"",false,false,true);
        add("Ends with (no case)",  String.class,Boolean.class,(text,b) -> text.toLowerCase().endsWith(b.toLowerCase()), String.class,"");
        add("Starts with (no case)",String.class,Boolean.class,(text,b) -> text.toLowerCase().startsWith(b.toLowerCase()), String.class,"");
        add("Matches regex",        String.class,Boolean.class,(text,r) -> r.matcher(text).matches(), Pattern.class,Pattern.compile(""));
        add("Is not",               String.class,Boolean.class,(text,b) -> !text.equals(b), String.class,"");
        add("Contains not",         String.class,Boolean.class,(text,b) -> !text.contains(b), String.class,"");
        add("Not ends with",        String.class,Boolean.class,(text,b) -> !text.endsWith(b), String.class,"");
        add("Not starts with",      String.class,Boolean.class,(text,b) -> !text.startsWith(b), String.class,"");
        add("Is not (no case)",     String.class,Boolean.class,(text,b) -> !text.equalsIgnoreCase(b), String.class,"");
        add("Contains not (no case)",        String.class,Boolean.class,(text,b) -> !text.toLowerCase().contains(b.toLowerCase()), String.class,"");
        add("Not ends with (no case)",       String.class,Boolean.class,(text,b) -> !text.toLowerCase().endsWith(b.toLowerCase()), String.class,"");
        add("Not starts with (no case)",     String.class,Boolean.class,(text,b) -> !text.toLowerCase().startsWith(b.toLowerCase()), String.class,"");
        add("Not matches regex",             String.class,Boolean.class,(text,r) -> !r.matcher(text).matches(), Pattern.class,Pattern.compile(""));
        add("More",             String.class,Boolean.class,(x,y) -> x.compareTo(y)>0, String.class,"");
        add("Less",             String.class,Boolean.class,(x,y) -> x.compareTo(y)<0, String.class,"");
        add("Not more",         String.class,Boolean.class,(x,y) -> x.compareTo(y)<=0, String.class,"");
        add("Not less",         String.class,Boolean.class,(x,y) -> x.compareTo(y)>=0, String.class,"");
        add("Longer than",      String.class,Boolean.class,(x,l) -> x.length()>l, Integer.class,0);
        add("Shorter than",     String.class,Boolean.class,(x,l) -> x.length()<l, Integer.class,0);
        add("Not longer than",  String.class,Boolean.class,(x,l) -> x.length()<=l, Integer.class,0);
        add("Not shorter than", String.class,Boolean.class,(x,l) -> x.length()>=l, Integer.class,0);
        add("Long exactly",     String.class,Boolean.class,(x,l) -> x.length()==l, Integer.class,0);
        add("Is empty",         String.class,Boolean.class, x -> x.isEmpty());
        add("Is funny",         String.class,Boolean.class, x -> x.contains("fun") && x.contains("y"));
        add("Is palindrome",    String.class,Boolean.class, x -> isNonEmptyPalindrome(x));

        add("Less",      Bitrate.class,Boolean.class,(x,y) -> x.compareTo(y)<0, Bitrate.class,new Bitrate(320));
        add("Is",        Bitrate.class,Boolean.class,(x,y) -> x.compareTo(y)==0, Bitrate.class,new Bitrate(320));
        add("More",      Bitrate.class,Boolean.class,(x,y) -> x.compareTo(y)>0, Bitrate.class,new Bitrate(320));
        add("Not more",  Bitrate.class,Boolean.class,(x,y) -> x.compareTo(y)<=0, Bitrate.class,new Bitrate(320));
        add("Is not",    Bitrate.class,Boolean.class,(x,y) -> x.compareTo(y)!=0, Bitrate.class,new Bitrate(320));
        add("Not less",  Bitrate.class,Boolean.class,(x,y) -> x.compareTo(y)>=0, Bitrate.class,new Bitrate(320),false,false,true);
        add("Is good",   Bitrate.class,Boolean.class, x -> x.getValue()>=320);
        add("Is bad",    Bitrate.class,Boolean.class, x -> x.getValue()<=128);
        add("Is unknown",Bitrate.class,Boolean.class, x -> x.getValue()==-1);
        add("Is known",  Bitrate.class,Boolean.class, x -> x.getValue()>-1);

        add("Less",      FormattedDuration.class,Boolean.class,(x,y) -> x.compareTo(y)<0, FormattedDuration.class, new FormattedDuration(0));
        add("Is",        FormattedDuration.class,Boolean.class,(x,y) ->  x.compareTo(y)==0, FormattedDuration.class, new FormattedDuration(0));
        add("More",      FormattedDuration.class,Boolean.class,(x,y) ->  x.compareTo(y)>0, FormattedDuration.class, new FormattedDuration(0),false,false,true);
        add("Not less",  FormattedDuration.class,Boolean.class,(x,y) -> x.compareTo(y)>=0, FormattedDuration.class, new FormattedDuration(0));
        add("Is not",    FormattedDuration.class,Boolean.class,(x,y) ->  x.compareTo(y)!=0, FormattedDuration.class, new FormattedDuration(0));
        add("Not more",  FormattedDuration.class,Boolean.class,(x,y) ->  x.compareTo(y)<=0, FormattedDuration.class, new FormattedDuration(0));

        add("<  Less",      NofX.class,Boolean.class, (x,y) -> x.compareTo(y)< 0, NofX.class,new NofX(1,1));
        add("=  Is",        NofX.class,Boolean.class, (x,y) -> x.compareTo(y)==0, NofX.class,new NofX(1,1));
        add(">  More",      NofX.class,Boolean.class, (x,y) -> x.compareTo(y)> 0, NofX.class,new NofX(1,1),false,false,true);
        add(">= Not less",  NofX.class,Boolean.class, (x,y) -> x.compareTo(y)>=0, NofX.class,new NofX(1,1));
        add("<> Is not",    NofX.class,Boolean.class, (x,y) -> x.compareTo(y)!=0, NofX.class,new NofX(1,1));
        add("<= Not more",  NofX.class,Boolean.class, (x,y) -> x.compareTo(y)<=0, NofX.class,new NofX(1,1));

        add("<  Less",      FileSize.class,Boolean.class, (x,y) -> x.compareTo(y)< 0, FileSize.class,new FileSize(0),false,false,true);
        add("=  Is",        FileSize.class,Boolean.class, (x,y) -> x.compareTo(y)==0, FileSize.class,new FileSize(0));
        add(">  More",      FileSize.class,Boolean.class, (x,y) -> x.compareTo(y)> 0, FileSize.class,new FileSize(0));
        add(">= Not more",  FileSize.class,Boolean.class, (x,y) -> x.compareTo(y)<=0, FileSize.class,new FileSize(0));
        add("<> Is not",    FileSize.class,Boolean.class, (x,y) -> x.compareTo(y)!=0, FileSize.class,new FileSize(0));
        add("<= Not less",  FileSize.class,Boolean.class, (x,y) -> x.compareTo(y)>=0, FileSize.class,new FileSize(0));
        add("Is unknown",FileSize.class,Boolean.class, x -> x.inBytes()==-1);
        add("Is known",  FileSize.class,Boolean.class, x -> x.inBytes()>-1);
        add("Is 1.21GB", FileSize.class,Boolean.class, x -> x.inGBytes()==1.21);

        add("After",     Year.class,Boolean.class, (x,y) -> x.compareTo(y)> 0, Year.class,Year.now());
        add("Is",        Year.class,Boolean.class, (x,y) -> x.compareTo(y)==0, Year.class,Year.now());
        add("Before",    Year.class,Boolean.class, (x,y) -> x.compareTo(y)< 0, Year.class,Year.now());
        add("Not After", Year.class,Boolean.class, (x,y) -> x.compareTo(y)<=0, Year.class,Year.now(),false,false,true);
        add("Not",       Year.class,Boolean.class, (x,y) -> x.compareTo(y)!=0, Year.class,Year.now());
        add("Not before",Year.class,Boolean.class, (x,y) -> x.compareTo(y)>=0, Year.class,Year.now());
        add("Is leap",   Year.class,Boolean.class, x -> x.isLeap());

        add("After",   LocalDateTime.class,Boolean.class, (x,y) -> x.isAfter(y), LocalDateTime.class,LocalDateTime.now());
        add("Before",  LocalDateTime.class,Boolean.class, (x,y) -> x.isBefore(y), LocalDateTime.class,LocalDateTime.now());
        add("Is",      LocalDateTime.class,Boolean.class, (x,y) -> x.isEqual(y), LocalDateTime.class,LocalDateTime.now());

        add("Is supported", AudioFileFormat.class,Boolean.class, x -> x.isSupported(APP));
        add("Is playable", AudioFileFormat.class,Boolean.class, x -> x.isSupported(PLAYBACK));
        addPredicatesOf(AudioFileFormat.class);
        addPredicatesOf(ImageFileFormat.class);

        addPredicatesComparable(Short.class, new Short("0"));
        addPredicatesComparable(Integer.class, 0);
        addPredicatesComparable(Long.class, 0l);
        addPredicatesComparable(Double.class, 0d);
        addPredicatesComparable(Float.class, 0f);
    }

    public static<I,O> void add(String name, Class<I> i ,Class<O> o, Ƒ1<? super I,O> f) {
        addF(new PƑ0(name,i,o,f));
    }
    public static<I,P1,O> void add(String name, Class<I> i, Class<O> o, Ƒ2<I,P1,O> f, Class<P1> p1, P1 p1def) {
        addF(new PƑ1(name,i,o,p1,p1def,f));
    }
    public static<I,P1,P2,O> void add(String name, Class<I> i,Class<O> o, Ƒ3<I,P1,P2,O> f, Class<P1> p1, Class<P2> p2, P1 p1def, P2 p2def) {
        addF(new PƑ2(name,i,o,p1,p2,p1def,p2def,f));
    }
    public static<I,P1,P2,P3,O> void add(String name, Class<I> i,Class<O> o, Ƒ4<I,P1,P2,P3,O> f, Class<P1> p1, Class<P2> p2, Class<P3> p3, P1 p1def, P2 p2def, P3 p3def) {
        addF(new PƑ3(name,i,o,p1,p2,p3,p1def,p2def,p3def,f));
    }

    public static<I,O> void add(String name, Class<I> i ,Class<O> o, Ƒ1<I,O> f, boolean pi, boolean po, boolean pio) {
        addF(new PƑ0(name,i,o,f),pi,po,pio);
    }
    public static<I,P1,O> void add(String name, Class<I> i, Class<O> o, Ƒ2<I,P1,O> f, Class<P1> p1, P1 p1def, boolean pi, boolean po, boolean pio) {
        addF(new PƑ1(name,i,o,p1,p1def,f),pi,po,pio);
    }
    public static<I,P1,P2,O> void add(String name, Class<I> i,Class<O> o, Ƒ3<I,P1,P2,O> f, Class<P1> p1, Class<P2> p2, P1 p1def, P2 p2def, boolean pi, boolean po, boolean pio) {
        addF(new PƑ2(name,i,o,p1,p2,p1def,p2def,f),pi,po,pio);
    }
    public static<I,P1,P2,P3,O> void add(String name, Class<I> i,Class<O> o, Ƒ4<I,P1,P2,P3,O> f, Class<P1> p1, Class<P2> p2, Class<P3> p3, P1 p1def, P2 p2def, P3 p3def, boolean pi, boolean po, boolean pio) {
        addF(new PƑ3(name,i,o,p1,p2,p3,p1def,p2def,p3def,f),pi,po,pio);
    }

    public static <E extends Enum> void addPredicatesOf(Class<E> c) {
        add("Is", c,Boolean.class, (a,b) -> a==b, c, c.getEnumConstants()[0], false,false,true);
    }
    public static <C extends Comparable> void addPredicatesComparable(Class<C> c, C def_val) {
        add("Is less",     c,Boolean.class, (x,y) -> x.compareTo(y)<0,  c,def_val);
        add("Is",          c,Boolean.class, (x,y) -> x.compareTo(y)==0, c,def_val);
        add("Is more",     c,Boolean.class, (x,y) -> x.compareTo(y)>0,  c,def_val);
        add("Is not less", c,Boolean.class, (x,y) -> x.compareTo(y)>=0, c,def_val);
        add("Is not",      c,Boolean.class, (x,y) -> x.compareTo(y)!=0, c,def_val);
        add("Is not more", c,Boolean.class, (x,y) -> x.compareTo(y)<=0, c,def_val);
    }

    /** Add function to the pool. */
    public static void addF(PƑ f) {
        fsI.accumulate(f);
        fsO.accumulate(f);
        fsIO.accumulate(f);
    }
    /** Add function to the pool and sets as preferred according to parameters. */
    public static void addF(PƑ f, boolean i, boolean o, boolean io) {
        fsI.accumulate(f, i);
        fsO.accumulate(f, o);
        fsIO.accumulate(f, io);
    }
    /** Remove function from the pool. */
    public static void remF(PƑ f) {
        fsI.deaccumulate(f);
        fsO.deaccumulate(f);
        fsIO.deaccumulate(f);
    }

    /** Returns all functions taking input I. */
    public static <I> PrefList<PƑ<I,?>> getI(Class<I> i) {
        PrefList l = (PrefList) fsI.getElementsOf(getSuperClassesInc(unPrimitivize(i)));
        return l;

//        PrefList l = (PrefList) fsI.get(unPrimitivize(i));
//        return l==null ? new PrefList() : l;
    }
    /** Returns all functions producing output O. */
    public static <O> PrefList<PƑ<?,O>> getO(Class<O> o) {
        List l =  fsO.get(unPrimitivize(o));
        return l==null ? new PrefList() : (PrefList) l;
    }
    /** Returns all functions taking input I and producing output O. */
    public static <I,O> PrefList<PƑ<I,O>> getIO(Class<I> i, Class<O> o) {
        // this is rather messy, but works
        // we accumulate result for al superclasses & retain first found preferred element, while
        // keeping duplicate elements in check
        PrefList pl = new PrefList();
        Object pref = null;
        for(Class c : getSuperClassesInc(unPrimitivize(i))) {
            List l = fsIO.get(Objects.hash(c,unPrimitivize(o)));
            PrefList ll = l==null ? null : (PrefList) l;
            if(ll!=null) {
                if(pref==null && ll.getPrefered()!=null) pref = ll.getPrefered();
                pl.addAll(ll);
            }
        }
        Object prefcpy = pref;
        pl.removeIf(e -> e==prefcpy || e==null);
        if(pref!=null) pl.addPreferred(pref);
        if(pl.getPrefered()==null && !pl.isEmpty()) {
            Object e = pl.get(pl.size()-1);
            pl.remove(pl.size()-1);
            pl.addPreferred(e);
        }
        return pl;

        // old impl, ignores super classes
//        PrefList l = (PrefList) fsIO.get(Objects.hash(unPrimitivize(i),unPrimitivize(o)));
//        return l==null ? new PrefList() : l;
    }
    /** Returns all functions taking input IO and producing output IO. */
    public static <IO> PrefList<PƑ<IO,IO>> getIO(Class<IO> io) {
        return getIO(io, io);
    }

    public static <I> PƑ<I,?> getPrefI(Class<I> i) {
        PrefList<PƑ> l = (PrefList<PƑ>)fsI.get(i);
        return l==null ? null : l.getPrefered();
    }
    public static <O> PƑ<?,O> getPrefO(Class<O> o) {
        PrefList<PƑ> l = (PrefList<PƑ>)fsI.get(o);
        return l==null ? null : l.getPrefered();
    }
    public static <I,O> PƑ<I,O> getPrefIO(Class<I> i, Class<O> o) {
        PrefList<PƑ<I,O>> l = getIO(i, o);
        return l==null ? null : l.getPrefered();

//        PrefList<PƑ> l = (PrefList<PƑ>)fsIO.get(Objects.hash(i,o));
    }
    public static <IO> PƑ<IO,IO> getPrefIO(Class<IO> io) {
        return getPrefIO(io,io);
    }



    public static class Parameter<P> {
        public final Class<P> type;
        public final P defaultValue;

        public Parameter(Class<P> type, P defaultValue) {
            this.type = type;
            this.defaultValue = defaultValue;
        }
    }
    public static interface Parameterized<P> {
        public List<Parameter<P>> getParameters();
    }
    // parameterized function - variadic I -> O function factory with parameters
    public static abstract class PƑ<I,O> implements Ƒ2<I,Object[],O>, Parameterized<Object> {
        public final String name;
        public final Class<I> in;
        public final Class<O> out;
        private final IO<I,O> ff;

        public PƑ(String name, Class<I> in, Class<O> out, IO<I,O> f) {
            this.name = name;
            this.in = unPrimitivize(in);
            this.out = unPrimitivize(out);
            this.ff = f;
        }

        public Ƒ1<I,O> toFunction() {
            return i -> apply(i, new Object[]{});
        }

        @Override
        public abstract O apply(I t, Object... is);

        @Override
        public Ƒ1<I,O> toƑ1(Object...is) {
            // retain predicate identity
            if(isInR(ff, ISØ,ISNTØ,ALL,NONE)) return (Ƒ1<I,O>)ff;
            return i -> apply(i, is);
        }

    }
    // solely to hide generic parameter of PF above, the 3rd parameter (F) is implementation
    // detail - we do not want it to pollute external code, in fact this parameter exists solely
    // so PƑ can access its underlaying function, while not breaking typesafety for subclasses
    public static abstract class PƑB<I,O,F extends IO<I,O>> extends PƑ<I,O> {

        public final F f;

        public PƑB(String name, Class<I> in, Class<O> out, F f) {
            super(name, in, out, f);
            this.f = f;
        }

    }
    public static class PƑ0<I,O> extends PƑB<I,O,Ƒ1<I,O>> {

        public PƑ0(String _name, Class<I> i, Class<O> o, Ƒ1<I,O> f) {
            super(_name,i,o,f);
        }

        @Override
        public List<Parameter<Object>> getParameters() {
            return EMPTY_LIST;
        }

        @Override
        public O apply(I t, Object... ps) {
            return f.apply(t);
        }

    }
    public static class PƑ1<I,P1,O> extends PƑB<I,O,Ƒ2<I,P1,O>> {
        private Class<P1> p1;
        private P1 p1def;

        public PƑ1(String _name, Class<I> i, Class<O> o, Class<P1> p1type, P1 p1def, Ƒ2<I,P1,O> f) {
            super(_name,i,o,f);
            this.p1 = unPrimitivize(p1type);
            this.p1def = p1def;
        }

        @Override
        public List<Parameter<Object>> getParameters() {
            return singletonList(new Parameter(p1,p1def));
        }

        @Override
        public O apply(I t, Object... ps) {
             return f.apply(t, (P1)ps[0]);
        }
    }
    public static class PƑ2<I,P1,P2,O> extends PƑB<I,O,Ƒ3<I,P1,P2,O>> {
        private Class<P1> p1;
        private Class<P2> p2;
        private P1 p1def;
        private P2 p2def;

        public PƑ2(String _name, Class<I> i, Class<O> o, Class<P1> p1type, Class<P2> p2type, P1 p1def, P2 p2def, Ƒ3<I,P1,P2,O> f) {
            super(_name,i,o,f);
            this.p1 = unPrimitivize(p1type);
            this.p2 = unPrimitivize(p2type);
            this.p1def = p1def;
            this.p2def = p2def;
        }

        @Override
        public List<Parameter<Object>> getParameters() {
            return list(new Parameter(p1,p1def),new Parameter(p2,p2def));
        }

        @Override
        public O apply(I t, Object... ps) {
             return f.apply(t, (P1)ps[0], (P2)ps[1]);
        }
    }
    public static class PƑ3<I,P1,P2,P3,O> extends PƑB<I,O,Ƒ4<I,P1,P2,P3,O>> {
        private Class<P1> p1;
        private Class<P2> p2;
        private Class<P2> p3;
        private P1 p1def;
        private P2 p2def;
        private P3 p3def;

        public PƑ3(String _name, Class<I> i, Class<O> o, Class<P1> p1type, Class<P2> p2type, Class<P3> p3type, P1 p1def, P2 p2def, P3 p3def, Ƒ4<I,P1,P2,P3,O> f) {
            super(_name,i,o,f);
            this.p1 = unPrimitivize(p1type);
            this.p2 = unPrimitivize(p2type);
            this.p3 = unPrimitivize(p3type);
            this.p1def = p1def;
            this.p2def = p2def;
            this.p3def = p3def;
        }

        @Override
        public List<Parameter<Object>> getParameters() {
            return list(new Parameter(p1,p1def),new Parameter(p2,p2def),new Parameter(p3,p3def));
        }

        @Override
        public O apply(I t, Object... ps) {
             return f.apply(t, (P1)ps[0], (P2)ps[1], (P3)ps[2]);
        }
    }
    public static enum StringDirection {
        FROM_START,
        FROM_END;
    }
    public static class CƑ<I,O> implements Ƒ1<I,O>, Configurable<Object> {

        public final PƑ<I,O> pf;
        private final List<Config<Object>> cs = new ArrayList();

        public CƑ(PƑ<I, O> pf) {
            this.pf = pf;
            cs.addAll(map(pf.getParameters(),p->{
                Ѵ a = new Ѵ(p.defaultValue);
                return new AccessorConfig("",a::setValue,a::getValue);
            }));
        }

        @Override
        public O apply(I i) {
            return pf.apply(i, cs.stream().map(c->c.getValue()).toArray());
        }

    }
}