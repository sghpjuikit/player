/*
 * Copyright (c) 2007-2008 Timothy Wall, All Rights Reserved
 * Parts Copyright (c) 2007 Olivier Chafik
 *
 * The contents of this file is dual-licensed under 2
 * alternative Open Source/Free licenses: LGPL 2.1 or later and
 * Apache License 2.0. (starting with JNA version 4.0.0).
 *
 * You can freely decide which license you want to apply to
 * the project.
 *
 * You may obtain a copy of the LGPL License at:
 *
 * http://www.gnu.org/licenses/licenses.html
 *
 * A copy is also included in the downloadable source code package
 * containing JNA, in file "LGPL2.1".
 *
 * You may obtain a copy of the Apache License at:
 *
 * http://www.apache.org/licenses/
 *
 * A copy is also included in the downloadable source code package
 * containing JNA, in file "AL2.0".
 */
package sp.it.util.ui;

import com.sun.jna.Memory;
import com.sun.jna.Pointer;
import com.sun.jna.platform.WindowUtils;
import com.sun.jna.platform.win32.GDI32;
import com.sun.jna.platform.win32.User32;
import com.sun.jna.platform.win32.WinDef.HDC;
import com.sun.jna.platform.win32.WinDef.HICON;
import com.sun.jna.platform.win32.WinGDI.BITMAPINFO;
import com.sun.jna.platform.win32.WinGDI.BITMAPINFOHEADER;
import com.sun.jna.platform.win32.WinGDI.ICONINFO;
import java.awt.Dimension;
import java.awt.image.BufferedImage;

public class IconExtractorJNA {

	public static BufferedImage getWindowIcon(HICON hIcon) {
		final Dimension iconSize = getIconSize(hIcon);
		if (iconSize.width == 0 || iconSize.height == 0)
			return null;

		final int width = iconSize.width;
		final int height = iconSize.height;
		final short depth = 24;

		final byte[] lpBitsColor = new byte[width * height * depth / 8];
		final Pointer lpBitsColorPtr = new Memory(lpBitsColor.length);
		final byte[] lpBitsMask = new byte[width * height * depth / 8];
		final Pointer lpBitsMaskPtr = new Memory(lpBitsMask.length);
		final BITMAPINFO bitmapInfo = new BITMAPINFO();
		final BITMAPINFOHEADER hdr = new BITMAPINFOHEADER();

		bitmapInfo.bmiHeader = hdr;
		hdr.biWidth = width;
		hdr.biHeight = height;
		hdr.biPlanes = 1;
		hdr.biBitCount = depth;
		hdr.biCompression = 0;
		hdr.write();
		bitmapInfo.write();

		final HDC hDC = User32.INSTANCE.GetDC(null);
		final ICONINFO iconInfo = new ICONINFO();
		User32.INSTANCE.GetIconInfo(hIcon, iconInfo);
		iconInfo.read();
		GDI32.INSTANCE.GetDIBits(hDC, iconInfo.hbmColor, 0, height, lpBitsColorPtr, bitmapInfo, 0);
		lpBitsColorPtr.read(0, lpBitsColor, 0, lpBitsColor.length);
		GDI32.INSTANCE.GetDIBits(hDC, iconInfo.hbmMask, 0, height, lpBitsMaskPtr, bitmapInfo, 0);
		lpBitsMaskPtr.read(0, lpBitsMask, 0, lpBitsMask.length);
		final BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);

		int r, g, b, a, argb;
		int x = 0, y = height - 1;
		for (int i = 0; i < lpBitsColor.length; i = i + 3) {
			b = lpBitsColor[i] & 0xFF;
			g = lpBitsColor[i + 1] & 0xFF;
			r = lpBitsColor[i + 2] & 0xFF;
			a = 0xFF - lpBitsMask[i] & 0xFF;
			argb = (a << 24) | (r << 16) | (g << 8) | b;
			image.setRGB(x, y, argb);
			x = (x + 1) % width;
			if (x == 0)
				y--;
		}

		User32.INSTANCE.ReleaseDC(null, hDC);

		return image;
	}

	public static Dimension getIconSize(final HICON hIcon) {
		return WindowUtils.getIconSize(hIcon);
	}

}
