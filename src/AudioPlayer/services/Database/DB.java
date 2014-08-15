/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package AudioPlayer.services.Database;

import AudioPlayer.playlist.Item;
import AudioPlayer.tagging.Metadata;
import AudioPlayer.tagging.MetadataReader;
import java.io.File;
import java.util.List;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
import javax.persistence.TypedQuery;
import main.App;
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
            TypedQuery<Metadata> query = em.createQuery("SELECT p FROM Item p", Metadata.class);
            result = query.getResultList();
        }
        finally {
            em.close();
        }
        return result;
    }
    public static List<String> getAllArtists() {
        EntityManager em = emf.createEntityManager();
        List result;
        try {
            TypedQuery<String> query = em.createQuery("SELECT DISTINCT p.artist FROM Item p", String.class);
            result = query.getResultList();
            result.forEach(System.out::println);
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
    public static void updateItemsFromFile(List<? extends Item> items) {System.out.println("UP START");
        MetadataReader.readMetadata(items, (success,result) -> {System.out.println("UP R DONE");
            if(success) updateItems(result);System.out.println("U DONE");
        });
    }
    
    
    
    public static final EventSource<Void> librarychange = new EventSource();
}
