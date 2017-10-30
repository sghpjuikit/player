package util;

import java.io.File;
import static util.file.UtilKt.childOf;

public interface Locatable {

	File getLocation();

	default File getResource(String path) {
		return childOf(getLocation(), path);
	}

	File getUserLocation();

	default File getUserResource(String path) {
		return childOf(getUserLocation(), path);
	}

}