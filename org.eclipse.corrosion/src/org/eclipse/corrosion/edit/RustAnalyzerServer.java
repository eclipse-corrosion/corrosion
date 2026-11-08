/*********************************************************************
 * Copyright (c) 2026 Contributors to the Eclipse Foundation.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.corrosion.edit;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

import org.eclipse.lsp4j.jsonrpc.services.JsonRequest;
import org.eclipse.lsp4j.services.LanguageServer;

import com.google.gson.JsonElement;

/**
 * The rust-analyzer requests that are not in the LSP specification. LSP4J must
 * know the reply type of a request: for an unknown request it gives the reply
 * as null.
 */
public interface RustAnalyzerServer extends LanguageServer {

	/**
	 * Returns the things that rust-analyzer can run at a position, such as tests.
	 *
	 * @param params a map with "textDocument" (a TextDocumentIdentifier) and
	 *               "position" (a Position)
	 * @return the runnables as a JSON array
	 */
	@JsonRequest("experimental/runnables")
	CompletableFuture<JsonElement> runnables(Map<String, Object> params);
}
