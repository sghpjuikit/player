package sp.it.pl.main;

import java.io.File;
import java.net.URI;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Predicate;
import static java.util.stream.Collectors.toList;
import static sp.it.pl.util.functional.Util.ISNTØ;

/**
 * Processes application parameters.
 */
public class AppParameterProcessor {
	private final List<ParameterProcessor> processors = new ArrayList<>();

	public void process(Collection<String> params) {
		processors.forEach(pp -> pp.process(params));
	}

	public void addProcessor(ParameterProcessor pp) {
		processors.add(pp);
	}

	public void addStringProcessor(Predicate<String> isProcessible, Consumer<List<String>> processor) {
		processors.add(new StringParameterProcessor(isProcessible, processor));
	}

	public void addURIProcessor(Predicate<URI> isProcessible, Consumer<List<URI>> processor) {
		processors.add(new URIParameterProcessor(isProcessible, processor));
	}

	public void addFileProcessor(Predicate<File> isProcessible, Consumer<List<File>> processor) {
		processors.add(new FileParameterProcessor(isProcessible, processor));
	}

	public interface ParameterProcessor {
		void process(Collection<String> params);
	}

	public static class StringParameterProcessor implements ParameterProcessor {
		private final Predicate<String> isProcessible;
		private final Consumer<List<String>> processor;

		public StringParameterProcessor(Predicate<String> isProcessible, Consumer<List<String>> processor) {
			this.isProcessible = isProcessible;
			this.processor = processor;
		}

		@Override
		public void process(Collection<String> params) {
			List<String> strings = params.stream().filter(isProcessible).collect(toList());
			if (!strings.isEmpty()) processor.accept(strings);
		}
	}

	public static class URIParameterProcessor implements ParameterProcessor {
		private final Predicate<URI> isProcessible;
		private final Consumer<List<URI>> processor;

		public URIParameterProcessor(Predicate<URI> isProcessible, Consumer<List<URI>> processor) {
			this.isProcessible = isProcessible;
			this.processor = processor;
		}

		@Override
		public void process(Collection<String> params) {
			List<URI> uris = params.stream()
					.map(p -> {
						try {
							if (p.length()>=2 && p.charAt(1)==':')
								return URI.create("file:///" + URLEncoder.encode(p, "UTF-8").replace("+", "%20"));
							return URI.create(java.net.URLEncoder.encode(p, "UTF-8").replace("+", "%20"));
						} catch (IllegalArgumentException|java.io.UnsupportedEncodingException e) {
							return null;
						}
					})
					.filter(ISNTØ)
					.filter(isProcessible)
					.collect(toList());
			if (!uris.isEmpty()) processor.accept(uris);
		}
	}

	public static class FileParameterProcessor implements ParameterProcessor {
		private final Predicate<File> isProcessible;
		private final Consumer<List<File>> processor;

		public FileParameterProcessor(Predicate<File> isProcessible, Consumer<List<File>> processor) {
			this.isProcessible = isProcessible;
			this.processor = processor;
		}

		@Override
		public void process(Collection<String> params) {
			List<File> files = params.stream()
					.map(p -> {
						try {
							if (p.length()>=2 && p.charAt(1)==':')
								return URI.create("file:///" + URLEncoder.encode(p, "UTF-8").replace("+", "%20"));
							return URI.create(java.net.URLEncoder.encode(p, "UTF-8").replace("+", "%20"));
						} catch (IllegalArgumentException|java.io.UnsupportedEncodingException e) {
							return null;
						}
					})
					.filter(ISNTØ)
					.map(u -> {
						try {
							return new File(u);
						} catch (IllegalArgumentException e) {
							return null;
						}
					})
					.filter(ISNTØ)
					.filter(isProcessible)
					.collect(toList());
			if (!files.isEmpty()) processor.accept(files);
		}
	}

}
