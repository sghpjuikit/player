package util.functional;

import audio.Item;
import audio.playlist.PlaylistItem;
import audio.tagging.Metadata;
import audio.tagging.MetadataGroup;
import gui.itemnode.StringSplitParser;
import gui.itemnode.StringSplitParser.SplitData;
import java.io.File;
import java.nio.charset.Charset;
import java.time.LocalDateTime;
import java.time.Year;
import java.util.Base64;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;
import javafx.util.Duration;
import org.atteo.evo.inflector.English;
import util.collections.list.PrefList;
import util.collections.map.PrefListMap;
import util.file.AudioFileFormat;
import util.file.FileType;
import util.file.Util;
import util.file.UtilKt;
import util.file.WindowsShortcut;
import util.file.mimetype.MimeExt;
import util.file.mimetype.MimeType;
import util.functional.Functors.Parameter;
import util.functional.Functors.PƑ;
import util.functional.Functors.PƑ0;
import util.functional.Functors.PƑ1;
import util.functional.Functors.PƑ2;
import util.functional.Functors.PƑ3;
import util.functional.Functors.Ƒ1;
import util.functional.Functors.Ƒ2;
import util.functional.Functors.Ƒ3;
import util.functional.Functors.Ƒ4;
import util.text.Strings;
import util.units.Bitrate;
import util.units.FileSize;
import util.units.NofX;
import util.units.RangeYear;
import static java.nio.charset.StandardCharsets.UTF_8;
import static util.Util.StringDirection;
import static util.Util.StringDirection.FROM_START;
import static util.file.AudioFileFormat.Use.APP;
import static util.file.AudioFileFormat.Use.PLAYBACK;
import static util.file.mimetype.MimeTypesKt.mimeType;
import static util.functional.Util.IDENTITY;
import static util.functional.Util.IS;
import static util.functional.Util.ISØ;
import static util.functional.Util.stream;
import static util.functional.Util.toS;
import static util.text.UtilKt.isPalindrome;
import static util.type.Util.getEnumConstants;
import static util.type.Util.getSuperClassesInc;
import static util.type.Util.isEnum;
import static util.type.Util.unPrimitivize;

// TODO: fix all warnings
public class FunctorPool {

	// functor pools must not be accessed directly, as accessor must insert IDENTITY functor
	private final PrefListMap<PƑ,Class<?>> fsI = new PrefListMap<>(pf -> pf.in);
	private final PrefListMap<PƑ,Class<?>> fsO = new PrefListMap<>(pf -> pf.out);
	private final PrefListMap<PƑ,Integer> fsIO = new PrefListMap<>(pf -> Objects.hash(pf.in,pf.out));
	private final Set<Class> cacheStorage = new HashSet<>();

	{
		/*
		 * (1) Negated predicates are disabled, user interface should provide negation ability
		 * or simply generate all the negations when needed (and reuse functors while at it).
		 *
		 * (2) Adding identity function here is impossible as its type is erased to Object -> Object
		 * and we need its proper type X -> X (otherwise it erases type in function chains). Single
		 * instance per class is required for Identity function.
		 * Unfortunately:
		 *     - we cant put it to functor pool, as we do not know which classes will need it
		 *     - we cant put it to functor pool on requests, as the returned functors for class X
		 *       return functors for X and all superclasses of X, which causes IDENTITY function
		 *       to be inserted multiple times, even worse, only one of them has proper signature!
		 *     - hence we cant even return Set to prevent duplicates, as the order of class iteration
		 *       is undefined. In addition, functors are actually wrapped.
		 * Solution is to insert the proper IDENTITY functor into results, after they were
		 * collected. This guarantees single instance and correct signature. The downside is that
		 * the functor pool does not contain IDENTITY functor at all, meaning the pool must never
		 * be accessed directly. Additionally, question arises, whether IDENTITY functor should be
		 * inserted when no functors are returned.
		 *
		 * add("As self",      Object.class, Object.class, IDENTITY, true, true, true);
		 */

		Class<Boolean> B = Boolean.class;
		Class<String> S = String.class;

		add("Is null",      Object.class, B, ISØ);
		//  add("Isn't null", Object.class, BOOL, ISNTØ);
		add("As String",    Object.class, String.class, Objects::toString);
		add("As Boolean",   String.class, B, Boolean::parseBoolean);

		add("Is true",  B, B, IS);
		//  add("Is false", BOOL, BOOL, ISNT);
		add("Negate",   B, B, b -> !b);
		add("And",      B, B, Boolean::logicalAnd, B, true);
		add("Or",       B, B, Boolean::logicalOr,  B, true);
		add("Xor",      B, B, Boolean::logicalXor, B, true);

		add("'_' -> ' '",    S, S, s -> s.replace("_", " "));
		add("-> file name",  S, S, util.Util::filenamizeString);
		add("Anime",         S, S, util.Util::renameAnime);
		add("To upper case",       S, S, String::toUpperCase);
		add("To lower case",       S, S, String::toLowerCase);
		add("Plural",              S, S, English::plural);
		add("Replace 1st (regex)", S, S, util.Util::replace1st, Pattern.class, S, Pattern.compile(""), "");
		add("Remove 1st (regex)",  S, S, util.Util::remove1st, Pattern.class, Pattern.compile(""));
		add("Replace all",         S, S, util.Util::replaceAll, S, S, "", "");
		add("Replace all (regex)", S, S, util.Util::replaceAllRegex, Pattern.class, S, Pattern.compile(""), "");
		add("Remove all",          S, S, util.Util::removeAll, S, "");
		add("Remove all (regex)",  S, S, util.Util::removeAllRegex, Pattern.class, Pattern.compile(""));
		add("Text",                S, S, (s, r) -> r, S, "");
		add("Re-encode",           S, S, (s, c1, c2) -> new String(s.getBytes(c1), c2), Charset.class, Charset.class, UTF_8, UTF_8);
		add("Add text",            S, S, util.Util::addText, S, StringDirection.class, "", FROM_START);
		add("Remove chars",        S, S, util.Util::removeChars, Integer.class, StringDirection.class, 0, FROM_START);
		add("Retain chars",        S, S, util.Util::retainChars, Integer.class, StringDirection.class, 0, FROM_START);
		add("Trim",                S, S, String::trim);
		add("Split",               S, SplitData.class, util.Util::split, StringSplitParser.class, new StringSplitParser("%all%"));
		add("Split-join",          S, S, util.Util::splitJoin, StringSplitParser.class, StringSplitParser.class, new StringSplitParser("%all%"), new StringSplitParser("%all%"));
		add("Is",            S, B, util.Util::equalsNoCase, S, B, "", true);
		add("Contains",      S, B, util.Util::containsNoCase, S, B, "", true, false, false, true);
		add("Ends with",     S, B, util.Util::endsWithNoCase, S, B, "", true);
		add("Starts with",   S, B, util.Util::startsWithNoCase, S, B, "", true);
		add("Matches regex", S, B, (text, r) -> r.matcher(text).matches(), Pattern.class, Pattern.compile(""));
		add("After",         S, B, (x, y) -> x.compareTo(y) > 0, S, "");
		add("Before",        S, B, (x, y) -> x.compareTo(y) < 0, S, "");
		add("Char at",       S, Character.class, util.Util::charAt, Integer.class, StringDirection.class, 0, FROM_START);
		add("Length",        S, Integer.class, String::length);
		add("Length >",      S, B, (x, l) -> x.length() > l, Integer.class, 0);
		add("Length <",      S, B, (x, l) -> x.length() < l, Integer.class, 0);
		add("Length =",      S, B, (x, l) -> x.length() == l, Integer.class, 0);
		add("Is empty",      S, B, s -> s.isEmpty());
		add("Is palindrome", S, B, s -> isPalindrome(s));
		add("Base64 encode", S, S, s -> Base64.getEncoder().encodeToString(s.getBytes()));
		add("Base64 decode", S, S, s -> {
			try {
				return new String(Base64.getDecoder().decode(s.getBytes()));
			} catch (IllegalArgumentException e) {
				return null;
			}
		});
		add("To file",       S, File.class, File::new);

		add("Any contains",  Strings.class, B, Strings::anyContains, S, B, "", true);
		add("Is empty",      Strings.class, B, Strings::isEmpty);
		add("Elements",      Strings.class, Integer.class, Strings::size);

		add("to ASCII",     Character.class, Integer.class, x -> (int) x);

		add("Path",         File.class,String.class, File::getAbsolutePath);
		add("Size",         File.class,FileSize.class, FileSize::new);
		add("Name",         File.class,String.class, UtilKt::getNameWithoutExtensionOrRoot, true,true,true);
		add("Name.Suffix",  File.class,String.class, UtilKt::getNameOrRoot);
		add("Suffix",       File.class,String.class, Util::getSuffix);
		add("MimeType",     File.class,MimeType.class, f -> mimeType(f));
		add("MimeGroup",    File.class,String.class, f -> mimeType(f).getGroup());
		add("Shortcut of",  File.class,File.class, f -> WindowsShortcut.targetedFile(f).orElse(null));
		add("Type",         File.class,FileType.class, FileType::of);
		add("Exists",       File.class,B,File::exists);
		add("Anime",        File.class,S, f -> util.Util.renameAnime(UtilKt.getNameWithoutExtensionOrRoot(f)));

		add("Group",        MimeType.class,S, MimeType::getGroup);
		add("Extensions",   MimeType.class,S, m -> toS(", ", m.getExtensions()));

		add("Is",           MimeExt.class, B, (x,y) -> x.equals(y), MimeExt.class, new MimeExt("mp3"));

		add("Less",         Bitrate.class,B, (x,y) -> x.compareTo(y)<0, Bitrate.class,new Bitrate(320));
		add("Is",           Bitrate.class,B, (x,y) -> x.compareTo(y)==0, Bitrate.class,new Bitrate(320));
		add("More",         Bitrate.class,B, (x,y) -> x.compareTo(y)>0, Bitrate.class,new Bitrate(320));
		add("Is good",      Bitrate.class,B, x -> x.getValue()>=320);
		add("Is bad",       Bitrate.class,B, x -> x.getValue()<=128);
		add("Is variable",  Bitrate.class,B, x -> x.isVariable());
		add("Is constant",  Bitrate.class,B, x -> x.isConstant());
		add("Is known",     Bitrate.class,B, x -> !x.isUnknown());

		add("Less",         Duration.class,B,(x, y) -> x.compareTo(y)<0, Duration.class, new Duration(0));
		add("Is",           Duration.class,B,(x,y) -> x.compareTo(y)==0, Duration.class, new Duration(0));
		add("More",         Duration.class,B,(x,y) -> x.compareTo(y)>0, Duration.class, new Duration(0),false,false,true);

		add("<  Less",      NofX.class,B, (x,y) -> x.compareTo(y)< 0, NofX.class,new NofX(1,1));
		add("=  Is",        NofX.class,B, (x,y) -> x.compareTo(y)==0, NofX.class,new NofX(1,1));
		add(">  More",      NofX.class,B, (x,y) -> x.compareTo(y)> 0, NofX.class,new NofX(1,1),false,false,true);
		add(">= Not less",  NofX.class,B, (x,y) -> x.compareTo(y)>=0, NofX.class,new NofX(1,1));
		add("<> Is not",    NofX.class,B, (x,y) -> x.compareTo(y)!=0, NofX.class,new NofX(1,1));
		add("<= Not more",  NofX.class,B, (x,y) -> x.compareTo(y)<=0, NofX.class,new NofX(1,1));

		add("<  Less",      FileSize.class,B, (x,y) -> x.compareTo(y)< 0, FileSize.class,new FileSize(0),false,false,true);
		add("=  Is",        FileSize.class,B, (x,y) -> x.compareTo(y)==0, FileSize.class,new FileSize(0));
		add(">  More",      FileSize.class,B, (x,y) -> x.compareTo(y)> 0, FileSize.class,new FileSize(0));
		add("Is unknown",   FileSize.class,B, x -> x.inBytes()==-1);
		add("Is known",     FileSize.class,B, x -> x.inBytes()>-1);
		add("In bytes",     FileSize.class,Long.class, x -> x.inBytes());

		add("Is after",         Year.class,B, (x,y) -> x.compareTo(y)> 0, Year.class,Year.now());
		add("Is",               Year.class,B, (x,y) -> x.compareTo(y)==0, Year.class,Year.now());
		add("Is before",        Year.class,B, (x,y) -> x.compareTo(y)< 0, Year.class,Year.now());
		add("Is in the future", Year.class,B, x -> x.compareTo(Year.now())> 0);
		add("Is now",           Year.class,B, x -> x.compareTo(Year.now())==0);
		add("Is in the past",   Year.class,B, x -> x.compareTo(Year.now())< 0);
		add("Is leap",          Year.class,B, Year::isLeap);

		add("Contains year",    RangeYear.class,B, RangeYear::contains, Year.class,Year.now());
		add("Is after",         RangeYear.class,B, RangeYear::isAfter,  Year.class,Year.now());
		add("Is before",        RangeYear.class,B, RangeYear::isBefore, Year.class,Year.now());
		add("Is in the future", RangeYear.class,B, x -> x.contains(Year.now()));
		add("Is now",           RangeYear.class,B, x -> x.isAfter(Year.now()));
		add("Is in the past",   RangeYear.class,B, x -> x.isBefore(Year.now()));

		add("After",   LocalDateTime.class,B, LocalDateTime::isAfter,  LocalDateTime.class,LocalDateTime.now());
		add("Before",  LocalDateTime.class,B, LocalDateTime::isBefore, LocalDateTime.class,LocalDateTime.now());
		add("Is",      LocalDateTime.class,B, LocalDateTime::isEqual,  LocalDateTime.class,LocalDateTime.now());

		add("File",    Item.class,File.class, Item::getFile);

		add("Is supported",     AudioFileFormat.class,B, x -> x.isSupported(APP));
		add("Is playable",      AudioFileFormat.class,B, x -> x.isSupported(PLAYBACK));

		addPredicatesComparable(Short.class, (short)0);
		addPredicatesComparable(Integer.class, 0);
		addPredicatesComparable(Long.class, 0L);
		addPredicatesComparable(Double.class, 0d);
		addPredicatesComparable(Float.class, 0f);

		// TODO: read from APP.classFields
		// fielded values
		for (Metadata.Field f : Metadata.Field.FIELDS)
			add(f.name(), Metadata.class, f.getType(), m -> (f).getOf(m));
		for (PlaylistItem.Field f : PlaylistItem.Field.FIELDS)
			add(f.name(), PlaylistItem.class, f.getType(), m -> (f).getOf(m));
		for (MetadataGroup.Field f : MetadataGroup.Field.FIELDS)
			add(f.name(), MetadataGroup.class, f.getType(), m -> (f).getOf(m));
	}

	public <I,O> void add(String name, Class<I> i ,Class<O> o, Ƒ1<? super I, ? extends O> f) {
		addF(new PƑ0(name,i,o,f));
	}

	public <I,P1,O> void add(String name, Class<I> i, Class<O> o, Ƒ2<I,P1,O> f, Class<P1> p1, P1 p1def) {
		add(name,i,o,f,new Parameter<>(p1, p1def));
	}

	public <I,P1,O> void add(String name, Class<I> i, Class<O> o, Ƒ2<I,P1,O> f, Parameter<P1> p1) {
		addF(new PƑ1<>(name,i,o,f,p1));
	}

	public <I,P1,P2,O> void add(String name, Class<I> i,Class<O> o, Ƒ3<I,P1,P2,O> f, Class<P1> p1, Class<P2> p2, P1 p1def, P2 p2def) {
		add(name,i,o,f,new Parameter<>(p1, p1def),new Parameter<>(p2, p2def));
	}

	public <I,P1,P2,O> void add(String name, Class<I> i,Class<O> o, Ƒ3<I,P1,P2,O> f, Parameter<P1> p1, Parameter<P2> p2) {
		addF(new PƑ2<>(name,i,o,f,p1,p2));
	}

	public <I,P1,P2,P3,O> void add(String name, Class<I> i,Class<O> o, Ƒ4<I,P1,P2,P3,O> f, Class<P1> p1, Class<P2> p2, Class<P3> p3, P1 p1def, P2 p2def, P3 p3def) {
		add(name,i,o,f,new Parameter<>(p1, p1def),new Parameter<>(p2, p2def),new Parameter<>(p3, p3def));
	}

	public <I,P1,P2,P3,O> void add(String name, Class<I> i,Class<O> o, Ƒ4<I,P1,P2,P3,O> f, Parameter<P1> p1, Parameter<P2> p2, Parameter<P3> p3) {
		addF(new PƑ3<>(name,i,o,f,p1,p2,p3));
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
		fsI.deAccumulate(f);
		fsO.deAccumulate(f);
		fsIO.deAccumulate(f);
	}

	@SuppressWarnings("unckeched")
	private <T> void addSelfFunctor(Class<T> c) {
		if (cacheStorage.contains(c)) return;
		cacheStorage.add(c);

		// add self functor
		addF(new PƑ0("As self", c, c, IDENTITY));

		// add enum is predicates
		if (isEnum(c))
			add("Is", c,Boolean.class, (a,b) -> a==b, c, (T)getEnumConstants(c)[0], false,false,true);
	}

	/** Returns all functions taking input I. */
	public <I> PrefList<PƑ<I,?>> getI(Class<I> i) {
		addSelfFunctor(i);
		return (PrefList) fsI.getElementsOf(getSuperClassesInc(unPrimitivize(i)));
	}

	/** Returns all functions producing output O. */
	public <O> PrefList<PƑ<?,O>> getO(Class<O> o) {
		addSelfFunctor(o);
		List ll = fsO.get(unPrimitivize(o));
		return ll==null ? new PrefList() : (PrefList) ll;
	}

	/** Returns all functions taking input I and producing output O. */
	public <I,O> PrefList<PƑ<I,O>> getIO(Class<I> i, Class<O> o) {
		addSelfFunctor(i);
		addSelfFunctor(o);

		// this is rather messy, but works
		// we accumulate result for all superclasses & retain first found preferred element, while
		// keeping duplicate elements in check
		PrefList pl = new PrefList();
		Object pref = null;
		for (Class c : getSuperClassesInc(unPrimitivize(i))) {
			List l = fsIO.get(Objects.hash(c, unPrimitivize(o)));
			PrefList ll = l==null ? null : (PrefList) l;
			if (ll!=null) {
				if (pref==null && ll.getPreferredOrFirst()!=null) pref = ll.getPreferredOrFirst();
				pl.addAll(ll);
			}
		}
		Object prefCopy = pref;
		pl.removeIf(e -> e==prefCopy || e==null);
		if (pref!=null) pl.addPreferred(pref);
		if (pl.getPreferredOrFirst()==null && !pl.isEmpty()) {
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
	public <IO> PrefList<PƑ<IO,IO>> getIO(Class<IO> io) {
		return getIO(io, io);
	}

	public <I,O> PƑ<I,O> getPF(String name, Class<I> i, Class<O> o) {
		@SuppressWarnings("unchecked")
		List<PƑ<I,O>> l = (List) fsIO.get(Objects.hash(unPrimitivize(i), unPrimitivize(o)));
		return l==null ? null : stream(l).filter(f -> f.name.equals(name)).findAny().orElse(null);
	}

	public <I> PƑ<I,?> getPrefI(Class<I> i) {
		PrefList<PƑ<I,?>> pl = getI(i);
		return pl==null ? null : pl.getPreferredOrFirst();
	}

	public <O> PƑ<?,O> getPrefO(Class<O> o) {
		PrefList<PƑ<?,O>> l = getO(o);
		return l==null ? null : l.getPreferredOrFirst();
	}

	public <I,O> PƑ<I,O> getPrefIO(Class<I> i, Class<O> o) {
		PrefList<PƑ<I,O>> l = getIO(i,o);
		return l==null ? null : l.getPreferredOrFirst();
	}

	public <IO> PƑ<IO,IO> getPrefIO(Class<IO> io) {
		return getPrefIO(io,io);
	}

}