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

import static java.nio.file.StandardWatchEventKinds.ENTRY_CREATE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_DELETE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_MODIFY;
import static java.nio.file.StandardWatchEventKinds.OVERFLOW;
import static util.async.Async.runFX;
import static util.async.Async.runNew;
import static util.dev.Util.log;

/**
 *
 * @author Plutonium_
 */
public class FileMonitor {

    private File monitoredFile;
    private File monitoredFileDir;
    private WatchService watchService;
    private BiConsumer<Kind<Path>,File> action;

    public static FileMonitor monitorFile(File toMonitor, Consumer<Kind<Path>> handler) {
//        if(!toMonitor.isFile()) throw new IllegalArgumentException("Can not monitor a directory.");

        FileMonitor fm = new FileMonitor();
        fm.monitoredFile = toMonitor;
        fm.monitoredFileDir = fm.monitoredFile.getParentFile();
        fm.action = (type,file) -> handler.accept(type);

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
                            if(fm.monitoredFile.equals(modifiedFile)) {
                                log(FileMonitor.class).info("File monitor detected event {} on {}", type,modifiedFile);
                                runFX(() -> fm.action.accept((Kind)type,modifiedFile));
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
                            log(FileMonitor.class).info("Directory monitor detected event {} on {}", type,modifiedFile);
                            runFX(() -> fm.action.accept((Kind)type,modifiedFile));
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
