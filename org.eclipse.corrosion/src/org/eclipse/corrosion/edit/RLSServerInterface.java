/*********************************************************************
 * Copyright (c) 2017 Red Hat Inc. and others.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *  Lucas Bullen (Red Hat Inc.) - Initial implementation
 *******************************************************************************/
package org.eclipse.corrosion.edit;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import org.eclipse.lsp4j.Location;
import org.eclipse.lsp4j.TextDocumentPositionParams;
import org.eclipse.lsp4j.jsonrpc.services.JsonRequest;
import org.eclipse.lsp4j.services.LanguageServer;

public interface RLSServerInterface extends LanguageServer {

	@JsonRequest("rustDocument/implementations")
	CompletableFuture<List<Location>> implementations(TextDocumentPositionParams position);

}
