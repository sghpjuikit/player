/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package AudioPlayer.services.Database;

import AudioPlayer.playlist.Item;
import AudioPlayer.services.Database.POJO.MetadataGroup;
import AudioPlayer.tagging.Metadata;
import AudioPlayer.tagging.MetadataReader;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
import javax.persistence.TypedQuery;
import main.App;
import org.reactfx.BiEventSource;
import org.reactfx.EventSource;

/**
 *
 * @author Plutonium_
 */
public class DB {
    
    public static EntityManagerFactory emf;
    
    public static void start() {
        emf = Persistence.createEntityManagerFactory(App.LIBRARY_FOLDER().getPath() + File.separator + "library_database.odb");
    }
    
    public static void stop() {
        if(emf!=null) {
           emf.close();
        }
    }
    
    
    
    public static void addItems(List<Metadata> items) {
        if (items.isEmpty()) return;
        
        EntityManager em = emf.createEntityManager();
        try {
            em.getTransaction().begin();
            items.forEach( m -> {
                if(em.find(Metadata.class, m.getId()) == null) em.persist(m);
            });
            em.getTransaction().commit();
        }
        finally {
            em.close();
        }
        librarychange.push(null);
    }
    public static List<Metadata> getAllItems() {
        EntityManager em = emf.createEntityManager();
        List result;
        try {
            TypedQuery<Metadata> query = em.createQuery("SELECT p FROM MetadataItem p", Metadata.class);
            result = query.getResultList();
        }
        finally {
            em.close();
        }
        return result;
    }
    public static List<Metadata> getAllItemsWhere(Metadata.Field field, Object value) {
        EntityManager em = emf.createEntityManager();
        List result;
        try {
            
            String q = (value instanceof String)
                    ? "SELECT p FROM MetadataItem p WHERE p."+field.name().toLowerCase()+" LIKE '" + value.toString() + "'"
                    : "SELECT p FROM MetadataItem p WHERE p."+field.name().toLowerCase()+" IS " + value.toString();
            TypedQuery<Metadata> query = em.createQuery(q, Metadata.class);
            result = query.getResultList();
        }
        finally {
            em.close();
        }
        return result;
    }
    public static List<Object[]> getAllArtists() {
        EntityManager em = emf.createEntityManager();
        List result;
        try {
            TypedQuery<String> query = em.createQuery("SELECT p.artist, count(p) FROM MetadataItem p GROUP BY p.artist", String.class);
            result = query.getResultList();
        }
        finally {
            em.close();
        }
        return result;
    }
    
    public static List<MetadataGroup> getAllGroups(Metadata.Field metadata_field) {
        EntityManager em = emf.createEntityManager();
        List<MetadataGroup> result = new ArrayList();
        try {
            String f = "p." + metadata_field.toString().toLowerCase();
            String q = "SELECT " + f + ", COUNT(p), SUM(p.duration), SUM(p.filesize) FROM MetadataItem p GROUP BY " + f;
            //query = "SELECT p.FIELD, COUNT(p), SUM(p.length), SUM(p.filesize) FROM MetadataItem p GROUP BY p.FIELD");
            TypedQuery query = em.createQuery(q,Metadata.class);
            List<Object[]> rs = query.getResultList();
            rs.stream()
            .map(r->
                // or some strange reason sum(length) returns long! not double below is the original line
                //System.out.println(r[0]+" "+r[1].getClass()+" "+r[2].getClass()+" "+r[3].getClass());
                //return new MetadataGroup( metadata_field, r[0], (long)r[1], (long)r[1], (double)r[2], (long)r[3]);
                new MetadataGroup( metadata_field, r[0], (long)r[1], (long)r[1], Double.valueOf(String.valueOf(r[2])), (long)r[3])
            )
            .forEach(result::add);
        }
        finally {
            em.close();
        }
        return result;
    }
    
    public static void updateItems(List<Metadata> items) {
        EntityManager em = emf.createEntityManager();
        try {
            em.getTransaction().begin();
            items.forEach( m -> {
//                Metadata in_db = em.find(Metadata.class, m.getId());
//                if(in_db != null) 
                    em.merge(m);
            });
            em.getTransaction().commit();
        }
        finally {
            em.close();
        }
        librarychange.push(null);
    }

    public static void removeItems(List<Metadata> items) {
        EntityManager em = emf.createEntityManager();
        try {
            em.getTransaction().begin();
            items.forEach( m -> {
                Metadata in_db = em.find(Metadata.class, m.getId());
                if(in_db != null) em.remove(in_db);
            });
            em.getTransaction().commit();
        }
        finally {
            em.close();
        }
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
            
   
}
