package sp.it.pl.ui.nodeinfo

import java.time.LocalTime
import javafx.scene.control.TableColumn
import javafx.scene.control.TableView.UNCONSTRAINED_RESIZE_POLICY
import javafx.scene.layout.StackPane
import kotlin.Double.Companion.NaN
import sp.it.pl.main.toUi
import sp.it.pl.ui.objects.table.FilteredTable
import sp.it.pl.ui.objects.table.ImprovedTable.PojoV
import sp.it.pl.ui.objects.table.autoResizeColumns
import sp.it.pl.ui.objects.table.buildFieldedCell
import sp.it.pl.ui.objects.window.popup.PopWindow.Companion.onIsShowing1st
import sp.it.util.access.fieldvalue.ObjectFieldBase
import sp.it.util.collections.setTo
import sp.it.util.reactive.syncNonNullWhile
import sp.it.util.text.capital
import sp.it.util.type.VType
import sp.it.util.type.dataComponentProperties
import sp.it.util.type.rawJ
import sp.it.util.ui.lay

class WeatherInfoForecastMeteors: StackPane() {

   init {
      val t = object: FilteredTable<Shower>(Shower::class.java, null) {
         override fun computeFieldsAll() = Shower::class.dataComponentProperties().map { p ->
            object: ObjectFieldBase<Shower, Any?>(VType(p.returnType), { p.getter.call(it) }, p.name.capital(), "", { v, or -> v?.toUi() ?: or }) {}
         }
      }.apply {
         itemsRaw setTo data()
         setColumnFactory({ f ->
            val c = TableColumn<Shower, Any>(f.toString())
            c.styleClass.add(if (String::class.java.isAssignableFrom(f.type.rawJ)) "column-header-align-left" else "column-header-align-right")
            c.setCellValueFactory { cf -> if (cf.value==null) null else PojoV(f.getOf(cf.value)) }
            c.setCellFactory { f.buildFieldedCell() }
            c.isResizable = true
            c
         })
         columnState = defaultColumnInfo
         columnResizePolicy = UNCONSTRAINED_RESIZE_POLICY
      }
      lay += t.root
      t.sceneProperty().flatMap { it.windowProperty() }.syncNonNullWhile { w -> w.onIsShowing1st { t.autoResizeColumns() } }
   }

   data class Shower(
      val shower: String,
      val `class`: String,
      val activityPeriod: String,
      val maximumDate: String,
      val maximumSl: String,
      val radiantRa: String,
      val radiantDec: String,
      val velocity: Double,
      val r: Double,
      val max: Int,
      val time: LocalTime,
      val moon: String
   )

   /** Source https://www.amsmeteors.org/meteor-showers/2020-meteor-shower-list/ */
   @Suppress("SpellCheckingInspection")
   fun data() = listOf(
      Shower("Quadrantids (QUA)"                ,   "I", "Dec 26-Jan 16", "Jan 04",   "283.3°", "15:20",   "+49.7°", 40.2, 2.1, 120, LocalTime.of( 5, 0), "02"),
      Shower("Lyrids (LYR)"                     ,   "I", "Apr 15-Apr 29", "Apr 22",   "032.4°", "18:10",   "+33.3°", 46.8, 2.1,  18, LocalTime.of( 4, 0), "21"),
      Shower("eta Aquarids (ETA)"               ,   "I", "Apr 15-May 27", "May 05",   "046.2°", "22:30",   "-01.1°", 65.5, 2.4,  60, LocalTime.of( 4, 0), "05"),
      Shower("Southern delta Aquarids (SDA)"    ,   "I", "Jul 18-Aug 21", "Jul 31",   "127.6°", "22:42",   "-16.3°", 40.3, 3.2,  20, LocalTime.of( 3, 0), "03"),
      Shower("Perseids (PER)"                   ,   "I", "Jul 14-Sep 01", "Aug 13",   "140.0°", "03:13",   "+58.0°", 58.8, 2.6, 100, LocalTime.of( 4, 0), "17"),
      Shower("Orionids (ORI)"                   ,   "I", "Sep 26-Nov 22", "Oct 21",   "207.5°", "06:21",   "+15.6°", 66.1, 2.5,  23, LocalTime.of( 5, 0), "27"),
      Shower("Leonids (LEO)"                    ,   "I", "Nov 03-Dec 02", "Nov 18",   "236.0°", "10:17",   "+21.6°", 70.0, 2.5,  15, LocalTime.of( 5, 0), "24"),
      Shower("Geminids (GEM)"                   ,   "I", "Nov 19-Dec 24", "Dec 14",   "262°0" , "07:34",   "+32.3°", 33.8, 2.6, 120, LocalTime.of( 1, 0), "21"),
      Shower("Ursids (URS)"                     ,   "I", "Dec 13-Dec 24", "Dec 22",   "270°5" , "14:36",   "+75.3°", 33.0, 3.0,  10, LocalTime.of( 5, 0), "29"),

      Shower("Anthelion Source (ANT)"           ,  "II", "Dec 17-Sep 22",       "",         "",      "",       "", 30.0, 3.0,   3, LocalTime.of( 1, 0),   ""),
      Shower("alpha Centaurids (ACE)"           ,  "II", "Feb 03-Feb 20", "Feb 08",    "319°4", "14:04",	"-58.2°", 59.3, 2.0,	  6, LocalTime.of( 5, 0), "08"),
      Shower("eta Lyrids (ELY)"                 ,  "II", "May 06-May 15", "May 10",   "049.6°", "19:22",	"+43.5°", 43.9, 3.0,	  3, LocalTime.of( 4, 0), "10"),
      Shower("July Pegasids (JPE)"              ,  "II", "Jul 04-Aug 08", "Jul 11",   "108.4°", "23:11",	"+10.8°", 64.1, 3.0,	  5, LocalTime.of( 4, 0), "13"),
      Shower("alpha Capricornids (CAP)"         ,  "II", "Jul 07-Aug 15", "Jul 31",     "128°", "20:26",	"-09.1°", 22.0, 2.5,	  4, LocalTime.of( 1, 0), "03"),
      Shower("kappa Cygnids (KCG)"              ,  "II", "Aug 01-Aug 27", "Aug 14",     "141°", "19:05",	"+50.2°", 22.2, 3.0,	  3, LocalTime.of( 3, 0), "18"),
      Shower("Aurigids (AUR)"                   ,  "II", "Aug 26-Sep 04", "Sep 01",    "158°5", "06:04",	"+39.2°", 65.4, 2.6,	  6, LocalTime.of( 4, 0), "05"),
      Shower("September epsilon Perseids (SPE)" ,  "II", "Sep 02-Sep 23", "Sep 10",     "167°", "03:10",	"+39.5°", 64.2, 2.9,	  5, LocalTime.of( 5, 0), "15"),
      Shower("Southern Taurids (STA)"           ,  "II", "Sep 23-Nov 12", "Oct 18",   "205.5°", "02:36",	"+10.5°", 28.2, 2.3,	  5, LocalTime.of( 0, 0), "24"),
      Shower("epsilon Geminids (EGE)"           ,  "II", "Sep 27-Nov 08", "Oct 19",   "205.5°", "06:45",	"+28.2°", 68.5, 3.0,	  2, LocalTime.of( 4, 0), "25"),
      Shower("Leonis Minorids (LMI)"            ,  "II", "Oct 13-Nov 03", "Oct 21",     "208°", "10:35",	"+37.2°", 61.4, 2.7,	  2, LocalTime.of( 5, 0), "27"),
      Shower("Southern Taurids (STA)"           ,  "II", "Oct 11-Dec 08", "Nov 05",     "223°", "03:35",	"+14.4°", 27.7, 2.3,	  5, LocalTime.of( 0, 0), "12"),
      Shower("Northern Taurids (NTA)"           ,  "II", "Oct 13-Dec 02", "Nov 12",     "230°", "03:55",	"+22.8°", 27.6, 2.3,	  5, LocalTime.of( 0, 0), "19"),
      Shower("November Orionids (NOO)"          ,  "II", "Nov 13-Dec 12", "Nov 30",     "248°", "06:06",	"+15.4°", 42.3, 2.3,	  3, LocalTime.of( 4, 0), "07"),
      Shower("sigma Hydrids (HYD)"              ,  "II", "Nov 22-Jan 04", "Dec 07",     "255°", "08:17",	"+02.9°", 58.8, 2.3,	  3, LocalTime.of( 3, 0), "14"),
      Shower("Puppid/Velids (PUP)"              ,  "II", "Dec 01-Dec 15", "Dec 07",     "255°", "08:12",	"-45.0°", 40.0, 2.7,	 10, LocalTime.of( 4, 0), "14"),
      Shower("Monocerotids (MON)"               ,  "II", "Nov 23-Dec 24", "Dec 11",     "259°", "06:44",	"+08.2°", 41.0, 2.3,	  2, LocalTime.of( 1, 0), "19"),
      Shower("Coma Berenicids (COM)"            ,  "II", "Dec 12-Dec 23", "Dec 15",     "264°", "11:40",	"+18.0°", 65.0, 3.0,	  5, LocalTime.of( 5, 0), "23"),
      Shower("December Leonis Minorids (DLM)"   ,  "II", "Dec 01-Feb 10", "Dec 19",     "267°", "10:46",	"+31.1°", 63.0, 2.3,	  5, LocalTime.of( 5, 0), "26"),

      Shower("pi Puppids (PPU)"                 , "III", "Apr 16-Apr 30", "Apr 24",  "033°.6" , "07:22",	"-45.1°", 15.0, 2.0,  -1, LocalTime.of(19, 0), "23"),
      Shower("tau Herculids (TAH)"              , "III", "May 19-Jun 14", "May 31",  "069°.45", "13:56",	"+28°"  , 16.0, 2.2,  -1, LocalTime.of(22, 0), "01"),
      Shower("June Bootids (JBO)"               , "III", "Jun 25-Jun 29", "Jun 27",  "096°.3" , "14:48",	"+47.9°", 14.1, 2.2,  -1, LocalTime.of(21, 0), "29"),
      Shower("beta Hydusids (BHY)"              , "III", "Aug 15-Aug 19", "Aug 17",  "143°.8" , "02:25",	"-74.5°", 23.0, 2.6,  -1, LocalTime.of( 5, 0), "21"),
      Shower("Draconids (GIA)"                  , "III", "Oct 08-Oct 09", "Oct 08",  "195°.0" , "17:32",	"+55°.7", 20.7, 2.6,  -1, LocalTime.of(18, 0), "13"),
      Shower("alpha Monocerotids (AMO)"         , "III", "Nov 13-Nov 27", "Nov 22",  "239°9"  , "07:50",	"+00.7°", 61.6, 2.4,  -1, LocalTime.of( 3, 0), "28"),
      Shower("Dec Phoenicids (PHO)"             , "III", "Dec 04-Dec 06", "Dec 05",  "253°0"  , "01:02",	"-44.7°", 11.7, 2.8,  -1, LocalTime.of(20, 0), "12"),
      Shower("Volantids (VOL)"                  , "III", "Dec 27-Jan 04", "Dec 31",  "279°197", "08:02",	"-72°"  , 28.4, 2.8,  -1, LocalTime.of( 1, 0), "08"),

      Shower("January Leonids (JLE)"            ,  "IV", "Dec 27-Jan 07", "Jan 02",	"282.2°  ", "09:50",	"+23.9°", 52.0, NaN,   2, LocalTime.of( 3, 0), "00"),
      Shower("alpha Hydrids (AHY)"              ,  "IV", "Dec 15-Jan 22", "Jan 05",	"285.0°  ", "08:32",	"-08.4°", 43.3, NaN,   2, LocalTime.of( 3, 0), "03"),
      Shower("omicron Leonids (OLE)"            ,  "IV", "Dec 20-Jan 22", "Jan 09",	"289.315°", "09:11",	"+09.6°", 41.7, NaN,   2, LocalTime.of( 5, 0), "07"),
      Shower("xi Coronae Borealids (XCB)"       ,  "IV", "Jan 09-Jan 20", "Jan 15",	"295.0°  ", "16:40",	"+30.0°", 45.5, NaN,   2, LocalTime.of( 5, 0), "13"),
      Shower("gamma Ursae Minorids (GUM)"       ,  "IV", "Jan 09-Jan 20", "Jan 18",	"298.0°  ", "15:13",	"+69.2°", 28.8, NaN,   2, LocalTime.of( 5, 0), "16"),
      Shower("January xi Ursae Majorids (XUM)"  ,  "IV", "Jan 14-Jan 21", "Jan 19",	"299.5°  ", "11:20",	"+32.4°", 40.9, NaN,   2, LocalTime.of( 3, 0), "17"),
      Shower("eta Corvids (ECV)"                ,  "IV", "Jan 07-Feb 05", "Jan 21",	"301.0°  ", "12:42",	"-17.7°", 67.6, NaN,   2, LocalTime.of( 5, 0), "19"),
      Shower("Alpha Coronae Borealids (ACB)"    ,  "IV", "Jan 26-Feb 05", "Jan 27",	"307.4°  ", "15:24",	"+28.1°", 56.5, NaN,   2, LocalTime.of( 5, 0), "24"),
      Shower("alpha Antliids (AAN)"             ,  "IV", "Jan 22-Feb 06", "Feb 02",	"313.1°  ", "10:33",	"-09.9°", 44.3, NaN,   2, LocalTime.of( 1, 0), "02"),
      Shower("theta Centaurid Complex (TCE)"    ,  "IV", "Feb 02-Feb 06", "Feb 04",	"314.0°  ", "13:16",	"-42.0°", 60.2, NaN,   2, LocalTime.of( 5, 0), "04"),
      Shower("pi Hydrids (PIH)"                 ,  "IV", "Feb 03-Feb 09", "Feb 06",	"317.0°  ", "14:00",	"-21.0°", 55.3, NaN,   2, LocalTime.of( 4, 0), "06"),
      Shower("gamma Crucids (GCR)"              ,  "IV", "Feb 11-Feb 15", "Feb 14",	"325.0°  ", "12:48",	"-56.0°", 55.8, NaN,   2, LocalTime.of( 4, 0), "13"),
      Shower("xi Herculids (XHE)"               ,  "IV", "Mar 06-Mar 20", "Mar 12",	"351.3°  ", "16:58",	"+48.6°", 35.4, NaN,   2, LocalTime.of( 4, 0), "09"),
      Shower("delta Mensids (DME)"              ,  "IV", "Mar 02-Mar 26", "Mar 12",	"352.0°  ", "04:09",	"-74.4°", 30.9, NaN,   2, LocalTime.of( 5, 0), "09"),
      Shower("beta Tucanids (BTU)"              ,  "IV", "Mar 02-Mar 26", "Mar 13",	"352.33° ", "04:07",	"-77.0°", 31.0, NaN,   2, LocalTime.of( 5, 0), "10"),
      Shower("delta Pavonids (DPA)"             ,  "IV", "Mar 11-Apr 16", "Mar 31",	"010.0°  ", "20:32",	"-63.0°", 58.0, NaN,   2, LocalTime.of( 5, 0), "00"),
      Shower("April epsilon Delphinids (AED)"   ,  "IV", "Mar 31-Apr 20", "Apr 09",	"019.5°  ", "20:30",	"+11.5°", 60.5, NaN,   2, LocalTime.of( 4, 0), "07"),
      Shower("kappa Serpentids (KSE)"           ,  "IV", "Apr 11-Apr 22", "Apr 16",	"026.0°  ", "16:30",	"+17.9°", 45.6, NaN,   2, LocalTime.of( 4, 0), "15"),
      Shower("alpha Virginids (AVB)"            ,  "IV", "Apr 06-May 01", "Apr 18",	"028.0°  ", "13:26",	"+03.9°", 19.3, NaN,   2, LocalTime.of( 1, 0), "17"),
      Shower("h-Virginids (HVI)"                ,  "IV", "Apr 24-May 04", "May 01",	"041.0°  ", "13:35",	"-11.4°", 17.6, NaN,   2, LocalTime.of( 4, 0), "01"),
      Shower("Daytime Arietids (ARI)"           ,  "IV", "May 29-Jun 17", "Jun 04",	"073.8°  ", "02:46",	"+23.7°", 40.5, NaN,   2, LocalTime.of( 4, 0), "04"),
      Shower("June Iota Pegasids (JIP)"         ,  "IV", "Jun 25-Jun 27", "Jun 25",	"093.8°  ", "22:06",	"+29.3°", 58.6, NaN,   2, LocalTime.of( 4, 0), "27"),
      Shower("phi Piscids (PPS)"                ,  "IV", "Jun 13-Jul 05", "Jun 25",	"094.0°  ", "00:40",	"+21.4°", 66.5, NaN,   2, LocalTime.of( 4, 0), "27"),
      Shower("Microscopiids (MIC)"              ,  "IV", "Jun 25-Jul 16", "Jul 06",	"104.0°  ", "21:13",	"-27.0°", 39.7, NaN,   2, LocalTime.of( 4, 0), "07"),
      Shower("July chi Arietids (JXA)"          ,  "IV", "Jun 26-Jul 22", "Jul 07",	"105.5°  ", "02:11",	"+07.8°", 68.4, NaN,   2, LocalTime.of( 4, 0), "08"),
      Shower("phi Piscids (PPS)"                ,  "IV", "Jul 02-Jul 22", "Jul 10",	"108.0°  ", "01:23",	"+27.9°", 66.5, NaN,   2, LocalTime.of( 4, 0), "11"),
      Shower("c-Andromedids (CAN)"              ,  "IV", "Jun 21-Jul 28", "Jul 12",	"110.0°  ", "02:10",	"+48.3°", 56.9, NaN,   2, LocalTime.of( 4, 0), "13"),
      Shower("Northern June Aquilids (NZC)"     ,  "IV", "Jun 26-Jul 22", "Jul 15",	"113.0°  ", "21:18",	"-02.4°", 37.7, NaN,   2, LocalTime.of( 2, 0), "16"),
      Shower("zeta Cassiopeiids (ZCS)"          ,  "IV", "Jul 07-Jul 22", "Jul 16",	"113.5°  ", "00:30",	"+50.9°", 57.2, NaN,   2, LocalTime.of( 4, 0), "17"),
      Shower("July gamma Draconids (GDR)"       ,  "IV", "Jul 23-Aug 03", "Jul 28",	"125.3°  ", "18:42",	"+50.6°", 27.3, NaN,   2, LocalTime.of(22, 0), "00"),
      Shower("Eta Eridanids (ERI)"              ,  "IV", "Jul 10-Sep 10", "Aug 06",	"134.0°  ", "02:44",	"-13.0°", 63.9, NaN,   2, LocalTime.of( 4, 0), "08"),
      Shower("Piscis Austrinids (PAU)"          ,  "IV", "Aug 01-Aug 10", "Aug 07",	"135.0°  ", "23:53",	"-20.2°", 43.0, NaN,   2, LocalTime.of( 3, 0), "09"),
      Shower("Northern delta Aquariids (NDA)"   ,  "IV", "Aug 02-Aug 17", "Aug 12",	"139.5°	", "23:02",	"+00.9 ", 39.1, NaN,   2, LocalTime.of( 3, 0), "16"),
      Shower("August xi Draconids (AXD)"        ,  "IV", "Aug 04-Aug 28", "Aug 15",	"142.0°  ", "18:26",	"+53.6°", 20.3, NaN,   2, LocalTime.of(21, 0), "19"),
      Shower("beta Hydusids (BHY)"              ,  "IV", "Aug 15-Aug 19", "Aug 17",	"143.8°  ", "02:25",	"-74.5°", 22.8, NaN,   2, LocalTime.of(21, 0), "21"),
      Shower("August beta Piscids (BPI)"        ,  "IV", "Aug 17-Sep 08", "Aug 21",	"148.0°  ", "23:30",	"+04.4°", 38.2, NaN,   2, LocalTime.of(21, 0), "25"),
      Shower("zeta Draconids (AUD)"             ,  "IV", "Aug 12-Sep 05", "Aug 26",	"153.0°  ", "17:16",	"+62.8°", 21.3, NaN,   2, LocalTime.of(21, 0), "29"),
      Shower("August Gamma Cepheids (AGC)"      ,  "IV", "Aug 17-Sep 06", "Aug 29",	"155.6°  ", "23:57",	"+76.9°", 43.8, NaN,   2, LocalTime.of( 2, 0), "02"),
      Shower("Nu Eridanids (NUE)"               ,  "IV", "Aug 31-Sep 21", "Sep 11",	"168.0°  ", "04:33",	"+00.7°", 65.7, NaN,   2, LocalTime.of( 5, 0), "16"),
      Shower("September Lyncids (SLY)"          ,  "IV", "Aug 30-Sep 20", "Sep 11",	"168.0°  ", "07:15",	"+55.8°", 59.3, NaN,   2, LocalTime.of( 5, 0), "16"),
      Shower("chi Cygnids (CCY)"                ,  "IV", "Sep 08-Sep 17", "Sep 13",	"170.8°  ", "20:00",	"+31.0°", 19.0, NaN,   2, LocalTime.of(21, 0), "18"),
      Shower("Daytime Sextantids (DSX)"         ,  "IV", "Sep 22-Oct 13", "Oct 03",	"190.0°  ", "10:27",	"-03.3°", 32.1, NaN,   2, LocalTime.of( 5, 0), "08"),
      Shower("October Camelopardalids (OCT)"    ,  "IV", "Oct 05-Oct 07", "Oct 06",	"192.7°  ", "11:09",	"+78.6°", 45.4, NaN,   2, LocalTime.of( 5, 0), "11"),
      Shower("A Carinids (CRN)"                 ,  "IV", "Oct 13-Oct 14", "Oct 14",	"200.883°", "06:27",	"-54.3°", 32.4, NaN,   2, LocalTime.of( 5, 0), "20"),
      Shower("October Ursae Majorids (OCU)"     ,  "IV", "Oct 10-Oct 20", "Oct 16",	"202.5°  ", "09:41",	"+64.2°", 55.3, NaN,   2, LocalTime.of( 5, 0), "22"),
      Shower("tau Cancrids (TCA)"               ,  "IV", "Sep 23-Nov 12", "Oct 21",	"208.0°  ", "09:13",	"+29.6°", 67.1, NaN,   2, LocalTime.of( 5, 0), "27"),
      Shower("October zeta Perseids (OZP)"      ,  "IV", "Oct 25-Oct 25", "Oct 25",	"211.36° ", "03:53",	"+33.7°", 48.1, NaN,   2, LocalTime.of( 5, 0), "01"),
      Shower("lambda Ursae Majorids (LUM)"      ,  "IV", "Oct 18-Nov 07", "Oct 28",	"214.8°  ", "10:32",	"+49.4°", 60.8, NaN,   2, LocalTime.of( 5, 0), "04"),
      Shower("Southern lambda Draconids (SLD)"  ,  "IV", "Oct 29-Nov 08", "Nov 04",	"221.5°  ", "10:46",	"+68.2°", 48.5, NaN,   2, LocalTime.of(19, 0), "11"),
      Shower("chi Taurids (CTA)"                ,  "IV", "Oct 24-Nov 13", "Nov 04",	"222.0°  ", "04:16",	"+27.2°", 40.1, NaN,   2, LocalTime.of( 3, 0), "11"),
      Shower("kappa Ursae Majorids (KUM)"       ,  "IV", "Oct 28-Nov 17", "Nov 05",	"223.0°  ", "09:37",	"+45.6°", 64.7, NaN,   2, LocalTime.of( 5, 0), "12"),
      Shower("Andromedids (AND)"                ,  "IV", "Oct 24-Dec 02", "Nov 06",	"224.0°  ", "01:23",	"+28.0°", 18.1, NaN,   2, LocalTime.of(22, 0), "13"),
      Shower("Omicron Eridanids (OER)"          ,  "IV", "Oct 23-Dec 02", "Nov 13",	"231.0°  ", "03:54",	"-01.0°", 27.7, NaN,   2, LocalTime.of( 1, 0), "20"),
      Shower("Nov. sigma Ursae Majorids (NSU)"  ,  "IV", "Nov 17-Dec 02", "Nov 24",	"242.0°  ", "09:56",	"+59.0°", 54.5, NaN,   2, LocalTime.of(20, 0), "01"),
      Shower("theta Pyxidids (TPY)"             ,  "IV", "Nov 28-Dec 06", "Dec 01",	"249.4°  ", "09:15",	"-25.6°", 59.7, NaN,   2, LocalTime.of(20, 0), "08"),
      Shower("Southern chi Orionids (ORS)"      ,  "IV", "Nov 14-Dec 16", "Dec 02",	"250.0°  ", "05:20",	"+18.1°", 26.5, NaN,   2, LocalTime.of(20, 0), "09"),
      Shower("December Kappa Draconids (DKD)"   ,  "IV", "Nov 29-Dec 13", "Dec 03",	"250.9°  ", "12:24",	"+70.7°", 43.4, NaN,   2, LocalTime.of( 5, 0), "10"),
      Shower("psi Ursa Majorids (PSU)"          ,  "IV", "Nov 29-Dec 11", "Dec 04",	"252.0°  ", "12:15",	"+43.9°", 60.8, NaN,   2, LocalTime.of( 5, 0), "11"),
      Shower("December phi Cassiopeiids (DPC)"  ,  "IV", "Nov 28-Dec 10", "Dec 04",	"252.0°  ", "01:18",	"+57.7°", 16.5, NaN,   2, LocalTime.of(20, 0), "11"),
      Shower("December rho Virginids (DRV)"     ,  "IV", "Nov 29-Dec 22", "Dec 05",	"253.4°  ", "12:22",	"+12.9°", 68.2, NaN,   2, LocalTime.of(20, 0), "12"),
      Shower("December chi Virginids (XVI)"     ,  "IV", "Nov 26-Dec 30", "Dec 12",	"260.0°  ", "12:38",	"-09.3°", 68.2, NaN,   2, LocalTime.of( 5, 0), "20"),
      Shower("eta Hydrids (EHY)"                ,  "IV", "Nov 26-Jan 01", "Dec 12",	"260.0°  ", "09:02",	"+01.8°", 61.8, NaN,   2, LocalTime.of( 5, 0), "20"),
      Shower("theta Pyxidids (TPY)"             ,  "IV", "Dec 08-Jan 08", "Dec 18",	"266.6°  ", "10:17",	"-24.4°", 62.5, NaN,   2, LocalTime.of(20, 0), "24"),
      Shower("December sigma Virginids (DSV)"   ,  "IV", "Nov 26-Jan 24", "Dec 22",	"270.0°  ", "13:49",	"+04.6°", 66.1, NaN,   2, LocalTime.of( 5, 0), "29"),
      Shower("c Velids (CVE)"                   ,  "IV", "Dec 26-Dec 31", "Dec 29",	"277.0°  ", "09:20",	"-54.0°", 39.0, NaN,   2, LocalTime.of( 2, 0), "06"),
   )

}