/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package gui.objects.image.cover;

import java.io.File;
import java.util.Objects;

import javafx.scene.image.Image;

import gui.objects.image.Thumbnail;
import util.Util;

/**
 * Denotes Cover represented by a {@link File}.
 * <p/>
 * This class is fully polymorphic
 * Should never be used directly but instead use the {@Cover} interface
 * and leverage polymorphism.
 *
 * @author Martin Polakovic
 */
public class FileCover implements Cover {
    private final File file;
    private final String info;

    public FileCover(File image, String description) {
        Objects.requireNonNull(description);

        this.file = image;
        this.info = description;
    }

    /** {@inheritDoc} */
    @Override
    public Image getImage() {
        return file==null ? null : new Image(file.toURI().toString());
    }

    /** {@inheritDoc} */
    @Override
    public Image getImage(double width, double height) {
        Image cached = Thumbnail.getCached(file, width, width);
        if(cached!=null) return cached;
        return file==null ? null : Util.loadImage(file, width, height);
    }

    /** {@inheritDoc} */
    @Override
    public File getFile() {
        return file;
    }

    @Override
    public boolean isEmpty() {
        return file != null;
    }

    /** {@inheritDoc} */
    @Override
    public String getDestription() {
        return info;
    }

    @Override
    public boolean equals(Object o) {
        if(this==o) return true;

        if(o != null && o instanceof FileCover) {
            FileCover other = (FileCover)o;
            return file.equals(other.file);
        }
        return false;
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 43 * hash + Objects.hashCode(this.file);
        return hash;
    }


}