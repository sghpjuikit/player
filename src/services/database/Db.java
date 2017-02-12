/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package services.database;

import audio.tagging.Metadata.Field;
import java.io.File;
import java.net.URI;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import javax.persistence.Entity;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;

import audio.Item;
import audio.tagging.Metadata;
import audio.tagging.MetadataWriter;
import gui.objects.window.stage.Window;
import layout.widget.controller.io.InOutput;
import util.async.future.Fut;
import util.collections.mapset.MapSet;
import util.functional.Functors.Ƒ2;

import static java.util.UUID.fromString;
import static main.App.APP;
import static util.async.Async.FX;
import static util.async.Async.runFX;
import static util.file.Util.readFileLines;
import static util.functional.Util.listRO;
import static util.functional.Util.stream;

/**
 *
 * @author Martin Polakovic
 */
public class Db {

    private static final File MOODS_FILE = new File(APP.DIR_RESOURCES, "moods.cfg");

    public static EntityManagerFactory emf;
    public static EntityManager em;

    public static void start() {
        File dbFile = new File(APP.DIR_LIBRARY, "library_database.odb");
        emf = Persistence.createEntityManagerFactory(dbFile.getPath());
        em = emf.createEntityManager();

        new Fut<>()
                // load database
                .supply(Db::getAllItems)
                .use(Db::setInMemoryDB, FX)
                .then(() -> {
                    // load string store
                    List<StringStore> sss = em.createQuery("SELECT p FROM StringStore p", StringStore.class).getResultList();
                    string_pool = sss.isEmpty() ? new StringStore() : sss.get(0);

                    // populate metadata fields strings if empty
                    if (string_pool.getStrings("album").isEmpty() && !items_byId.isEmpty()) {
                        stream(Metadata.Field.FIELDS)
                                .filter(Field::isAutoCompletable)
                                .forEach(f -> {
                                    Set<String> pool = string_pool.getStrings(f.name());
                                    items_byId.stream()
                                            .map(m -> m.getFieldS(f,""))
                                            .filter(t -> !t.isEmpty())
                                            .distinct()
                                            // .peek(System.out::println) // debug
                                            .forEach(pool::add);
                                });

                        // add default moods (stored in file)
                        Set<String> pool = string_pool.getStrings(Metadata.Field.MOOD.name());
                        readFileLines(MOODS_FILE).forEach(pool::add);

                        // persist
                        em.getTransaction().begin();
                        em.merge(string_pool);
                        em.getTransaction().commit();
                    }

                    // add default moods (stored in file)
                    Set<String> pool = string_pool.getStrings(Metadata.Field.MOOD.name());
                    readFileLines(MOODS_FILE).forEach(pool::add);

                    // persist
                    em.getTransaction().begin();
                    em.merge(string_pool);
                    em.getTransaction().commit();

                })
                .showProgress(APP.windowManager.getActive().map(Window::taskAdd))
                .run();
    }

    public static void stop() {
        if (em!=null && em.isOpen()) em.close();
        if (emf!=null) emf.close();
    }

    /******************************** OBTAINING ***********************************/

    public static boolean exists(Item item) {
        return exists(item.getURI());
    }

    public static boolean exists(URI uri) {
        return em!=null && em.find(Metadata.class, uri.toString())!=null;
    }

    /**
     * Returns item from library.
     * <p/>
     * Never pass the returned item into the application before making sure it
     * is not null.
     * @return item from library with the URI of the specified item or null if not found.
     */
    public static Metadata getItem(Item item) {
        return getItem(item.getURI());
    }

    /**
     * Returns item from library.
     *
     * @return item from library with the specified URI or null if not found.
     */
    public static Metadata getItem(URI uri) {
        return em==null ? null : em.find(Metadata.class, uri.toString());
    }

    public static List<Metadata> getAllItems() {
        return em==null ? listRO() : em.createQuery("SELECT p FROM MetadataItem p", Metadata.class).getResultList();
    }

    public static void addItems(Collection<? extends Metadata> items) {
        if (em==null) return;

        if (items.isEmpty()) return;
        List<Metadata> l = new ArrayList<>();
        // add to db
        em.getTransaction().begin();
        items.forEach( m -> {
            if (em.find(Metadata.class, m.getId()) == null) {
                em.persist(m);
                l.add(m);
            }
        });
        em.getTransaction().commit();

        MetadataWriter.use(l, MetadataWriter::setLibraryAddedNowIfEmpty);

        // update model
        updateInMemoryDbFromPersisted();
    }

    public static void removeItems(Collection<? extends Item> items) {
        if (em==null) return;

        // remove in db
        em.getTransaction().begin();
        items.forEach(item -> {
            Metadata in_db = em.find(Metadata.class, item.getId());
            if (in_db != null) em.remove(in_db);
        });
        em.getTransaction().commit();
        // update model
        updateInMemoryDbFromPersisted();
    }

    public static void removeAllItems() {
        removeItems(items.i.getValue());
    }

    public static void updatePer(Collection<? extends Metadata> items) {
        if (em==null) return;

        // update db
        em.getTransaction().begin();
        items.forEach(em::merge);
        em.getTransaction().commit();
    }

    /**
     * Thread safe.
     */
    private static void setInMemoryDB(List<Metadata> l) {
        items_byId.clear();
        items_byId.addAll(l);
        runFX(() -> items.i.setValue(l));
    }

    /**
     * Thread safe.
     */
    public static void updateInMemoryDbFromPersisted() {
        setInMemoryDB(getAllItems());
    }


    /**
     * In memory item database. Use for library.
     * <p/>
     * Items are hashed by {@link Item#getId()}.
     * <p/>
     * Thread-safe, accessible (write/read) from any thread, uses
     * {@link ConcurrentHashMap} underneath.
     */
    public static final MapSet<String,Metadata> items_byId = new MapSet<>(new ConcurrentHashMap<>(2000,1,3),Metadata::getId);
    public static final InOutput<List<Metadata>> items = new InOutput<>(fromString("396d2407-7040-401e-8f85-56bc71288818"),"All library songs", List.class);


    /**
     * Comparator defining the sorting for items in operations that wish to
     * provide consistent sorting across the application.
     * <p/>
     * The comparator should reflect library table sort order.
     */
    public static ObjectProperty<Comparator<? super Metadata>> library_sorter = new SimpleObjectProperty<>(Metadata::compareTo);

    /**
     * In memory storage for strings that persists in database.
     * Map that maps sets of strings to string keys. The keys are case-insensitive.
     * <p/>
     * The store is loaded when DB starts. Changes persist immediately.
     */
    public static StringStore string_pool;

    public static boolean autocompletionContains = true;
    public static final Ƒ2<String,String,Boolean> autocompletionFilter = (text, phrase) -> autocompletionContains
            ? text.contains(phrase) : text.startsWith(phrase);

    /******************************************************************************/

    @Entity(name = "StringStore")
    public static class StringStore {
        private HashMap<String,HashSet<String>> pool = new HashMap<>();

        private StringStore() {}

        public Set<String> getStrings(String name) {
            String n = name.toLowerCase();
            if (!string_pool.pool.containsKey(n)) string_pool.pool.put(n, new HashSet<>());
            return string_pool.pool.get(n);
        }

        public void addString(String name, String s) {
            boolean b = getStrings(name.toLowerCase()).add(s);
            if (b) {
                em.getTransaction().begin();
                em.merge(string_pool);
                em.getTransaction().commit();
            }
        }

        public void addStrings(String name, List<String> s) {
            boolean b = getStrings(name.toLowerCase()).addAll(s);
            if (b) {
                em.getTransaction().begin();
                em.merge(string_pool);
                em.getTransaction().commit();
            }
        }

        @Override
        public int hashCode() {
            return 143635;
        }

        @Override
        public boolean equals(Object obj) {
            return obj instanceof StringStore;
        }
    }
}
