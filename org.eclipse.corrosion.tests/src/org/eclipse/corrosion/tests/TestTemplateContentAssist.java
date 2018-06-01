/*********************************************************************
 * Copyright (c) 2018 Red Hat Inc. and others.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *  Lucas Bullen   (Red Hat Inc.) - Initial implementation
 *******************************************************************************/
package org.eclipse.corrosion.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.eclipse.corrosion.edit.TemplateHelper;
import org.junit.Test;

public class TestTemplateContentAssist {
	@Test
	public void testSingleLineComment() {
		assertTrue(TemplateHelper.isOffestInARustComment("// in a comment"));
		// With prefix
		assertTrue(TemplateHelper.isOffestInARustComment("not a comment // in a comment"));
		// With multiline
		assertTrue(TemplateHelper.isOffestInARustComment("not a comment\nalso not a comment // in a comment"));

		// Fails
		assertFalse(TemplateHelper.isOffestInARustComment("not a comment"));
		// Multiline fail
		assertFalse(TemplateHelper.isOffestInARustComment("// a comment\n not a comment"));
	}
	@Test
	public void testMultiLineComment() {
		assertTrue(TemplateHelper.isOffestInARustComment("/* in a comment"));
		// With prefix
		assertTrue(TemplateHelper.isOffestInARustComment("not a comment /* in a comment"));
		// With multiline
		assertTrue(TemplateHelper.isOffestInARustComment("not a comment \n not a comment /* in \n a comment"));
		// With multiple opens
		assertTrue(TemplateHelper.isOffestInARustComment("not a comment \n not a comment /* in /* a /* comment"));
		// Preceding closure
		assertTrue(TemplateHelper.isOffestInARustComment("/* in a comment */ not a comment /* in a comment"));

		// Fails
		assertFalse(TemplateHelper.isOffestInARustComment("/* in a comment */ not a comment"));
		// Multiline fail
		assertFalse(TemplateHelper.isOffestInARustComment("/* in a comment */ \n not a comment"));
	}

	@Test
	public void testConvertPatternToInsertion() {
		isConversionCorrect("${test}", "${0:test}");
		isConversionCorrect("${test test}", "${0:test test}");
		isConversionCorrect("${test${test}", "${0:test${test}");
		isConversionCorrect("${test", "${test");
		isConversionCorrect("$test}", "$test}");
		isConversionCorrect("$${test}", "$${test}");
		isConversionCorrect("$$$$$${test}", "$$$$$${test}");
		isConversionCorrect("$$$$${test}", "$$$$${0:test}");

		isConversionCorrect("${first}", "${0:first}");
		isConversionCorrect("${}", "${0}");
		isConversionCorrect("${cursor}", "${0}");
		isConversionCorrect("${cursor}${cursor}", "${0}${1}");
		isConversionCorrect("${first}${cursor}${second}${}${first}${second}",
				"${0:first}${1}${2:second}${3}${0:first}${2:second}");
	}

	private void isConversionCorrect(String pattern, String expectedInsertion) {
		assertEquals(expectedInsertion, TemplateHelper.convertPatternToInsertion(pattern));
	}
}
