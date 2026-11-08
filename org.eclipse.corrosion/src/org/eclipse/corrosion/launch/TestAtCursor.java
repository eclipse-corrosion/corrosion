/*********************************************************************
 * Copyright (c) 2026 Contributors to the Eclipse Foundation.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.corrosion.launch;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.eclipse.corrosion.CorrosionPlugin;
import org.eclipse.corrosion.edit.RustAnalyzerServer;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.lsp4e.LSPEclipseUtils;
import org.eclipse.lsp4e.LanguageServers;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.TextDocumentIdentifier;
import org.eclipse.ui.texteditor.ITextEditor;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

/**
 * The test function at the cursor in a Rust editor, as rust-analyzer reports it
 * with its {@code experimental/runnables} request.
 *
 * @param name      the test path, for example {@code tests::adds_two_numbers}
 * @param options   the Cargo options, for example {@code --package foo --lib}
 * @param arguments the arguments for the test binary, for example
 *                  {@code --exact}
 */
public record TestAtCursor(String name, String options, String arguments) {

	private static final String TEST_LABEL_PREFIX = "test "; //$NON-NLS-1$
	private static final String NO_CAPTURE = "--nocapture"; //$NON-NLS-1$
	private static final long TIMEOUT_SECONDS = 3;

	/**
	 * Asks rust-analyzer for the test at the cursor of the given editor.
	 *
	 * @param editor
	 * @return the test at the cursor, or an empty value if the cursor is not in a
	 *         test or rust-analyzer does not reply in time
	 */
	public static Optional<TestAtCursor> find(ITextEditor editor) {
		IDocument document = LSPEclipseUtils.getDocument(editor);
		ISelection selection = editor.getSelectionProvider().getSelection();
		if (document == null || !(selection instanceof ITextSelection textSelection)) {
			return Optional.empty();
		}
		URI uri = LSPEclipseUtils.toUri(document);
		if (uri == null) {
			return Optional.empty();
		}
		try {
			Position position = LSPEclipseUtils.toPosition(textSelection.getOffset(), document);
			Map<String, Object> params = Map.of("textDocument", new TextDocumentIdentifier(uri.toString()), //$NON-NLS-1$
					"position", position); //$NON-NLS-1$
			Optional<JsonElement> reply = LanguageServers.forDocument(document)
					.computeFirst(server -> server instanceof RustAnalyzerServer rustAnalyzer
							? rustAnalyzer.runnables(params)
							// Other servers, such as GitHub Copilot, do not know this request
							: CompletableFuture.completedFuture(null))
					.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
			return reply.flatMap(result -> fromRunnables(result, position));
		} catch (BadLocationException | ExecutionException | TimeoutException | RuntimeException e) {
			CorrosionPlugin.logError(e);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}
		return Optional.empty();
	}

	/**
	 * Selects the test runnable with the smallest range that contains the
	 * position. A test runnable has a label such as "test tests::adds".
	 */
	static Optional<TestAtCursor> fromRunnables(JsonElement runnables, Position position) {
		if (!(runnables instanceof JsonArray array)) {
			return Optional.empty();
		}
		JsonObject best = null;
		int bestSize = Integer.MAX_VALUE;
		for (JsonElement element : array) {
			if (!(element instanceof JsonObject runnable) || !runnable.has("label") //$NON-NLS-1$
					|| !runnable.get("label").getAsString().startsWith(TEST_LABEL_PREFIX) //$NON-NLS-1$
					|| !(runnable.get("location") instanceof JsonObject location) //$NON-NLS-1$
					|| !(location.get("targetRange") instanceof JsonObject range)) { //$NON-NLS-1$
				continue;
			}
			int startLine = range.getAsJsonObject("start").get("line").getAsInt(); //$NON-NLS-1$ //$NON-NLS-2$
			int endLine = range.getAsJsonObject("end").get("line").getAsInt(); //$NON-NLS-1$ //$NON-NLS-2$
			int size = endLine - startLine;
			if (startLine <= position.getLine() && position.getLine() <= endLine && size < bestSize) {
				best = runnable;
				bestSize = size;
			}
		}
		if (best == null || !(best.get("args") instanceof JsonObject args)) { //$NON-NLS-1$
			return Optional.empty();
		}
		List<String> cargoArgs = toList(args.getAsJsonArray("cargoArgs")); //$NON-NLS-1$
		List<String> executableArgs = toList(args.getAsJsonArray("executableArgs")); //$NON-NLS-1$
		if (cargoArgs.isEmpty() || !"test".equals(cargoArgs.get(0)) || executableArgs.isEmpty()) { //$NON-NLS-1$
			return Optional.empty();
		}
		String name = executableArgs.get(0);
		String options = String.join(" ", cargoArgs.subList(1, cargoArgs.size())); //$NON-NLS-1$
		// The test output goes to the unit test view, so do not mix it with the
		// output of the test code.
		List<String> rest = new ArrayList<>(executableArgs.subList(1, executableArgs.size()));
		rest.remove(NO_CAPTURE);
		return Optional.of(new TestAtCursor(name, options, String.join(" ", rest))); //$NON-NLS-1$
	}

	private static List<String> toList(JsonArray array) {
		List<String> result = new ArrayList<>();
		if (array != null) {
			array.forEach(element -> result.add(element.getAsString()));
		}
		return result;
	}
}
