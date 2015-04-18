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
import java.util.Collections;
import static java.util.Collections.EMPTY_LIST;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
import javax.persistence.TypedQuery;
import main.App;
import util.access.Accessor;
import static util.async.Async.FXAFTER;
import util.async.future.Fut;
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
            // read db on bgr thread
            .supply(DB::getAllItems)                        
            // populate lib - overloads FX thread, so delay
            // todo: detect when app starts up and run right afterwards
            // .use(i -> views.push(1, i), FX)
            .use(i -> views.push(1, i), FXAFTER(7000))
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

    public static void updateLib() {
        views.push(1, getAllItems());
    }
    
    public static void clearLib() {
        views.push(1, EMPTY_LIST);
        em.clear();
        em.flush();
    }
    
    
    public static CascadingStream<List<Metadata>> views = new CascadingStream<>();
    
    /** Comparator defining the sorting for items in operations that wish to
    provide consistent sorting. For example playing an album might presort the
    songs before putting them to playlist.
    <p>
    The comparator should reflect library table sort order.
    */
    public static Comparator<Metadata> library_sorter = Metadata::compareTo;
}
