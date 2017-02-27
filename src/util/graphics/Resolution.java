package util.graphics;

public enum Resolution {
	R_1x1(1, 1),
	R_3x2(3, 2),
	R_4x5(4, 5),
	R_16x9(16, 9),
	R_16x10(16, 10),
	R_1024x768(1024, 768),
	R_1600x1200(1600, 1200),
	R_1920x1080(1920, 1080),
	R_1920x1200(1920, 1200),
	R_2560x1440(2560, 1440),
	R_3840x2160(3840, 2160);

	public final double width, height, ratio;

	Resolution(double width, double height) {
		this.width = width;
		this.height = height;
		this.ratio = width/height;
	}
}