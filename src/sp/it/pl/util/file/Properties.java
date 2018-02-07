
/*
 * Based on java.util.Properties
 */

/*
 * Copyright (c) 1995, 2015, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

package sp.it.pl.util.file;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import static java.util.stream.Collectors.toMap;
import static sp.it.pl.util.Util.hasNoReadableText;
import static sp.it.pl.util.Util.hasReadableText;
import static sp.it.pl.util.dev.Util.logger;
import static sp.it.pl.util.dev.Util.noØ;
import static sp.it.pl.util.file.Util.isValidatedDirectory;
import static sp.it.pl.util.functional.Util.byNC;

public interface Properties {

	static Map<String,String> load(File file) {
		noØ(file);
		try (InputStream is = new FileInputStream(file)) {
			return load(is);
		} catch (IOException e) {
			return new HashMap<>();
		}
	}

	static Map<String,String> load(Reader reader) {
		noØ(reader);
		return load(new LineReader(reader));
	}

	static Map<String,String> load(InputStream inStream) {
		noØ(inStream);
		return load(new LineReader(inStream));
	}

	private static Map<String,String> load(LineReader lr) {
		Map<String,String> keyValues = new HashMap<>();
		char[] convertBuffer = new char[1024];
		int limit;
		int keyLen;
		int valueStart;
		char c;
		boolean hasSep;
		boolean precedingBackslash;

		try {
			while ((limit = lr.readLine())>=0) {
				keyLen = 0;
				valueStart = limit;
				hasSep = false;

				precedingBackslash = false;
				while (keyLen<limit) {
					c = lr.lineBuf[keyLen];
					// need check if escaped.
					if ((c=='=' || c==':') && !precedingBackslash) {
						valueStart = keyLen + 1;
						hasSep = true;
						break;
					} else if ((c==' ' || c=='\t' || c=='\f') && !precedingBackslash) {
						valueStart = keyLen + 1;
						break;
					}
					precedingBackslash = c=='\\' && !precedingBackslash;
					keyLen++;
				}
				while (valueStart<limit) {
					c = lr.lineBuf[valueStart];
					if (c!=' ' && c!='\t' && c!='\f') {
						if (!hasSep && (c=='=' || c==':')) {
							hasSep = true;
						} else {
							break;
						}
					}
					valueStart++;
				}
				String key = loadConvert(lr.lineBuf, 0, keyLen, convertBuffer);
				String value = loadConvert(lr.lineBuf, valueStart, limit - valueStart, convertBuffer);
				keyValues.put(key, value);
			}
		} catch (IOException e) {
			sp.it.pl.util.dev.Util.logger(Properties.class).error("Could not load properties from {}", lr);
			return keyValues;
		}
		return keyValues;
	}

	static void save(File file, String comments, Map<String,String> keyValues) {
		isValidatedDirectory(file.getParentFile().getAbsoluteFile());
		try (FileOutputStream os = new FileOutputStream(file)) {
			save(os, comments, keyValues);
		} catch (IOException e) {
			sp.it.pl.util.dev.Util.logger(Properties.class).error("Could not save properties into file {}", file, e);
		}
	}

	static void saveP(File file, String comments, Map<String,Property> keyValues) {
		isValidatedDirectory(file.getParentFile().getAbsoluteFile());
		try (FileOutputStream os = new FileOutputStream(file)) {
			saveP(os, comments, keyValues);
		} catch (IOException e) {
			sp.it.pl.util.dev.Util.logger(Properties.class).error("Could not save properties into file {}", file, e);
		}
	}

	static void save(OutputStream out, String comments, Map<String,String> keyValues) {
		try {
			store(
				new BufferedWriter(new OutputStreamWriter(out, "8859_1")),
				comments,
				true,
				keyValues.entrySet().stream().collect(toMap(Entry::getKey, e -> new Property("", e.getValue())))
			);
		} catch (IOException e) {
			sp.it.pl.util.dev.Util.logger(Properties.class).error("Could not save properties into output stream {}", out, e);
		}
	}

	static void saveP(OutputStream out, String comments, Map<String,Property> keyValues) {
		try {
			store(new BufferedWriter(new OutputStreamWriter(out, "8859_1")), comments, true, keyValues);
		} catch (IOException e) {
			sp.it.pl.util.dev.Util.logger(Properties.class).error("Could not save properties into output stream {}", out, e);
		}
	}

	static void save(Writer writer, String comments, Map<String,String> keyValues) {
		try {
			BufferedWriter bw = writer instanceof BufferedWriter ? (BufferedWriter) writer : new BufferedWriter(writer);
			store(bw, comments, false, keyValues.entrySet().stream().collect(toMap(Entry::getKey, e -> new Property("", e.getValue()))));
		} catch (IOException e) {
			sp.it.pl.util.dev.Util.logger(Properties.class).error("Could not save properties with writer {}", writer, e);
		}
	}

	private static void store(BufferedWriter bw, String comments, boolean escUnicode, Map<String,Property> keyValues) throws IOException {
		if (!hasNoReadableText(comments))
			writeComment(bw, comments);

		String cc = "# ";
		String cs = System.lineSeparator() + cc;
		try {
			keyValues.entrySet().stream()
					.sorted(byNC(Entry::getKey))
					.forEach(e -> {
						boolean hasComment = hasReadableText(e.getValue().comment); // ignore useless comments, just in case
						String comment = hasComment ? e.getValue().comment.replace("\n", cs) : null; // support multiline
						String key = saveConvert(e.getKey(), true, escUnicode);
						String val = saveConvert(e.getValue().value, false, escUnicode); // No need to escape embedded and trailing spaces -> false flag
						try {
							if (hasComment) {
								bw.write(cc + comment);
								bw.newLine();
							}
							bw.write(key + "=" + val);
							bw.newLine();
						} catch (IOException x) {
							throw new ExceptionWrapper(x);
						}
					});
		} catch (ExceptionWrapper e) {
			throw (IOException) e.getCause();
		}

		bw.flush();
	}

	/**
	 * Converts encoded &#92;uxxxx to unicode chars and changes special saved chars to their original forms
	 */
	private static String loadConvert(char[] in, int off, int len, char[] convertBuffer) {
		if (convertBuffer.length<len) {
			int newLen = len*2;
			if (newLen<0) {
				newLen = Integer.MAX_VALUE;
			}
			convertBuffer = new char[newLen];
		}
		char aChar;
		char[] out = convertBuffer;
		int outLen = 0;
		int end = off + len;

		while (off<end) {
			aChar = in[off++];
			if (aChar=='\\') {
				aChar = in[off++];
				if (aChar=='u') {
					// Read the xxxx
					int value = 0;
					for (int i = 0; i<4; i++) {
						aChar = in[off++];
						switch (aChar) {
							case '0': case '1': case '2': case '3': case '4':
							case '5': case '6': case '7': case '8': case '9':
								value = (value<<4) + aChar - '0';
								break;
							case 'a': case 'b': case 'c':
							case 'd': case 'e': case 'f':
								value = (value<<4) + 10 + aChar - 'a';
								break;
							case 'A': case 'B': case 'C':
							case 'D': case 'E': case 'F':
								value = (value<<4) + 10 + aChar - 'A';
								break;
							default:
								throw new IllegalArgumentException(
										"Malformed \\uxxxx encoding.");
						}
					}
					out[outLen++] = (char) value;
				} else {
					if (aChar=='t') aChar = '\t';
					else if (aChar=='r') aChar = '\r';
					else if (aChar=='n') aChar = '\n';
					else if (aChar=='f') aChar = '\f';
					out[outLen++] = aChar;
				}
			} else {
				out[outLen++] = aChar;
			}
		}
		return new String(out, 0, outLen);
	}

	/**
	 * Converts unicodes to encoded &#92;uxxxx and escapes special characters with a preceding slash
	 */
	private static String saveConvert(String theString, boolean escapeSpace, boolean escapeUnicode) {
		int len = theString.length();
		int bufLen = len*2;
		if (bufLen<0) {
			bufLen = Integer.MAX_VALUE;
		}
		StringBuilder outBuffer = new StringBuilder(bufLen);

		for (int x = 0; x<len; x++) {
			char aChar = theString.charAt(x);
			// Handle common case first, selecting largest block that
			// avoids the specials below
			if ((aChar>61) && (aChar<127)) {
				if (aChar=='\\') {
					outBuffer.append('\\'); outBuffer.append('\\');
					continue;
				}
				outBuffer.append(aChar);
				continue;
			}
			switch (aChar) {
				case ' ':
					if (x==0 || escapeSpace)
						outBuffer.append('\\');
					outBuffer.append(' ');
					break;
				case '\t': outBuffer.append('\\'); outBuffer.append('t');
					break;
				case '\n': outBuffer.append('\\'); outBuffer.append('n');
					break;
				case '\r': outBuffer.append('\\'); outBuffer.append('r');
					break;
				case '\f': outBuffer.append('\\'); outBuffer.append('f');
					break;
				case '=': // Fall through
				case ':': // Fall through
				case '#': // Fall through
				case '!':
					outBuffer.append('\\'); outBuffer.append(aChar);
					break;
				default:
					if (((aChar<0x0020) || (aChar>0x007e))&escapeUnicode) {
						outBuffer.append('\\');
						outBuffer.append('u');
						outBuffer.append(toHex((aChar>>12)&0xF));
						outBuffer.append(toHex((aChar>>8)&0xF));
						outBuffer.append(toHex((aChar>>4)&0xF));
						outBuffer.append(toHex(aChar&0xF));
					} else {
						outBuffer.append(aChar);
					}
			}
		}
		return outBuffer.toString();
	}

	/**
	 * Writes the string to the writer as property file comment.
	 */
	private static void writeComment(BufferedWriter bw, String comment) throws IOException {
		bw.write("#");
		int len = comment.length();
		int current = 0;
		int last = 0;
		char[] uu = new char[6];
		uu[0] = '\\';
		uu[1] = 'u';
		while (current<len) {
			char c = comment.charAt(current);
			if (c>'\u00ff' || c=='\n' || c=='\r') {
				if (last!=current)
					bw.write(comment.substring(last, current));
				if (c>'\u00ff') {
					uu[2] = toHex((c>>12)&0xf);
					uu[3] = toHex((c>>8)&0xf);
					uu[4] = toHex((c>>4)&0xf);
					uu[5] = toHex(c&0xf);
					bw.write(new String(uu));
				} else {
					bw.newLine();
					if (c=='\r' &&
							current!=len - 1 &&
							comment.charAt(current + 1)=='\n') {
						current++;
					}
					if (current==len - 1 ||
							(comment.charAt(current + 1)!='#' &&
									comment.charAt(current + 1)!='!'))
						bw.write("#");
				}
				last = current + 1;
			}
			current++;
		}
		if (last!=current)
			bw.write(comment.substring(last, current));
		bw.newLine();
		bw.newLine();
	}

	/**
	 * Convert a nibble to a hex character
	 *
	 * @param nibble the nibble to convert.
	 */
	private static char toHex(int nibble) {
		return hexDigit[(nibble&0xF)];
	}

	/**
	 * A table of hex digits
	 */
	char[] hexDigit = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F'};

	/**
	 * Read in a "logical line" from an InputStream/Reader, skip all comment
	 * and blank lines and filter out those leading whitespace characters
	 * (\u0020, \u0009 and \u000c) from the beginning of a "natural line".
	 * Method returns the char length of the "logical line" and stores
	 * the line in "lineBuf".
	 */
	class LineReader {
		LineReader(InputStream inStream) {
			this.inStream = inStream;
			inByteBuf = new byte[8192];
		}

		LineReader(Reader reader) {
			this.reader = reader;
			inCharBuf = new char[8192];
		}

		byte[] inByteBuf;
		char[] inCharBuf;
		char[] lineBuf = new char[1024];
		int inLimit = 0;
		int inOff = 0;
		InputStream inStream;
		Reader reader;

		int readLine() throws IOException {
			int len = 0;
			char c;

			boolean skipWhiteSpace = true;
			boolean isCommentLine = false;
			boolean isNewLine = true;
			boolean appendedLineBegin = false;
			boolean precedingBackslash = false;
			boolean skipLF = false;

			while (true) {
				if (inOff>=inLimit) {
					inLimit = (inStream==null) ? reader.read(inCharBuf)
							: inStream.read(inByteBuf);
					inOff = 0;
					if (inLimit<=0) {
						if (len==0 || isCommentLine) {
							return -1;
						}
						if (precedingBackslash) {
							len--;
						}
						return len;
					}
				}
				if (inStream!=null) {
					//The line below is equivalent to calling a
					//ISO8859-1 decoder.
					c = (char) (0xff&inByteBuf[inOff++]);
				} else {
					c = inCharBuf[inOff++];
				}
				if (skipLF) {
					skipLF = false;
					if (c=='\n') {
						continue;
					}
				}
				if (skipWhiteSpace) {
					if (c==' ' || c=='\t' || c=='\f') {
						continue;
					}
					if (!appendedLineBegin && (c=='\r' || c=='\n')) {
						continue;
					}
					skipWhiteSpace = false;
					appendedLineBegin = false;
				}
				if (isNewLine) {
					isNewLine = false;
					if (c=='#' || c=='!') {
						isCommentLine = true;
						continue;
					}
				}

				if (c!='\n' && c!='\r') {
					lineBuf[len++] = c;
					if (len==lineBuf.length) {
						int newLength = lineBuf.length*2;
						if (newLength<0) {
							newLength = Integer.MAX_VALUE;
						}
						char[] buf = new char[newLength];
						System.arraycopy(lineBuf, 0, buf, 0, lineBuf.length);
						lineBuf = buf;
					}
					precedingBackslash = c=='\\' && !precedingBackslash;  // flip the preceding backslash flag
				} else {
					// reached EOL
					if (isCommentLine || len==0) {
						isCommentLine = false;
						isNewLine = true;
						skipWhiteSpace = true;
						len = 0;
						continue;
					}
					if (inOff>=inLimit) {
						inLimit = (inStream==null)
								? reader.read(inCharBuf)
								: inStream.read(inByteBuf);
						inOff = 0;
						if (inLimit<=0) {
							if (precedingBackslash) {
								len--;
							}
							return len;
						}
					}
					if (precedingBackslash) {
						len -= 1;
						//skip the leading whitespace characters in following line
						skipWhiteSpace = true;
						appendedLineBegin = true;
						precedingBackslash = false;
						if (c=='\r') {
							skipLF = true;
						}
					} else {
						return len;
					}
				}
			}
		}
	}

	class ExceptionWrapper extends RuntimeException {
		ExceptionWrapper(Throwable e) {
			super(e);
		}
	}

	class Property {
		public final String comment, value;

		public Property(String comment, String value) {
			this.comment = comment;
			this.value = value;
		}
	}
}
