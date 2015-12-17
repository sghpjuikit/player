/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package util.File;

import java.io.File;
import java.io.IOException;
import java.nio.file.ClosedWatchServiceException;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.WatchEvent;
import java.nio.file.WatchEvent.Kind;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import util.async.executor.EventReducer;
import util.collections.Tuple2;

import static java.nio.file.StandardWatchEventKinds.ENTRY_CREATE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_DELETE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_MODIFY;
import static java.nio.file.StandardWatchEventKinds.OVERFLOW;
import static util.async.Async.runFX;
import static util.async.Async.runNew;
import static util.collections.Tuples.tuple;
import static util.dev.Util.log;

/**
 *
 * @author Plutonium_
 */
public class FileMonitor {

    // Relatively simple to monitor a file? Think again.
    // 1) WatchService allows us to only monitor a directory. We then must simple ignore other
    //    events of files other than the one we monitor. This is really bad if we want to monitor
    //    multiple files in a single directory (each on its own). We would have to use 1 thread
    //    and 1 watch service per each file!
    // 2) Modification events. Not even goign to tryto understand - modifications events fire
    //    multiple times! When I edit java source file in Netbeans and save I get 3 events at about
    //    8-13 ms time gap (tested on SSD)!
    //    We clearly have to use an event reducer here

    private File monitoredFile;
    private File monitoredFileDir;
    private WatchService watchService;
    private BiConsumer<Kind<Path>,File> action;
    private boolean isFile;
    private String name; // purely for logging "Directory" or "File"

    EventReducer<Tuple2<Kind<Path>,File>> modificationReducer = EventReducer.toLast(50, e -> emitEvent(e._1,e._2));

    private void emitEvent(Kind<Path> type, File file) {
        // This works as it should.
        // If anyone needs logging, they are free to do so in the event handler.
        // log(FileMonitor.class).info("{} event {} on {}", name,type,file);

        // always run on fx thread
        runFX(() -> action.accept(type,file));
    }

    public static FileMonitor monitorFile(File toMonitor, Consumer<Kind<Path>> handler) {
//        if(!toMonitor.isFile()) throw new IllegalArgumentException("Can not monitor a directory.");

        FileMonitor fm = new FileMonitor();
        fm.monitoredFile = toMonitor;
        fm.monitoredFileDir = fm.monitoredFile.getParentFile();
        fm.action = (type,file) -> handler.accept(type);
        fm.isFile = true;
        fm.name = fm.isFile ? "File" : "Directory";

        Path dir = fm.monitoredFileDir.toPath();
        try {
            fm.watchService = FileSystems.getDefault().newWatchService();
            dir.register(fm.watchService, ENTRY_CREATE,ENTRY_DELETE,ENTRY_MODIFY,OVERFLOW);
            runNew(() -> {
                boolean valid;
                WatchKey watchKey;
                do {
                    try {
                        watchKey = fm.watchService.take();
                    } catch (InterruptedException e) {
                        log(FileMonitor.class).error("Interrupted monitoring of directory {}", dir,e);
                        return;
                    } catch (ClosedWatchServiceException e) {
                        // watching ended
                        return;
                    }

                    for (WatchEvent<?> event : watchKey.pollEvents()) {
                        Kind<?> type = event.kind();

                        if (type==OVERFLOW) continue;

                        WatchEvent<Path> ev = (WatchEvent<Path>) event;
                        String modifiedFileName = ev.context().toString();
                        File modifiedFile = new File(fm.monitoredFileDir, modifiedFileName);

                        if(fm.monitoredFile.equals(modifiedFile)) {
                            if(type==ENTRY_MODIFY) {
                                runFX(() ->
                                    fm.modificationReducer.push(tuple((Kind)type, modifiedFile))
                                );
                            } else {
                                fm.emitEvent((Kind)type, modifiedFile);
                            }
                        }
                    }

                    valid = watchKey.reset();
                } while (valid);
            });
        } catch (IOException e) {
            log(FileMonitor.class).error("Error when starting file monitoring {}", fm.monitoredFile,e);
        }

        return fm;
    }

    public static FileMonitor monitorDirectory(File toMonitor, BiConsumer<Kind<Path>,File> handler) {
//        if(!toMonitor.isDirectory()) throw new IllegalArgumentException("Can not monitor a file.");

        FileMonitor fm = new FileMonitor();
        fm.monitoredFile = toMonitor;
        fm.monitoredFileDir = toMonitor;
        fm.action = handler;
        fm.isFile = false;
        fm.name = fm.isFile ? "File" : "Directory";

        Path dir = fm.monitoredFileDir.toPath();
        try {
            fm.watchService = FileSystems.getDefault().newWatchService();
            dir.register(fm.watchService, ENTRY_CREATE,ENTRY_DELETE,ENTRY_MODIFY,OVERFLOW);
            runNew(() -> {
                boolean valid = true;
                WatchKey watchKey;
                do {
                    try {
                        watchKey = fm.watchService.take();
                    } catch (InterruptedException e) {
                        log(FileMonitor.class).error("Interrupted monitoring of directory {}", dir,e);
                        return;
                    } catch (ClosedWatchServiceException e) {
                        // watching ended
                        return;
                    }

                    for (WatchEvent<?> event : watchKey.pollEvents()) {
                        Kind<?> type = event.kind();
                        if (type!=OVERFLOW) {
                            WatchEvent<Path> ev = (WatchEvent<Path>) event;
                            String modifiedFileName = ev.context().toString();
                            File modifiedFile = new File(fm.monitoredFileDir, modifiedFileName);

                            fm.emitEvent((Kind)type, modifiedFile);
                        }
                    }

                    valid = watchKey.reset();
                } while (valid);
            });
        } catch (IOException e) {
            log(FileMonitor.class).error("Error when starting directory monitoring {}", fm.monitoredFile,e);
        }

        return fm;
    }

    public void stop() {
        try {
            if(watchService!=null) watchService.close();
        } catch (IOException e) {
            log(FileMonitor.class).error("Error when closing file monitoring {}", monitoredFile, e);
        }
    }
}
