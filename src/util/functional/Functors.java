package util.functional;

import java.io.File;
import java.nio.charset.Charset;
import java.time.LocalDateTime;
import java.time.Year;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.*;
import java.util.regex.Pattern;

import javafx.util.Callback;

import org.atteo.evo.inflector.English;

import audio.Item;
import audio.playlist.PlaylistItem;
import audio.tagging.Metadata;
import audio.tagging.MetadataGroup;
import gui.itemnode.StringSplitParser;
import gui.itemnode.StringSplitParser.SplitData;
import main.App;
import util.access.V;
import util.collections.list.PrefList;
import util.collections.map.PrefListMap;
import util.conf.AccessorConfig;
import util.conf.Config;
import util.conf.Configurable;
import util.file.AudioFileFormat;
import util.file.ImageFileFormat;
import util.file.Util;
import util.file.WindowsShortcut;
import util.file.mimetype.MimeType;
import util.units.*;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Collections.EMPTY_LIST;
import static util.Util.StringDirection;
import static util.Util.StringDirection.FROM_START;
import static util.dev.Util.noØ;
import static util.file.AudioFileFormat.Use.APP;
import static util.file.AudioFileFormat.Use.PLAYBACK;
import static util.functional.Util.*;
import static util.type.Util.unPrimitivize;

@SuppressWarnings("unchecked")
public interface Functors {

    /** Marker interface for lambda. */
    interface Λ {}
    /** Marker interface for lambda denoting its first input and output. */
    interface IO<I,O> extends Λ {
        // not sure if good idea
        // for default impl i want to use reflection to inspect generic type in runtime
        // subclasses may want to override, like PF or TypeAwareF
        // default Class<? super I> getTypeInput() {}
        // default Class<? super I> getTypeOutput() {}
    }
    interface Ƒ extends Λ, IO<Void,Void>, Runnable {
        void apply();

        /** Equivalent to {@link #apply()}. Exists for compatibility with {@link Runnable}. */
        default void run() {
            apply();
        }
    }
    interface Ƒ0<O> extends Λ, IO<Void,O>, Supplier<O> {
        O apply();

        /** Equivalent to {@link #apply()}. Exists for compatibility with {@link Supplier}. */
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
            return this::apply;
        }
    }
    /**
     * Function. Provides additional methods.
     * <p/>
     * Can also be used as a callback (it falls back to the {@link #apply(java.lang.Object)}
     * method) or consumer (same and in addition ignores output - this is not pointless due to side
     * effects - consumer by nature relies on side effects.)
     */
    interface Ƒ1<I,O> extends Λ, IO<I,O>, Function<I,O>, Callback<I,O>, Consumer<I> {

        static Ƒ1<Void,Void> f1(Runnable r) {
            return i -> {
                r.run();
                return null;
            };
        }

        static <T> Ƒ1<Void,T> f1(Supplier<T> s) {
            return i -> s.get();
        }

        static <T> Ƒ1<T,Void> f1(Consumer<T> c) {
            return i -> {
                c.accept(i);
                return null;
            };
        }

        @Override
        O apply(I i);

        /** Equivalent to {@link #apply(Object)}. Exists for compatibility with {@link Callback}. */
        @Override
        default O call(I i) {
            return apply(i);
        }

        /** Equivalent to {@link #apply(Object)}}, ignoring the result. Exists for compatibility with {@link Consumer}. */
        @Override
        default void accept(I i) {
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
        default <R> Ƒ1<I,R> andThen(Function<? super O, ? extends R> after) {
            noØ(after);
            return (I t) -> after.apply(apply(t));
        }

        //* Purely to avoid ambiguity of method overloading. Same as andThen(Function). */
        default <R> Ƒ1<I,R> andThen(Ƒ1<? super O, ? extends R> after) {
            noØ(after);
            return (I t) -> after.apply(apply(t));
        }

        /**
         * Creates function which runs the action aftewards
         * @return  function identical to this one, but one which runs the runnable after it computes
         * @param after action that executes right after computation is done and before returning the output
         */
        default Ƒ1<I,O> andThen(Runnable after) {
            noØ(after);
            return i -> {
                O o = apply(i);
                after.run();
                return o;
            };
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
        default <R> Ƒ1<R,O> compose(Function<? super R, ? extends I> before) {
            noØ(before);
            return (R v) -> apply(before.apply(v));
        }

        /**
         * @param mutator consumer that takes the input of this function and applies it
         * on output of this function after this function finishes
         * @return composed function that applies this function to its input and then
         * mutates the output before returning it.
         */
        default Ƒ1<I,O> andApply(Consumer<O> mutator) {
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
        default Ƒ1<I,O> andApply(BiConsumer<I,O> mutator) {
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
        default <O2> Ƒ1<I,O2> andThen(Ƒ2<I,O,O2> mutator) {
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
        @SuppressWarnings("unchecked")
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

        @SuppressWarnings("unchecked")
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
    /**
     * Predicate.
     * <p/>
     * {@link Ƒ1} can not extend Predicate, doing so would not be typesafe, hence this subclass.
     * This class also preserves predicate identity during predicate combination operations.
     */
    @SuppressWarnings("unchecked")
    interface ƑP<I> extends Ƒ1<I,Boolean>, Predicate<I> {

        /** Equivalent to {@link #apply(Object)}}. Exists for compatibility with {@link Predicate}. */
        @Override
        default boolean test(I i) {
            return apply(i);
        }

        @Override
        default ƑP<I> negate() {
            // we should retain the predicate identity if possible. Of course it can be leveraged
            // only if unique predicates are used, not dynamically created ones, e.g. (o -> o==null)
            if(this==ISØ) return (ƑP)ISNTØ;
            else if(this==ISNTØ) return (ƑP)ISØ;
            else if(this==IS) return (ƑP)IS;
            else if(this==ISNT) return (ƑP)ISNT;
            return i -> !apply(i);
        }

        @Override
        default ƑP<I> and(Predicate<? super I> p) {
            // we should retain the predicate identity if possible
            if(this==p) return this;
            else if((this==ISØ && p==ISNTØ) || (this==ISNTØ && p==ISØ)) return (ƑP)ISNT;
            else if(p==ISNT || this==ISNT) return (ƑP)ISNT;
            return i -> apply(i) && apply(i);
        }

        @Override
        default ƑP<I> or(Predicate<? super I> p) {
            // we should retain the predicate identity if possible
            if(this==p) return this;
            else if((this==ISØ && p==ISNTØ) || (this==ISNTØ && p==ISØ)) return (ƑP)IS;
            else if(this==IS || p==IS) return (ƑP)IS;
            return i -> apply(i) || apply(i);
        }

    }
    /**
     * Function throwing an exception.
     * <p/>
     * Due to the signature, it is impossible to extend {@link Consumer}
     */
    interface Ƒ1E<I,O,E extends Exception> extends Λ, IO<I,O> {
        O apply(I i) throws E;

        default Ƒ1E<I,O,E> onEx(O or, Class<?>... ecs) {
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
    /**
     * {@link Consumer} which throws an exception.
     * <p/>
     * Consumer version of {@link Ƒ1E}, so lambda expression does not need to return void (null)
     * at the end
     */
    // this class is ~pointless, although now lambda does not have to return null like in case of F1E,
    // but now the some method takes parameter of this class. Which will prevent
    // other F1E from being used!
    interface ƑEC<I,E extends Exception> extends Ƒ1E<I,Void,E> {

        @Override
        default Void apply(I i) throws E{
            accept(i);
            return null;
        }

        void accept(I i) throws E;
    }
    interface Ƒ2<I,I2,O> extends Λ, IO<I,O>, BiFunction<I,I2,O> {
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
    interface Ƒ3<I,I2,I3,O> extends Λ, IO<I,O> {
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
    interface Ƒ4<I,I2,I3,I4,O> extends Λ, IO<I,O> {
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
    interface Ƒ5<I,I2,I3,I4,I5,O> extends Λ, IO<I,O> {
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
    interface Ƒ6<I,I2,I3,I4,I5,I6,O> extends Λ, IO<I,O> {
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

    enum NullIn {
        NULL,
        APPLY
    }
    enum NullOut {
        NULL,
        INPUT
    }

    FunctorPool pool = new FunctorPool();

    class FunctorPool {
     
        // functor pools must not be accessed directly, as accessor must insert IDENTITY functor
        private final PrefListMap<PƑ,Class> fsI = new PrefListMap<>(pf -> pf.in);
        private final PrefListMap<PƑ,Class> fsO = new PrefListMap<>(pf -> pf.out);
        private final PrefListMap<PƑ,Integer> fsIO = new PrefListMap<>(pf -> Objects.hash(pf.in,pf.out));
    
        {
            /*
             * (1) Negated predicates are disabled, user interface should provide negation ability
             * or simply generate all the negations when needed (and reuse functors while at it).
             *
             * (2) Adding identity function here is impossible as its type is erased to Object -> Object
             * and we need its proper type X -> X (otherwise it erases type in function chains). Single
             * instance per class is required for Identity function.
             * Unfortunately:
             *     - we cant put it to functor pool, as we do not need which classes will need it
             *     - we cant put it to functor pool on requests, as the returned functors for class X
             *       return functors for X and all superclasses of X, which causes IDENTITY function
             *       to be inserted multiple times, even worse, only one of them has proper signature!
             *     - hence we cant even return Set to prevent duplicates, as the order of class
             *       is undefined. In addition, functors are actually wrapped.
             * Solution is to insert the proper IDENTITY functor into results, after they were
             * collected. This guarantees single instance and correct signature. The downside is that
             * the functor pool does not contain IDENTITY functor at all, meaning the pool must never
             * be accessed directly. Additionally, question arises, whether IDENTITY functor should be
             * inserted when no functors are returned.
             *
             * add("As self",      Object.class, Object.class, IDENTITY, true, true, true);
             */

            Class<Boolean> BOOL = Boolean.class;
            
            
            add("Is null",      Object.class, BOOL, ISØ);
        //  add("Isn't null", Object.class, BOOL, ISNTØ);
            add("As String",    Object.class, String.class, Objects::toString);
            add("As Boolean",   String.class, BOOL, Boolean::parseBoolean);
        
            add("Is true",  BOOL, BOOL, IS);
        //  add("Is false", BOOL, BOOL, ISNT);
            add("Negate",   BOOL, BOOL, b -> !b);
            add("And",      BOOL, BOOL, Boolean::logicalAnd, BOOL, true);
            add("Or",       BOOL, BOOL, Boolean::logicalOr, BOOL, true);
            add("Xor",      BOOL, BOOL, Boolean::logicalXor, BOOL, true);
            
            add("'_' -> ' '",   String.class, String.class, s -> s.replace("_", " "));
            add("-> file name", String.class, String.class, util.Util::filenamizeString);
            add("Anime",        String.class, String.class, s -> {
                // remove the super annoying '_'
                s = s.replaceAll("_", " ");

                // remove hash
                if (s.endsWith("]") && s.lastIndexOf('[') == s.length() - 10) s = s.substring(0, s.length() - 10);

                // remove fansub group
                String group = null;
                if (s.startsWith("[")) {
                    int i = s.indexOf(']');
                    if (i != -1) {
                        group = s.substring(0, i + 1);
                        s = s.substring(i + 1);
                    }
                }

                // remove leading and trailing shit
                s = s.trim();

                // add fansub groups at the end
                if (group != null) s = s + "." + group;

                return s;
            });
            add("To upper case",       String.class, String.class, String::toUpperCase);
            add("To lower case",       String.class, String.class, String::toLowerCase);
            add("Plural",              String.class, String.class, English::plural);
            add("Replace 1st (regex)", String.class, String.class, util.Util::replace1st, Pattern.class, String.class, Pattern.compile(""), "");
            add("Remove 1st (regex)",  String.class, String.class, util.Util::remove1st, Pattern.class, Pattern.compile(""));
            add("Replace all",         String.class, String.class, util.Util::replaceAll, String.class, String.class, "", "");
            add("Replace all (regex)", String.class, String.class, util.Util::replaceAllRegex, Pattern.class, String.class, Pattern.compile(""), "");
            add("Remove all",          String.class, String.class, util.Util::removeAll, String.class, "");
            add("Remove all (regex)",  String.class, String.class, util.Util::removeAllRegex, Pattern.class, Pattern.compile(""));
            add("Text",                String.class, String.class, (s, r) -> r, String.class, "");
            add("Text",                String.class, String.class, (s, c1, c2) -> new String(s.getBytes(c1), c2), Charset.class, Charset.class, UTF_8, UTF_8);
            add("Add text",            String.class, String.class, util.Util::addText, String.class, StringDirection.class, "", FROM_START);
            add("Remove chars",        String.class, String.class, util.Util::removeChars, Integer.class, StringDirection.class, 0, FROM_START);
            add("Retain chars",        String.class, String.class, util.Util::retainChars, Integer.class, StringDirection.class, 0, FROM_START);
            add("Trim",                String.class, String.class, String::trim);
            add("Split",               String.class, SplitData.class, util.Util::split, StringSplitParser.class, new StringSplitParser("%all%"));
            add("Split-join",          String.class, String.class, util.Util::splitJoin, StringSplitParser.class, StringSplitParser.class, new StringSplitParser("%all%"), new StringSplitParser("%all%"));
            add("Is",            String.class, BOOL, util.Util::equalsNoCase, String.class, BOOL, "", true);
            add("Contains",      String.class, BOOL, util.Util::containsNoCase, String.class, BOOL, "", true, false, false, true);
            add("Ends with",     String.class, BOOL, util.Util::endsWithNoCase, String.class, BOOL, "", true);
            add("Starts with",   String.class, BOOL, util.Util::startsWithNoCase, String.class, BOOL, "", true);
            add("Matches regex", String.class, BOOL, (text, r) -> r.matcher(text).matches(), Pattern.class, Pattern.compile(""));
            add("More",          String.class, BOOL, (x, y) -> x.compareTo(y) > 0, String.class, "");
            add("Less",          String.class, BOOL, (x, y) -> x.compareTo(y) < 0, String.class, "");
            add("Char at",       String.class, Character.class, util.Util::charAt, Integer.class, StringDirection.class, 0, FROM_START);
            add("Length",        String.class, Integer.class, String::length);
            add("Length >",      String.class, BOOL, (x, l) -> x.length() > l, Integer.class, 0);
            add("Length <",      String.class, BOOL, (x, l) -> x.length() < l, Integer.class, 0);
            add("Length =",      String.class, BOOL, (x, l) -> x.length() == l, Integer.class, 0);
            add("Is empty",      String.class, BOOL, String::isEmpty);
            add("Is palindrome", String.class, BOOL, util.Util::isNonEmptyPalindrome);
            
            add("to ASCII", Character.class, Integer.class, x -> (int) x);
            
            add("Path",       File.class,String.class, File::getAbsolutePath);
            add("Size",       File.class,FileSize.class, FileSize::new);
            add("Name",       File.class,String.class, Util::getName, true,true,true);
            add("Name.Suffix",File.class,String.class, File::getName);
            add("Suffix",     File.class,String.class, Util::getSuffix);
            add("MimeType",   File.class,MimeType.class, f -> App.APP.mimeTypes.ofFile(f));
            add("MimeGroup",  File.class,String.class, f -> App.APP.mimeTypes.ofFile(f).getGroup());
            add("Shortcut target",  File.class,File.class, f -> WindowsShortcut.targetedFile(f).orElse(null));
    
            add("Group",      MimeType.class,String.class, MimeType::getGroup);
            add("Extensions", MimeType.class,String.class, m -> toS(", ", m.getExtensions()));
    
            add("Less",      Bitrate.class,BOOL,(x,y) -> x.compareTo(y)<0, Bitrate.class,new Bitrate(320));
            add("Is",        Bitrate.class,BOOL,(x,y) -> x.compareTo(y)==0, Bitrate.class,new Bitrate(320));
            add("More",      Bitrate.class,BOOL,(x,y) -> x.compareTo(y)>0, Bitrate.class,new Bitrate(320));
            add("Is good",   Bitrate.class,BOOL, x -> x.getValue()>=320);
            add("Is bad",    Bitrate.class,BOOL, x -> x.getValue()<=128);
            add("Is unknown",Bitrate.class,BOOL, x -> x.getValue()==-1);
            add("Is known",  Bitrate.class,BOOL, x -> x.getValue()>-1);
    
            add("Less",      FormattedDuration.class,BOOL,(x,y) -> x.compareTo(y)<0, FormattedDuration.class, new FormattedDuration(0));
            add("Is",        FormattedDuration.class,BOOL,(x,y) -> x.compareTo(y)==0, FormattedDuration.class, new FormattedDuration(0));
            add("More",      FormattedDuration.class,BOOL,(x,y) -> x.compareTo(y)>0, FormattedDuration.class, new FormattedDuration(0),false,false,true);
    
            add("<  Less",      NofX.class,BOOL, (x,y) -> x.compareTo(y)< 0, NofX.class,new NofX(1,1));
            add("=  Is",        NofX.class,BOOL, (x,y) -> x.compareTo(y)==0, NofX.class,new NofX(1,1));
            add(">  More",      NofX.class,BOOL, (x,y) -> x.compareTo(y)> 0, NofX.class,new NofX(1,1),false,false,true);
            add(">= Not less",  NofX.class,BOOL, (x,y) -> x.compareTo(y)>=0, NofX.class,new NofX(1,1));
            add("<> Is not",    NofX.class,BOOL, (x,y) -> x.compareTo(y)!=0, NofX.class,new NofX(1,1));
            add("<= Not more",  NofX.class,BOOL, (x,y) -> x.compareTo(y)<=0, NofX.class,new NofX(1,1));
    
            add("<  Less",      FileSize.class,BOOL, (x,y) -> x.compareTo(y)< 0, FileSize.class,new FileSize(0),false,false,true);
            add("=  Is",        FileSize.class,BOOL, (x,y) -> x.compareTo(y)==0, FileSize.class,new FileSize(0));
            add(">  More",      FileSize.class,BOOL, (x,y) -> x.compareTo(y)> 0, FileSize.class,new FileSize(0));
            add("Is unknown",   FileSize.class,BOOL, x -> x.inBytes()==-1);
            add("Is known",     FileSize.class,BOOL, x -> x.inBytes()>-1);
    
            add("Is after",     Year.class,BOOL, (x,y) -> x.compareTo(y)> 0, Year.class,Year.now());
            add("Is",           Year.class,BOOL, (x,y) -> x.compareTo(y)==0, Year.class,Year.now());
            add("Is before",    Year.class,BOOL, (x,y) -> x.compareTo(y)< 0, Year.class,Year.now());
            add("Is in the future",  Year.class,BOOL, x -> x.compareTo(Year.now())> 0);
            add("Is now",            Year.class,BOOL, x -> x.compareTo(Year.now())==0);
            add("Is in the past",    Year.class,BOOL, x -> x.compareTo(Year.now())< 0);
            add("Is leap",      Year.class,BOOL, Year::isLeap);
    
            add("Contains year",RangeYear.class,BOOL, RangeYear::contains, Year.class,Year.now());
            add("Is after",     RangeYear.class,BOOL, RangeYear::isAfter,  Year.class,Year.now());
            add("Is before",    RangeYear.class,BOOL, RangeYear::isBefore, Year.class,Year.now());
            add("Is in the future",  RangeYear.class,BOOL, x -> x.contains(Year.now()));
            add("Is now",            RangeYear.class,BOOL, x -> x.isAfter(Year.now()));
            add("Is in the past",    RangeYear.class,BOOL, x -> x.isBefore(Year.now()));
    
            add("After",   LocalDateTime.class,BOOL, LocalDateTime::isAfter,  LocalDateTime.class,LocalDateTime.now());
            add("Before",  LocalDateTime.class,BOOL, LocalDateTime::isBefore, LocalDateTime.class,LocalDateTime.now());
            add("Is",      LocalDateTime.class,BOOL, LocalDateTime::isEqual,  LocalDateTime.class,LocalDateTime.now());
    
            add("File",    Item.class,File.class, Item::getFile);
    
            add("Is supported", AudioFileFormat.class,Boolean.class, x -> x.isSupported(APP));
            add("Is playable", AudioFileFormat.class,Boolean.class, x -> x.isSupported(PLAYBACK));
            addPredicatesOf(AudioFileFormat.class);
            addPredicatesOf(ImageFileFormat.class);
    
            addPredicatesComparable(Short.class, new Short("0"));
            addPredicatesComparable(Integer.class, 0);
            addPredicatesComparable(Long.class, 0L);
            addPredicatesComparable(Double.class, 0d);
            addPredicatesComparable(Float.class, 0f);
    
            // fielded values
            for(Metadata.Field f : Metadata.Field.values())
                add(f.name(), Metadata.class, f.getType(), f::getOf);
            for(PlaylistItem.Field f : PlaylistItem.Field.values())
                add(f.name(), PlaylistItem.class, f.getType(), f::getOf);
            for(MetadataGroup.Field f : MetadataGroup.Field.values())
                add(f.name(), MetadataGroup.class, f.getType(), f::getOf);
        }
    
        public <I,O> void add(String name, Class<I> i ,Class<O> o, Ƒ1<? super I,O> f) {
            addF(new PƑ0(name,i,o,f));
        }
    
        public <I,P1,O> void add(String name, Class<I> i, Class<O> o, Ƒ2<I,P1,O> f, Class<P1> p1, P1 p1def) {
            addF(new PƑ1(name,i,o,f,new Parameter<>(p1, p1def)));
        }
    
        public <I,P1,P2,O> void add(String name, Class<I> i,Class<O> o, Ƒ3<I,P1,P2,O> f, Class<P1> p1, Class<P2> p2, P1 p1def, P2 p2def) {
            addF(new PƑ2(name,i,o,f,new Parameter<>(p1, p1def),new Parameter<>(p2, p2def)));
        }
    
        public <I,P1,P2,P3,O> void add(String name, Class<I> i,Class<O> o, Ƒ4<I,P1,P2,P3,O> f, Class<P1> p1, Class<P2> p2, Class<P3> p3, P1 p1def, P2 p2def, P3 p3def) {
            addF(new PƑ3(name,i,o,f,new Parameter<>(p1, p1def),new Parameter<>(p2, p2def),new Parameter<>(p3, p3def)));
        }
    
        public <I,O> void add(String name, Class<I> i ,Class<O> o, Ƒ1<I,O> f, boolean pi, boolean po, boolean pio) {
            addF(new PƑ0(name,i,o,f),pi,po,pio);
        }
    
        public <I,P1,O> void add(String name, Class<I> i, Class<O> o, Ƒ2<I,P1,O> f, Class<P1> p1, P1 p1def, boolean pi, boolean po, boolean pio) {
            addF(new PƑ1(name,i,o,f,new Parameter<>(p1,p1def)),pi,po,pio);
        }
    
        public <I,P1,P2,O> void add(String name, Class<I> i,Class<O> o, Ƒ3<I,P1,P2,O> f, Class<P1> p1, Class<P2> p2, P1 p1def, P2 p2def, boolean pi, boolean po, boolean pio) {
            addF(new PƑ2(name,i,o,f,new Parameter<>(p1,p1def),new Parameter<>(p2,p2def)),pi,po,pio);
        }
    
        public <I,P1,P2,P3,O> void add(String name, Class<I> i,Class<O> o, Ƒ4<I,P1,P2,P3,O> f, Class<P1> p1, Class<P2> p2, Class<P3> p3, P1 p1def, P2 p2def, P3 p3def, boolean pi, boolean po, boolean pio) {
            addF(new PƑ3(name,i,o,f,new Parameter<>(p1,p1def),new Parameter<>(p2,p2def),new Parameter<>(p3,p3def)),pi,po,pio);
        }
    
        public <E extends Enum> void addPredicatesOf(Class<E> c) {
            add("Is", c,Boolean.class, (a,b) -> a==b, c, c.getEnumConstants()[0], false,false,true);
        }
    
        public <C extends Comparable> void addPredicatesComparable(Class<C> c, C def_val) {
            add("Is less",     c,Boolean.class, (x,y) -> x.compareTo(y)<0,  c,def_val);
            add("Is",          c,Boolean.class, (x,y) -> x.compareTo(y)==0, c,def_val);
            add("Is more",     c,Boolean.class, (x,y) -> x.compareTo(y)>0,  c,def_val);
            // add("Is not less", c,Boolean.class, (x,y) -> x.compareTo(y)>=0, c,def_val);
            // add("Is not",      c,Boolean.class, (x,y) -> x.compareTo(y)!=0, c,def_val);
            // add("Is not more", c,Boolean.class, (x,y) -> x.compareTo(y)<=0, c,def_val);
        }
    
        /** Add function to the pool. */
        public void addF(PƑ f) {
            fsI.accumulate(f);
            fsO.accumulate(f);
            fsIO.accumulate(f);
        }
    
        /** Add function to the pool and sets as preferred according to parameters. */
        public void addF(PƑ f, boolean i, boolean o, boolean io) {
            fsI.accumulate(f, i);
            fsO.accumulate(f, o);
            fsIO.accumulate(f, io);
        }
    
        /** Remove function from the pool. */
        public void remF(PƑ f) {
            fsI.deaccumulate(f);
            fsO.deaccumulate(f);
            fsIO.deaccumulate(f);
        }
    
        private <T> void addSelfFunctor(PrefList l, Class<T> c) {
            Object p = l.getPrefered();
            // l.addPreferred(new PƑ0("As self", c, c, IDENTITY), p==null); // !working as it should?
            l.add(0,new PƑ0("As self", c, c, IDENTITY));
        }
    
        /** Returns all functions taking input I. */
        public <I> PrefList<PƑ<I,?>> getI(Class<I> i) {
            PrefList l = (PrefList) fsI.getElementsOf(util.type.Util.getSuperClassesInc(unPrimitivize(i)));
            addSelfFunctor(l,i);
            return l;
        }
    
        /** Returns all functions producing output O. */
        public <O> PrefList<PƑ<?,O>> getO(Class<O> o) {
            List ll = fsO.get(unPrimitivize(o));
            PrefList l =  ll==null ? new PrefList() : (PrefList) ll;
            addSelfFunctor(l,o);
            return l;
        }
    
        /** Returns all functions taking input I and producing output O. */
        public <I,O> PrefList<PƑ<I,O>> getIO(Class<I> i, Class<O> o) {
            // this is rather messy, but works
            // we accumulate result for all superclasses & retain first found preferred element, while
            // keeping duplicate elements in check
            PrefList pl = new PrefList();
            Object pref = null;
            for(Class c : util.type.Util.getSuperClassesInc(unPrimitivize(i))) {
                List l = fsIO.get(Objects.hash(c, unPrimitivize(o)));
                PrefList ll = l==null ? null : (PrefList) l;
                if(ll!=null) {
                    if(pref==null && ll.getPreferedOrFirst()!=null) pref = ll.getPreferedOrFirst();
                    pl.addAll(ll);
                }
            }
            Object prefCopy = pref;
            pl.removeIf(e -> e==prefCopy || e==null);
            if(pref!=null) pl.addPreferred(pref);
            if(pl.getPreferedOrFirst()==null && !pl.isEmpty()) {
                Object e = pl.get(pl.size()-1);
                pl.remove(pl.size()-1);
                pl.addPreferred(e);
            }
    
            if(i.equals(o)) addSelfFunctor(pl,o);
            return pl;
    
            // old impl, ignores super classes
    //        PrefList l = (PrefList) fsIO.get(Objects.hash(unPrimitivize(i),unPrimitivize(o)));
    //        return l==null ? new PrefList() : l;
        }
    
        /** Returns all functions taking input IO and producing output IO. */
        public <IO> PrefList<PƑ<IO,IO>> getIO(Class<IO> io) {
            return getIO(io, io);
        }
    
        public <I,O> PƑ<I,O> getPF(String name, Class<I> i, Class<O> o) {
            @SuppressWarnings("unchecked")
            List<PƑ<I,O>> l = (List) fsIO.get(Objects.hash(unPrimitivize(i), unPrimitivize(o)));
            return l==null ? null : stream(l).findAny(f -> f.name.equals(name)).orElse(null);
        }
    
        public <I> PƑ<I,?> getPrefI(Class<I> i) {
            PrefList<PƑ<I,?>> pl = getI(i);
            return pl==null ? null : pl.getPreferedOrFirst();
        }
    
        public <O> PƑ<?,O> getPrefO(Class<O> o) {
            PrefList<PƑ<?,O>> l = getO(o);
            return l==null ? null : l.getPreferedOrFirst();
        }
    
        public <I,O> PƑ<I,O> getPrefIO(Class<I> i, Class<O> o) {
            PrefList<PƑ<I,O>> l = getIO(i,o);
            return l==null ? null : l.getPreferedOrFirst();
        }
    
        public <IO> PƑ<IO,IO> getPrefIO(Class<IO> io) {
            return getPrefIO(io,io);
        }

    }

    class Parameter<P> {
        public final Class<P> type;
        public final P defaultValue;
        public final String name;
        public final String description;

        public Parameter(Class<P> type, P defaultValue) {
            this("", "", type, defaultValue);
        }

        public Parameter(String name, String description, Class<P> type, P defaultValue) {
            this.name = name;
            this.description = description;
            this.type = unPrimitivize(type);
            this.defaultValue = defaultValue;
        }
    }
    interface Parameterized<P> {
        List<Parameter<? extends P>> getParameters();
    }
    // parameterized function - variadic I -> O function factory with parameters
    abstract class PƑ<I,O> implements Ƒ2<I,Object[],O>, Parameterized<Object> {
        public final String name;
        public final Class<I> in;
        public final Class<O> out;
        private final IO<I,O> ff;

        @SuppressWarnings("unchecked")
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
            if(isInR(ff, IDENTITY,ISØ,ISNTØ,IS,ISNT)) return (Ƒ1<I,O>)ff;
            return new TypeAwareƑ<>(i -> apply(i, is),in,out);
            // return i -> apply(i, is); // would not preserve I,O types
        }

    }
    // solely to hide generic parameter of PF above, the 3rd parameter (F) is implementation
    // detail - we do not want it to pollute external code, in fact this parameter exists solely
    // so PƑ can access its underlying function, while not breaking type safety for subclasses
    abstract class PƑB<I,O,F extends IO<I,O>> extends PƑ<I,O> {

        public final F f;

        public PƑB(String name, Class<I> in, Class<O> out, F f) {
            super(name, in, out, f);
            this.f = f;
        }

    }
    /**
     * Parametric function, {@code In -> Out} function defined as {@code (In, P1, P2, ..., Pn) -> Out} variadic
     * function with parameters. Formally, the signature is {@code (In, Param...) -> Out}, but the parameters are
     * degrees of freedom, fixing of which collapses the signature to {@code In -> Out} (as in partial application),
     * which can be applied on the input. While the parameters themselves are technically inputs, they are transparent
     * for the function user, (which should only see the collapsed signature) and serve as a variadic generalisation
     * of a function - to express function of any number of parameters equally. This is useful for example for ui
     * function builders.
     */
    class PƑ0<I,O> extends PƑB<I,O,Ƒ1<I,O>> {

        public PƑ0(String _name, Class<I> i, Class<O> o, Ƒ1<I,O> f) {
            super(_name,i,o,f);
        }

        @Override
        public List<Parameter<? extends Object>> getParameters() {
            return EMPTY_LIST;
        }

        @Override
        public O apply(I t, Object... ps) {
            return f.apply(t);
        }

    }
    /** Unary parametric function. */
    class PƑ1<I,P1,O> extends PƑB<I,O,Ƒ2<I,P1,O>> {
        private Parameter<P1> p1;

        public PƑ1(String _name, Class<I> i, Class<O> o, Ƒ2<I,P1,O> f, Parameter<P1> p1) {
            super(_name,i,o,f);
            this.p1 = p1;
        }

        @Override
        public List<Parameter<? extends Object>> getParameters() {
            return list(p1);
        }

        @Override
        public O apply(I t, Object... ps) {
             return f.apply(t, (P1)ps[0]);
        }
    }
    /** Binary parametric function. */
    class PƑ2<I,P1,P2,O> extends PƑB<I,O,Ƒ3<I,P1,P2,O>> {
        private Parameter<P1> p1;
        private Parameter<P2> p2;

        public PƑ2(String _name, Class<I> i, Class<O> o, Ƒ3<I,P1,P2,O> f, Parameter<P1> p1, Parameter<P2> p2) {
            super(_name,i,o,f);
            this.p1 = p1;
            this.p2 = p2;
        }

        @Override
        public List<Parameter<? extends Object>> getParameters() {
            return list(p1, p2);
        }

        @Override
        public O apply(I t, Object... ps) {
             return f.apply(t, (P1)ps[0], (P2)ps[1]);
        }
    }
    /** Tertiary  parametric function. */
    class PƑ3<I,P1,P2,P3,O> extends PƑB<I,O,Ƒ4<I,P1,P2,P3,O>> {
        private Parameter<P1> p1;
        private Parameter<P2> p2;
        private Parameter<P3> p3;

        public PƑ3(String _name, Class<I> i, Class<O> o, Ƒ4<I,P1,P2,P3,O> f, Parameter<P1> p1, Parameter<P2> p2, Parameter<P3> p3) {
            super(_name,i,o,f);
            this.p1 = p1;
            this.p2 = p2;
            this.p3 = p3;
        }

        @Override
        public List<Parameter<? extends Object>> getParameters() {
            return list(p1, p2, p3);
        }

        @Override
        public O apply(I t, Object... ps) {
             return f.apply(t, (P1)ps[0], (P2)ps[1], (P3)ps[2]);
        }
    }
    /** N-ary parametric function. */
    class PƑn<I,O> extends PƑB<I,O,Ƒ2<I,Object[],O>> {
        private Parameter<Object>[] ps;

        public PƑn(String _name, Class<I> i, Class<O> o, Ƒ2<I,Object[],O> f, Parameter<Object>[] ps) {
            super(_name,i,o,f);
            this.ps = ps;
        }

        @Override
        public List<Parameter<? extends Object>> getParameters() {
            return list(ps);
        }

        @Override
        public O apply(I t, Object... ps) {
             return f.apply(t,ps);
        }
    }

    class CƑ<I,O> implements Ƒ1<I,O>, Configurable<Object> {

        final PƑ<I,O> pf;
        private final List<Config<Object>> cs = new ArrayList();

        public CƑ(PƑ<I, O> pf) {
            this.pf = pf;
            cs.addAll(map(pf.getParameters(), p -> {
                V<Object> a = new V<>(p.defaultValue);
                return new AccessorConfig<>(p.type,"",a::setValue,(Supplier)a::getValue);
            }));
        }

        @Override
        public O apply(I i) {
            return pf.apply(i, cs.stream().map(Config::getValue).toArray());
        }

    }
    class TypeAwareƑ<I,O> implements Ƒ1<I,O> {
        public final Class<I> in;
        public final Class<O> out;
        public final Ƒ1<I,O> f;

        public TypeAwareƑ(Ƒ1<I,O> f, Class<I> in, Class<O> out) {
            this.in = in;
            this.out = out;
            this.f = f;
        }

        @Override
        public O apply(I i) {
            return f.apply(i);
        }

    }
}