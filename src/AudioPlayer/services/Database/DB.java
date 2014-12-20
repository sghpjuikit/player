/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package AudioPlayer.services.Database;

import AudioPlayer.playlist.Item;
import AudioPlayer.tagging.Metadata;
import AudioPlayer.tagging.MetadataGroup;
import AudioPlayer.tagging.MetadataReader;
import java.io.File;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import static javafx.application.Platform.runLater;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
import javax.persistence.TypedQuery;
import main.App;
import org.reactfx.BiEventSource;
import org.reactfx.EventSource;
import util.access.Accessor;
import static util.async.Async.run;

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
    
    
    public static void addItems(List<Metadata> items) {
        if (items.isEmpty()) return;
        
        em.getTransaction().begin();
        items.forEach( m -> {
            if(em.find(Metadata.class, m.getId()) == null) em.persist(m);
        });
        em.getTransaction().commit();
        
        librarychange.push(null);
    }
    
    public static List<Metadata> getAllItems() {
        TypedQuery<Metadata> query = em.createQuery("SELECT p FROM MetadataItem p", Metadata.class);
        return query.getResultList();
    }
    
    public static List<Metadata> getAllItemsWhere(Metadata.Field field, Object value) {
        return getAllItemsWhere(Collections.singletonMap(field, Collections.singletonList(value)));
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
    public static List<String> getAllArtists() {
        TypedQuery<String> query = em.createQuery("SELECT p.artist, count(p) FROM MetadataItem p GROUP BY p.artist", String.class);
        return query.getResultList();
    }
    
    public static List<MetadataGroup> getAllGroups(Metadata.Field metadata_field) {
        return getAllGroups(metadata_field, new HashMap<>());
    } 
    public static List<MetadataGroup> getAllGroups(Metadata.Field groupByField, Map<Metadata.Field,List<Object>> filters) {
        List<MetadataGroup> result = new ArrayList();
            
//            filters.put(Metadata.Field.PUBLISHER, Collections.singletonList("Import"));

        Accessor<String> filter = new Accessor("");

        filters.forEach((field,values) -> {

            if (values.isEmpty()) throw new IllegalArgumentException("value list for query must not be empty");

            String f = field.isTypeNumber()
                ? " WHERE p."+field.name().toLowerCase() + " = " + values.get(0).toString()
                : " WHERE p."+field.name().toLowerCase()+ " LIKE '" + values.get(0).toString() + "'";
            filter.setValue(filter.getValue() + f);
        });

        String f = "p." + groupByField.toString().toLowerCase();
        String q = "SELECT " + f + ", COUNT(p), SUM(p.duration), SUM(p.filesize) FROM MetadataItem p " + filter.getValue() + " GROUP BY " + f;
        System.out.println(q);
        TypedQuery<Object[]> query = em.createQuery(q,Object[].class);
        query.getResultList().stream().map(r->
                // or some strange reason sum(length) returns long! not double below is the original line
                //System.out.println(r[0]+" "+r[1].getClass()+" "+r[2].getClass()+" "+r[3].getClass());
                //return new MetadataGroup( metadata_field, r[0], (long)r[1], (long)r[1], (double)r[2], (long)r[3]);
                new MetadataGroup( groupByField, r[0], (long)r[1], (long)r[1], Double.valueOf(String.valueOf(r[2])), (long)r[3])
            )
            .forEach(result::add);
        
//        Query qq = em.createQuery("SELECT p.album, COUNT(p), SUM(p.duration), SUM(p.filesize) FROM MetadataItem p "
//                + "WHERE p.artist = aa OR p.filesize >0"
//                + "GROUP BY p.album");
        
        return result;
//        EntityManager em = emf.createEntityManager();
//        List<MetadataGroup> result = new ArrayList();
//        try {
//            String f = "p." + groupByField.toString().toLowerCase();
//            String q = "SELECT " + f + ", COUNT(p), SUM(p.duration), SUM(p.filesize) FROM MetadataItem p GROUP BY " + f;
//            //query = "SELECT p.FIELD, COUNT(p), SUM(p.length), SUM(p.filesize) FROM MetadataItem p GROUP BY p.FIELD");
//            TypedQuery query = em.createQuery(q,Metadata.class);
//            List<Object[]> rs = query.getResultList();
//            rs.stream()
//            .map(r->
//                // or some strange reason sum(length) returns long! not double below is the original line
//                //System.out.println(r[0]+" "+r[1].getClass()+" "+r[2].getClass()+" "+r[3].getClass());
//                //return new MetadataGroup( metadata_field, r[0], (long)r[1], (long)r[1], (double)r[2], (long)r[3]);
//                new MetadataGroup( groupByField, r[0], (long)r[1], (long)r[1], Double.valueOf(String.valueOf(r[2])), (long)r[3])
//            )
//            .forEach(result::add);
//        }
//        finally {
//            em.close();
//        }
//        return result;
    }
    
    private static void updateItems(List<Metadata> items) {
        em.getTransaction().begin();
        items.forEach( m -> {
//                Metadata in_db = em.find(Metadata.class, m.getId());
//                if(in_db != null) 
                em.merge(m);
        });
        em.getTransaction().commit();

        librarychange.push(null);
    }

    public static void removeItems(List<Metadata> items) {
        em.getTransaction().begin();
        items.forEach( m -> {
            Metadata in_db = em.find(Metadata.class, m.getId());
            if(in_db != null) em.remove(in_db);
        });
        em.getTransaction().commit();
    
        librarychange.push(null);
    }
    public static void updateItemsFromFile(List<? extends Item> items) {
        MetadataReader.readMetadata(items, (success,result) -> {
            if(success) updateItems(result);
        });
    }
    
    
    /**
     * Fires on every remove, update, insert operation on the database.
     */
    public static final EventSource<Void> librarychange = new EventSource();
    
    /**
    * Fires on every library view field change. The event indicates for library to
    * refresh items it displays according to the new field as a filter.
    * <p>
    * A widget serving a role of a library view can push new value into this
    * event source to emit new event.
    * <p>
    * A library widget can monitor this stream instead of {@link #librarychange}
    */
    public static final BiEventSource<Metadata.Field,Object> fieldSelectionChange = new BiEventSource();
    public static final EventSource<List<Metadata>> filteredItemsEvent = new EventSource<>();
//            fieldSelectionChange.map((metaField,value) -> getAllItemsWhere(metaField, value));
    public static Map<Integer,String> filterFields = new HashMap();
            
    static {
        fieldSelectionChange.subscribe((metaField,value)->{
            run(()->{
                List<Metadata> items = getAllItemsWhere(metaField, value);
                runLater(()->filteredItemsEvent.push(items));
                // performance testing - even this still causes GUI lag
                // runLater(()->filteredItemsEvent.push(getAllItems()));
            });
        });
    }
    
    
}
