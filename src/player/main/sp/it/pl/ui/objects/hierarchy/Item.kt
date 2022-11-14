package sp.it.pl.ui.objects.hierarchy

import java.awt.image.BufferedImage
import java.awt.image.BufferedImage.TYPE_INT_ARGB
import java.io.File
import java.util.Locale
import java.util.UUID
import java.util.concurrent.Executor
import java.util.function.Consumer
import javafx.scene.image.Image
import sp.it.pl.audio.SimpleSong
import sp.it.pl.audio.tagging.read
import sp.it.pl.image.ImageLoader
import sp.it.pl.image.ImageStandardLoader
import sp.it.pl.main.Double01
import sp.it.pl.main.imageExtensionsRead
import sp.it.pl.main.isAudio
import sp.it.pl.main.isImage
import sp.it.pl.main.isVideo
import sp.it.pl.ui.objects.grid.GridViewSkin
import sp.it.pl.ui.objects.grid.ImageLoad
import sp.it.pl.ui.objects.grid.ImageLoad.DoneErr
import sp.it.pl.ui.objects.grid.ImageLoad.DoneInterrupted
import sp.it.pl.ui.objects.grid.ImageLoad.DoneOk
import sp.it.pl.ui.objects.grid.ImageLoad.Loading
import sp.it.pl.ui.objects.grid.ImageLoad.NotStarted
import sp.it.pl.ui.objects.image.Cover.CoverSource
import sp.it.util.HierarchicalBase
import sp.it.util.JavaLegacy
import sp.it.util.access.fieldvalue.CachingFile
import sp.it.util.async.SemaphoreLock
import sp.it.util.async.VT
import sp.it.util.async.future.Fut
import sp.it.util.async.future.Fut.Companion.fut
import sp.it.util.async.limitAccess
import sp.it.util.async.limitParallelism
import sp.it.util.async.runOn
import sp.it.util.dev.failIfFxThread
import sp.it.util.dev.failIfNotFxThread
import sp.it.util.file.FileType
import sp.it.util.file.FileType.Companion.invoke
import sp.it.util.file.FileType.DIRECTORY
import sp.it.util.file.FileType.FILE
import sp.it.util.file.children
import sp.it.util.functional.Option
import sp.it.util.functional.Option.None
import sp.it.util.functional.Option.Some
import sp.it.util.functional.getOrSupply
import sp.it.util.ui.IconExtractor.getFileIcon
import sp.it.util.ui.image.FitFrom
import sp.it.util.ui.image.FitFrom.OUTSIDE
import sp.it.util.ui.image.ImageSize
import sp.it.util.ui.image.Interrupts
import sp.it.util.ui.image.toBuffered
import sp.it.util.ui.image.toFX

/**
 * File wrapper, content of Cell with an image cover.
 * We cache various stuff in here, including the cover image and children files.
 */
abstract class Item(parent: Item?, value: File, valueType: FileType): HierarchicalBase<File, Item>(value, parent) {

   val valType: FileType = valueType

   /** Children representing filtered files */
   @Volatile
   protected var children: MutableList<Item>? = null

   /** All file children. Super set of [children] */
   @Volatile
   protected var childrenRaw: MutableSet<String>? = null

   @Volatile var cover: ImageLoad = NotStarted

   @Volatile
   protected var coverFile: Option<File?> = None

   @Volatile
   protected var loadingThread: Thread? = null

   @Volatile
   protected var loadingPreInterrupted = false

   @Volatile
   protected var disposed = false

   protected val properties = HashMap<String, Any>()

   /** 0-1  */
   var loadProgress: Double01 = 0.0

   /** -1 || 0-1  */
   var lastScrollPosition: Double01 = -1.0

   /** GridViewSkin.NO_SELECT || 0-N  */
   var lastSelectedChild = GridViewSkin.NO_SELECT

   /** Returns children items. Evaluates children lazily at first invocation at most once  */
   open fun children(): List<Item> {
      if (children==null) children = ArrayList(computeChildren())
      return children!!
   }

   /** Returns children items as are - null if not yet evaluated. See [.children].  */
   open fun childrenRO(): List<Item>? {
      failIfNotFxThread()
      return children
   }

   open fun removeChild(item: Item) {
      failIfNotFxThread()
      if (disposed) return
      if (children==null) return
      children?.remove(item)
      childrenRaw?.remove(item.value.name.lowercase())
   }

   /** Dispose of this as to never be used again.  */
   open fun dispose() {
      failIfNotFxThread()
      if (children!=null) children!!.forEach(Consumer { obj: Item -> obj.dispose() })
      children = null
      childrenRaw = null
      cover = DoneErr
      coverFile = None
      computeCoverInterrupt()
      loadingThread = null
      disposed = true
   }

   /** Dispose of the cover as to be able to load it again.  */
   open fun disposeCover() {
      failIfNotFxThread()
      cover = NotStarted
      coverFile = None
      computeCoverInterrupt()
      loadingThread = null
      loadProgress = 0.0
   }

   /** Dispose of this as to be fully usable, but children will be [.disposeContent].  */
   open fun disposeChildrenContent() {
      failIfNotFxThread()
      if (children!=null) children!!.forEach(Consumer { obj: Item -> obj.disposeContent() })
   }

   /** Dispose of this as to be usable again. Children will be [.dispose].  */
   open fun disposeContent() {
      failIfNotFxThread()
      if (children!=null) children!!.forEach(Consumer { obj: Item -> obj.dispose() })
      children = null
      childrenRaw = null
      cover = NotStarted
      coverFile = None
      loadProgress = 0.0
      lastScrollPosition = -1.0
      lastSelectedChild = GridViewSkin.NO_SELECT
   }

   protected open fun computeChildren(): List<Item> {
      failIfFxThread()
      val all = HashSet<String>()
      val dirs = ArrayList<Item>()
      val files = ArrayList<Item>()
      childrenFiles().forEach {
         val type = FileType(it)
         if (type===DIRECTORY) {
            dirs.add(createItem(this, it, type))
         } else {
            all.add(it.name.lowercase(Locale.getDefault()))
            if (filterChildFile(it)) files.add(createItem(this, it, type))
         }
      }
      return if (!disposed) {
         children = ArrayList<Item>(dirs.size + files.size).apply {
            this += dirs
            this += files
         }
         childrenRaw = all
         children!!
      } else {
         listOf()
      }
   }

   protected open fun childrenFiles(): Sequence<File> = value.children().map<File, File> { if (it is CachingFile) it else CachingFile(it) }

   protected open fun filterChildFile(f: File): Boolean = true

   protected abstract fun createItem(parent: Item, value: File, type: FileType): Item

   protected open fun getImage(dir: File?, name: String?): File? {
      if (dir==null) return null
      if (name==null) return null
      val ch = childrenRaw
      for (ext in imageExtensionsRead) {
         val n = "$name.$ext"
         if (dir==value) {
            return if (fileExists(n, ch)) File(dir, n) else null
         } else if (parent!=null && dir==parent!!.value) {
            if (fileExists(n, parent!!.childrenRaw)) return File(dir, n)
         } else {
            val f = File(dir, n)
            if (f.exists()) return f
         }
      }
      return null
   }

   protected open fun getImageT(dir: File?, name: String): File? {
      if (dir==null) return null
      val ch = childrenRaw
      for (ext in imageExtensionsRead) {
         val n = "$name.$ext"
         if (fileExists(n, ch)) return File(dir, n)
      }
      return null
   }

   open fun computeCoverInterrupt() {
      loadingPreInterrupted = true
      Interrupts.interrupt(loadingThread)
   }

	fun computeCover(size: ImageSize, fit: FitFrom): Fut<ImageLoad> {
		failIfNotFxThread()

		return when (val c = cover) {
			is DoneOk -> fut(c)
			is DoneErr -> fut(c)
			is Loading -> c.loading
			is NotStarted -> computeCoverAsync(None, size, fit)
			is DoneInterrupted -> computeCoverAsync(c.fileOpt, size, fit)
		}
	}

   private fun computeCoverAsync(imgFile: Option<File?>, size: ImageSize, fit: FitFrom): Fut<ImageLoad> {
      if (disposed) {
         cover = DoneErr
         return fut(DoneErr)
      }

      loadingPreInterrupted = false
      val str = coverStrategy
      val cl = runOn(CoverStrategy.VT_IMAGE) {
         loadingThread = Thread.currentThread()
         if (loadingPreInterrupted) return@runOn DoneInterrupted(imgFile)
         if (Interrupts.isInterrupted) return@runOn DoneInterrupted(imgFile)

         var ci = null as Image?
         val cf = imgFile.getOrSupply { computeCoverFile(str) }
         if (Interrupts.isInterrupted) return@runOn DoneInterrupted(Some(cf))

         try {
            val ch: List<Item>? = children
            if (cf!=null) {
               ci = str.loader.invoke(cf, size, fit)
            } else {
               if (valType===DIRECTORY) {
                  if (str.useComposedDirCover) {
                     val subCovers = ch?.asSequence().orEmpty()
                        .filter { it.valType===FILE }
                        .mapNotNull { it.computeCoverFile(str) }
                        .take(4)
                        .mapNotNull { str.loader.invoke(it, size / 2.0, OUTSIDE, true) }
                        .toList()
                     val w = size.width.toInt()
                     val h = size.height.toInt()
                     val imgFin = BufferedImage(w, h, TYPE_INT_ARGB)
                     val imgFinGraphics = imgFin.graphics
                     subCovers.forEachIndexed { i, img ->
                        val bi = img.toBuffered()
                        imgFinGraphics.drawImage(bi, w/2*(i%2), h/2*(i/2), size.width.toInt()/2, size.height.toInt()/2, null)
                        bi?.flush()
                        JavaLegacy.destroyImage(img)
                     }
                     ci = imgFin.toFX()
                  }
               } else if (valType===FILE) {
                  if (value.isVideo() && str.useVideoFrameCover) {
                     ci = str.loader.invoke(value, size, fit)
                  } else if (value.isAudio()) {
                     val c = SimpleSong(value).read().getCover(CoverSource.ANY)
                     ci = if (c.isEmpty()) null else c.getImage(size, fit)
                  } else if (value.path.endsWith(".pdf")) {
                     ci = str.loader.invoke(value, size, fit)
                  } else if (str.useNativeIconIfNone) {
                     ci = getFileIcon(value)
                  }
               }
            }
            if (ci==null && Interrupts.isInterrupted) DoneInterrupted(Some(cf))
            else DoneOk(ci, cf)
         } catch (e: Throwable) {
            if (Interrupts.isInterrupted) DoneInterrupted(Some(cf))
            else DoneErr
         }
      } ui {
         Interrupts.dispose(loadingThread)
         loadingThread = null
         cover = it
         it
      }
      cover = Loading(cl)
      return cl
   }

	protected open fun computeCoverFile(strategy: CoverStrategy): File? {
		failIfFxThread()

      return when (val coverFileOpt = coverFile) {
         is Some<File?> -> coverFileOpt.value
         else -> {
            var cf: File? = null

            if (valType==DIRECTORY) {
               if (children==null) computeChildren()
               cf = getImageT(value, "cover")
            } else {
               if (cf==null && value.isImage()) {
                  cf = value
               }
               if (cf==null) {
                  var i: File?
                     i = getImage(value.parentFile, value.nameWithoutExtension)
                  if (i==null && parent!=null && strategy.useParentCoverIfNone)
                     i = parent!!.computeCoverFile(strategy)
                  if (i!=null)
                     cf = i
               }
               if (cf==null && value.isVideo() && hParent!=null) {
                  val n = value.nameWithoutExtension
                  if (n.endsWith(")")) {
                     val i = n.lastIndexOf("(")
                     val coverName = if (i==-1) null else n.substring(0, i) + "(BQ)"
                     cf = getImage(value.parentFile, coverName)
                  }
               }
            }

            coverFile = Some(cf)
            return cf
         }
      }
	}

   override val hChildren: List<Item> get() = children()

   private fun property(key: String, o: Any) {
      failIfNotFxThread()
      val root = hRoot!!
      root.properties[key] = o
   }

   private fun property(key: String): Any? {
      failIfNotFxThread()
      val root = hRoot!!
      return root.properties[key]
   }

   var coverStrategy: CoverStrategy
      get() = property("coverStrategy") as CoverStrategy? ?: CoverStrategy.DEFAULT
      set(v) { property("coverStrategy", v) }

   private fun fileExists(f: String, all_children: Set<String>?): Boolean {
      return all_children!=null && all_children.contains(f)
   }

   data class CoverStrategy(
      /** Enable using parent directory cover if file has no cover  */
      val useParentCoverIfNone: Boolean,
      /** Enable using directory covers composed of child file covers  */
      val useComposedDirCover: Boolean,
      /** Enables using small native OS icon for the file cover. Has no effect if [.useParentCoverIfNone] is true.  */
      val useNativeIconIfNone: Boolean,
      /** Enables video frame extraction to display cover for video files  */
      val useVideoFrameCover: Boolean,
      /** Enables [ImageLoader.memoized]  */
      val diskCache: UUID?
   ) {
      /** The actual cover image loader  */
      val loader: ImageLoader = if (diskCache!=null) ImageStandardLoader.memoized(diskCache) else ImageStandardLoader

      companion object {
         @JvmField val DEFAULT = CoverStrategy(true, true, false, true, null)
         @JvmField val VT_IMAGE_THROTTLE = SemaphoreLock()
         @JvmField val VT_IMAGE: Executor = VT.limitParallelism(6).limitAccess(VT_IMAGE_THROTTLE)
      }
   }
}