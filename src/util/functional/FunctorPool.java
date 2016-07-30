package util.functional;

import java.io.File;
import java.nio.charset.Charset;
import java.time.LocalDateTime;
import java.time.Year;
import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;

import org.atteo.evo.inflector.English;

import audio.Item;
import audio.playlist.PlaylistItem;
import audio.tagging.Metadata;
import audio.tagging.MetadataGroup;
import gui.itemnode.StringSplitParser;
import gui.itemnode.StringSplitParser.SplitData;
import main.App;
import util.collections.list.PrefList;
import util.collections.map.PrefListMap;
import util.file.AudioFileFormat;
import util.file.ImageFileFormat;
import util.file.Util;
import util.file.WindowsShortcut;
import util.file.mimetype.MimeType;
import util.functional.Functors.*;
import util.units.*;

import static java.nio.charset.StandardCharsets.UTF_8;
import static util.Util.StringDirection;
import static util.Util.StringDirection.FROM_START;
import static util.file.AudioFileFormat.Use.APP;
import static util.file.AudioFileFormat.Use.PLAYBACK;
import static util.functional.Util.*;
import static util.type.Util.getSuperClassesInc;
import static util.type.Util.unPrimitivize;

public class FunctorPool {

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
		add("Or",       BOOL, BOOL, Boolean::logicalOr,  BOOL, true);
		add("Xor",      BOOL, BOOL, Boolean::logicalXor, BOOL, true);

		add("'_' -> ' '",   String.class, String.class, s -> s.replace("_", " "));
		add("-> file name", String.class, String.class, util.Util::filenamizeString);
		add("Anime",        String.class, String.class, util.Util::renameAnime);
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

		add("Less",      Bitrate.class,BOOL,(x, y) -> x.compareTo(y)<0, Bitrate.class,new Bitrate(320));
		add("Is",        Bitrate.class,BOOL,(x,y) -> x.compareTo(y)==0, Bitrate.class,new Bitrate(320));
		add("More",      Bitrate.class,BOOL,(x,y) -> x.compareTo(y)>0, Bitrate.class,new Bitrate(320));
		add("Is good",   Bitrate.class,BOOL, x -> x.getValue()>=320);
		add("Is bad",    Bitrate.class,BOOL, x -> x.getValue()<=128);
		add("Is unknown",Bitrate.class,BOOL, x -> x.getValue()==-1);
		add("Is known",  Bitrate.class,BOOL, x -> x.getValue()>-1);

		add("Less",      FormattedDuration.class,BOOL,(x, y) -> x.compareTo(y)<0, FormattedDuration.class, new FormattedDuration(0));
		add("Is",        FormattedDuration.class,BOOL,(x,y) -> x.compareTo(y)==0, FormattedDuration.class, new FormattedDuration(0));
		add("More",      FormattedDuration.class,BOOL,(x,y) -> x.compareTo(y)>0, FormattedDuration.class, new FormattedDuration(0),false,false,true);

		add("<  Less",      NofX.class,BOOL, (x, y) -> x.compareTo(y)< 0, NofX.class,new NofX(1,1));
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

		add("Is after",     Year.class,BOOL, (x, y) -> x.compareTo(y)> 0, Year.class,Year.now());
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

		addPredicatesComparable(Short.class, (short)0);
		addPredicatesComparable(Integer.class, 0);
		addPredicatesComparable(Long.class, 0L);
		addPredicatesComparable(Double.class, 0d);
		addPredicatesComparable(Float.class, 0f);

		// fielded values
		for (Metadata.Field f : Metadata.Field.values())
			add(f.name(), Metadata.class, f.getType(), f::getOf);
		for (PlaylistItem.Field f : PlaylistItem.Field.values())
			add(f.name(), PlaylistItem.class, f.getType(), f::getOf);
		for (MetadataGroup.Field f : MetadataGroup.Field.values())
			add(f.name(), MetadataGroup.class, f.getType(), f::getOf);
	}

	public <I,O> void add(String name, Class<I> i ,Class<O> o, Ƒ1<? super I, ? extends O> f) {
		addF(new PƑ0(name,i,o,f));
	}

	public <I,P1,O> void add(String name, Class<I> i, Class<O> o, Ƒ2<I,P1,O> f, Class<P1> p1, P1 p1def) {
		addF(new PƑ1<>(name,i,o,f,new Parameter<>(p1, p1def)));
	}

	public <I,P1,P2,O> void add(String name, Class<I> i,Class<O> o, Ƒ3<I,P1,P2,O> f, Class<P1> p1, Class<P2> p2, P1 p1def, P2 p2def) {
		addF(new PƑ2<>(name,i,o,f,new Parameter<>(p1, p1def),new Parameter<>(p2, p2def)));
	}

	public <I,P1,P2,P3,O> void add(String name, Class<I> i,Class<O> o, Ƒ4<I,P1,P2,P3,O> f, Class<P1> p1, Class<P2> p2, Class<P3> p3, P1 p1def, P2 p2def, P3 p3def) {
		addF(new PƑ3<>(name,i,o,f,new Parameter<>(p1, p1def),new Parameter<>(p2, p2def),new Parameter<>(p3, p3def)));
	}

	public <I,O> void add(String name, Class<I> i ,Class<O> o, Ƒ1<I,O> f, boolean pi, boolean po, boolean pio) {
		addF(new PƑ0<>(name,i,o,f),pi,po,pio);
	}

	public <I,P1,O> void add(String name, Class<I> i, Class<O> o, Ƒ2<I,P1,O> f, Class<P1> p1, P1 p1def, boolean pi, boolean po, boolean pio) {
		addF(new PƑ1<>(name,i,o,f,new Parameter<>(p1,p1def)),pi,po,pio);
	}

	public <I,P1,P2,O> void add(String name, Class<I> i,Class<O> o, Ƒ3<I,P1,P2,O> f, Class<P1> p1, Class<P2> p2, P1 p1def, P2 p2def, boolean pi, boolean po, boolean pio) {
		addF(new PƑ2<>(name,i,o,f,new Parameter<>(p1,p1def),new Parameter<>(p2,p2def)),pi,po,pio);
	}

	public <I,P1,P2,P3,O> void add(String name, Class<I> i,Class<O> o, Ƒ4<I,P1,P2,P3,O> f, Class<P1> p1, Class<P2> p2, Class<P3> p3, P1 p1def, P2 p2def, P3 p3def, boolean pi, boolean po, boolean pio) {
		addF(new PƑ3<>(name,i,o,f,new Parameter<>(p1,p1def),new Parameter<>(p2,p2def),new Parameter<>(p3,p3def)),pi,po,pio);
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
		PrefList l = (PrefList) fsI.getElementsOf(getSuperClassesInc(unPrimitivize(i)));
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
		for (Class c : getSuperClassesInc(unPrimitivize(i))) {
			List l = fsIO.get(Objects.hash(c, unPrimitivize(o)));
			PrefList ll = l==null ? null : (PrefList) l;
			if (ll!=null) {
				if (pref==null && ll.getPreferedOrFirst()!=null) pref = ll.getPreferedOrFirst();
				pl.addAll(ll);
			}
		}
		Object prefCopy = pref;
		pl.removeIf(e -> e==prefCopy || e==null);
		if (pref!=null) pl.addPreferred(pref);
		if (pl.getPreferedOrFirst()==null && !pl.isEmpty()) {
			Object e = pl.get(pl.size()-1);
			pl.remove(pl.size()-1);
			pl.addPreferred(e);
		}

		if (i.equals(o)) addSelfFunctor(pl,o);
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
