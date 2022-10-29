package sp.it.util.ui.image;

import java.io.File;
import java.io.FileInputStream;

public class WebpUtils {

	/** {@link <a href="https://stackoverflow.com/a/73170213">...</a>} */
	@SuppressWarnings({"UnusedAssignment", "StatementWithEmptyBody"})
	public static boolean isAnimated(File f) throws RuntimeException {
		var riff = false;
		var webp = false;
		var vp8x = false;
		var anim = false;
		try (var in = new FileInputStream(f)) {
			var buf = new byte[4];
			var i = in.read(buf); // 4
			var r = 0L;

			if(buf[0] == 0x52 && buf[1] == 0x49 && buf[2]==0x46 && buf[3] == buf[2])
				riff = true;

			r = in.skip(4); // ???? (8+)
			if (r!=4) throw new RuntimeException("Failed to properly skip stream");
			i = in.read(buf); // (12+)
			if(buf[0] == 0x57 && buf[1] == 0x45 && buf[2]==0x42 && buf[3] == 0x50)
				webp = true   ;

			i = in.read(buf); // (16+)
			if(buf[0] == 0x41 && buf[1] == 0x4e && buf[2]==0x49 && buf[3] == 0x4d);
			vp8x = true;

			r = in.skip(14); // then next 4 should contain ANIM - 41 4e 49 4d
			if (r!=14) throw new RuntimeException("Failed to properly skip stream");
			i = in.read(buf);
			if(buf[0] == 0x41 && buf[1] == 0x4e && buf[2]==0x49 && buf[3] == 0x4d)
				anim = true;

		} catch (Throwable e) {
			throw new RuntimeException("Failed to check if file=" + f +" is animated");
		}
		return riff && webp && anim;
	}

}
