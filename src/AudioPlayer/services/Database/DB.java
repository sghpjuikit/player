/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package AudioPlayer.services.Database;

import AudioPlayer.playlist.Item;
import AudioPlayer.tagging.Metadata;
import java.io.File;
import java.net.URI;
import java.util.*;
import static java.util.Collections.EMPTY_LIST;
import static javafx.application.Platform.runLater;
import javax.persistence.*;
import main.App;
import util.access.Accessor;
import static util.async.Async.FX;
import util.async.future.Fut;
import static util.functional.Util.stream;
import util.reactive.CascadingStream;

/**
 *
 * @author Plutonium_
 */
public class DB {
    
    public static EntityManagerFactory emf;
    public static EntityManager em;
    
    public static void start() {
        emf = Persistence.createEntityManagerFactory(App.LIBRARY_FOLDER().getPath() + File.separator + "library_database.odb");
        em = emf.createEntityManager();
        
        new Fut<>()
            // load database
            .supply(DB::getAllItems)
            // update gui
            .use(all_items -> views.push(1, all_items), FX)
            .thenR(() -> {
             // load string store
                List<StringStore> sss = em.createQuery("SELECT p FROM StringStore p", StringStore.class).getResultList();
                string_pool = sss.isEmpty() ? new StringStore() : sss.get(0);
             
             // populate metadata fields strings if empty
                if(string_pool.getStrings("album").isEmpty() && !views.getValue(1).isEmpty()) {
                    stream(Metadata.Field.values())
                        .filter(f -> f.isAutoCompleteable())
                        .forEach(f -> {
                            Set<String> pool = string_pool.getStrings(f.name());
                            views.getValue(1).stream()
                                 .map(m -> m.getFieldS(f,""))
                                 .filter(t -> !t.isEmpty())
                                 .distinct()
                                 // .peek(System.out::println) // debug
                                 .forEach(t -> pool.add(t));
                        });
                    // persist
                    em.getTransaction().begin();
                    em.merge(string_pool);
                    em.getTransaction().commit();
                }
            })
            .showProgress(App.getWindow().taskAdd())
            .run();
    }
    
    public static void stop() {
        if(em!=null && em.isOpen()) em.close();
        if(emf!=null) emf.close();
    }
    
/******************************** OBTAINING ***********************************/
    
    public static boolean exists(Item item) {
        return exists(item.getURI());
    }
    
    public static boolean exists(URI uri) {
        return null != em.find(Metadata.class, uri.toString());
    }
    
    /**
     * Returns item from library.
     * <p>
     * Never pass the returned item into the application before making sure it
     * is not null.
     * @return item from library with the URI of the specified item or null if not found.
     */
    public static Metadata getItem(Item item) {
        return getItem(item.getURI());
    }
    
    /**
     * Returns item from library.
     * <p>
     * Never pass the returned item into the application before making sure it
     * is not null.
     * @return item from library with the specified URI or null if not found.
     */
    public static Metadata getItem(URI uri) {
        return em.find(Metadata.class, uri.toString());
    }
    
    public static List<Metadata> getAllItems() {
        return em.createQuery("SELECT p FROM MetadataItem p", Metadata.class)
                 .getResultList();
    }
    
    public static List<Metadata> getAllItemsWhere(Metadata.Field field, Object value) {
        return getAllItemsWhere(Collections.singletonMap(field, Collections.singletonList(value)));
    }
    
    public static List<String> getAllArtists() {
        return em.createQuery("SELECT p.artist, count(p) FROM MetadataItem p GROUP BY p.artist", String.class)
                 .getResultList();
    }
    
    public static List<Metadata> getAllItemsWhere(Map<Metadata.Field,List<Object>> filters) {
        List result;
        
            Accessor<String> filter = new Accessor("");
            
            filters.forEach((field,values) -> {

                if (values.isEmpty()) throw new IllegalArgumentException("value list for query must not be empty");

                String f = field.isTypeNumber()
                    ? " WHERE p."+field.name().toLowerCase() + " = " + values.get(0).toString().replaceAll("'", "''")
                    : " WHERE p."+field.name().toLowerCase()+ " LIKE '" + values.get(0).toString().replaceAll("'", "''") + "'";
                filter.setValue(filter.getValue() + f);
            });
            
        TypedQuery<Metadata> query = em.createQuery("SELECT p FROM MetadataItem p" + filter.getValue(), Metadata.class);
        result = query.getResultList();

        return result;
    }
    
    public static void addItems(List<Metadata> items) {
        if (items.isEmpty()) return;
        // add to db
        em.getTransaction().begin();
        items.forEach( m -> {
            if(em.find(Metadata.class, m.getId()) == null) em.persist(m);
        });
        em.getTransaction().commit();
       // update model
        updateLib();
    }
    
    public static void removeItems(List<Metadata> items) {
        // remove in db
        em.getTransaction().begin();
        items.forEach( m -> {
            Metadata in_db = em.find(Metadata.class, m.getId());
            if(in_db != null) em.remove(in_db);
        });
        em.getTransaction().commit();
       // update model
        updateLib();
    }
    
    public static void updateItems(List<Metadata> items) {
        // update db
        em.getTransaction().begin();
        items.forEach(em::merge);
        em.getTransaction().commit();
        // update model
        updateLib();
    }
    public static void updateItemsBgr(List<Metadata> items) {
        // update db
        em.getTransaction().begin();
        items.forEach(em::merge);
        em.getTransaction().commit();
        // update model
        runLater(() -> views.push(1, getAllItems()));
    }

    public static void updateLib() {
        views.push(1, getAllItems());
    }
    
    public static void clearLib() {
        views.push(1, EMPTY_LIST);
        em.clear();
        em.flush();
    }
    
    /** In memory item database. Use for library.*/
    public static CascadingStream<List<Metadata>> views = new CascadingStream<>();
    
    /** 
     * Comparator defining the sorting for items in operations that wish to
     * provide consistent sorting across the application.
     * <p>
     * The comparator should reflect library table sort order.
     */
    public static Comparator<Metadata> library_sorter = Metadata::compareTo;

    /**
     * In memory storage for strings that persists in database.
     * Map that maps sets of strings to string keys. The keys are case-insensitive.
     * <p>
     * The store is loaded when DB starts. Changes persist immediately.
     */
    public static StringStore string_pool;
    

/******************************************************************************/
    
    @Entity(name = "StringStore")
    public static class StringStore {
        private HashMap<String,HashSet<String>> pool = new HashMap();

        private StringStore() {}
        
        public Set<String> getStrings(String name) {
            String n = name.toLowerCase();
            if (!string_pool.pool.containsKey(n)) string_pool.pool.put(n, new HashSet());
            return string_pool.pool.get(n);
        }

        public void addString(String name, String s) {
            boolean b = getStrings(name.toLowerCase()).add(s);
            if(b) {
                em.getTransaction().begin();
                em.merge(string_pool);
                em.getTransaction().commit();
            }
        }

        public void addStrings(String name, List<String> s) {
            boolean b = getStrings(name.toLowerCase()).addAll(s);
            if(b) {
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
